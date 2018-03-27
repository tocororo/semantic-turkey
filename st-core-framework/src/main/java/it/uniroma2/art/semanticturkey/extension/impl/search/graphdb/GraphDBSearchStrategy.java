package it.uniroma2.art.semanticturkey.extension.impl.search.graphdb;

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

import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategy;
import it.uniroma2.art.semanticturkey.extension.impl.search.AbstractSearchStrategy;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.search.ServiceForSearches;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;

public class GraphDBSearchStrategy extends AbstractSearchStrategy implements SearchStrategy {

	protected static Logger logger = LoggerFactory.getLogger(GraphDBSearchStrategy.class);

	// private final static String INDEX_NAME="vocbenchIndex";
	final static private String LUCENEIMPORT = "http://www.ontotext.com/owlim/lucene#";
	//final static private String LUCENEINDEX = "http://www.ontotext.com/owlim/lucene#vocbench";
	final static public String LUCENEINDEXLITERAL = "http://www.ontotext.com/owlim/lucene#vocbenchLabel";
	final static public String LUCENEINDEXLOCALNAME = "http://www.ontotext.com/owlim/lucene#vocbenchLocalName";

	@Override
	public void initialize(RepositoryConnection connection) throws Exception {
		
			//@formatter:off
			//the index LUCENEINDEXLABEL indexes labels
			String query = "PREFIX luc: <"+LUCENEIMPORT+">"+
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
			// execute this query
			update = connection.prepareUpdate(query);
			update.execute();
	
			//@formatter:off
			//the index LUCENEINDEXLOCALNAME indexes localNames
			query = "PREFIX luc: <"+LUCENEIMPORT+">"+
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
			// execute this query
			update = connection.prepareUpdate(query);
			update.execute();
	}

	@Override
	public void update(RepositoryConnection connection) throws Exception {
		// it does not work with resources already present in the indexes (it does not consider the new label)
		// this is not a problem, since now the indexes (both of them) use molecules of size 0

			//update the index for labels
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
	
			//update the index for localName
			//@formatter:off
			query = "PREFIX luc: <"+LUCENEIMPORT+">" + 
					"\nINSERT DATA { " +
					"\n<"+LUCENEINDEXLOCALNAME+"> luc:updateIndex _:b1 . " +
					"\n}";
			//@formatter:on
			logger.debug("query = " + query);
			update = connection.prepareUpdate(query);
			update.execute();
	}

	@Override
	public Collection<AnnotatedValue<Resource>> searchResource(STServiceContext stServiceContext,
			String searchString, String[] rolesArray, boolean useLocalName, boolean useURI, SearchMode searchMode,
			@Optional List<IRI> schemes, @Optional List<String> langs, boolean includeLocales) 
					throws IllegalStateException, STPropertyAccessException {

		logger.debug("searchResource in GraphDBSearchStrategy, useURI="+useURI+", useLocalName="+useLocalName);
		
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, stServiceContext.getProject());

		//@formatter:off
		String query = "SELECT DISTINCT ?resource (GROUP_CONCAT(DISTINCT ?scheme; separator=\",\") AS ?attr_schemes)"+ 
			"\nWHERE{";
		
		//prepare the part relative to the ?resource, specifying the searchString, the searchMode, 
		// the useLocalName and useURI
		query+=prepareQueryforResource(searchString, searchMode, useLocalName, useURI, langs, includeLocales);
		
		//filter the resource according to its type
		query+=serviceForSearches.filterResourceTypeAndScheme("?resource", "?type", serviceForSearches.isClassWanted(), 
				serviceForSearches.isInstanceWanted(), serviceForSearches.isPropertyWanted(), 
				serviceForSearches.isConceptWanted(), serviceForSearches.isConceptSchemeWanted(), 
				serviceForSearches.isCollectionWanted(), serviceForSearches.isLexicalEntryWanted(), schemes, null);

		//NOT DONE ANYMORE, NOW IT USES THE QUERY BUILDER !!!
		//add the show part according to the Lexicalization Model
		//query+=ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), stServiceContext.getProject())+
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
			String searchString, SearchMode searchMode, List<IRI> lexicons, List<String> langs,
			boolean includeLocales) throws IllegalStateException, STPropertyAccessException {
		
		logger.debug("searchLexicalEntry in GraphDBSearchStrategy, searchString="+searchString);
		
		//since we are interested just in the LexicalEntry, add this type automatically
		String[] rolesArray = {RDFResourceRole.ontolexLexicalEntry.name()};
		
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, stServiceContext.getProject());

		//@formatter:off
		String query = "SELECT DISTINCT ?resource (GROUP_CONCAT(DISTINCT ?lexicon; separator=\",\") AS ?attr_lexicons)"+ 
			"\nWHERE{";
		
		//prepare the part relative to the ?resource, specifying the searchString, the searchMode, 
		// the useLocalName and useURI
		query+=prepareQueryforResource(searchString, searchMode, false, false, langs, includeLocales);
		
		//filter the resource according to its type
		query+=serviceForSearches.filterResourceTypeAndScheme("?resource", "?type", serviceForSearches.isClassWanted(), 
				serviceForSearches.isInstanceWanted(), serviceForSearches.isPropertyWanted(), 
				serviceForSearches.isConceptWanted(), serviceForSearches.isConceptSchemeWanted(), 
				serviceForSearches.isCollectionWanted(), serviceForSearches.isLexicalEntryWanted(), null, null);

		//add the information about the lexicon
		query+="\nOPTIONAL{ ?lexicon <"+LIME.ENTRY.stringValue()+"> ?resoruce . }";
		
		//NOT DONE ANYMORE, NOW IT USES THE QUERY BUILDER !!!
		//add the show part according to the Lexicalization Model
		//query+=ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), stServiceContext.getProject())+
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
		
		if(useLocalName){
			//the part related to the localName (with the indexes)
			query+="\n{"+
					searchSpecificModePrepareQuery("?resource", searchString, searchMode,
							LUCENEINDEXLOCALNAME, null, false)+
					"\n}"+
					"\nUNION";
		}
		
		//if there is a part related to the localName, then the part related to the label
		// is inside { and } 
		if(useLocalName ){
			query+="\n{";
		}
		
		//use the indexes to search in the literals, and then get the associated resource
		query+=searchSpecificModePrepareQuery("?label", searchString, searchMode, LUCENEINDEXLITERAL, langs,
				includeLocales);
		
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
		if(useLocalName ){
			query+="\n}";
		}
		
		//if the user specify a role, filter the results according to the type
		if(rolesArray!=null && rolesArray.length>0){
			//filter the resource according to its type
			query+=serviceForSearches.filterResourceTypeAndScheme("?resource", "?type", serviceForSearches.isClassWanted(), 
				serviceForSearches.isInstanceWanted(), serviceForSearches.isPropertyWanted(), 
				serviceForSearches.isConceptWanted(), serviceForSearches.isConceptSchemeWanted(), 
				serviceForSearches.isCollectionWanted(), serviceForSearches.isLexicalEntryWanted(), schemes, cls);
		}
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
					serviceForSearches.filterResourceTypeAndScheme("?resource", "?type", serviceForSearches.isClassWanted(), 
				serviceForSearches.isInstanceWanted(), serviceForSearches.isPropertyWanted(), 
				serviceForSearches.isConceptWanted(), serviceForSearches.isConceptSchemeWanted(), 
				serviceForSearches.isCollectionWanted(), serviceForSearches.isLexicalEntryWanted(), schemes, cls) +
				"\n}" +
				"\n}";
		} else {
			//no roles is selected, so add a simple triple, otherwise the FILTER may not work
			query += "\n?resource a ?type .";
		}
		query += searchModePrepareQueryNoIndexes("?resource", searchString, searchMode)+
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
			"\nWHERE{"; // +

		//do a subquery to get the candidate resources
		query+="\nSELECT DISTINCT ?resource ?type" +
			"\nWHERE{" + 
			"\n ?resource a <"+cls.stringValue()+"> . " +
			"\n ?resource a ?type . " +
			"\n}" +
			"\n}";

		//prepare the part relative to the ?resource, specifying the searchString, the searchMode, 
		// the useLocalName and useURI
		query += prepareQueryforResource(searchString, searchMode, useLocalName, useURI, langs, includeLocales);
				
		//NOT DONE ANYMORE, NOW IT USES THE QUERY BUILDER !!!
		//add the show part according to the Lexicalization Model
		//query+=ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), stServiceContext.getProject())+
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
		
		//return serviceForSearches.executeInstancesSearchQuery(query, stServiceContext.getRGraphs(), getThreadBoundTransaction(stServiceContext));
	}
	
	
	
	private String prepareQueryforResource(String searchString, SearchMode searchMode, boolean useLocalName, 
			boolean useURI, List<String> langs, boolean includeLocales) {
		String query="";
		
		
		//prepare an inner query, which seems to be working faster (since it executed by GraphDB before the
		// rest of the query and it uses the Lucene indexes)
		query+="\n{SELECT ?resource ?type "+
				"\nWHERE{";
		
		if(useLocalName){
			//the part related to the localName (with the indexes)
			query+="\n{"+
					searchSpecificModePrepareQuery("?resource", searchString, searchMode,
							LUCENEINDEXLOCALNAME, null, false)+
					"\n}"+
					"\nUNION";
		}
		if(useURI){
			//the part related to the URI. Since the indexes are not able to indexing URI, a standard regex is
			// used
			query+="\n{"+
					"\n?resource a ?type . " + // otherwise the filter may not be computed
					searchModePrepareQueryNoIndexes("?resource", searchString, searchMode) +
					"\n}"+
					"\nUNION";
		}
		
		//if there is a part related to the localName or the URI, then the part related to the label
		// is inside { and } and linked to the previous part with an UNION
		if(useLocalName || useURI){
			query+="\n{";
		}
		
		//use the indexes to search in the literals, and then get the associated resource
		query+=searchSpecificModePrepareQuery("?label", searchString, searchMode, LUCENEINDEXLITERAL, langs, 
				includeLocales);
		
		
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
				"\n}" +
		//search in (ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+ONTOLEX.CANONICAL_FORM.stringValue()+"> | <"+ONTOLEX.OTHER_FORM.stringValue()+">) ?ontoForm ." +
				"\n?ontoForm <"+ONTOLEX.WRITTEN_REP.stringValue()+"> ?label ." +
				"\n}";
		
		if(useLocalName || useURI){
			query+="\n}";
		}
		
		//close the nested query
		query+="\n}"+
			 "\n}";
		
		return query;
		
	}
	
	/**
	 * It uses the lucene indexes
	 * @param variable
	 * @param value
	 * @param searchMode
	 * @param indexToUse
	 * @param langs
	 * @param includeLocales
	 * @return
	 */
	public String searchSpecificModePrepareQuery(String variable, String value, SearchMode searchMode, 
			String indexToUse, List<String> langs, boolean includeLocales){
		String query ="";
		
		if(indexToUse==null || indexToUse.length()==0) {
			//if no lucene index is specified, then assume it is the Index_Literal
			indexToUse = LUCENEINDEXLITERAL;
		}
		
		if(searchMode == SearchMode.startsWith){
			query="\n"+variable+" <"+indexToUse+"> '"+value+"*' ."+
				// the GraphDB indexes (Lucene) consider as the start of the string all the starts of the 
				//single word, so filter them afterward
				"\nFILTER regex(str("+variable+"), '^"+value+"', 'i')";
		} else if(searchMode == SearchMode.endsWith){
			query="\n"+variable+" <"+indexToUse+"> '*"+value+"' ."+
			// the GraphDB indexes (Lucene) consider as the start of the string all the starts of the 
			//single word, so filter them afterward
			"\nFILTER regex(str("+variable+"), '"+value+"$', 'i')";
		} else if(searchMode == SearchMode.contains){
			query="\n"+variable+" <"+indexToUse+"> '*"+value+"*' .";
		} else { // searchMode.equals(exact)
			query="\n"+variable+" <"+indexToUse+"> '"+value+"' .";
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
