package it.uniroma2.art.semanticturkey.extension.impl.search.regex;

import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.data.nature.NatureRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategy;
import it.uniroma2.art.semanticturkey.extension.impl.search.AbstractSearchStrategy;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
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
	public void initialize(RepositoryConnection connection) throws Exception {
		// Nothing to do
	}

	@Override
	public void update(RepositoryConnection connection) throws Exception {
		// Nothing to do
	}

	@Override
	public String searchResource(STServiceContext stServiceContext,
			String searchString, String[] rolesArray, boolean useLocalName, boolean useURI, boolean useNotes,
			SearchMode searchMode, @Optional List<IRI> schemes, @Optional List<String> langs, 
			boolean includeLocales, IRI lexModel, boolean searchInRDFSLabel, 
			boolean searchInSKOSLabel, boolean searchInSKOSXLLabel, boolean searchInOntolex) 
					throws IllegalStateException, STPropertyAccessException {

		ServiceForSearches serviceForSearches = new ServiceForSearches();

		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, false);

		// create the query to be executed for the search
		//@formatter:off
		String query = 
				"SELECT DISTINCT ?resource (GROUP_CONCAT(DISTINCT ?scheme; separator=\",\") AS ?attr_schemes)"+ 
				NatureRecognitionOrchestrator.getNatureSPARQLSelectPart() +
			"\nWHERE{"; // +
		//get the candidate resources
		query+=serviceForSearches.filterResourceTypeAndSchemeAndLexicons("?resource", "?type", schemes, null, 
				null);
		
		//now examine the rdfs:label and/or skos:xlabel/skosxl:label
		//see if the localName and/or URI should be used in the query or not
		query += prepareQueryforResourceUsingSearchString(searchString, searchMode, useLocalName, useURI, 
				useNotes, langs, includeLocales, lexModel, searchInRDFSLabel, searchInSKOSLabel, 
				searchInSKOSXLLabel, searchInOntolex, true);
		
		
		

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
	public String searchLexicalEntry(STServiceContext stServiceContext,
			String searchString, boolean useLocalName, boolean useURI, boolean useNotes, SearchMode searchMode, 
			List<IRI> lexicons, List<String> langs, boolean includeLocales, IRI lexModel, 
			boolean searchInRDFSLabel, boolean searchInSKOSLabel, boolean searchInSKOSXLLabel, 
			boolean searchInOntolex) 
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
		query+=serviceForSearches.filterResourceTypeAndSchemeAndLexicons("?resource", "?type", null, null,
				lexicons);
		
		query+=prepareQueryforResourceUsingSearchString(searchString, searchMode, useLocalName, useURI, 
				useNotes, langs, includeLocales, lexModel, searchInRDFSLabel, searchInSKOSLabel, 
				searchInSKOSXLLabel, searchInOntolex, false);
		
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
			@Optional List<IRI> schemes, @Optional List<String> langs, @Optional IRI cls, boolean includeLocales) 
					throws IllegalStateException, STPropertyAccessException {
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, false);

		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?label"+ 
			"\nWHERE{";
		
		//get the candidate resources
		query+=serviceForSearches.filterResourceTypeAndSchemeAndLexicons("?resource", "?type", schemes, cls, 
				null);
		
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
				"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+">) ?label ."+
				searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
				"\n}" +
		//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." +
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
			@Optional List<IRI> schemes, @Optional IRI cls) throws IllegalStateException, STPropertyAccessException {
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, false);

		//@formatter:off
		String query = "SELECT DISTINCT ?resource "+ 
			"\nWHERE{";
		
		//if the user specify a role, filter the results according to the type
		if(rolesArray!=null && rolesArray.length>0){
			//filter the resource according to its type
			query+= "\n{ SELECT ?resource \nWHERE {\n" +
					serviceForSearches.filterResourceTypeAndSchemeAndLexicons("?resource", "?type", schemes, 
							cls, null) +
				"\n}" +
				"\n}";
		} else {
			//no roles is selected, so add a simple triple, otherwise the FILTER may not work
			query += "\n?resource a ?type .";
		}
		query += searchSpecificModePrepareQuery("?resource", searchString, searchMode, null, null, false)+
				"\n}";
		//@formatter:on

		logger.debug("query = " + query);

		return serviceForSearches.executeGenericSearchQueryForStringList(query, stServiceContext.getRGraphs(),
				getThreadBoundTransaction(stServiceContext));
	}

	@Override
	public String searchInstancesOfClass(STServiceContext stServiceContext,
			List<List<IRI>> clsListList, String searchString, boolean useLocalName, boolean useURI, 
			boolean useNotes, SearchMode searchMode,@Optional List<String> langs, boolean includeLocales,
			boolean searchStringCanBeNull, boolean searchInSubTypes, IRI lexModel, boolean searchInRDFSLabel, 
			boolean searchInSKOSLabel, boolean searchInSKOSXLLabel, boolean searchInOntolex) 
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
			
		//now examine the rdf:label and/or skos:xlabel/skosxl:label
		//see if the localName and/or URI should be used in the query or not
		
		
		if(searchString!=null && searchString.length()>0) {
			query += prepareQueryforResourceUsingSearchString(searchString, searchMode, useLocalName, useURI, 
					useNotes, langs, includeLocales, lexModel, searchInRDFSLabel, searchInSKOSLabel, 
					searchInSKOSXLLabel, searchInOntolex, true);
		}
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

		if (searchMode == SearchMode.startsWith) {
			query = "\nFILTER regex(str(" + variable + "), '^" + value + "', 'i')" +
					"\nBIND('startsWith' AS ?match)";
		} else if (searchMode == SearchMode.endsWith) {
			query = "\nFILTER regex(str(" + variable + "), '" + value + "$', 'i')" +
					"\nBIND('endsWith' AS ?match)";
		} else if (searchMode == SearchMode.contains) {
			query = "\nFILTER regex(str(" + variable + "), '" + value + "', 'i')" +
					"\nBIND('contains' AS ?match)";
		} else if (searchMode == SearchMode.fuzzy) {
			List<String> wordForNoIndex = ServiceForSearches.wordsForFuzzySearch(value, ".");
			String wordForNoIndexAsString = ServiceForSearches.listToStringForQuery(wordForNoIndex, "^", "$");
			query += "\nFILTER regex(str("+variable+"), \""+wordForNoIndexAsString+"\", 'i')" +
					"\nBIND('fuzzy' AS ?match)";
		} else { // searchMode.equals(exact)
			query = "\nFILTER regex(str(" + variable + "), '^" + value + "$', 'i')" +
					"\nBIND('exact' AS ?match)";
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
					query+="regex(lang("+variable+"), '^"+lang+"')";
				} else {
					query+="lang("+variable+")="+"'"+lang+"'";
				}
			}
			query+=")";
		}

		return query;
	}
	
	private String prepareQueryforResourceUsingSearchString(String searchString, SearchMode searchMode, 
			boolean useLocalName, boolean useURI, boolean useNotes, List<String> langs, 
			boolean includeLocales,  IRI lexModel, boolean searchInRDFSLabel, boolean searchInSKOSLabel, 
			boolean searchInSKOSXLLabel, boolean searchInOntolex, boolean includeResToLexicalEntry) {
		String query="";
	
		//check if the request want to search in the local name
		if(useLocalName){
			query+="\n{" +
					"\n?resource a ?type . " + // otherwise the localName is not computed
					"\nBIND(REPLACE(str(?resource), '^.*(#|/)', \"\") AS ?localName)"+
					searchSpecificModePrepareQuery("?localName", searchString, searchMode, null, null, 
							includeLocales) +
					"\n}"+
					"\nUNION";
		}
		
		//check if the request want to search in the complete URI
		if(useURI){
			query+="\n{" +
					"\n?resource a ?type . " + // otherwise the completeURI is not computed
					"\nBIND(str(?resource) AS ?complURI)"+
					searchSpecificModePrepareQuery("?complURI", searchString, searchMode, null, null, includeLocales) +
					"\n}"+
					"\nUNION";
		}
		//check if the request want to search in the notes as well (plain or reified)
		if(useNotes) {
			query+="\n{" +
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
					"\n}"+
					"\nUNION";
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
		
		boolean unionNeeded = false;
		if(lexModel.equals(Project.RDFS_LEXICALIZATION_MODEL) || searchInRDFSLabel) {
			//search in the rdfs:label
			query+="\n{" +
				"\n?resource <"+RDFS.LABEL+"> ?label ." +
				searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
				"\n}";
			unionNeeded = true;
		}
		if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL) || searchInSKOSLabel) {
			//search in skos:prefLabel and skos:altLabel
			if(unionNeeded) {
				query += "\nUNION";
			}
			unionNeeded = true;
			//search in skos:prefLabel and skos:altLabel
			query+="\n{" +
				"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+">) ?label ."+
				searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
				"\n}" ;
		}
		if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || searchInSKOSXLLabel) {
			if(unionNeeded) {
				query += "\nUNION";
			}
			unionNeeded = true;
			//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
			query+="\n{" +
				"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." +
				"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?label ." +
				searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
				"\n}";
		}
		if(lexModel.equals(Project.ONTOLEXLEMON_LEXICALIZATION_MODEL) || searchInOntolex) {
			if(unionNeeded) {
				query += "\nUNION";
			}
			unionNeeded = true;
			//search in dct:title
			query+="\n{" +
				"\n?resource <"+DCTERMS.TITLE+"> ?label ." +
				searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
				"\n}"+	
				
				
				//search in (ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+ONTOLEX.CANONICAL_FORM.stringValue()+"> | <"+ONTOLEX.OTHER_FORM.stringValue()+">) ?ontoForm ." +
				"\n?ontoForm <"+ONTOLEX.WRITTEN_REP.stringValue()+"> ?label ." +
				searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
				"\n}"+
				//search in allResToLexicalEntry/(ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep
				"\nUNION" +
				"\n{" +
				"\n?resource ("+allResToLexicalEntry+")/"+
				"(<"+ONTOLEX.CANONICAL_FORM.stringValue()+"> | <"+ONTOLEX.OTHER_FORM.stringValue()+">) ?ontoForm ." +
				"\n?ontoForm <"+ONTOLEX.WRITTEN_REP.stringValue()+"> ?label ." +
				searchSpecificModePrepareQuery("?label", searchString, searchMode, null, langs, includeLocales) +
				"\n}";
		}
		
		return query;
	}

}
