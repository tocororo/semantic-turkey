package it.uniroma2.art.semanticturkey.plugin.impls.search;

import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.plugin.extpts.SearchStrategy;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.search.ServiceForSearches;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
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
	public Collection<AnnotatedValue<Resource>> searchResource(STServiceContext stServiceContext,
			String searchString, String[] rolesArray, boolean useLocalName, boolean useURI, SearchMode searchMode,
			@Optional List<IRI> schemes) throws IllegalStateException, STPropertyAccessException {

		ServiceForSearches serviceForSearches = new ServiceForSearches();

		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, stServiceContext.getProject());

		// create the query to be executed for the search
		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?type ?show ?scheme"+ 
			"\nWHERE{"; // +
		//get the candidate resources
		query+=serviceForSearches.filterResourceTypeAndScheme("?resource", "?type", serviceForSearches.isClassWanted(), 
				serviceForSearches.isInstanceWanted(), serviceForSearches.isPropertyWanted(), 
				serviceForSearches.isConceptWanted(), serviceForSearches.isConceptSchemeWanted(), 
				serviceForSearches.isCollectionWanted(), schemes);
		
		//now examine the rdfs:label and/or skos:xlabel/skosxl:label
		//see if the localName and/or URI should be used in the query or not
		
		
		//check if the request want to search in the local name
		if(useLocalName){
			query+="\n{" +
					"\n?resource a ?type . " + // otherwise the localName is not computed
					"\nBIND(REPLACE(str(?resource), '^.*(#|/)', \"\") AS ?localName)"+
					searchModePrepareQuery("?localName", searchString, searchMode) +
					"\n}"+
					"\nUNION";
		}
		
		//check if the request want to search in the complete URI
		if(useURI){
			query+="\n{" +
					"\n?resource a ?type . " + // otherwise the completeURI is not computed
					"\nBIND(str(?resource) AS ?complURI)"+
					searchModePrepareQuery("?complURI", searchString, searchMode) +
					"\n}"+
					"\nUNION";
		}
		
		//search in the rdfs:label
		query+="\n{" +
				"\n?resource <"+RDFS.LABEL+"> ?rdfsLabel ." +
				searchModePrepareQuery("?rdfsLabel", searchString, searchMode) +
				"\n}"+
		//search in skos:prefLabel and skos:altLabel
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+">) ?skosLabel ."+
				searchModePrepareQuery("?skosLabel", searchString, searchMode) +
				"\n}" +
				//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." +
				"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?literalForm ." +
				searchModePrepareQuery("?literalForm", searchString, searchMode) +
				"\n}"+
		//add the show part according to the Lexicalization Model
				ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), stServiceContext.getProject())+
				"\n}";
		//@formatter:on

		logger.debug("query = " + query);

		return serviceForSearches.executeGenericSearchQuery(query, stServiceContext.getRGraphs(),
				getThreadBoundTransaction(stServiceContext));
	}

	@Override
	public Collection<String> searchStringList(STServiceContext stServiceContext, String searchString,
			@Optional String[] rolesArray, boolean useLocalName, SearchMode searchMode,
			@Optional List<IRI> schemes) throws IllegalStateException, STPropertyAccessException {
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, stServiceContext.getProject());

		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?label"+ 
			"\nWHERE{";
		
		//get the candidate resources
		query+=serviceForSearches.filterResourceTypeAndScheme("?resource", "?type", serviceForSearches.isClassWanted(), 
				serviceForSearches.isInstanceWanted(), serviceForSearches.isPropertyWanted(), 
				serviceForSearches.isConceptWanted(), serviceForSearches.isConceptSchemeWanted(), 
				serviceForSearches.isCollectionWanted(), schemes);
		
		//check if the request want to search in the local name
		if(useLocalName){
			query+="\n{" +
					"\n?resource a ?type . " + // otherwise the localName is not computed
					"\nBIND(REPLACE(str(?resource), '^.*(#|/)', \"\") AS ?localName)"+
					searchModePrepareQuery("?localName", searchString, searchMode) +
					"\n}"+
					"\nUNION";
		}
		
		//if the user specify a role, then get the resource associated to the label, since it will be use 
		// later to filter the results
		if(rolesArray!=null && rolesArray.length>0){
			//search in the rdfs:label
			query+="\n{" +
					"\n?resource <"+RDFS.LABEL+"> ?label ." +
					searchModePrepareQuery("?label", searchString, searchMode) +
					"\n}"+
			//search in skos:prefLabel and skos:altLabel
					"\nUNION" +
					"\n{" +
					"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+">) ?label ."+
					searchModePrepareQuery("?label", searchString, searchMode) +
					"\n}" +
			//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
					"\nUNION" +
					"\n{" +
					"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." +
					"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?label ." +
					searchModePrepareQuery("?label", searchString, searchMode) +
					"\n}";		
		}
		
		query+="\n}";
		//@formatter:on

		logger.debug("query = " + query);

		return serviceForSearches.executeGenericSearchQueryForStringList(query, stServiceContext.getRGraphs(),
				getThreadBoundTransaction(stServiceContext));
	}

	@Override
	public Collection<AnnotatedValue<Resource>> searchInstancesOfClass(STServiceContext stServiceContext,
			IRI cls, String searchString, boolean useLocalName, boolean useURI, SearchMode searchMode,
			@Optional String lang) throws IllegalStateException, STPropertyAccessException {

		ServiceForSearches serviceForSearches = new ServiceForSearches();

		String[] rolesArray = { RDFResourceRole.individual.name() };
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, stServiceContext.getProject());

		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?type ?show"+ 
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
					searchModePrepareQuery("?localName", searchString, searchMode) +
					"\n}"+
					"\nUNION";
		}
		
		//check if the request want to search in the complete URI
		if(useURI){
			query+="\n{" +
					"\nBIND(str(?resource) AS ?complURI)"+
					searchModePrepareQuery("?complURI", searchString, searchMode) +
					"\n}"+
					"\nUNION";
		}
		
		//search in the rdfs:label
		query+="\n{" +
				"\n?resource <"+RDFS.LABEL+"> ?rdfsLabel ." +
				searchModePrepareQuery("?rdfsLabel", searchString, searchMode) +
				"\n}";
		
		//add the show part in SPARQL query
		query+=ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), stServiceContext.getProject());
		
		query+="\n}";
		//@formatter:on

		logger.debug("query = " + query);

		return serviceForSearches.executeInstancesSearchQuery(query, stServiceContext.getRGraphs(),
				getThreadBoundTransaction(stServiceContext));
	}

	private String searchModePrepareQuery(String variable, String value, SearchMode searchMode) {
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

		return query;
	}

}
