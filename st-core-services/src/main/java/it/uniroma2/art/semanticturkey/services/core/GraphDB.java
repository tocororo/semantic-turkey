package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.ServiceForSearches;
import it.uniroma2.art.semanticturkey.user.UsersManager;

@STService
public class GraphDB extends STServiceAdapter {
	
	protected static Logger logger = LoggerFactory.getLogger(GraphDB.class);
	
	//private final static String INDEX_NAME="vocbenchIndex";
	final static private String LUCENEIMPORT = "http://www.ontotext.com/owlim/lucene#";
	final static private String LUCENEINDEX = "http://www.ontotext.com/owlim/lucene#vocbench";
	
	private static String START_SEARCH_MODE = "start";
	private static String CONTAINS_SEARCH_MODE = "contain";
	private static String END_SEARCH_MODE = "end";
	private static String EXACT_SEARCH_MODE = "exact";
	
	@STServiceOperation
	@Write
	//TODO decide the @PreAuthorize
	//#@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'w')")
	public void createIndexes(){
		System.out.println("inizio createIndexes"); //da cancellare
		
		RepositoryConnection repositoryConnection = getManagedConnection();
		
		String query;
		
		//@formatter:off
		/*query = "PREFIX luc: <"+LUCENEIMPORT+">" +
				"\nINSERT DATA {" +
				"\nluc:moleculeSize luc:setParam \"1\" ."+
				"\nluc:languages luc:setParam \"\" . "+
				"\nluc:include luc:setParam \"centre\" . "+
				"\nluc:index luc:setParam \"literals\" . "+
				"\n}";*/
		
		query = "PREFIX luc: <http://www.ontotext.com/owlim/lucene#>"+
				"\nINSERT DATA {"+
				// index just the URIs
				"\nluc:index luc:setParam \"uri\" ."+ 
				//to include the resource itself (for search in its URI) and the literals associated to it
				"\nluc:include luc:setParam \"centre, literal\" ."+ 
				//to hop from the literalForm to the concept (in skosxl)
				"\nluc:moleculeSize luc:setParam \"2\" ."+ 
				//only these properties are considered when building the molecule
				"\nluc:includePredicates luc:setParam \""+
					" "+RDFS.LABEL +
					" "+SKOS.PREF_LABEL +
					" "+SKOS.ALT_LABEL +
					" "+SKOSXL.PREF_LABEL +
					" "+SKOSXL.ALT_LABEL +
					" "+SKOSXL.LITERAL_FORM+"\" ."+
				"\n}";
		
		//@formatter:on
		logger.debug("query = "+query);
		System.out.println("first INSERT = "+query); //da cancellare
		//execute this query
		Update update;
		update = repositoryConnection.prepareUpdate(query);
		update.execute();
		
		
		//@formatter:off
		query="PREFIX luc: <"+LUCENEIMPORT+"> "+
			"\nINSERT DATA { " +
			"\n<"+LUCENEINDEX+"> luc:createIndex \"true\" . " + 
			"\n}";
		//@formatter:on
		
		logger.debug("query = "+query);
		System.out.println("second INSERT = "+query); //da cancellare
		//execute this query
		update = repositoryConnection.prepareUpdate(query);
		update.execute();
	}
	
	
	/*@STServiceOperation
	@Write
	//TODO decide the @PreAuthorize
	//#@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'w')")
	public void updateIndexes(){
		//TODO it does not work with reasources alreded present in the indexes (it does not consider the new label)
		
		//@formatter:off
		String query = 	"PREFIX luc: <"+LUCENEIMPORT+">" + 
						"\nINSERT DATA { " +
						"\n<"+LUCENEINDEX+"> luc:updateIndex _:b1 . " +
						"\n}";
		//@formatter:on
		logger.debug("query = "+query);
		System.out.println("second INSERT = "+query); //da cancellare
		Update update;
		update = getManagedConnection().prepareUpdate(query);
		update.execute();
	}*/
	
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchResource(String searchString, String [] rolesArray, boolean useURI, // boolean useLocalName,
			String searchMode, @Optional List<IRI> schemes) throws IllegalStateException, STPropertyAccessException  {
		
		
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		String searchModeSelected = serviceForSearches.checksPreQuery(searchString, rolesArray, 
				searchMode, getProject());
		
		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?type ?show"+ 
			"\nWHERE{"; // +
		//get the candidate resources
		query+=serviceForSearches.filterResourceTypeAndScheme("?resource", "?type", serviceForSearches.isClassWanted(), 
				serviceForSearches.isInstanceWanted(), serviceForSearches.isPropertyWanted(), 
				serviceForSearches.isConceptWanted(), serviceForSearches.isConceptSchemeWanted(), 
				serviceForSearches.isCollectionWanted(), schemes);
		
		
		//use the lucene indexes to obtain all the resources from the searchString
		query+="\n{"+searchModePrepareQueryWithIndexes("?resource", searchString, searchModeSelected)+"\n}";
		
		
		//check if the request want to search in the local name. The local Name cannot be obtained using the
		// indexes created by Lucene, so do a standard SPARQL query
		//TODO
		
		if(useURI){
			query+="\nUNION" +
					"\n{" +
					"\n?resource a ?type . " + // otherwise the filter may not be coomputed
					searchModePrepareQueryNoIndexes("?resource", searchString, searchModeSelected) +
					"\n}";
		}
		
		//add the show part according to the Lexicalization Model
		query+=ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), getProject())+
				"\n}";
		
		//@formatter:on

		logger.debug("query = "+query);
		System.out.println("query = "+query); // da cancellare
		
		return serviceForSearches.executeGenericSearchQuery(query, getUserNamedGraphs(), getManagedConnection());
	}
	

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(cls, instances)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchInstancesOfClass(IRI cls, String searchString, boolean useLocalName, 
			boolean useURI, String searchMode, @Optional String lang) throws IllegalStateException, STPropertyAccessException {
		
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		
		String []rolesArray = {RDFResourceRole.individual.name()};
		String searchModeSelected = serviceForSearches.checksPreQuery(searchString, rolesArray, 
				searchMode, getProject());
		
		//@formatter:off
				String query = "SELECT DISTINCT ?resource ?type ?show"+ 
					"\nWHERE{"; // +
				//get the candidate resources
				query+=serviceForSearches.filterResourceTypeAndScheme("?resource", "?type", serviceForSearches.isClassWanted(), 
						serviceForSearches.isInstanceWanted(), serviceForSearches.isPropertyWanted(), 
						serviceForSearches.isConceptWanted(), serviceForSearches.isConceptSchemeWanted(), 
						serviceForSearches.isCollectionWanted(), null);
				
				
				//use the lucene indexes to obtain all the resources from the searchString
				query+="\n{"+searchModePrepareQueryWithIndexes("?resource", searchString, searchModeSelected)+"\n}";
				
				
				if(useURI){
					query+="\nUNION" +
							"\n{" +
							"\n?resource a ?type . " + // otherwise the filter may not be coomputed
							searchModePrepareQueryNoIndexes("?resource", searchString, searchModeSelected) +
							"\n}";
				}
				
				//add the show part according to the Lexicalization Model
				query+=ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), getProject())+
						"\n}";
		
		logger.debug("query = "+query);
		
		return serviceForSearches.executeInstancesSearchQuery(query, getUserNamedGraphs(), getManagedConnection());
	}
	
	
	
	private String searchModePrepareQueryWithIndexes(String variable, String value, String searchMode){
		String query ="";
		
		if(searchMode.equals(START_SEARCH_MODE)){
			query="\n"+variable+" <"+LUCENEINDEX+"> '"+value+"*' .";
		} else if(searchMode.equals(END_SEARCH_MODE)){
			query="\n"+variable+" <"+LUCENEINDEX+"> '*"+value+"' .";
		} else if(searchMode.equals(CONTAINS_SEARCH_MODE)){
			query="\n"+variable+" <"+LUCENEINDEX+"> '*"+value+"*' .";
		} else { // searchMode.equals(contains)
			query="\n"+variable+" <"+LUCENEINDEX+"> '"+value+"' .";
		}
		
		return query;
	}
	
	private String searchModePrepareQueryNoIndexes(String variable, String value, String searchMode){
		String query ="";
		
		if(searchMode.equals(START_SEARCH_MODE)){
			query="\nFILTER regex(str("+variable+"), '^"+value+"', 'i')";
		} else if(searchMode.equals(END_SEARCH_MODE)){
			query="\nFILTER regex(str("+variable+"), '"+value+"$', 'i')";
		} else if(searchMode.equals(CONTAINS_SEARCH_MODE)){
			query="\nFILTER regex(str("+variable+"), '"+value+"', 'i')";
		} else { // searchMode.equals(contains)
			query="\nFILTER regex(str("+variable+"), '"+value+"', 'i')";
		}
		
		return query;
	}
}
