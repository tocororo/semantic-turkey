package it.uniroma2.art.semanticturkey.plugin.impls.search;

import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.plugin.extpts.SearchStrategy;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.search.ServiceForSearches;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;

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
	public Collection<AnnotatedValue<Resource>> searchResource(STServiceContext stServiceContext,
			String searchString, String[] rolesArray, boolean useLocalName, boolean useURI, SearchMode searchMode,
			@Optional List<IRI> schemes, @Optional List<String> langs, boolean includeLocales) 
					throws IllegalStateException, STPropertyAccessException {

		ServiceForSearches serviceForSearches = new ServiceForSearches();

		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, stServiceContext.getProject());

		// create the query to be executed for the search
		//@formatter:off
		String query = "SELECT DISTINCT ?resource (GROUP_CONCAT(DISTINCT ?scheme; separator=\",\") AS ?attr_schemes)"+ 
			"\nWHERE{"; // +
		//get the candidate resources
		query+=serviceForSearches.filterResourceTypeAndSchemeAndLexicons("?resource", "?type", schemes, null, 
				null);
		
		//now examine the rdfs:label and/or skos:xlabel/skosxl:label
		//see if the localName and/or URI should be used in the query or not
		
		
		//check if the request want to search in the local name
		if(useLocalName){
			query+="\n{" +
					"\n?resource a ?type . " + // otherwise the localName is not computed
					"\nBIND(REPLACE(str(?resource), '^.*(#|/)', \"\") AS ?localName)"+
					searchSpecificModePrepareQuery("?localName", searchString, searchMode, null, null, includeLocales) +
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
		
		//search in the rdfs:label
		query+="\n{" +
				"\n?resource <"+RDFS.LABEL+"> ?rdfsLabel ." +
				searchSpecificModePrepareQuery("?rdfsLabel", searchString, searchMode, null, langs, includeLocales) +
				"\n}"+
		//search in skos:prefLabel and skos:altLabel
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+">) ?skosLabel ."+
				searchSpecificModePrepareQuery("?skosLabel", searchString, searchMode, null, langs, includeLocales) +
				"\n}" +
		//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." +
				"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?literalForm ." +
				searchSpecificModePrepareQuery("?literalForm", searchString, searchMode, null, langs, includeLocales) +
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
				
		//NOT DONE ANYMORE, NOW IT USES THE QUERY BUILDER !!!		
		//add the show part according to the Lexicalization Model
		//		ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), stServiceContext.getProject())+
		//		"\n}";

		query+="\n}"+
				"\nGROUP BY ?resource ";
		//@formatter:on
		

		logger.debug("query = " + query);
		
		QueryBuilder qb;
		qb = new QueryBuilder(stServiceContext, query);
		qb.processRole();
		qb.processRendering();
		return qb.runQuery();
		//return serviceForSearches.executeGenericSearchQuery(query, stServiceContext.getRGraphs(),
		//		getThreadBoundTransaction(stServiceContext));
	}
	
	@Override
	public Collection<AnnotatedValue<Resource>> searchLexicalEntry(STServiceContext stServiceContext,
			String searchString, boolean useLocalName, boolean useURI, SearchMode searchMode, 
			List<IRI> lexicons, List<String> langs, boolean includeLocales) 
					throws IllegalStateException, STPropertyAccessException {
		ServiceForSearches serviceForSearches = new ServiceForSearches();

		//since we are interested just in the LexicalEntry, add this type automatically
		String[] rolesArray = {RDFResourceRole.ontolexLexicalEntry.name()};
		
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, stServiceContext.getProject());

		// create the query to be executed for the search
		//@formatter:off
		String query = "SELECT DISTINCT ?resource (GROUP_CONCAT(DISTINCT ?lexicon; separator=\",\") AS ?attr_lexicons)"+
				"(GROUP_CONCAT(DISTINCT ?index; separator=\",\") AS ?attr_index)"+ 
				"\nWHERE{"; // +
		//get the candidate resources
		query+=serviceForSearches.filterResourceTypeAndSchemeAndLexicons("?resource", "?type", null, null, 
				lexicons);
		
		//now examine the rdfs:label and/or skos:xlabel/skosxl:label
		//see if the localName and/or URI should be used in the query or not
		
		//search in the rdfs:label
		query+="\n{" +
				"\n?resource <"+RDFS.LABEL+"> ?rdfsLabel ." +
				searchSpecificModePrepareQuery("?rdfsLabel", searchString, searchMode, null, langs, includeLocales) +
				"\n}"+
		//search in skos:prefLabel and skos:altLabel
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+">) ?skosLabel ."+
				searchSpecificModePrepareQuery("?skosLabel", searchString, searchMode, null, langs, includeLocales) +
				"\n}" +
		//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." +
				"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?literalForm ." +
				searchSpecificModePrepareQuery("?literalForm", searchString, searchMode, null, langs, includeLocales) +
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
		
		//add the information about the lexicon
		query+="\nOPTIONAL{ ?lexicon <"+LIME.ENTRY.stringValue()+"> ?resource . }";
		
		//NOT DONE ANYMORE, NOW IT USES THE QUERY BUILDER !!!		
		//add the show part according to the Lexicalization Model
		//		ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), stServiceContext.getProject())+
		//		"\n}";

		query+="\n}"+
				"\nGROUP BY ?resource ";
		//@formatter:on
		

		logger.debug("query = " + query);
		
		QueryBuilder qb;
		qb = new QueryBuilder(stServiceContext, query);
		qb.processRole();
		qb.processRendering();
		return qb.runQuery();
		//return serviceForSearches.executeGenericSearchQuery(query, stServiceContext.getRGraphs(),
		//		getThreadBoundTransaction(stServiceContext));
	}

	@Override
	public Collection<String> searchStringList(STServiceContext stServiceContext, String searchString,
			@Optional String[] rolesArray, boolean useLocalName, SearchMode searchMode,
			@Optional List<IRI> schemes, @Optional List<String> langs, @Optional IRI cls, boolean includeLocales) 
					throws IllegalStateException, STPropertyAccessException {
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, stServiceContext.getProject());

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
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, stServiceContext.getProject());

		//@formatter:off
		String query = "SELECT DISTINCT ?resource "+ 
			"\nWHERE{";
		
		//if the user specify a role, filter the results according to the type
		if(rolesArray!=null && rolesArray.length>0){
			//filter the resource according to its type
			query+= "\n{ SELECT ?resource \nWHERE {\n" +
					serviceForSearches.filterResourceTypeAndSchemeAndLexicons("?resource", "?type", schemes, cls,
							null) +
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
	public Collection<AnnotatedValue<Resource>> searchInstancesOfClass(STServiceContext stServiceContext,
			IRI cls, String searchString, boolean useLocalName, boolean useURI, SearchMode searchMode,
			@Optional List<String> langs, boolean includeLocales) throws IllegalStateException, STPropertyAccessException {

		ServiceForSearches serviceForSearches = new ServiceForSearches();

		String[] rolesArray = { RDFResourceRole.individual.name() };
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, stServiceContext.getProject());

		//@formatter:off
		String query = "SELECT DISTINCT ?resource "+ 
			"\nWHERE{" +
			"\n{";
		//do a subquery to get the candidate resources
		query+="\nSELECT DISTINCT ?resource ?type" +
			"\nWHERE{" + 
			"\n ?resource a <"+cls.stringValue()+"> . " +
			"\n ?resource a ?type . " +
			"\n}" +
			"\n}";
			
		//now examine the rdf:label and/or skos:xlabel/skosxl:label
		//see if the localName and/or URI should be used in the query or not
		
		
		//check if the request want to search in the local name
		if(useLocalName){
			query+="\n{" +
					"\nBIND(REPLACE(str(?resource), '^.*(#|/)', \"\") AS ?localName)"+
					searchSpecificModePrepareQuery("?localName", searchString, searchMode, null, null, false) +
					"\n}"+
					"\nUNION";
		}
		
		//check if the request want to search in the complete URI
		if(useURI){
			query+="\n{" +
					"\nBIND(str(?resource) AS ?complURI)"+
					searchSpecificModePrepareQuery("?complURI", searchString, searchMode, null, null, false) +
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
		
		//NOT DONE ANYMORE, NOW IT USES THE QUERY BUILDER !!!
		//add the show part in SPARQL query
		//query+=ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), stServiceContext.getProject());
		//query+="\n}";

		query+="\n}"+
				"\nGROUP BY ?resource ";
		//@formatter:on
		

		logger.debug("query = " + query);
		
		QueryBuilder qb;
		qb = new QueryBuilder(stServiceContext, query);
		qb.processRole();
		qb.processRendering();
		return qb.runQuery();

		//return serviceForSearches.executeInstancesSearchQuery(query, stServiceContext.getRGraphs(),
		//		getThreadBoundTransaction(stServiceContext));
	}

	public String searchSpecificModePrepareQuery(String variable, String value, SearchMode searchMode,
			String indexToUse, List <String> langs, boolean includeLocales) {
		String query = "";

		if (searchMode == SearchMode.startsWith) {
			query = "\nFILTER regex(str(" + variable + "), '^" + value + "', 'i')";
		} else if (searchMode == SearchMode.endsWith) {
			query = "\nFILTER regex(str(" + variable + "), '" + value + "$', 'i')";
		} else if (searchMode == SearchMode.contains) {
			query = "\nFILTER regex(str(" + variable + "), '" + value + "', 'i')";
		} else { // searchMode.equals(contains)
			query = "\nFILTER regex(str(" + variable + "), '^" + value + "$', 'i')";
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

}
