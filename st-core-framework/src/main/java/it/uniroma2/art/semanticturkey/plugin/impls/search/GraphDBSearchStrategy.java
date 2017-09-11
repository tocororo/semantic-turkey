package it.uniroma2.art.semanticturkey.plugin.impls.search;

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

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.plugin.extpts.SearchStrategy;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.search.ServiceForSearches;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;

public class GraphDBSearchStrategy extends AbstractSearchStrategy implements SearchStrategy {

	protected static Logger logger = LoggerFactory.getLogger(GraphDBSearchStrategy.class);

	// private final static String INDEX_NAME="vocbenchIndex";
	final static private String LUCENEIMPORT = "http://www.ontotext.com/owlim/lucene#";
	final static private String LUCENEINDEX = "http://www.ontotext.com/owlim/lucene#vocbench";
	final static private String LUCENEINDEXLITERAL = "http://www.ontotext.com/owlim/lucene#vocbenchLabel";
	final static private String LUCENEINDEXLOCALNAME = "http://www.ontotext.com/owlim/lucene#vocbenchLocalName";

	@Override
	public void initialize(Project project, RepositoryConnection connection) throws Exception {
		System.out.println("inizio createIndexes"); // da cancellare

		ValidationUtilities.executeWithoutValidation(connection, (RepositoryConnection conn) -> {

			//@formatter:off
			//the index LUCENEINDEXLABEL indexes labels
			String query = "PREFIX luc: <http://www.ontotext.com/owlim/lucene#>"+
					"\nINSERT DATA {"+
					// index just the literals
					"\nluc:index luc:setParam \"literal\" ."+ 
					//to include the literal itself 
					"\nluc:include luc:setParam \"centre\" ."+ 
					//to do no hop 
					"\nluc:moleculeSize luc:setParam \"0\" ."+ 
					"\n}";
			
			//@formatter:on
			logger.debug("query = " + query);
			System.out.println("\n\nINDEXES FOR LABELS = \n\n" + query); // da cancellare
			// execute this query
			Update update = connection.prepareUpdate(query);
			update.execute();
			
			//@formatter:off
			query="PREFIX luc: <"+LUCENEIMPORT+"> "+
				"\nINSERT DATA { " +
				"\n<"+LUCENEINDEXLITERAL+"> luc:createIndex \"true\" . " + 
				"\n}";
			//@formatter:on
	
			logger.debug("query = " + query);
			System.out.println(query); // da cancellare
			// execute this query
			update = connection.prepareUpdate(query);
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
			logger.debug("query = " + query);
			System.out.println("\n\nINDEXES FOR LOCAL NAME = \n\n" + query); // da cancellare
			// execute this query
			update = connection.prepareUpdate(query);
			update.execute();
	
			//@formatter:off
			query="PREFIX luc: <"+LUCENEIMPORT+"> "+
				"\nINSERT DATA { " +
				"\n<"+LUCENEINDEXLOCALNAME+"> luc:createIndex \"true\" . " + 
				"\n}";
			//@formatter:on
	
			logger.debug("query = " + query);
			System.out.println("second INSERT = " + query); // da cancellare
			// execute this query
			update = connection.prepareUpdate(query);
			update.execute();
		});
	}

	@Override
	public void update(Project project, RepositoryConnection connection) throws Exception {
		// it does not work with resources already present in the indexes (it does not consider the new label)
		// this is not a problem, since now the indexes (both of them) use molecules of size 0

		ValidationUtilities.executeWithoutValidation(connection, (RepositoryConnection conn) -> {
			//@formatter:off
			String query = 	"PREFIX luc: <"+LUCENEIMPORT+">" + 
							"\nINSERT DATA { " +
							"\n<"+LUCENEINDEXLITERAL+"> luc:updateIndex _:b1 . " +
							"\n}";
			//@formatter:on
			logger.debug("query = " + query);
			Update update;
			update = connection.prepareUpdate(query);
			update.execute();
	
			//@formatter:off
			query = "PREFIX luc: <"+LUCENEIMPORT+">" + 
					"\nINSERT DATA { " +
					"\n<"+LUCENEINDEXLOCALNAME+"> luc:updateIndex _:b1 . " +
					"\n}";
			//@formatter:on
			logger.debug("query = " + query);
			update = connection.prepareUpdate(query);
			update.execute();
		});
	}

	@Override
	public Collection<AnnotatedValue<Resource>> searchResource(STServiceContext stServiceContext,
			String searchString, String[] rolesArray, boolean useURI, boolean useLocalName, SearchMode searchMode,
			@Optional List<IRI> schemes) throws IllegalStateException, STPropertyAccessException {

		ServiceForSearches serviceForSearches = new ServiceForSearches();
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, stServiceContext.getProject());

		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?type ?show ?scheme"+ 
			"\nWHERE{";
		
		if(useLocalName){
			//the part related to the localName (with the indexes)
			query+="\n{"+
					searchModePrepareQueryWithIndexes("?resource", searchString, searchMode,
							LUCENEINDEXLOCALNAME)+
					"\n}"+
					"\nUNION";
		}
		if(useURI){
			//the part related to the localName (without the indexes)
			query+="\n{"+
					"\n?resource a ?type . " + // otherwise the filter may not be computed
					searchModePrepareQueryNoIndexes("?resource", searchString, searchMode) +
					"\n}"+
					"\nUNION";
		}
		
		//if there is a part related to the localName or the URI, then the part related to the label
		// is inside { and } 
		if(useLocalName || useURI){
			query+="\n{";
		}
		
		//use the indexes to search in the literals, and then get the associated resource
		query+=searchModePrepareQueryWithIndexes("?label", searchString, searchMode, LUCENEINDEXLITERAL);
				
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
		query+=ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), stServiceContext.getProject())+
				"\n}";
		//@formatter:on

		logger.debug("query = " + query);
		System.out.println("query = " + query); // da cancellare

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
		
		if(useLocalName){
			//the part related to the localName (with the indexes)
			query+="\n{"+
					searchModePrepareQueryWithIndexes("?resource", searchString, searchMode,
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
		query+=searchModePrepareQueryWithIndexes("?label", searchString, searchMode, LUCENEINDEXLITERAL);
		
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
					"\nWHERE{"; // +
				//get the candidate resources
				query+=serviceForSearches.filterResourceTypeAndScheme("?resource", "?type", serviceForSearches.isClassWanted(), 
						serviceForSearches.isInstanceWanted(), serviceForSearches.isPropertyWanted(), 
						serviceForSearches.isConceptWanted(), serviceForSearches.isConceptSchemeWanted(), 
						serviceForSearches.isCollectionWanted(), null);
				
				
				//use the lucene indexes to obtain all the resources from the searchString
				query+="\n{"+searchModePrepareQueryWithIndexes("?resource", searchString, searchMode, 
						LUCENEINDEX)+"\n}";
				
				
				if(useURI){
					query+="\nUNION" +
							"\n{" +
							"\n?resource a ?type . " + // otherwise the filter may not be coomputed
							searchModePrepareQueryNoIndexes("?resource", searchString, searchMode) +
							"\n}";
				}
				
				//add the show part according to the Lexicalization Model
				query+=ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), stServiceContext.getProject())+
						"\n}";
		
		logger.debug("query = "+query);
		
		return serviceForSearches.executeInstancesSearchQuery(query, stServiceContext.getRGraphs(), getThreadBoundTransaction(stServiceContext));
	}
	
	
	
	private String searchModePrepareQueryWithIndexes(String variable, String value, SearchMode searchMode, 
			String indexToUse){
		String query ="";
		
		if(searchMode == SearchMode.startsWith){
			query="\n"+variable+" <"+indexToUse+"> '"+value+"*' ."+
				// the GraphDB indexes (Lucene) consider as the start of the string all the sterts of the 
				//single word, so filter them afterward
				"\nFILTER regex(str("+variable+"), '^"+value+"', 'i')";
		} else if(searchMode == SearchMode.endsWith){
			query="\n"+variable+" <"+indexToUse+"> '*"+value+"' ."+
			// the GraphDB indexes (Lucene) consider as the start of the string all the sterts of the 
			//single word, so filter them afterward
			"\nFILTER regex(str("+variable+"), '"+value+"$', 'i')";
		} else if(searchMode == SearchMode.contains){
			query="\n"+variable+" <"+indexToUse+"> '*"+value+"*' .";
		} else { // searchMode.equals(exact)
			query="\n"+variable+" <"+indexToUse+"> '"+value+"' .";
		}
		
		return query;
	}
	
	private String searchModePrepareQueryNoIndexes(String variable, String value, SearchMode searchMode){
		String query ="";
		
		if(searchMode == SearchMode.startsWith){
			query="\nFILTER regex(str("+variable+"), '^"+value+"', 'i')";
		} else if(searchMode == SearchMode.endsWith){
			query="\nFILTER regex(str("+variable+"), '"+value+"$', 'i')";
		} else if(searchMode == SearchMode.contains){
			query="\nFILTER regex(str("+variable+"), '"+value+"', 'i')";
		} else { // searchMode.equals(exact)
			query="\nFILTER regex(str("+variable+"), '^"+value+"$', 'i')";
		}
		
		return query;
	}
}
