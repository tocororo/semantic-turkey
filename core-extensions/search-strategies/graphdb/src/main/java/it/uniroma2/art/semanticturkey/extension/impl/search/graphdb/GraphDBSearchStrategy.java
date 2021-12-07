package it.uniroma2.art.semanticturkey.extension.impl.search.graphdb;

import java.util.ArrayList;
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
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
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

public class GraphDBSearchStrategy extends AbstractSearchStrategy implements SearchStrategy {

	protected static Logger logger = LoggerFactory.getLogger(GraphDBSearchStrategy.class);


	// OLD "Lucene full-text search" https://graphdb.ontotext.com/documentation/9.6/standard/full-text-search.html
	//private final static String INDEX_NAME="vocbenchIndex";
	final static private String LUCENEIMPORT = "http://www.ontotext.com/owlim/lucene#";
	final static public String LUCENEINDEX_LITERAL = "http://www.ontotext.com/owlim/lucene#vocbenchLabel";
	final static public String LUCENEINDEX_LOCALNAME = "http://www.ontotext.com/owlim/lucene#vocbenchLocalName";


	// "Lucene GraphDB connector" https://graphdb.ontotext.com/documentation/standard/lucene-graphdb-connector.html
	//final static public String LUCENE_INDEX_LITERAL = "<http://www.ontotext.com/connectors/lucene/instance#labelConnectorLiteral>";
	//final static public String LUCENE_INDEX_URI = "<http://www.ontotext.com/connectors/lucene/instance#resourceConnectorIRI>";
	//final private String LUC_DROPCONNECTOR = "<http://www.ontotext.com/connectors/lucene#dropConnector>";
	//final private String LUC_CREATECONNECTOR = "<http://www.ontotext.com/connectors/lucene#createConnector>";
	//final private String LUC_ENTITIES = "<http://www.ontotext.com/connectors/lucene#entities>";
	//final private String LUC_QUERY = "<http://www.ontotext.com/connectors/lucene#query>";
	//final private String LUC_CONNECTORSTATUS = "<http://www.ontotext.com/connectors/lucene#connectorStatus>";

	//final static public String PREFIX_LUC = "PREFIX luc: <http://www.ontotext.com/connectors/lucene#>"; // luc
	//final static public String PREFIX_LUC_INDEX = "PREFIX luc-index: <http://www.ontotext.com/connectors/lucene/instance#>"; // luc-index



	@Override
	public void initialize(RepositoryConnection connection, boolean forceCreation) throws Exception {

		// the forceCreation is used only when Lucene Connector part is used

		String query;
		Update update;
		TupleQuery tupleQuery;
		TupleQueryResult tupleQueryResult;

		// The following part is using the Lucene Connector (currently in commented since the Lucene FTS is used)
		/*
		boolean existingLiteralIndex = false;
		boolean existingUriIndex = false;

		//first, drop the two connector (since, if they exist, trying to crete them will generate an
		// exception if you try to create them again)

		// drop the LUCENE_INDEX_LITERAL if it exists (useful if the method is called a second time to fix the index)
		//@formatter:off
		query = "SELECT ?cntStatus " +
				"\nWHERE {" +
				"\n"+LUCENE_INDEX_LITERAL +" "+LUC_CONNECTORSTATUS+" ?cntStatus ." +
				"\n}";
		//@formatter:on
		logger.debug("query = " + query);
		tupleQuery = connection.prepareTupleQuery(query);
		tupleQueryResult = tupleQuery.evaluate();
		if(tupleQueryResult.hasNext()) {
			existingLiteralIndex = true;
			if(forceCreation) {
				//the LUCENE_INDEX_LITERAL exists, so drop it
				//@formatter:off
				query = "INSERT DATA {" +
						"\n" + LUCENE_INDEX_LITERAL + " " + LUC_DROPCONNECTOR + " [] ." +
						"\n}";
				//@formatter:on

				logger.debug("query = " + query);
				update = connection.prepareUpdate(query);
				update.execute();
			}
		}



		// drop the LUCENE_INDEX_URI if it exists
		//@formatter:off
		query = "SELECT ?cntStatus " +
				"\nWHERE {" +
				"\n"+LUCENE_INDEX_URI +" "+LUC_CONNECTORSTATUS+" ?cntStatus ." +
				"\n}";
		//@formatter:on
		logger.debug("query = " + query);
		tupleQuery = connection.prepareTupleQuery(query);
		tupleQueryResult = tupleQuery.evaluate();
		if(tupleQueryResult.hasNext()) {
			existingUriIndex = true;
			if (forceCreation) {
				//@formatter:off
				query = "INSERT DATA {" +
						"\n"+LUCENE_INDEX_URI+" "+LUC_DROPCONNECTOR+" [] ." +
						"\n}";
				//@formatter:on

				logger.debug("query = " + query);
				update = connection.prepareUpdate(query);
				update.execute();
			}
		}


		if (forceCreation || !existingLiteralIndex) {
			//@formatter:off
			//the index LUCENE_INDEX_LITERAL indexes Literals
			query = "INSERT DATA {" +
					"\n" + LUCENE_INDEX_LITERAL + " " + LUC_CREATECONNECTOR + " '''" +
					"\n{" +
					"\n  \"fields\": [" +
					"\n    {" +
					"\n      \"fieldName\": \"label\"," +
					"\n      \"propertyChain\": [" +
					"\n        \"$literal\"" +
					"\n      ]," +
					"\n    }" +
					"\n  ]," +
					"\n  \"types\": [" +
					"\n    \"$untyped\"" +
					"\n  ]," +
					"\n}" +
					"\n''' ." +
					"\n} ";
			//@formatter:on

			logger.debug("query = " + query);
			// execute this query
			update = connection.prepareUpdate(query);
			update.execute();
		}

		if (forceCreation || !existingUriIndex) {
			//@formatter:off
			//the index LUCENE_INDEX_URI indexes localNames
			query = "INSERT DATA {\n" +
					"\n" + LUCENE_INDEX_URI + " " + LUC_CREATECONNECTOR + " '''\n" +
					"\n{ " +
					"\n  \"fields\": [" +
					"\n    {" +
					"\n      \"fieldName\": \"iri\"," +
					"\n      \"propertyChain\": [" +
					"\n        \"$self\"" +
					"\n      ]," +
					"\n    }" +
					"\n  ]," +
					"\n  \"types\": [" +
					"\n    \"$any\"" +
					"\n  ]," +
					"\n}" +
					"\n''' ." +
					"\n} ";
			//@formatter:on

			logger.debug("query = " + query);
			// execute this query
			update = connection.prepareUpdate(query);
			update.execute();
		}
		*/

		// The Lucene FTS part
		//@formatter:off
		//the index LUCENEINDEXLABEL indexes labels
		query = "PREFIX luc: <"+LUCENEIMPORT+">"+
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
		update = connection.prepareUpdate(query);
		update.execute();

		//@formatter:off
		query="PREFIX luc: <"+LUCENEIMPORT+"> "+
			"\nINSERT DATA { " +
			"\n<"+ LUCENEINDEX_LITERAL +"> luc:createIndex \"true\" . " +
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
			"\n<"+ LUCENEINDEX_LOCALNAME +"> luc:createIndex \"true\" . " +
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
						"\n<"+ LUCENEINDEX_LITERAL +"> luc:updateIndex _:b1 . " +
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
				"\n<"+ LUCENEINDEX_LOCALNAME +"> luc:updateIndex _:b1 . " +
				"\n}";
		//@formatter:on
		logger.debug("query = " + query);
		update = connection.prepareUpdate(query);
		update.execute();
	}

	@Override
	public String searchResource(STServiceContext stServiceContext,
			String searchString, String[] rolesArray, boolean useLexicalizations, boolean useLocalName, boolean useURI, boolean useNotes,
			SearchMode searchMode, @Optional List<IRI> schemes, String schemeFilter, @Optional List<String> langs,
			boolean includeLocales, IRI lexModel, boolean searchInRDFSLabel,
			boolean searchInSKOSLabel, boolean searchInSKOSXLLabel, boolean searchInOntolex, Map<String, String> prefixToNamespaceMap)
					throws IllegalStateException, STPropertyAccessException {

		logger.debug("searchResource in GraphDBSearchStrategy, searchString="+searchString +", "
				+ "useLexicalizations="+useLexicalizations+", useNotes="+useNotes+", "
				+ "useURI="+useURI+", useLocalName="+useLocalName);
		
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, false);

		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?attr_matchMode (GROUP_CONCAT(DISTINCT ?scheme; separator=\",\") AS ?attr_schemes)"+ 
				NatureRecognitionOrchestrator.getNatureSPARQLSelectPart() +
			"\nWHERE{";
		
		//prepare the part relative to the ?resource, specifying the searchString, the searchMode, useLexicalizations.
		// the useLocalName, useURI and useNotes
		query+=prepareQueryforResourceUsingSearchString(searchString, searchMode, useLexicalizations, useLocalName, useURI,
				useNotes, langs, includeLocales, lexModel, searchInRDFSLabel, searchInSKOSLabel, 
				searchInSKOSXLLabel, searchInOntolex, true, prefixToNamespaceMap, false);
		//filter the resource according to its type
		query+=serviceForSearches.filterResourceTypeAndSchemeAndLexicons("?resource", "?type", schemes, schemeFilter, null,
				null);

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
			String searchString, boolean useLexicalizations, boolean useLocalName,
			boolean useURI, boolean useNotes, SearchMode searchMode,
			List<IRI> lexicons, List<String> langs, boolean includeLocales, IRI lexModel,
			boolean searchInRDFSLabel, boolean searchInSKOSLabel, boolean searchInSKOSXLLabel,
			boolean searchInOntolex, Map<String, String> prefixToNamespaceMap)
					throws IllegalStateException, STPropertyAccessException {
		
		logger.debug("searchLexicalEntry in GraphDBSearchStrategy, searchString="+searchString+", "
				+ "useURI="+useURI+", useLocalName="+useLocalName);
		
		//since we are interested just in the LexicalEntry, add this type automatically
		String[] rolesArray = {RDFResourceRole.ontolexLexicalEntry.name()};
		
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, false);

		//@formatter:off
		String query = "SELECT DISTINCT ?resource (GROUP_CONCAT(DISTINCT ?lexicon; separator=\",\") AS ?attr_lexicons)"+
				"(GROUP_CONCAT(DISTINCT ?index; separator=\",\") AS ?attr_index)"+
				NatureRecognitionOrchestrator.getNatureSPARQLSelectPart() +
				"\nWHERE{";
		
		//prepare the part relative to the ?resource, specifying the searchString, the searchMode, 
		// the useLocalName and useURI
		query+=prepareQueryforResourceUsingSearchString(searchString, searchMode, useLexicalizations,
				useLocalName, useURI,
				useNotes, langs, includeLocales, lexModel, searchInRDFSLabel, searchInSKOSLabel, 
				searchInSKOSXLLabel, searchInOntolex, false, prefixToNamespaceMap, false);
		//filter the resource according to its type
		query+=serviceForSearches.filterResourceTypeAndSchemeAndLexicons("?resource", "?type", null, "or", null,
				lexicons);

		//add the information about the lexicon
		query+="\nOPTIONAL{ ?lexicon <"+LIME.ENTRY.stringValue()+"> ?resource . }";
		

		//adding the nature in the query (will be replaced by the appropriate processor), 
		//remember to change the SELECT as well
		query+=NatureRecognitionOrchestrator.getNatureSPARQLWherePart("?resource") +
		
				"\n}"+
				"\nGROUP BY ?resource ";
		//@formatter:on
		
		return query;
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
		String query =
				"SELECT DISTINCT ?resource ?label"+
				"\nWHERE{";
		
		if(useLocalName){
			//the part related to the localName (with the indexes)
			query+="\n{"+
					searchSpecificModePrepareQuery("?resource", searchString, searchMode,
							LUCENEINDEX_LOCALNAME, null, false, true)+
					"\n}"+
					"\nUNION";
		}
		
		//if there is a part related to the localName, then the part related to the label
		// is inside { and } 
		if(useLocalName ){
			query+="\n{";
		}
		
		//use the indexes to search in the literals, and then get the associated resource
		query+=searchSpecificModePrepareQuery("?label", searchString, searchMode, LUCENEINDEX_LITERAL, langs,
				includeLocales, false);
		
		//search in the rdfs:label
		query+="\n{" +
				"\n?resource <"+RDFS.LABEL+"> ?label ." +
				"\n}"+
		//search in skos:prefLabel and skos:altLabel
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+"> | <"+SKOS.HIDDEN_LABEL.stringValue()+">) ?label ."+
				"\n}" +
		//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+"> | <"+SKOSXL.HIDDEN_LABEL.stringValue()+">) ?skosxlLabel ." +
				"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?label ." +
				"\n}" +
		//search in dct:title
				"UNION" +
				"\n{" +
				"\n?resource <"+DCTERMS.TITLE+"> ?label ." +
				"\n}"+	
		//search in (ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+ONTOLEX.CANONICAL_FORM.stringValue()+"> | <"+ONTOLEX.OTHER_FORM.stringValue()+">) ?ontoForm ." +
				"\n?ontoForm <"+ONTOLEX.WRITTEN_REP.stringValue()+"> ?label ." +
				"\n}";
		if(useLocalName ){
			query+="\n}";
		}
		
		//if the user specify a role, filter the results according to the type
		if(rolesArray!=null && rolesArray.length>0){
			//filter the resource according to its type
			query+=serviceForSearches.filterResourceTypeAndSchemeAndLexicons("?resource", "?type", schemes,
					schemeFilter, cls, null);
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
			@Optional List<IRI> schemes, @Optional(defaultValue = "or") String schemeFilter,
			@Optional IRI cls, Map<String, String> prefixToNamespaceMap, int maxNumResults) throws IllegalStateException, STPropertyAccessException {
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
							schemeFilter, cls, null) +
				"\n}" +
				"\n}";
		} else {
			//no roles is selected, so add a simple triple, otherwise the FILTER may not work
			query += "\n?resource a ?type .";
		}
		//query += searchModePrepareQueryNoIndexes_UsedForURISearch("?resource", searchString, searchMode)+
		query += searchModePrepareQueryNoIndexes_UsedForURISearch("?resource", searchString, searchMode,
				prefixToNamespaceMap)+
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
			List<List<IRI>> clsListList, String searchString, boolean  useLexicalizations, boolean useLocalName, boolean useURI,
			boolean useNotes, SearchMode searchMode, @Optional List<String> langs, boolean includeLocales,
			boolean searchStringCanBeNull, boolean searchInSubTypes, IRI lexModel, boolean searchInRDFSLabel,
			boolean searchInSKOSLabel, boolean searchInSKOSXLLabel, boolean searchInOntolex,
			@Nullable List<List<IRI>> schemes, StatusFilter statusFilter,
			@Nullable List<Pair<IRI, List<Value>>> outgoingLinks,
			@Nullable List<TripleForSearch<IRI, String, SearchMode>> outgoingSearch,
			@Nullable List<Pair<IRI, List<Value>>> ingoingLinks, SearchStrategy searchStrategy, String baseURI,
			Map<String, String> prefixToNamespaceMap)
					throws IllegalStateException, STPropertyAccessException {

		ServiceForSearches serviceForSearches = new ServiceForSearches();

		String[] rolesArray = { RDFResourceRole.individual.name() };
		serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, searchStringCanBeNull);

		//@formatter:off
		String query = "SELECT DISTINCT ?resource "+ 
				NatureRecognitionOrchestrator.getNatureSPARQLSelectPart() +
			"\nWHERE{" +
			"\n{";
		//do a subquery to get the candidate resources
		query+=ServiceForSearches.getResourceshavingTypes(clsListList, "?resource", searchInSubTypes)+
				"\n}";

		boolean specialCaseXLabel = ServiceForSearches.isSpecialCaseXLabel(clsListList);

		//prepare the part relative to the ?resource, specifying the searchString, the searchMode, 
		// the useLocalName and useURI
		if(searchString!=null && searchString.length()>0) {
			query += prepareQueryforResourceUsingSearchString(searchString, searchMode, useLexicalizations, useLocalName, useURI,
				useNotes, langs, includeLocales,  lexModel, searchInRDFSLabel, searchInSKOSLabel, 
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
		
		//adding the nature in the query (will be replaced by the appropriate processor), 
		//remember to change the SELECT as well
		query+=NatureRecognitionOrchestrator.getNatureSPARQLWherePart("?resource") +
		
				"\n}"+
				"\nGROUP BY ?resource" +
				"\nHAVING BOUND(?resource) " ;
		//@formatter:on

		return query;
	}


	
	private String prepareQueryforResourceUsingSearchString(String searchString, SearchMode searchMode, 
			boolean useLexicalizations, boolean useLocalName, boolean useURI, boolean useNotes, List<String> langs,
			boolean includeLocales,  IRI lexModel, boolean searchInRDFSLabel, boolean searchInSKOSLabel, 
			boolean searchInSKOSXLLabel, boolean searchInOntolex, boolean includeResToLexicalEntry,
			Map<String, String> prefixToNamespaceMap, boolean specialCaseXLabel) {

		//prepare an inner query, which seems to be working faster (since it executed by GraphDB before the
		// rest of the query and it uses the Lucene indexes)
		String query="\n{SELECT ?resource ?type ?attr_matchMode "+
				"\nWHERE{";

		if(useLocalName){
			//the part related to the localName (with the indexes)
			query+="\n{"+
					searchSpecificModePrepareQuery("?resource", searchString, searchMode,
							LUCENEINDEX_LOCALNAME, null, false, true)+
					"\n}";
			if(useURI || useLexicalizations) {
				query+="\nUNION";
			}
		}
		if(useURI){

			query+="\n{"+
					"\n?resource a ?type . " + // otherwise the filter may not be computed
					searchModePrepareQueryNoIndexes_UsedForURISearch("?resource", searchString, searchMode,
							prefixToNamespaceMap) +

					"\n}";
			if(useLexicalizations) {
				query+="\nUNION";
			}
		}

		if(useLexicalizations){
			query+="\n{";
			//use the indexes to search in the literals, and then, in the rest of the query, get the associated resource
			query+=searchSpecificModePrepareQuery("?label", searchString, searchMode, LUCENEINDEX_LITERAL, langs,
					includeLocales, false);


			//check if the request want to search in the notes as well (plain or reified)
			if(useNotes) {
				query+="\n{" +
						"\n{SELECT ?propNote {?propNote <"+RDFS.SUBPROPERTYOF+">* <"+SKOS.NOTE+"> .}}" +
						"\n?resource ?propNote ?label ." +
						"\n}" +
						"\nUNION" +
						"\n{" +
						"\n{SELECT ?propNote {?propNote <"+RDFS.SUBPROPERTYOF+">* <"+SKOS.NOTE+"> .}}" +
						"\n?resource ?propNote ?refNote ." +
						"\n?refNote <"+RDF.VALUE+"> ?label ." +
						"\n}" +
						"\nUNION";
			}

			boolean unionNeeded = false;
			if(lexModel.equals(Project.RDFS_LEXICALIZATION_MODEL) || searchInRDFSLabel) {
				//search in the rdfs:label
				query+="\n{" +
						"\n?resource <"+RDFS.LABEL+"> ?label ." +
						"\n}";
				unionNeeded = true;
			}
			if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL) || searchInSKOSLabel) {
				//search in skos:prefLabel and skos:altLabel
				if(unionNeeded) {
					query += "\nUNION";
				}
				unionNeeded = true;
				query +="\n{" +
					"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+"> | <"+SKOS.HIDDEN_LABEL.stringValue()+">) ?label ."+
					"\n}";
			}

			if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || searchInSKOSXLLabel) {
				if(unionNeeded) {
					query += "\nUNION";
				}
				unionNeeded = true;
				//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
				query +="\n{" +
					"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+"> | <"+SKOSXL.HIDDEN_LABEL.stringValue()+"> ) ?skosxlLabel ." +
					"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?label ." +
					"\n}";
				if (specialCaseXLabel) {
					query+= "\nUNION " +
							"\n{?resource <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?label . }";
				}
			}
			if(lexModel.equals(Project.ONTOLEXLEMON_LEXICALIZATION_MODEL) || searchInOntolex) {
				if(unionNeeded) {
					query += "\nUNION";
				}
				unionNeeded = true;
				//search in dct:title
				query +="\n{" +
					"\n?resource <"+DCTERMS.TITLE+"> ?label ." +
					"\n}"+
					//search in (ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep
					"\nUNION" +
					"\n{" +
					"\n?resource (<"+ONTOLEX.CANONICAL_FORM.stringValue()+"> | <"+ONTOLEX.OTHER_FORM.stringValue()+">) ?ontoForm ." +
					"\n?ontoForm <"+ONTOLEX.WRITTEN_REP.stringValue()+"> ?label ." +
					"\n}";
				if(includeResToLexicalEntry) {
					//search in allResToLexicalEntry/(ontolex:canonicalForm->ontolex:writtenRep and ontolex:otherform->ontolex:writtenRep
					//construct the complex path from a resource to a LexicalEntry
					String allResToLexicalEntry = getAllPathRestToLexicalEntry();
					query+="\nUNION" +
						"\n{" +
						"\n?resource ("+allResToLexicalEntry+")/"+
						"(<"+ONTOLEX.CANONICAL_FORM.stringValue()+"> | <"+ONTOLEX.OTHER_FORM.stringValue()+">) ?ontoForm ." +
						"\n?ontoForm <"+ONTOLEX.WRITTEN_REP.stringValue()+"> ?label ." +
						"\n}";
				}
			}
			query +="\n}";
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
			String indexToUse, List<String> langs, boolean includeLocales, boolean forLocalName){
		String query ="";
		
		String valueForRegex = ServiceForSearches.escapeStringForRegexInSPARQL(value);
		//String valueForIndex = normalizeStringForLuceneIndex(value, searchMode);
		
		if(indexToUse==null || indexToUse.length()==0) {
			//if no lucene index is specified, then assume it is the Index_Literal
			indexToUse = LUCENEINDEX_LITERAL;
		}
		
		String varToUse;
		String queryPart;
		
		if(forLocalName) {
			varToUse = variable+"_locName";
			//since it should be considered the localname, but in "variable" there is the complete uri 
			//(returned by the lucene search, possibily by using the right index, e.g. LUCENEINDEXLOCALNAME)
			// create a new variable containing the local name
			queryPart="\nBIND(REPLACE(str("+variable+"), '^.*(#|/)', \"\") AS "+varToUse+" )";
		} else {
			varToUse = variable;
			queryPart="";
		}
		
		if(searchMode == SearchMode.startsWith){
			query= indexPart(variable, indexToUse, value, searchMode, forLocalName) +
					// the GraphDB indexes (Lucene) consider as the start of the string all the starts of the
					//single word, so filter them afterward
					queryPart+
					"\nFILTER regex(str("+varToUse+"), '^"+valueForRegex+"', 'i')" +
					"\nBIND('startsWith' AS ?attr_matchMode)";
		} else if(searchMode == SearchMode.endsWith){
			query= indexPart(variable, indexToUse, value, searchMode, forLocalName) +
					// the GraphDB indexes (Lucene) consider as the end of the string all the starts of the
					//single word, so filter them afterward
					queryPart+
					"\nFILTER regex(str("+varToUse+"), '"+valueForRegex+"$', 'i')" +
					"\nBIND('endsWith' AS ?attr_matchMode)";
		} else if(searchMode == SearchMode.contains){
			query= indexPart(variable, indexToUse, value, searchMode, forLocalName) +
					// the GraphDB indexes (Lucene) consider as the end of the string all the starts of the
					//single word, so filter them afterward
					queryPart+
					"\nFILTER regex(str("+varToUse+"), '"+valueForRegex+"', 'i')" + 
					"\nBIND('contains' AS ?attr_matchMode)";
			
		} else if(searchMode == SearchMode.fuzzy){
			query= indexPart(variable, indexToUse, value, searchMode, forLocalName);
			//in this case, you cannot use directly valueForRegex, since the service
			// will generate a list of values, so use value and let wordsForFuzzySearch clean it
			List<String> wordForNoIndex = ServiceForSearches.wordsForFuzzySearch(value, ".", true,
					false, searchMode);
			String wordForNoIndexAsString = ServiceForSearches.listToStringForQuery(wordForNoIndex, "^", "$");
			query += queryPart+
					"\nFILTER regex(str("+varToUse+"), \""+wordForNoIndexAsString+"\", 'i')" +
					"\nBIND('fuzzy' AS ?attr_matchMode)";
			
		} else { // searchMode.equals(exact)
			query= indexPart(variable, indexToUse, value, searchMode, forLocalName) +
					//"\n"+variable+" <"+indexToUse+"> '"+valueForIndex+"' ." +
					queryPart+
					"\nFILTER regex(str("+varToUse+"), '^"+valueForRegex+"$', 'i')" + 
					"\nBIND('exact' AS ?attr_matchMode)";
		}
		
		//if at least one language is specified, then filter the results of the label having such language
		query += ServiceForSearches.prepareLangFilter(langs, variable, includeLocales);
		
		return query;
	}

	private String indexPart(String variable, String indexToUse, String value, SearchMode searchMode,
							 boolean forLocalName){
		String valueForIndex = ServiceForSearches.normalizeStringForLuceneIndex(value, searchMode);
		String query = "";

		// The following part is to use the Lucene Connector, it is commented since the Lucene FTS is used
		/*
		String varSearch = variable+"_search";
		String varForLucene = forLocalName ? variable : variable+"_res_from_Lucene";
		String varProp = variable+"_prop";

		query = "\n"+varSearch+" a "+indexToUse+" ;";

		// this is needed, since the new lucene index dow not work only on the local name, but on the entire URI, so
		// the local name is just a part of the uri
		String startForLocalName = forLocalName ? "*" : "";

		// to avoid problem with languages using different alphabets, add the valueForIndex as it is and then with the part depending
		// on the searchMode
		// and use the UNION to combine these two parts
		if(searchMode == SearchMode.startsWith){
			query+="\n"+LUC_QUERY+" '("+valueForIndex+")|("+startForLocalName+valueForIndex+"*)' ; ";
		} else if(searchMode == SearchMode.endsWith){
			query+="\n"+LUC_QUERY+"  '("+valueForIndex+")|(*"+valueForIndex+")' ; ";
		} else if(searchMode == SearchMode.contains){
			query+="\n"+LUC_QUERY+"  '("+valueForIndex+")|(*"+valueForIndex+"*)' ; ";

		} else if(searchMode == SearchMode.fuzzy){
			//first add valueForIndex to wordForIndex
			List<String> wordForIndex = new ArrayList<>();
			wordForIndex.add(valueForIndex);
			//change each letter in the input searchTerm with * (INDEX) or . (NO_INDEX) to get all the elements
			//having just letter different form the input one
			wordForIndex.addAll(ServiceForSearches.wordsForFuzzySearch(value, "*", false,
					true, searchMode));
			String wordForIndexAsString = ServiceForSearches.listToStringForQuery(wordForIndex, "", "");
			query+="\n"+LUC_QUERY+"  \""+wordForIndexAsString+"\" ;";

		} else { // searchMode.equals(exact)
			query+= "\n" + LUC_QUERY + " '" + valueForIndex + "' ;";
		}

		query += "\n"+LUC_ENTITIES+" "+varForLucene+" .";
		if(!forLocalName) {
			query += "\n" + varForLucene + " " + varProp + " " + variable + " .";
		}
		//this part is used to place in "variable" the value (the Literal) as expected when this method is called
		String obtainLiteralPart = "";
		*/

		// for "Lucene FTS"

		// to avoid problem with languages using different alphabets, add the valueForIndex as it is and then with the part depending
		// on the searchMode
		// and use the UNION to combine these two parts
		if(searchMode == SearchMode.startsWith){
			query="\n"+variable+" <"+indexToUse+"> '("+valueForIndex+")|("+valueForIndex+"*)' . ";
		} else if(searchMode == SearchMode.endsWith){
			query="\n"+variable+" <"+indexToUse+"> '("+valueForIndex+")|(*"+valueForIndex+")' . ";
		} else if(searchMode == SearchMode.contains){
			query="\n"+variable+" <"+indexToUse+"> '("+valueForIndex+")|(*"+valueForIndex+"*)' . ";

		} else if(searchMode == SearchMode.fuzzy){
			//first add valueForIndex to wordForIndex
			List<String> wordForIndex = new ArrayList<>();
			wordForIndex.add(valueForIndex);
			//change each letter in the input searchTerm with * (INDEX) or . (NO_INDEX) to get all the elements
			//having just letter different form the input one
			wordForIndex.addAll(ServiceForSearches.wordsForFuzzySearch(value, "*", false,
					true, searchMode));
			String wordForIndexAsString = ServiceForSearches.listToStringForQuery(wordForIndex, "", "");
			query+="\n"+variable+" <"+indexToUse+"> \""+wordForIndexAsString+"\" .";

		} else { // searchMode.equals(exact)
			query = "\n" + variable + " <" + indexToUse + "> '" + valueForIndex + "' .";
		}

		return query;
	}

	/*
	private String normalizeStringForLuceneIndex(String inputString, SearchMode searchMode) {
		String outputString = inputString;

		//@formatter:off
		//replace all punctuation character except for the underscore <code>_<code>
		//replace all \ and - with a whitespace
		//replace the ' with \'
		outputString = outputString.replaceAll("\\p{Punct}&&[^_]", " ")
				.replace("\\", " ")
				.replace("\'", "\\'")
				.replace("-", " ")
				.trim();
		//@formatter:on

		// replace the '(' with '\\('
		outputString = outputString.replaceAll("\\(", "\\(");
		// replace the ')' with '\\)'
		outputString = outputString.replaceAll("\\)", "\\)");


		//if the search mode is not exactMatch, the starting and ending . should be replaced with (.)
		if(!searchMode.equals(SearchMode.exact)){
			if(outputString.startsWith(".")){
				outputString = "(.)"+outputString.substring(1);
			}
			if(outputString.endsWith(".")){
				outputString = outputString.substring(0, outputString.length()-1)+"(.)";
			}
		}
		return outputString;
	}
	 */
	
	private String searchModePrepareQueryNoIndexes_UsedForURISearch(String variable, String searchString,
												SearchMode searchMode, Map<String, String> prefixToNamespaceMap){
		String query ="";
		String searchStringForUri;

		searchStringForUri = ServiceForSearches.escapeStringForRegexInSPARQL(
				ServiceForSearches.getUriStartFromQname(searchString, prefixToNamespaceMap));

		if(searchMode == SearchMode.startsWith){
			query="\nFILTER regex(str("+variable+"), '^"+searchStringForUri+"', 'i')" +
					"\nBIND('startsWith' AS ?attr_matchMode)";
		} else if(searchMode == SearchMode.endsWith){
			query="\nFILTER regex(str("+variable+"), '"+searchStringForUri+"$', 'i')" +
					"\nBIND('endsWith' AS ?attr_matchMode)";
		} else if(searchMode == SearchMode.contains){
			query="\nFILTER regex(str("+variable+"), '"+searchStringForUri+"', 'i')" +
					"\nBIND('contains' AS ?attr_matchMode)";
		} else if(searchMode == SearchMode.fuzzy){
			List<String> wordForNoIndex = ServiceForSearches.wordsForFuzzySearch(searchString, ".", false, false, searchMode);
			String wordForNoIndexAsString = ServiceForSearches.listToStringForQuery(wordForNoIndex, "^", "$");
			query += "\nFILTER regex(str("+variable+"), \""+wordForNoIndexAsString+"\", 'i')" +
					"\nBIND('fuzzy' AS ?attr_matchMode)";
		} else { // searchMode.equals(exact)
			query="\nFILTER regex(str("+variable+"), '^"+searchStringForUri+"$', 'i')" +
					"\nBIND('exact' AS ?attr_matchMode)";
		}
		
		return query;
	}

	
}
