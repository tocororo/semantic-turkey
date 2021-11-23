package it.uniroma2.art.semanticturkey.extension.impl.search.regex;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.data.nature.NatureRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategy;
import it.uniroma2.art.semanticturkey.extension.impl.search.AbstractSearchStrategy;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.Pair;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.TripleForSearch;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.search.ServiceForSearches;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;

public class RegexSearchStrategy extends AbstractSearchStrategy implements SearchStrategy {

	protected static Logger logger = LoggerFactory.getLogger(RegexSearchStrategy.class);

	// private static String CLASS_ROLE = "class";
	// private static String CONCEPT_ROLE = "concept";
	// private static String INSTANCE_ROLE = "instance";


	@Override
	public void initialize(RepositoryConnection connection, boolean forceCreation) throws Exception {
		// Nothing to do
	}

	@Override
	public void update(RepositoryConnection connection) throws Exception {
		// Nothing to do
	}

	@Override
	public String searchResource(STServiceContext stServiceContext,
			String searchString, String[] rolesArray, boolean  useLexicalizations, boolean useLocalName,
			boolean useURI, boolean useNotes,
			SearchMode searchMode, @Optional List<IRI> schemes, @Optional(defaultValue="or") String schemeFilter,
			@Optional List<String> langs,
			boolean includeLocales, IRI lexModel, boolean searchInRDFSLabel, 
			boolean searchInSKOSLabel, boolean searchInSKOSXLLabel, boolean searchInOntolex, Map <String, String> prefixToNamespaceMap)
					throws IllegalStateException, STPropertyAccessException {

		ServiceForSearches serviceForSearches = new ServiceForSearches();

		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, false);

		// create the query to be executed for the search
		//@formatter:off
		String query = 
				"SELECT DISTINCT ?resource ?attr_matchMode (GROUP_CONCAT(DISTINCT ?scheme; separator=\",\") AS ?attr_schemes)"+ 
				NatureRecognitionOrchestrator.getNatureSPARQLSelectPart() +
			"\nWHERE{"; // +
		//get the candidate resources
		query+=serviceForSearches.filterResourceTypeAndSchemeAndLexicons("?resource", "?type", schemes,
				schemeFilter, null, null);
		
		//now examine the rdfs:label and/or skos:xlabel/skosxl:label
		//see if the localName and/or URI should be used in the query or not
		query += prepareQueryforResourceUsingSearchString(searchString, searchMode, useLexicalizations, useLocalName, useURI,
				useNotes, langs, includeLocales, lexModel, searchInRDFSLabel, searchInSKOSLabel, 
				searchInSKOSXLLabel, searchInOntolex, true, prefixToNamespaceMap, false);
		
		
		

		//adding the nature in the query (will be replaced by the appropriate processor), 
		//remember to change the SELECT as well
		query+=NatureRecognitionOrchestrator.getNatureSPARQLWherePart("?resource") +
		
				"\n}"+
				"\nGROUP BY ?resource ?attr_matchMode ";
		//@formatter:on

		return query;

		//return serviceForSearches.executeGenericSearchQuery(query, stServiceContext.getRGraphs(),
		//		getThreadBoundTransaction(stServiceContext));
	}
	
	@Override
	public String searchLexicalEntry(STServiceContext stServiceContext,
			String searchString, boolean useLexicalizations,
			boolean useLocalName, boolean useURI, boolean useNotes, SearchMode searchMode,
			List<IRI> lexicons, List<String> langs, boolean includeLocales, IRI lexModel, 
			boolean searchInRDFSLabel, boolean searchInSKOSLabel, boolean searchInSKOSXLLabel, 
			boolean searchInOntolex, Map<String, String> prefixToNamespaceMap)
					throws IllegalStateException, STPropertyAccessException {
		ServiceForSearches serviceForSearches = new ServiceForSearches();

		//since we are interested just in the LexicalEntry, add this type automatically
		String[] rolesArray = {RDFResourceRole.ontolexLexicalEntry.name()};
		
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, false);

		// create the query to be executed for the search
		//@formatter:off
		String query = 
				"SELECT DISTINCT ?resource (GROUP_CONCAT(DISTINCT ?lexicon; separator=\",\") AS ?attr_lexicons)"+
				"(GROUP_CONCAT(DISTINCT ?index; separator=\",\") AS ?attr_index)"+ 
				NatureRecognitionOrchestrator.getNatureSPARQLSelectPart() +
				"\nWHERE{"; // +
		//get the candidate resources
		query+=serviceForSearches.filterResourceTypeAndSchemeAndLexicons("?resource", "?type", null, "", null,
				lexicons);
		
		query+=prepareQueryforResourceUsingSearchString(searchString, searchMode, useLexicalizations,
				useLocalName, useURI,
				useNotes, langs, includeLocales, lexModel, searchInRDFSLabel, searchInSKOSLabel, 
				searchInSKOSXLLabel, searchInOntolex, false, prefixToNamespaceMap, false);
		
		//add the information about the lexicon
		query+="\nOPTIONAL{ ?lexicon <"+LIME.ENTRY.stringValue()+"> ?resource . }";

		//adding the nature in the query (will be replaced by the appropriate processor), 
		//remember to change the SELECT as well
		query+=NatureRecognitionOrchestrator.getNatureSPARQLWherePart("?resource") +
		
				"\n}"+
				"\nGROUP BY ?resource ";
		//@formatter:on
		
		return query;
		
		//return serviceForSearches.executeGenericSearchQuery(query, stServiceContext.getRGraphs(),
		//		getThreadBoundTransaction(stServiceContext));
	}

	@Override
	public Collection<String> searchStringList(STServiceContext stServiceContext, String searchString,
			@Optional String[] rolesArray, boolean useLocalName, SearchMode searchMode,
			@Optional List<IRI> schemes, @Optional(defaultValue="or") String schemeFilter,
			@Optional List<String> langs, @Optional IRI cls, boolean includeLocales)
					throws IllegalStateException, STPropertyAccessException {
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, false);

		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?label"+ 
			"\nWHERE{";
		
		//get the candidate resources
		query+=serviceForSearches.filterResourceTypeAndSchemeAndLexicons("?resource", "?type", schemes,
				schemeFilter, cls, null);
		
		//check if the request want to search in the local name
		if(useLocalName){
			query+="\n{" +
					"\n?resource a ?type . " + // otherwise the localName is not computed
					"\nBIND(REPLACE(str(?resource), '^.*(#|/)', \"\") AS ?localName)"+
					searchSpecificModePrepareQuery("?localName", searchString, searchMode, null, null, includeLocales) +
					"\n}"+
					"\nUNION";
		}
		
		//search in the rdfs:label
		query+="\n{" +
				"\n?resource <"+RDFS.LABEL+"> ?label ." +
				searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
				"\n}"+
		//search in skos:prefLabel and skos:altLabel
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+"> | <"+SKOS.HIDDEN_LABEL.stringValue()+">) ?label ."+
				searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
				"\n}" +
		//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+"> | <"+SKOSXL.HIDDEN_LABEL.stringValue()+">) ?skosxlLabel ." +
				"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?label ." +
				searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
				"\n}"+
		//search in dct:title
				"UNION" +
				"\n{" +
				"\n?resource <"+DCTERMS.TITLE+"> ?label ." +
				searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
				"\n}"+	
		//search in (ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep		
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+ONTOLEX.CANONICAL_FORM.stringValue()+"> | <"+ONTOLEX.OTHER_FORM.stringValue()+">) ?ontoForm ." +
				"\n?ontoForm <"+ONTOLEX.WRITTEN_REP.stringValue()+"> ?label ." +
				searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
				"\n}";	
		
		query+="\n}";
		//@formatter:on

		logger.debug("query = " + query);

		return serviceForSearches.executeGenericSearchQueryForStringList(query, stServiceContext.getRGraphs(),
				getThreadBoundTransaction(stServiceContext));
	}
	
	@Override
	public Collection<String> searchURIList(STServiceContext stServiceContext, String searchString,
			@Optional String[] rolesArray, SearchMode searchMode,
			@Optional List<IRI> schemes, @Optional(defaultValue = "or") String schemeFilter,
			@Optional IRI cls, Map<String, String> prefixToNamespaceMap, int maxNumResults) throws IllegalStateException, STPropertyAccessException {
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, false);

		//check if searchString represents a qname and searchMode is SearchMode.startsWith.
		// In this case, try to expand it via the prefixMap
		if(searchMode.equals(SearchMode.startsWith) ) {
			searchString = ServiceForSearches.getUriStartFromQname(searchString, prefixToNamespaceMap);
		}

		//@formatter:off
		String query = "SELECT DISTINCT ?resource "+ 
			"\nWHERE{";
		
		//if the user specify a role, filter the results according to the type
		if(rolesArray!=null && rolesArray.length>0){
			//filter the resource according to its type
			query+= "\n{ SELECT ?resource \nWHERE {\n" +
					serviceForSearches.filterResourceTypeAndSchemeAndLexicons("?resource", "?type", schemes, 
							schemeFilter, cls, null) +
				"\n}" +
				"\n}";
		} else {
			//no roles is selected, so add a simple triple, otherwise the FILTER may not work
			query += "\n?resource a ?type .";
		}
		query += searchSpecificModePrepareQuery("?resource", searchString, searchMode, null, null, false)+
				"\n}";
		if(maxNumResults>0){
			query+="\nLIMIT "+maxNumResults;
		}
		//@formatter:on

		logger.debug("query = " + query);

		return serviceForSearches.executeGenericSearchQueryForStringList(query, stServiceContext.getRGraphs(),
				getThreadBoundTransaction(stServiceContext));
	}

	@Override
	public String searchInstancesOfClass(STServiceContext stServiceContext,
			List<List<IRI>> clsListList, String searchString, boolean useLexicalizations, boolean useLocalName, boolean useURI,
			boolean useNotes, SearchMode searchMode, @Optional List<String> langs, boolean includeLocales,
			boolean searchStringCanBeNull, boolean searchInSubTypes, IRI lexModel, boolean searchInRDFSLabel,
			boolean searchInSKOSLabel, boolean searchInSKOSXLLabel, boolean searchInOntolex,
			@Nullable List<List<IRI>> schemes, StatusFilter statusFilter,
			@Nullable List<Pair<IRI, List<Value>>> outgoingLinks,
			@Nullable List<TripleForSearch<IRI, String, SearchMode>> outgoingSearch,
			@Nullable List<Pair<IRI, List<Value>>> ingoingLinks, SearchStrategy searchStrategy, String baseURI, Map<String, String> prefixToNamespaceMap)
					throws IllegalStateException, STPropertyAccessException {

		ServiceForSearches serviceForSearches = new ServiceForSearches();

		String[] rolesArray = { RDFResourceRole.individual.name() };
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, searchStringCanBeNull);

		//@formatter:off
		String query = 
				"SELECT DISTINCT ?resource "+ 
				NatureRecognitionOrchestrator.getNatureSPARQLSelectPart() +
			"\nWHERE{" +
			"\n{";
		//do a subquery to get the candidate resources
		query+=ServiceForSearches.getResourceshavingTypes(clsListList, "?resource", searchInSubTypes)+
				"\n}";

		boolean specialCaseXLabel = ServiceForSearches.isSpecialCaseXLabel(clsListList);

		//now examine the rdf:label and/or skos:xlabel/skosxl:label
		//see if the localName and/or URI should be used in the query or not

		if(searchString!=null && searchString.length()>0) {
			query += prepareQueryforResourceUsingSearchString(searchString, searchMode,
					useLexicalizations, useLocalName, useURI,
					useNotes, langs, includeLocales, lexModel, searchInRDFSLabel, searchInSKOSLabel, 
					searchInSKOSXLLabel, searchInOntolex, true, prefixToNamespaceMap, specialCaseXLabel);
		}
		
		
		//the part relative to the schemes
		//the schemes part
		String schemeOrTopConcept="(<"+SKOS.IN_SCHEME.stringValue()+">|<"+SKOS.TOP_CONCEPT_OF+">|"
				+ "^<"+SKOS.HAS_TOP_CONCEPT+">)";
		query += ServiceForSearches.filterWithOrOfAndValues("?resource", schemeOrTopConcept, schemes);
		
		//the part relative to the Status, the outgoingLinks, the outgoingSearch and ingoingLinks
		query += ServiceForSearches.prepareQueryWithStatusOutgoingIngoing(statusFilter, outgoingLinks, 
				outgoingSearch, ingoingLinks, searchStrategy, baseURI, includeLocales);
		
		
		
		//NOT DONE ANYMORE, NOW IT USES THE QUERY BUILDER !!!
		//add the show part in SPARQL query
		//query+=ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), stServiceContext.getProject());
		//query+="\n}";

		//adding the nature in the query (will be replaced by the appropriate processor), 
		//remember to change the SELECT as well
		query+=NatureRecognitionOrchestrator.getNatureSPARQLWherePart("?resource") +
		
				"\n}"+
				"\nGROUP BY ?resource ";
		//@formatter:on
		
		return query;
		
		//return serviceForSearches.executeInstancesSearchQuery(query, stServiceContext.getRGraphs(),
		//		getThreadBoundTransaction(stServiceContext));
	}

	public String searchSpecificModePrepareQuery(String variable, String value, SearchMode searchMode,
			String indexToUse, List <String> langs, boolean includeLocales) {
		return searchSpecificModePrepareQuery(variable, value, searchMode, indexToUse, langs, includeLocales, false);
	}
	
	public String searchSpecificModePrepareQuery(String variable, String value, SearchMode searchMode,
			String indexToUse, List <String> langs, boolean includeLocales, boolean forLocalName) {
		String query = "";
		
		String valueForRegex = ServiceForSearches.escapeStringForRegexInSPARQL(value);

		if (searchMode == SearchMode.startsWith) {
			query = "\nFILTER regex(str(" + variable + "), '^" + valueForRegex + "', 'i')" +
					"\nBIND('startsWith' AS ?attr_matchMode)";
		} else if (searchMode == SearchMode.endsWith) {
			query = "\nFILTER regex(str(" + variable + "), '" + valueForRegex + "$', 'i')" +
					"\nBIND('endsWith' AS ?attr_matchMode)";
		} else if (searchMode == SearchMode.contains) {
			query = "\nFILTER regex(str(" + variable + "), '" + valueForRegex + "', 'i')" +
					"\nBIND('contains' AS ?attr_matchMode)";
		} else if (searchMode == SearchMode.fuzzy) {
			//in this case case, you cannot use directly valueForRegex, since the service
			// will generate a list of values, so use value and let wordsForFuzzySearch clean it
			List<String> wordForNoIndex = ServiceForSearches.wordsForFuzzySearch(value, ".", true,
					false, searchMode);
			String wordForNoIndexAsString = ServiceForSearches.listToStringForQuery(wordForNoIndex, "^", "$");
			query += "\nFILTER regex(str("+variable+"), \""+wordForNoIndexAsString+"\", 'i')" +
					"\nBIND('fuzzy' AS ?attr_matchMode)";
		} else { // searchMode.equals(exact)
			query = "\nFILTER regex(str(" + variable + "), '^" + valueForRegex + "$', 'i')" +
					"\nBIND('exact' AS ?attr_matchMode)";
		}
		
		//if at least one language is specified, then filter the results of the label having such language
		query += ServiceForSearches.prepareLangFilter(langs, variable, includeLocales);

		return query;
	}
	
	private String prepareQueryforResourceUsingSearchString(String searchString, SearchMode searchMode,
			boolean useLexicalizations,
			boolean useLocalName, boolean useURI, boolean useNotes, List<String> langs,
			boolean includeLocales,  IRI lexModel, boolean searchInRDFSLabel, boolean searchInSKOSLabel, 
			boolean searchInSKOSXLLabel, boolean searchInOntolex, boolean includeResToLexicalEntry,
			Map<String, String> prefixToNamespaceMap, boolean specialCaseXLabel) {
		String query="";

		int countUse = 0;
		countUse +=  useLexicalizations ? 1 : 0;
		countUse +=  useLocalName ? 1 : 0;
		countUse +=  useURI ? 1 : 0;
		countUse +=  useNotes ? 1 : 0;


		String conditionalOpenPar = countUse>1 ? "{" : "";
		String conditionalClosePar = countUse>1 ? "}" : "";


		//check if the request want to search in the local name
		if(useLocalName){
			query+="\n" + conditionalOpenPar+
					"\n?resource a ?type2 . " + // otherwise the localName is not computed
					"\nBIND(REPLACE(str(?resource), '^.*(#|/)', \"\") AS ?localName)"+
					searchSpecificModePrepareQuery("?localName", searchString, searchMode, null, null, 
							includeLocales) +
					"\n"+conditionalClosePar;
			if(useURI || useLexicalizations) {
				query+="\nUNION";
			}
		}
		
		//check if the request want to search in the complete URI
		if(useURI){
			String searchStringForUri;
			//check if searchString represents a qname and searchMode is SearchMode.startsWith.
			// In this case, try to expand it via the prefixMap
			if(searchMode.equals(SearchMode.startsWith) ) {
				searchStringForUri = ServiceForSearches.getUriStartFromQname(searchString, prefixToNamespaceMap);
			} else {
				searchStringForUri = searchString;
			}

			query+="\n" + conditionalOpenPar +
					"\n?resource a ?type3 . " + // otherwise the completeURI is not computed
					"\nBIND(str(?resource) AS ?complURI)"+
					searchSpecificModePrepareQuery("?complURI", searchStringForUri, searchMode, null, null, includeLocales) +
					"\n" + conditionalClosePar;
			if(useLexicalizations) {
				query+="\nUNION";
			}
		}
		if(useLexicalizations){
			//check if the request want to search in the notes as well (plain or reified)
			if(useNotes) {
				query+="\n" + conditionalOpenPar +
						"\n{SELECT ?propNote{?propNote <"+RDFS.SUBPROPERTYOF+">* <"+SKOS.NOTE+"> .}}" +
						"\n?resource ?propNote ?label ." +
						searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
						"\n}" +
						"\nUNION" +
						"\n{" +
						"\n{SELECT?propNote {?propNote <"+RDFS.SUBPROPERTYOF+">* <"+SKOS.NOTE+"> .}}" +
						"\n?resource ?propNote ?refNote ." +
						"\n?refNote <"+RDF.VALUE+"> ?label ." +
						searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
						"\n"+ conditionalClosePar +
						"\nUNION";
			}


			boolean unionNeeded = false;

			int countLexMultiSearch = 0;
			countLexMultiSearch += searchInRDFSLabel ? 1 : 0 ;
			countLexMultiSearch += searchInSKOSLabel ? 1 : 0 ;
			countLexMultiSearch += searchInSKOSXLLabel ? 1 : 0 ;
			countLexMultiSearch += searchInOntolex ? 1 : 0 ;

			conditionalOpenPar = (countUse>1 || countLexMultiSearch > 1) || specialCaseXLabel ? "{" : "";
			conditionalClosePar = (countUse>1 || countLexMultiSearch > 1 || specialCaseXLabel) ? "}" : "";

			if(lexModel.equals(Project.RDFS_LEXICALIZATION_MODEL) || searchInRDFSLabel) {
				//search in the rdfs:label
				query+="\n" + conditionalOpenPar +
					"\n?resource <"+RDFS.LABEL+"> ?label ." +
					searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
					"\n"+conditionalClosePar;
				unionNeeded = true;
			}
			if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL) || searchInSKOSLabel) {
				//search in skos:prefLabel and skos:altLabel
				if(unionNeeded) {
					query += "\nUNION";
				}
				unionNeeded = true;
				//search in skos:prefLabel and skos:altLabel
				query+="\n" + conditionalOpenPar +
					"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+"> | <"+SKOS.HIDDEN_LABEL.stringValue()+">) ?label ."+
					searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
					"\n" + conditionalClosePar ;
			}
			if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || searchInSKOSXLLabel) {
				if(unionNeeded) {
					query += "\nUNION";
				}
				unionNeeded = true;
				//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
				query+="\n" + conditionalOpenPar +
					"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+"> | <"+SKOSXL.HIDDEN_LABEL.stringValue()+">) ?skosxlLabel ." +
					"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?label ." +
					searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
					"\n"+conditionalClosePar;
				if (specialCaseXLabel) {
					query+= "\nUNION " +
							"\n{" +
							"\n?resource <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?label . " +
							searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
							"\n}";
				}
			}
			if(lexModel.equals(Project.ONTOLEXLEMON_LEXICALIZATION_MODEL) || searchInOntolex) {
				//construct the complex path from a resource to a LexicalEntry
				String allResToLexicalEntry = getAllPathRestToLexicalEntry();
				if (unionNeeded) {
					query += "\nUNION";
				}
				unionNeeded = true;
				//search in dct:title
				query += "\n{" +
						"\n?resource <" + DCTERMS.TITLE + "> ?label ." +
						searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
						"\n}" +


						//search in (ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep
						"\nUNION" +
						"\n{" +
						"\n?resource (<" + ONTOLEX.CANONICAL_FORM.stringValue() + "> | <" + ONTOLEX.OTHER_FORM.stringValue() + ">) ?ontoForm ." +
						"\n?ontoForm <" + ONTOLEX.WRITTEN_REP.stringValue() + "> ?label ." +
						searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
						"\n}" +
						//search in allResToLexicalEntry/(ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep
						"\nUNION" +
						"\n{" +
						"\n?resource (" + allResToLexicalEntry + ")/" +
						"(<" + ONTOLEX.CANONICAL_FORM.stringValue() + "> | <" + ONTOLEX.OTHER_FORM.stringValue() + ">) ?ontoForm ." +
						"\n?ontoForm <" + ONTOLEX.WRITTEN_REP.stringValue() + "> ?label ." +
						searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
						"\n}";
			}
		}
		
		return query;
	}

}
