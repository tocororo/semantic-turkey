package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.ServiceForSearches;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;

@STService
public class GraphDB extends STServiceAdapter {
	
	protected static Logger logger = LoggerFactory.getLogger(GraphDB.class);
	
	//private final static String INDEX_NAME="vocbenchIndex";
	final static private String LUCENEIMPORT = "http://www.ontotext.com/owlim/lucene#";
	final static private String LUCENEINDEX = "http://www.ontotext.com/owlim/lucene#vocbench";
	final static private String LUCENEINDEXLITERAL = "http://www.ontotext.com/owlim/lucene#vocbenchLabel";
	final static private String LUCENEINDEXLOCALNAME = "http://www.ontotext.com/owlim/lucene#vocbenchLocalName";
	
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
		
		//disable validation, if it is enable
		if(getProject().isValidationEnabled()){
			ValidationUtilities.disableValidationIfEnabled(getProject(), repositoryConnection);
		}
		
		//@formatter:off
		//the index LUCENEINDEXLABEL indexes labels
		query = "PREFIX luc: <http://www.ontotext.com/owlim/lucene#>"+
				"\nINSERT DATA {"+
				// index just the literals
				"\nluc:index luc:setParam \"literal\" ."+ 
				//to include the literal itself 
				"\nluc:include luc:setParam \"centre\" ."+ 
				//to do no hop 
				"\nluc:moleculeSize luc:setParam \"0\" ."+ 
				"\n}";
		
		//@formatter:on
		logger.debug("query = "+query);
		System.out.println("\n\nINDEXES FOR LABELS = \n\n"+query); //da cancellare
		//execute this query
		Update update = repositoryConnection.prepareUpdate(query);
		update.execute();
		
		
		//@formatter:off
		query="PREFIX luc: <"+LUCENEIMPORT+"> "+
			"\nINSERT DATA { " +
			"\n<"+LUCENEINDEXLITERAL+"> luc:createIndex \"true\" . " + 
			"\n}";
		//@formatter:on
		
		logger.debug("query = "+query);
		System.out.println(query); //da cancellare
		//execute this query
		update = repositoryConnection.prepareUpdate(query);
		update.execute();
		
		
		//@formatter:off
		//the index LUCENEINDEX indexes both labels and localNames
		query = "PREFIX luc: <http://www.ontotext.com/owlim/lucene#>"+
				"\nINSERT DATA {"+
				// index just the URIs
				"\nluc:index luc:setParam \"uri\" ."+ 
				//to include the resource itself (for search in its URI) and the literals associated to it
				"\nluc:include luc:setParam \"centre\" ."+ 
				//to hop from the literalForm to the concept (in skosxl)
				"\nluc:moleculeSize luc:setParam \"0\" ."+ 
				"\n}";
		
		//@formatter:on
		logger.debug("query = "+query);
		System.out.println("\n\nINDEXES FOR LOCAL NAME = \n\n"+query); //da cancellare
		//execute this query
		update = repositoryConnection.prepareUpdate(query);
		update.execute();
		
		
		//@formatter:off
		query="PREFIX luc: <"+LUCENEIMPORT+"> "+
			"\nINSERT DATA { " +
			"\n<"+LUCENEINDEXLOCALNAME+"> luc:createIndex \"true\" . " + 
			"\n}";
		//@formatter:on
		
		logger.debug("query = "+query);
		System.out.println("second INSERT = "+query); //da cancellare
		//execute this query
		update = repositoryConnection.prepareUpdate(query);
		update.execute();
	}
	
	
	@STServiceOperation
	@Write
	//TODO decide the @PreAuthorize
	//#@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'w')")
	public void updateIndexes(){
		RepositoryConnection repositoryConnection = getManagedConnection();
		//it does not work with resources already present in the indexes (it does not consider the new label)
		// this is not a problem, since now the indexes (both of them) use molecules of size 0
		
		//disable validation, if it is enable
		if(getProject().isValidationEnabled()){
			ValidationUtilities.disableValidationIfEnabled(getProject(), repositoryConnection);
		}
		
		//@formatter:off
		String query = 	"PREFIX luc: <"+LUCENEIMPORT+">" + 
						"\nINSERT DATA { " +
						"\n<"+LUCENEINDEXLITERAL+"> luc:updateIndex _:b1 . " +
						"\n}";
		//@formatter:on
		logger.debug("query = "+query);
		Update update;
		update = repositoryConnection.prepareUpdate(query);
		update.execute();
		
		//@formatter:off
		query = "PREFIX luc: <"+LUCENEIMPORT+">" + 
				"\nINSERT DATA { " +
				"\n<"+LUCENEINDEXLOCALNAME+"> luc:updateIndex _:b1 . " +
				"\n}";
		//@formatter:on
		logger.debug("query = "+query);
		update = repositoryConnection.prepareUpdate(query);
		update.execute();
	}
	
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchResource(String searchString, String [] rolesArray, boolean useURI, boolean useLocalName,
			String searchMode, @Optional List<IRI> schemes) throws IllegalStateException, STPropertyAccessException  {
		
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		String searchModeSelected = serviceForSearches.checksPreQuery(searchString, rolesArray, 
				searchMode, getProject());
		
		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?type ?show"+ 
			"\nWHERE{";
		
		if(useLocalName){
			//the part related to the localName (with the indexes)
			query+="\n{"+
					searchModePrepareQueryWithIndexes("?resource", searchString, searchModeSelected,
							LUCENEINDEXLOCALNAME)+
					"\n}"+
					"\nUNION";
		}
		if(useURI){
			//the part related to the localName (without the indexes)
			query+="\n{"+
					"\n?resource a ?type . " + // otherwise the filter may not be computed
					searchModePrepareQueryNoIndexes("?resource", searchString, searchModeSelected) +
					"\n}"+
					"\nUNION";
		}
		
		//if there is a part related to the localName or the URI, then the part related to the label
		// is inside { and } 
		if(useLocalName || useURI){
			query+="\n{";
		}
		
		//use the indexes to search in the literals, and then get the associated resource
		query+=searchModePrepareQueryWithIndexes("?label", searchString, searchModeSelected, LUCENEINDEXLITERAL);
				
		//search in the rdfs:label
		query+="\n{" +
				"\n?resource <"+RDFS.LABEL+"> ?label ." +
				"\n}"+
		//search in skos:prefLabel and skos:altLabel
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+">) ?label ."+
				"\n}" +
		//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." +
				"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?label ." +
				"\n}";		
		
		if(useLocalName || useURI){
			query+="\n}";
		}
		
		//filter the resource according to its type
		query+=serviceForSearches.filterResourceTypeAndScheme("?resource", "?type", serviceForSearches.isClassWanted(), 
				serviceForSearches.isInstanceWanted(), serviceForSearches.isPropertyWanted(), 
				serviceForSearches.isConceptWanted(), serviceForSearches.isConceptSchemeWanted(), 
				serviceForSearches.isCollectionWanted(), schemes);

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
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<String> searchStringList(String searchString, @Optional String [] rolesArray,  boolean useLocalName,
			String searchMode, @Optional List<IRI> schemes) throws IllegalStateException, STPropertyAccessException  {
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		String searchModeSelected = serviceForSearches.checksPreQuery(searchString, rolesArray, 
				searchMode, getProject());
		
		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?label"+ 
			"\nWHERE{";
		
		if(useLocalName){
			//the part related to the localName (with the indexes)
			query+="\n{"+
					searchModePrepareQueryWithIndexes("?resource", searchString, searchModeSelected,
							LUCENEINDEXLOCALNAME)+
					"\n}"+
					"\nUNION";
		}
		
		//if there is a part related to the localName or the URI, then the part related to the label
		// is inside { and } 
		if(useLocalName ){
			query+="\n{";
		}
		
		//use the indexes to search in the literals, and then get the associated resource
		query+=searchModePrepareQueryWithIndexes("?label", searchString, searchModeSelected, LUCENEINDEXLITERAL);
		
		//if the user specify a role, then get the resource associated to the label, since it will be use 
		// later to filter the results
		if(rolesArray!=null && rolesArray.length>0){
			//search in the rdfs:label
			query+="\n{" +
					"\n?resource <"+RDFS.LABEL+"> ?label ." +
					"\n}"+
			//search in skos:prefLabel and skos:altLabel
					"\nUNION" +
					"\n{" +
					"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+">) ?label ."+
					"\n}" +
			//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
					"\nUNION" +
					"\n{" +
					"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." +
					"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?label ." +
					"\n}";		
		}
		if(useLocalName ){
			query+="\n}";
		}
		
		//if the user specify a role, filter the results according to the type
		if(rolesArray!=null && rolesArray.length>0){
			//filter the resource according to its type
			query+=serviceForSearches.filterResourceTypeAndScheme("?resource", "?type", serviceForSearches.isClassWanted(), 
				serviceForSearches.isInstanceWanted(), serviceForSearches.isPropertyWanted(), 
				serviceForSearches.isConceptWanted(), serviceForSearches.isConceptSchemeWanted(), 
				serviceForSearches.isCollectionWanted(), schemes);
		}
		query+="\n}";
		//@formatter:on

		logger.debug("query = "+query);
		System.out.println("query = "+query); // da cancellare
		
		return serviceForSearches.executeGenericSearchQueryForStringList(query, getUserNamedGraphs(), getManagedConnection());
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
				query+="\n{"+searchModePrepareQueryWithIndexes("?resource", searchString, searchModeSelected, 
						LUCENEINDEX)+"\n}";
				
				
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
	
	
	
	private String searchModePrepareQueryWithIndexes(String variable, String value, String searchMode, 
			String indexToUse){
		String query ="";
		
		if(searchMode.equals(START_SEARCH_MODE)){
			query="\n"+variable+" <"+indexToUse+"> '"+value+"*' .";
		} else if(searchMode.equals(END_SEARCH_MODE)){
			query="\n"+variable+" <"+indexToUse+"> '*"+value+"' .";
		} else if(searchMode.equals(CONTAINS_SEARCH_MODE)){
			query="\n"+variable+" <"+indexToUse+"> '*"+value+"*' .";
		} else { // searchMode.equals(contains)
			query="\n"+variable+" <"+indexToUse+"> '"+value+"' .";
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
