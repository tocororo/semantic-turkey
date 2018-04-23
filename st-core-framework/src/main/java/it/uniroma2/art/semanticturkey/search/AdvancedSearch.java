package it.uniroma2.art.semanticturkey.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.extension.impl.search.graphdb.GraphDBSearchStrategy;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;

public class AdvancedSearch {
	
	protected static Logger logger = LoggerFactory.getLogger(AdvancedSearch.class);

	public Collection<AnnotatedValue<Resource>> searchResources(String inputText, String[] rolesArray, List<SearchMode> searchModeList,
			SearchScope searchScope, List<String> langs, RepositoryConnection conn, boolean useIndexes, 
			InWhatToSearch inWhatToSearch, WhatToShow whatToShow) throws IllegalStateException{
		List<AnnotatedValue<Resource>> annotateResList = new ArrayList<>();
		
		SearchMode searchModeTemp = null;
		if(searchModeList!=null && searchModeList.size()>0) {
			searchModeTemp = searchModeList.get(0);
		}
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		serviceForSearches.checksPreQuery(inputText, rolesArray, searchModeTemp);
		
		String varResource = "?resource";
		String varType = "?type";
		String varLabel = "?label";
		String varSingleShow = "?singleShow";
		String varShow = "?show";
		
		
		//prepare a query according to the searchMode and searchScope
		String query = "SELECT DISTINCT "+varResource+" "+varType+ " (GROUP_CONCAT(DISTINCT ?singleShow; separator=\",\") AS ?show )" +
				"\nWHERE{";
		
		//get the type 
		query+="\nOPTIONAL{"+varResource+ " a "+varType+" . }";
		
		boolean first = true;
		for(SearchMode searchMode : searchModeList) {
			//do a union from all the different searchMode
			if(!first) {
				query += "\nUNION";
			} else {
				first = false;
			}
			query += searchWithOrWithoutIndexes(varResource, varLabel, inputText, searchMode, null, langs, 
					inWhatToSearch, useIndexes);
		}
		
		//filter the resource according to its type
		query+=serviceForSearches.filterResourceTypeAndSchemeAndLexicons(varResource, varType, null, null, null);
		
		//calculate the show
		query+= calculateShow(varResource, varSingleShow, whatToShow );
		
		query+="\n}" + 
				"\nGROUP BY ?resource ?type";
		
		
		logger.debug("query = " + query);
		//System.out.println("query = " + query); // da cancellare
		
		//TODO for the moment, create a tuple query, but it should be better to use the QueryBuilder
		TupleQuery tupleQuery = conn.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		//iterate over the results 
		while(tupleQueryResult.hasNext()) {
			BindingSet bindingSet = tupleQueryResult.next();
			if(!bindingSet.hasBinding(varResource.substring(1))) {
				//it is a null tuple, so process the next tuple, which should not exist, so the while will
				// end
				continue;
			}
			
			IRI iriRes = (IRI) bindingSet.getBinding(varResource.substring(1)).getValue();
			String show = bindingSet.getBinding(varShow.substring(1)).getValue().stringValue();
			IRI type = (IRI) bindingSet.getBinding(varType.substring(1)).getValue();
			
			AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(iriRes);
			annotatedValue.setAttribute("show", show);
			if(type!=null) {
				annotatedValue.setAttribute("type", type);
			}
			
			annotateResList.add(annotatedValue);
			
		}
		
		return annotateResList;
	}
	
	

	private String searchWithOrWithoutIndexes(String varResource, String varLabel, String inputText,
			SearchMode searchMode, Object object, List<String> langs, InWhatToSearch inWhatToSearch, boolean useIndexes) {
		
		String query="";
		
		//prepare an inner query, which seems to be working faster (since it executed by GraphDB before the
		// rest of the query and it uses the Lucene indexes)
		query+="\n{SELECT ?resource ?type "+
				"\nWHERE{";
		
		if(inWhatToSearch.isSearchInLocalName()){
			//the part related to the localName (with the indexes)
			query+="\n{";
			if(useIndexes) {
				query += searchUsingIndexes(varResource, inputText, searchMode, 
						GraphDBSearchStrategy.LUCENEINDEXLOCALNAME, langs, inWhatToSearch.isSearchIncludingLocales(),
						true);
			} else {
				String varLocalName = "?localName";
				query += "\n"+varResource+" a ?type . " + // otherwise the localName is not computed
						"\nBIND(REPLACE(str("+varResource+"), '^.*(#|/)', \"\") AS "+varLocalName+")"+
				searchUsingNoIndexes(varLocalName, inputText, searchMode, langs, inWhatToSearch.isSearchIncludingLocales());
			}
			query+="\n}"+
					"\nUNION";
		}
		if(inWhatToSearch.isSearchInURI()){
			//the part related to the URI. Since the indexes are not able to indexing URI, a standard regex is
			// used
			query+="\n{"+
					"\n?resource a ?type . " + // otherwise the filter may not be computed
					searchUsingNoIndexes(varResource, inputText, searchMode, langs, 
							inWhatToSearch.isSearchIncludingLocales()) +
					"\n}"+
					"\nUNION";
		}
		
		//if there is a part related to the localName or the URI, then the part related to the label
		// is inside { and } and linked to the previous part with an UNION
		if(inWhatToSearch.isSearchInLocalName() || inWhatToSearch.isSearchInURI()){
			query+="\n{";
		}
		
		//use the indexes to search in the literals, and then get the associated resource
		if(useIndexes) {
			query += searchUsingIndexes(varLabel, inputText, searchMode, 
					GraphDBSearchStrategy.LUCENEINDEXLITERAL, langs, inWhatToSearch.isSearchIncludingLocales(),
					true);
		} else {
			query += searchUsingNoIndexes(varLabel, inputText, searchMode, langs, 
					inWhatToSearch.isSearchIncludingLocales());
		}
		
		
		boolean first = true;
		
		if(!inWhatToSearch.isSearchInRDFLabel() && !inWhatToSearch.isSearchInSkosLabel() && 
				!inWhatToSearch.isSearchInSkosxlLabel() && !inWhatToSearch.isSearchInDCTitle() &&
				!inWhatToSearch.isSearchInWrittenRep()) {
			//since they are all false, set, by default, all to true
			inWhatToSearch.setSearchInRDFLabel(true);
			inWhatToSearch.setSearchInSkosLabel(true);
			inWhatToSearch.setSearchInSkosxlLabel(true);
			inWhatToSearch.setSearchInDCTitle(true);
			inWhatToSearch.setSearchInWrittenRep(true);

		}
		
		if(inWhatToSearch.isSearchInRDFLabel()) {
			//search in the rdfs:label
			query+="\n{" +
					"\n?resource <"+RDFS.LABEL+"> ?label ." +
					"\n}";
			first = false;
		}
		if(inWhatToSearch.isSearchInSkosLabel()) {
			if(!first) {
				query+="\nUNION";
			}
			//search in skos:prefLabel and skos:altLabel
			query+="\n{" +
				"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+">) ?label ."+
				"\n}";
			first = false;
		}
		if(inWhatToSearch.isSearchInSkosxlLabel()) {
			if(!first) {
				query+="\nUNION";
			}
			//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
			query+="\n{" +
				"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." +
				"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?label ." +
				"\n}";
			first = false;
		}
		if(inWhatToSearch.isSearchInDCTitle()) {
			if(!first) {
				query+="\nUNION";
			}
			//search in dct:title
			query+="\n{" +
				"\n?resource <"+DCTERMS.TITLE+"> ?label ." +
				"\n}";
			first = false;
		}
		if(inWhatToSearch.isSearchInWrittenRep()) {
			if(!first) {
				query+="\nUNION";
			}
			//search in (ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep
			query+="\n{" +
				"\n?resource (<"+ONTOLEX.CANONICAL_FORM.stringValue()+"> | <"+ONTOLEX.OTHER_FORM.stringValue()+">) ?ontoForm ." +
				"\n?ontoForm <"+ONTOLEX.WRITTEN_REP.stringValue()+"> ?label ." +
				"\n}";
			first = false;
		}
		
		if(inWhatToSearch.isSearchInLocalName() || inWhatToSearch.isSearchInURI()){
			query+="\n}";
		}
		
		//close the nested query and the outer query
		query+="\n}"+
			 "\n}";
		
		return query;
	}




	private String searchUsingIndexes(String varToUse, String searchTerm, SearchMode searchMode, 
			String indexToUse, List<String> langs, boolean includeLocales, boolean forLocalName){
		String query ="";
		
		if(indexToUse==null || indexToUse.length()==0) {
			//if no lucene index is specified, then assume it is the Index_Literal
			indexToUse = GraphDBSearchStrategy.LUCENEINDEXLITERAL;
		}
		
		if(searchMode == SearchMode.startsWith){
			query="\n"+varToUse+" <"+indexToUse+"> '"+searchTerm+"*' .";
				// the GraphDB indexes (Lucene) consider as the start of the string all the starts of the 
				//single word, so filter them afterward
				if(forLocalName) {
					query+= "\nBIND(REPLACE(str("+varToUse+"), '^.*(#|/)', \"\") AS "+varToUse+"_locName )"+
							"\nFILTER regex(str("+varToUse+"_locName), '^"+searchTerm+"', 'i')";
				} else {
					query+="\nFILTER regex(str("+varToUse+"), '^"+searchTerm+"', 'i')";
				}
		} else if(searchMode == SearchMode.endsWith){
			query="\n"+varToUse+" <"+indexToUse+"> '*"+searchTerm+"' .";
			// the GraphDB indexes (Lucene) consider as the start of the string all the starts of the 
			//single word, so filter them afterward
			if(forLocalName) {
				query+= "\nBIND(REPLACE(str("+varToUse+"), '^.*(#|/)', \"\") AS "+varToUse+"_locName )"+
						"\nFILTER regex(str("+varToUse+"_locName), '"+searchTerm+"$', 'i')";
			} else {
				query+="\nFILTER regex(str("+varToUse+"), '"+searchTerm+"$', 'i')";
			}
		} else if(searchMode == SearchMode.contains){
			query="\n"+varToUse+" <"+indexToUse+"> '*"+searchTerm+"*' .";
		} else if(searchMode == searchMode.fuzzy){
			//change each letter in the input searchTerm with * (INDEX) or . (NO_INDEX) to get all the elements 
			//having just letter different form the input one
			List<String> wordForIndex = wordsForFuzzySearch(searchTerm, "*");
			String wordForIndexAsString = listToStringForQuery(wordForIndex, "", "");
			query+="\n"+varToUse+" <"+indexToUse+"> \""+wordForIndexAsString+"\" .";
			
			List<String> wordForNoIndex = wordsForFuzzySearch(searchTerm, ".");
			String wordForNoIndexAsString = listToStringForQuery(wordForNoIndex, "^", "$");
			query += "\nFILTER regex(str("+varToUse+"), \""+wordForNoIndexAsString+"\", 'i')";
			
		}else { // searchMode.equals(exact)
			query="\n"+varToUse+" <"+indexToUse+"> '"+searchTerm+"' .";
		}
		
		//if at least one language is specified, then filter the results of the label having such language
		if(langs!=null && langs.size()>0) {
			boolean first=true;
			query+="\nFILTER(";
			for(String lang : langs) {
				if(!first) {
					query+=" || ";
				}
				first=false;
				if(includeLocales) {
					query+="regex(lang("+varToUse+"), '^"+lang+"')";
				} else {
					query+="lang("+varToUse+")="+"'"+lang+"'";
				}
			}
			query+=")";
		}
		
		return query;
	}
	
	private String searchUsingNoIndexes(String varToUse, String searchTerm, SearchMode searchMode, List<String> langs, 
			boolean includeLocales){
		String query ="";
		
		if(searchMode == SearchMode.startsWith){
			query="\nFILTER regex(str("+varToUse+"), '^"+searchTerm+"', 'i')";
		} else if(searchMode == SearchMode.endsWith){
			query="\nFILTER regex(str("+varToUse+"), '"+searchTerm+"$', 'i')";
		} else if(searchMode == SearchMode.contains){
			query="\nFILTER regex(str("+varToUse+"), '"+searchTerm+"', 'i')";
		} else if(searchMode == SearchMode.fuzzy){
			List<String> wordForNoIndex = wordsForFuzzySearch(searchTerm, ".");
			String wordForNoIndexAsString = listToStringForQuery(wordForNoIndex, "^", "$");
			query += "\nFILTER regex(str("+varToUse+"), \""+wordForNoIndexAsString+"\", 'i')";
		} else{ // searchMode.equals(exact)
			query="\nFILTER regex(str("+varToUse+"), '^"+searchTerm+"$', 'i')";
		}
		
		//if at least one language is specified, then filter the results of the label having such language
		if(langs!=null && langs.size()>0) {
			boolean first=true;
			query+="\nFILTER(";
			for(String lang : langs) {
				if(!first) {
					query+=" || ";
				}
				first=false;
				if(includeLocales) {
					query+="regex(lang("+varToUse+"), '^"+lang+"')";
				} else {
					query+="lang("+varToUse+")="+"'"+lang+"'";
				}
			}
			query+=")";
		}
		
		return query;
	}
	
	private List<String> wordsForFuzzySearch(String text, String replaceChar){
		List<String> wordsList = new ArrayList<>();
		wordsList.add(replaceChar+text);
		for(int i=0; i<text.length(); ++i) {
			wordsList.add(text.substring(0, i)+replaceChar+text.substring(i+1));
		}
		wordsList.add(text+replaceChar);
		return wordsList;
	}
	
	private String listToStringForQuery(List<String> wordsList, String startSymbol, String endSymbol) {
		String textForQuery="";
		boolean first = true;
		for(int i=0; i<wordsList.size(); ++i) {
			if(!first) {
				textForQuery+="|";
			}
			else {
				first = false;
			}
			textForQuery+="("+startSymbol+wordsList.get(i)+endSymbol+")";
		}
		return textForQuery;
	}
	
	private String calculateShow(String varResource, String varSingleShow, WhatToShow whatToShow) {
		String query="";
		boolean first = true;
		
		String varShow = "?tempLabel";
		
		if(whatToShow.isShowRDFLabel()) {
			query+="\n{" +
					"\n?resource <"+RDFS.LABEL+"> "+varShow+" ." +
					"\n}";
			first = false;
		}
		if(whatToShow.isShowSKOSLabel()) {
			if(!first) {
				query+="\nUNION";
			}
			query+="\n{" +
				"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+">) "+varShow+" ."+
				"\n}";
			first = false;
		}
		if(whatToShow.isShowSKOSXLLabel()) {
			if(!first) {
				query+="\nUNION";
			}
			query+="\n{" +
				"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." +
				"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> "+varShow+" ." +
				"\n}";
			first = false;
		}
		if(whatToShow.isShowDCTitle()) {
			if(!first) {
				query+="\nUNION";
			}
			//search in dct:title
			query+="\n{" +
				"\n?resource <"+DCTERMS.TITLE+"> ?label ." +
				"\n}";
			first = false;
		}
		if(whatToShow.isShowWrittenRep()) {
			if(!first) {
				query+="\nUNION";
			}
			//search in (ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep
			query+="\n{" +
				"\n?resource (<"+ONTOLEX.CANONICAL_FORM.stringValue()+"> | <"+ONTOLEX.OTHER_FORM.stringValue()+">) ?ontoForm ." +
				"\n?ontoForm <"+ONTOLEX.WRITTEN_REP.stringValue()+"> "+varShow+" ." +
				"\n}";
			first = false;
		}
		
		if(whatToShow.getLangs() != null && whatToShow.getLangs().size()!=0) {
			first = true;
			//filter the obtained labels according to the input languages
			query+= "\nFILTER(";
			for(String lang : whatToShow.getLangs()) {
				if(!first) {
					query+= " || ";
				}
				first = false;
				query+="lang("+varShow+")= '"+lang+"'";
			}
			query+=")";
					
		}
		
		//TODO extract the value and language from the varShow and place in varSingleShow
		query+="\nBIND(REPLACE(str("+varShow+"), \"(,)|(@)\", \"\\\\\\\\$0\") as ?labelLexicalForm)"+
			"\nBIND(REPLACE(lang("+varShow+"), \"(,)|(@)\", \"\\\\\\\\$0\") as ?labelLang)"+
			"\nBIND(IF(?labelLang != \"\", CONCAT(STR(?labelLexicalForm), \"(\", ?labelLang, \")\"), "+
				"?labelLexicalForm) AS "+varSingleShow+")";
		
		return query;
	}
	
	public class InWhatToSearch {
		private boolean searchInRDFLabel;
		private boolean searchInSkosLabel;
		private boolean searchInSkosxlLabel;
		private boolean searchInDCTitle;
		private boolean searchInWrittenRep;
		private boolean searchInLocalName;
		private boolean searchInURI;
		private boolean searchIncludingLocales;
		
		public InWhatToSearch() {
			searchInRDFLabel = false;
			searchInSkosLabel = false;
			searchInSkosxlLabel = false;
			searchInDCTitle = false;
			searchInWrittenRep = false;
			searchInLocalName = false;
			searchInURI = false;
			searchIncludingLocales = false;
		}

		public boolean isSearchInRDFLabel() {
			return searchInRDFLabel;
		}

		public void setSearchInRDFLabel(boolean searchInRDFLabel) {
			this.searchInRDFLabel = searchInRDFLabel;
		}

		public boolean isSearchInSkosLabel() {
			return searchInSkosLabel;
		}

		public void setSearchInSkosLabel(boolean searchInSkosLabel) {
			this.searchInSkosLabel = searchInSkosLabel;
		}

		public boolean isSearchInSkosxlLabel() {
			return searchInSkosxlLabel;
		}

		public void setSearchInSkosxlLabel(boolean searchInSkosxlLabel) {
			this.searchInSkosxlLabel = searchInSkosxlLabel;
		}
		
		public boolean isSearchInDCTitle() {
			return searchInDCTitle;
		}
		
		public void setSearchInDCTitle(boolean searchInDCTitle) {
			this.searchInDCTitle = searchInDCTitle;
		}
		
		public boolean isSearchInWrittenRep() {
			return searchInWrittenRep;
		}
		
		public void setSearchInWrittenRep(boolean searchInWrittenRep) {
			this.searchInWrittenRep = searchInWrittenRep;
		}
		
		public boolean isSearchInLocalName() {
			return searchInLocalName;
		}

		public void setSearchInLocalName(boolean searchInLocalName) {
			this.searchInLocalName = searchInLocalName;
		}
		
		public boolean isSearchInURI() {
			return searchInURI;
		}

		public void setSearchInURI(boolean searchInURI) {
			this.searchInURI = searchInURI;
		}
		
		public boolean isSearchIncludingLocales() {
			return searchIncludingLocales;
		}

		public void setSearchIncludingLocales(boolean searchIncludingLocales) {
			this.searchIncludingLocales = searchIncludingLocales;
		}
		
	}
	
	public class WhatToShow {
		private boolean showRDFLabel;
		private boolean showSKOSLabel;
		private boolean showSKOSXLLabel;
		private boolean showDCTitle;
		private boolean showWrittenRep;
		private List<String> langs;
		
		public WhatToShow() {
			showRDFLabel = false;
			showSKOSLabel = false;
			showSKOSXLLabel = false;
			showDCTitle = false;
			showWrittenRep = false;
			langs = new ArrayList<>();
		}

		public boolean isShowRDFLabel() {
			return showRDFLabel;
		}

		public void setShowRDFLabel(boolean showRDFLabel) {
			this.showRDFLabel = showRDFLabel;
		}

		public boolean isShowSKOSLabel() {
			return showSKOSLabel;
		}

		public void setShowSKOSLabel(boolean showSKOSLabel) {
			this.showSKOSLabel = showSKOSLabel;
		}

		public boolean isShowSKOSXLLabel() {
			return showSKOSXLLabel;
		}

		public void setShowSKOSXLLabel(boolean showSKOSXLLabel) {
			this.showSKOSXLLabel = showSKOSXLLabel;
		}

		public boolean isShowDCTitle() {
			return showDCTitle;
		}

		public void setShowDCTitle(boolean showDCTitle) {
			this.showDCTitle = showDCTitle;
		}

		public boolean isShowWrittenRep() {
			return showWrittenRep;
		}

		public void setShowWrittenRep(boolean showWrittenRep) {
			this.showWrittenRep = showWrittenRep;
		}
		
		public void addLang(String lang) {
			if(!langs.contains(lang)) {
				langs.add(lang);
			}
		}
		
		public List<String> getLangs() {
			return langs;
		}
			
	}
	
	
}
