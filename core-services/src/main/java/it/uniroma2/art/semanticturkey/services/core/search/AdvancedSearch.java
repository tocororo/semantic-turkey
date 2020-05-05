package it.uniroma2.art.semanticturkey.services.core.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.extension.impl.search.regex.RegexSearchStrategy;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.search.ServiceForSearches;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

public class AdvancedSearch {
	
	protected static Logger logger = LoggerFactory.getLogger(AdvancedSearch.class);

	//@formatter:off
	//OLD
	/*public Collection<AnnotatedValue<Resource>> searchResources(String inputText, String[] rolesArray, List<SearchMode> searchModeList,
			SearchScope searchScope, List<String> langs, RepositoryConnection conn, 
			InWhatToSearch inWhatToSearch, WhatToShow whatToShow) throws IllegalStateException{
		List<AnnotatedValue<Resource>> annotateResList = new ArrayList<>();
		
		SearchMode searchModeTemp = null;
		if(searchModeList!=null && searchModeList.size()>0) {
			searchModeTemp = searchModeList.get(0);
		}
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		serviceForSearches.checksPreQuery(inputText, rolesArray, searchModeTemp, false);
		
		String varResource = "?resource";
		String varType = "?type";
		String varLabel = "?label";
		String varSingleShow = "?singleShow";
		String varShow = "?show";
		
		
		//prepare a query according to the searchMode and searchScope
		String query = "SELECT DISTINCT "+varResource+" "+varType+ " (GROUP_CONCAT(DISTINCT ?singleShow; separator=\",\") AS ?show )" +
				"\nWHERE{";
		
		//get the type 
		//query+="\nOPTIONAL{"+varResource+ " a "+varType+" . }"; // maybe it should be removed, not useful
		
		boolean first = true;
		for(SearchMode searchMode : searchModeList) {
			//do a union from all the different searchMode
			if(!first) {
				query += "\nUNION";
			} else {
				first = false;
			}
			query += searchWithoutIndexes(varResource, varLabel, inputText, searchMode, langs, 
					inWhatToSearch);
		}
		
		//filter the resource according to its type
		query+=serviceForSearches.filterResourceTypeAndSchemeAndLexicons(varResource, varType, null, null, null);
		
		//calculate the show
		query+= calculateShow(varResource, varSingleShow, whatToShow );
		
		query+="\n}" + 
				"\nGROUP BY ?resource ?type";
		
		
		logger.debug("query = " + query);
		
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
	}*/
	//@formatter:on
	
	
	//@formatter:off
	public List<AnnotatedValue<Resource>> searchResources(Literal label, String[] rolesArray, 
			List<SearchMode> searchModeList, RepositoryConnection conn, IRI targetLexMod, 
			InWhatToSearch inWhatToSearch, WhatToShow whatToShow) {
		List<AnnotatedValue<Resource>> annotateResList = new ArrayList<>();
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		serviceForSearches.checksPreQuery(label.getLabel(), rolesArray, searchModeList.get(0), false);
		
		String varResource = "?resource";
		String varType = "?type";
		String varLabel = "?label";
		String varSingleShow = "?singleShow";
		//String varShow = "?show";
		String varMatch = "?attr_matchMode";
		
		//prepare a query according to the searchMode and searchScope
		String query = "SELECT DISTINCT "+varResource+" "+varType + " " +varSingleShow + " "+ varMatch+
				//" (GROUP_CONCAT(DISTINCT ?singleShow; separator=\",\") AS ?show ) " +
				//"(GROUP_CONCAT(DISTINCT ?match; separator=\",\") AS ?matchType) " +
				"\nWHERE{";
		
		//get the type 
		//query+="\nOPTIONAL{"+varResource+ " a "+varType+" . }"; // maybe it should be removed, not useful
		
		boolean first = true;
		for(SearchMode searchMode : searchModeList) {
			//do a union from all the different searchMode
			if(!first) {
				query += "\nUNION";
			} else {
				first = false;
			}
			List<String> langs = new ArrayList<>();
			langs.add(label.getLanguage().get());
			query += prepareQueryforResourceUsingSearchString(varResource, varLabel, label.getLabel(), searchMode, 
					langs, inWhatToSearch);
		}
		
		//filter the resource according to its type
		query+=serviceForSearches.filterResourceTypeAndSchemeAndLexicons(varResource, varType, null, "or", null, null);
		
		//calculate the show
		query+= calculateShow(varResource, varSingleShow, whatToShow );
		
		query+="\n}"; 
				//"\nGROUP BY ?resource ?type";
		
		
		logger.debug("query = " + query);
		//System.out.println("query3 = "+query); // da cancellare
		
		TupleQuery tupleQuery = conn.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		List<String> resList = new ArrayList<>();
		Map<String, String> resToShow = new HashMap<>();
		Map<String, String> resToMatchMode = new HashMap<>();
		Map<String, IRI> resToIri = new HashMap<>();
		Map<String, IRI> resToType = new HashMap<>();
		while(tupleQueryResult.hasNext()) {
			//since it may be not possible to do a GROUP_CONCAT, a post processing is required (to aggregate
			// ?singleShow and ?match)
			BindingSet bindingSet = tupleQueryResult.next();
			if(!bindingSet.hasBinding(varResource.substring(1))) {
				//it is a null tuple, so process the next tuple, which should not exist, so the while will
				// end
				continue;
			}
			
			IRI iriRes = (IRI) bindingSet.getBinding(varResource.substring(1)).getValue();
			String res = iriRes.stringValue();
			String singleShow = "";
			if(bindingSet.hasBinding(varSingleShow.substring(1))) {
				singleShow  = bindingSet.getBinding(varSingleShow.substring(1)).getValue().stringValue();
			}
			String match = "";
			if(bindingSet.hasBinding(varMatch.substring(1))) {
				match  = bindingSet.getBinding(varMatch.substring(1)).getValue().stringValue();
			}
			IRI type = null;
			if(bindingSet.hasBinding(varType.substring(1))) {
				type = (IRI) bindingSet.getBinding(varType.substring(1)).getValue();
			}
			
			if(!resList.contains(res)) {
				resList.add(res);
			}
			if(!resToIri.containsKey(res)) {
				resToIri.put(res, iriRes);
			}
			if(type != null && !resToType.containsKey(res)) {
				resToType.put(res, type);
			}
			//the show part
			if(singleShow!="" && !resToShow.containsKey(res)) {
				resToShow.put(res, "");
			}
			if(singleShow!="" && !resToShow.get(res).contains(singleShow)) {
				resToShow.put(res, resToShow.get(res)+","+singleShow);
			}
			//the match part
			if(match!="" && !resToMatchMode.containsKey(res)) {
				resToMatchMode.put(res, "");
			}
			if(match!="" && !resToMatchMode.get(res).contains(match)) {
				resToMatchMode.put(res, resToMatchMode.get(res)+","+match);
			}
			
		}
		
		//construct each AnnotatedValue and then add them to the returned list
		for(String res : resList) {
			AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(resToIri.get(res));
			annotatedValue.setAttribute("show", resToShow.get(res).substring(1));
			if(resToType.get(res)!=null) {
				annotatedValue.setAttribute("type", resToType.get(res).stringValue());
			}
			annotatedValue.setAttribute("matchMode", resToMatchMode.get(res).substring(1));
			
			annotateResList.add(annotatedValue);
		}
		return annotateResList;
	}
	//@formatter:on
	

	private String prepareQueryforResourceUsingSearchString(String varResource, String varLabel, String inputText,
			SearchMode searchMode, List<String> langs, InWhatToSearch inWhatToSearch) {
		
		//@formatter:of
		String query="";
		
		//prepare an inner query, which seems to be working faster (since it executed by GraphDB before the
		// rest of the query and it uses the Lucene indexes)
		query+="\n{SELECT ?resource ?type ?attr_matchMode"+
				"\nWHERE{";
		
		//search only in 
		
		/*if(inWhatToSearch.isSearchInLocalName()){
			//the part related to the localName (with the indexes)
			query+="\n{";
			
			String varLocalName = "?localName";
			query += "\n"+varResource+" a ?type . " + // otherwise the localName is not computed
					"\nBIND(REPLACE(str("+varResource+"), '^.*(#|/)', \"\") AS "+varLocalName+")"+
			searchUsingNoIndexes(varLocalName, inputText, searchMode, langs, inWhatToSearch.isSearchIncludingLocales());
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
		}*/
		
		//if there is a part related to the localName or the URI, then the part related to the label
		// is inside { and } and linked to the previous part with an UNION
		/*if(inWhatToSearch.isSearchInLocalName() || inWhatToSearch.isSearchInURI()){
			query+="\n{";
		}*/
		
		
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
		
		//construct the complex path from a resource to a LexicalEntry
		String directResToLexicalEntry = NTriplesUtil.toNTriplesString(ONTOLEX.IS_DENOTED_BY) +
				"|^"+NTriplesUtil.toNTriplesString(ONTOLEX.DENOTES)+
				"|"+NTriplesUtil.toNTriplesString(ONTOLEX.IS_EVOKED_BY)+
				"|^"+NTriplesUtil.toNTriplesString(ONTOLEX.EVOKES);
		String doubleStepResToLexicalEntry = "("+NTriplesUtil.toNTriplesString(ONTOLEX.LEXICALIZED_SENSE) +
				"|^"+NTriplesUtil.toNTriplesString(ONTOLEX.IS_LEXICALIZED_SENSE_OF)+
				"|^"+NTriplesUtil.toNTriplesString(ONTOLEX.REFERENCE)+
				"|"+NTriplesUtil.toNTriplesString(ONTOLEX.IS_REFERENCE_OF)+")"+
				"/(^"+NTriplesUtil.toNTriplesString(ONTOLEX.SENSE)+
				"|"+NTriplesUtil.toNTriplesString(ONTOLEX.IS_SENSE_OF)+")";
		String allResToLexicalEntry = directResToLexicalEntry+"|"+doubleStepResToLexicalEntry;
		
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
				"\n?resource a ?type ." + 
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
					"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?label ." +
					"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." +
					"\n?resource a ?type ." + 
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
				"\n?resource a ?type ." + 
				"\n}";
			first = false;
		}
		if(inWhatToSearch.isSearchInWrittenRep()) {
			if(!first) {
				query+="\nUNION";
			}
			//search in (ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep
			query+="\n{" +
					"\n?ontoForm <"+ONTOLEX.WRITTEN_REP.stringValue()+"> ?label ." +
					"\n?resource (<"+ONTOLEX.CANONICAL_FORM.stringValue()+"> | <"+ONTOLEX.OTHER_FORM.stringValue()+">) ?ontoForm ." +
					"\n?resource a ?type ." + 
					"\n}" +
					//search in allResToLexicalEntry/(ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep
					"\nUNION" +
					"\n{" +
					"\n?resource ("+allResToLexicalEntry+")/"+
					"(<"+ONTOLEX.CANONICAL_FORM.stringValue()+"> | <"+ONTOLEX.OTHER_FORM.stringValue()+">) ?ontoForm ." +
					"\n?ontoForm <"+ONTOLEX.WRITTEN_REP.stringValue()+"> ?label ." +
					"\n?resource a ?type ." + 
					"\n}";
			first = false;
		}
		
		
		query += searchUsingNoIndexes(varLabel, inputText, searchMode, langs, 
					inWhatToSearch.isSearchIncludingLocales());
		
		/*if(inWhatToSearch.isSearchInLocalName() || inWhatToSearch.isSearchInURI()){
			query+="\n}";
		}*/
		
		//close the nested query and the outer query
		query+="\n}"+
			 "\n}";
		//@formatter:on
		
		return query;
	}
	
	public List<Literal> getLabelsFromLangs(STServiceContext stServiceContext, IRI inputRes, IRI lexModel, 
			List<String> langs, RepositoryConnection conn){
		String query = "SELECT ?label"+
				"\nWHERE {";
		
		
		if(lexModel.equals(Project.RDFS_LEXICALIZATION_MODEL)) {
			//search in the rdfs:label
			query+="\n"+NTriplesUtil.toNTriplesString(inputRes)+" <"+RDFS.LABEL+"> ?label .";
		} else if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL)) {
			//search in skos:prefLabel and skos:altLabel
			query+="\n"+NTriplesUtil.toNTriplesString(inputRes)+" (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+">) ?label .";
		} else if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
			//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
			query+="\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?label ." +
					"\n"+NTriplesUtil.toNTriplesString(inputRes)+" (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." ;
		} else if(lexModel.equals(Project.ONTOLEXLEMON_LEXICALIZATION_MODEL)){
			
			//construct the complex path from a resource to a LexicalEntry
			String directResToLexicalEntry = NTriplesUtil.toNTriplesString(ONTOLEX.IS_DENOTED_BY) +
					"|^"+NTriplesUtil.toNTriplesString(ONTOLEX.DENOTES)+
					"|"+NTriplesUtil.toNTriplesString(ONTOLEX.IS_EVOKED_BY)+
					"|^"+NTriplesUtil.toNTriplesString(ONTOLEX.EVOKES);
			String doubleStepResToLexicalEntry = "("+NTriplesUtil.toNTriplesString(ONTOLEX.LEXICALIZED_SENSE) +
					"|^"+NTriplesUtil.toNTriplesString(ONTOLEX.IS_LEXICALIZED_SENSE_OF)+
					"|"+NTriplesUtil.toNTriplesString(ONTOLEX.REFERENCE)+
					"|^"+NTriplesUtil.toNTriplesString(ONTOLEX.IS_REFERENCE_OF)+")"+
					"/(^"+NTriplesUtil.toNTriplesString(ONTOLEX.SENSE)+
					"|"+NTriplesUtil.toNTriplesString(ONTOLEX.IS_SENSE_OF)+")";
			String allResToLexicalEntry = directResToLexicalEntry+"|"+doubleStepResToLexicalEntry;
			
			
			query +="\n{" +
				//search in dct:title
				"\n"+NTriplesUtil.toNTriplesString(inputRes)+" <"+DCTERMS.TITLE+"> ?label ." +
				"\n}"+	
				//search in (ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep
				"\nUNION" +
				"\n{" +
				"\n"+NTriplesUtil.toNTriplesString(inputRes)+" (<"+ONTOLEX.CANONICAL_FORM.stringValue()+"> | <"+ONTOLEX.OTHER_FORM.stringValue()+">) ?ontoForm ." +
				"\n?ontoForm <"+ONTOLEX.WRITTEN_REP.stringValue()+"> ?label ." +
				"\n}" +
				//search in allResToLexicalEntry/(ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep
				"\nUNION" +
				"\n{" +
				"\n"+NTriplesUtil.toNTriplesString(inputRes)+" ("+allResToLexicalEntry+")/"+
				"(<"+ONTOLEX.CANONICAL_FORM.stringValue()+"> | <"+ONTOLEX.OTHER_FORM.stringValue()+">) ?ontoForm ." +
				"\n?ontoForm <"+ONTOLEX.WRITTEN_REP.stringValue()+"> ?label ." +
				"\n}";
		}
		//now filter the labels according to the input languages list (if such list is not null and contains at least 
		// one elemetn)
		if(langs!=null && langs.size()>0) {
			boolean first = true;
			query+="\nFILTER(";
			for(String lang : langs) {
				if(!first) {
					query+=" || ";
				}
				query+=" lang(?label) = '"+lang+"' ";
				first = false;
			}
			query+=")";
		}
		
		
		query+="\n}";
		logger.debug("query: " + query);
		
		//execute the query
		TupleQuery tupleQuery;
		tupleQuery = conn.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		
		//set the dataset to search just in the UserNamedGraphs
		SimpleDataset dataset = new SimpleDataset();
		for(Resource namedGraph : stServiceContext.getRGraphs()){
			if(namedGraph instanceof IRI){
				dataset.addDefaultGraph((IRI) namedGraph);
			}
		}
		tupleQuery.setDataset(dataset);
		
		TupleQueryResult tupleBindingsIterator = tupleQuery.evaluate();
		List<Literal> labelList = new ArrayList<>();
		while(tupleBindingsIterator.hasNext()) {
			BindingSet bindingSet = tupleBindingsIterator.next();
			labelList.add((Literal) bindingSet.getValue("label"));
		}
		
		return labelList;
	}
	
	
	private String searchUsingNoIndexes(String varToUse, String searchTerm, SearchMode searchMode, List<String> langs, 
			boolean includeLocales){
		
		RegexSearchStrategy regexSearchStrategy = new RegexSearchStrategy();
		return regexSearchStrategy.searchSpecificModePrepareQuery(varToUse, searchTerm, searchMode, null, langs, 
				includeLocales);
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
				"\n?resource <"+SKOS.PREF_LABEL.stringValue()+"> "+varShow+" ."+
				"\n}";
			first = false;
		}
		if(whatToShow.isShowSKOSXLLabel()) {
			if(!first) {
				query+="\nUNION";
			}
			query+="\n{" +
				"\n?resource <"+SKOSXL.PREF_LABEL.stringValue()+"> ?skosxlLabel ." +
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
		//private boolean searchInLocalName;
		//private boolean searchInURI;
		private boolean searchIncludingLocales;
		
		public InWhatToSearch() {
			searchInRDFLabel = false;
			searchInSkosLabel = false;
			searchInSkosxlLabel = false;
			searchInDCTitle = false;
			searchInWrittenRep = false;
			//searchInLocalName = false;
			//searchInURI = false;
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
		
		/*public boolean isSearchInLocalName() {
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
		}*/
		
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
