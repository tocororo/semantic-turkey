package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.exceptions.GlobalSearchIndexLockException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@STService
public class GlobalSearch extends STServiceAdapter {

	protected static Logger logger = LoggerFactory.getLogger(GlobalSearch.class);

	private final String indexMainDir = "index";

	private final String lucDirName = "globalContentIndex";

	private final String LEXICALIZATION = "lexicalization";
	private final String NOTE = "note";

	private final String INDEX_RESOURCE = "resource";
	private final String INDEX_RESOURCE_LOCAL_NAME = "resourceLocalName";
	private final String INDEX_RESOURCE_TYPE = "resourceType";
	private final String INDEX_LANG = "lang";
	private final String INDEX_VALUE = "value";
	private final String INDEX_PREDICATE = "predicate";
	private final String INDEX_REPO_ID = "repoId";
	private final String INDEX_TYPE = "type";
	private final String INDEX_ROLE = "resource";


	private final int MAX_RESULTS = 300;

	private final int SLEEP_TIME_LOCK = 2000; // 2 sec
	private final int MAX_SLEEP_TENTATIVE = 300; // 300*2 sec = 600 sec = 10 minutes of tentatives

	// private static String CLASS_ROLE = "class";
	// private static String CONCEPT_ROLE = "concept";
	// private static String INSTANCE_ROLE = "instance";

	//@formatter:off
	/*protected SearchStrategy instantiateSearchStrategy() {
		SearchStrategies searchStrategy = STRepositoryInfoUtils
				.getSearchStrategy(getProject().getRepositoryManager()
						.getSTRepositoryInfo(STServiceContextUtils.getRepostoryId(stServiceContext)));

		return SearchStrategyUtils.instantiateSearchStrategy(searchStrategy);
	}*/
	//@formatter:on

	/**
	 * Create the Lucene index for the current project
	 */
	//@STServiceOperation
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('pm(project, index)', 'C')")
	public void createIndex() throws Exception {
		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		int count=0;
		try {
			Directory directory = FSDirectory.open(getLuceneDir().toPath());
			boolean taskCompleted = false;

			RepositoryConnection conn = getManagedConnection();
			Map<String, String> resTypeToRoleMap = computeAllRoles(conn);
			do {
				try {
					SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
					IndexWriterConfig config = new IndexWriterConfig(simpleAnalyzer);
					try (IndexWriter writer = new IndexWriter(directory, config)) {

						//@formatter:off
						String query;
						IRI lexModel = getProject().getLexicalizationModel();
						//prepare the query for the part associated to the LexicalizationModel
						if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) { //SKOS-XL
							String labelsProp = "( skosxl:prefLabel, skosxl:altLabel, skosxl:hiddenLabel )";
							query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
									+ "\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>"
									+ "\nSELECT DISTINCT ?resource ?resourceType ?predicate ?value ?lang"
									+ "\nWHERE{"
									+ "\nGRAPH "+ NTriplesUtil.toNTriplesString(getWorkingGraph()) +"{"
									+ "\n?xlabel skosxl:literalForm ?value ."
									+ "\nFILTER(isLiteral(?value)) "
									+ "\nBIND(lang(?value) AS ?lang)"
									+ "\n?resource ?predicate ?xlabel ."
									+ "\nFILTER( ?predicate IN "+labelsProp+")"
									+ "\nFILTER(isIRI(?resource)) "
									+ "\n?resource a ?resourceType ."
									+ "\n}"
									+ "\n}";
							//add to the index the result of the query
							addDirectlyToIndex(query, conn, writer, LEXICALIZATION, resTypeToRoleMap);
						} else if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL)) { // SKOS
							String labelsProp = "( skos:prefLabel, skos:altLabel, skos:hiddenLabel )";
							query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
									+ "\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>"
									+ "\nSELECT DISTINCT ?resource ?resourceType ?predicate ?value ?lang"
									+ "\nWHERE{"
									+ "\nGRAPH "+NTriplesUtil.toNTriplesString(getWorkingGraph()) + "{"
									+ "\n?resource ?predicate ?value ."
									+ "\nFILTER(isLiteral(?value)) "
									+ "\nBIND(lang(?value) AS ?lang)"
									+ "\nFILTER( ?predicate IN "+labelsProp+")"
									+ "\nFILTER(isIRI(?resource)) "
									+ "\n?resource a ?resourceType ."
									+ "\n}"
									+ "\n}";
							//add to the index the result of the query
							addDirectlyToIndex(query, conn, writer, LEXICALIZATION, resTypeToRoleMap);
						} else if (lexModel.equals(Project.RDFS_LEXICALIZATION_MODEL)){ // RDFS
							String labelsProp = "( rdfs:label )";
							query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
									+ "\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>"
									+ "\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
									+ "\nSELECT DISTINCT ?resource ?resourceType ?predicate ?value ?lang"
									+ "\nWHERE{"
									+ "\nGRAPH "+NTriplesUtil.toNTriplesString(getWorkingGraph()) + "{"
									+ "\n?resource ?predicate ?value ."
									+ "\nFILTER(isLiteral(?value)) "
									+ "\nBIND(lang(?value) AS ?lang)"
									+ "\nFILTER( ?predicate IN "+labelsProp+")"
									+ "\nFILTER(isIRI(?resource)) "
									+ "\n?resource a ?resourceType ."
									+ "\n}"
									+ "\n}";
							//add to the index the result of the query
							addDirectlyToIndex(query, conn, writer, LEXICALIZATION, resTypeToRoleMap);
						} else { //ONTOLEX
							//see for more details https://www.w3.org/2016/05/ontolex/#core

							//first get all the LexicalConcept (or Ontology Entity) connected to the LexicalEntry that are
							// then connected to the form (the Literal). Take also the LexicalEntry and store them in a List.
							// Then get just the LexicalEntry and index only those not present in the List constructed in
							// the first query

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
							String allResToLexicalEntry = " ("+directResToLexicalEntry+"|"+doubleStepResToLexicalEntry+") ";

							String canonicalFormOrOtherForm = " ("+NTriplesUtil.toNTriplesString(ONTOLEX.CANONICAL_FORM)
										+" | "+NTriplesUtil.toNTriplesString(ONTOLEX.OTHER_FORM)+") ";

							query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
									+ "\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>"
									+ "\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
									+ "\nSELECT DISTINCT ?resource ?resourceType ?predicate ?value ?lang ?lexicalEntry"
									+ "\nWHERE{"
									+ "\nGRAPH "+NTriplesUtil.toNTriplesString(getWorkingGraph()) + "{"
									+ "\n?lexicalEntry a "+NTriplesUtil.toNTriplesString(ONTOLEX.LEXICAL_ENTRY)+ "."
									+ "\n?resource"+allResToLexicalEntry+"?lexicalEntry ."
									+ "\n?lexicalEntry "+canonicalFormOrOtherForm+" ?ontoForm ."
									+ "\n?lexicalEntry ?predicate ?ontoForm ."
									+ "\n?ontoForm "+NTriplesUtil.toNTriplesString(ONTOLEX.WRITTEN_REP)+" ?value ."
									+ "\nFILTER(isLiteral(?value)) "
									+ "\nBIND(lang(?value) AS ?lang)"
									+ "\nFILTER(isIRI(?resource)) "
									+ "\n?resource a ?resourceType ."
									+ "\n}"
									+ "\n}";
							logger.debug("query = "+query);
							TupleQuery tupleQuery = conn.prepareTupleQuery(query);
							Set<String> lexEntryInInConceptSet = new HashSet<>();
							Map<String, List<String>> lexConceptToLexEntryMap = new HashMap<>();
							try (TupleQueryResult tupleQueryResult = tupleQuery.evaluate()) {
								while (tupleQueryResult.hasNext()) {
									BindingSet bindingSet = tupleQueryResult.next();
									Value resource = bindingSet.getValue("resource");
									String resourceIRI = resource.stringValue();
									String resourceLocalName = ((IRI)resource).getLocalName();
									String lexEntryIRI = bindingSet.getValue("lexicalEntry").stringValue();
									String resourceType = bindingSet.getValue("resourceType").stringValue();
									String predicate = bindingSet.getValue("predicate").stringValue();
									String value = bindingSet.getValue("value").stringValue();
									String lang = bindingSet.getValue("lang").stringValue();
									String repId = getProject().getName();
									String role = getRoleFromResType(resourceType, resTypeToRoleMap);
									String type = LEXICALIZATION;

									String resourceIRI_repId = resourceIRI+"_"+repId;
									String lexEntryIRI_repId = lexEntryIRI+"_"+repId;

									if(!lexEntryInInConceptSet.contains(lexEntryIRI_repId)) {
										lexEntryInInConceptSet.add(lexEntryIRI_repId);
									}

									if(lexConceptToLexEntryMap.containsKey(resourceIRI_repId)) {
										if(lexConceptToLexEntryMap.get(resourceIRI_repId).contains(lexEntryIRI_repId)) {
											//the couple resourceIRI and lexEntryIRI has already been processed for this repId
											// so just skip it
											continue;
										}
									} else {
										lexConceptToLexEntryMap.put(resourceIRI_repId, new ArrayList<>());
									}
									lexConceptToLexEntryMap.get(resourceIRI_repId).add(lexEntryIRI_repId);

									//now add to the index the current element
									writer.addDocument(addResourceWithLabel(
											new ResourceWithLabel(resourceIRI, resourceLocalName, resourceType, lang,
													value, predicate, repId, type, role)));
								}
							}

							//now create a SPARQL query to get all LexicalEntry and check those not already found in the
							//previous query
							query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
									+ "\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>"
									+ "\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
									+ "\nSELECT DISTINCT  ?resource ?resourceType ?predicate ?value ?lang"
									+ "\nWHERE{"
									+ "\nGRAPH "+NTriplesUtil.toNTriplesString(getWorkingGraph()) + "{"
									+ "\n?resource a "+NTriplesUtil.toNTriplesString(ONTOLEX.LEXICAL_ENTRY)+ "."
									+ "\n?resource "+canonicalFormOrOtherForm+" ?ontoForm ."
									+ "\n?resource ?predicate ?ontoForm ."
									+ "\n?ontoForm "+NTriplesUtil.toNTriplesString(ONTOLEX.WRITTEN_REP)+" ?value ."
									+ "\nFILTER(isLiteral(?value)) "
									+ "\nBIND(lang(?value) AS ?lang)"
									+ "\nFILTER(isIRI(?resource)) "
									+ "\n?resource a ?resourceType ."
									+ "\n}"
									+ "\n}";
							logger.debug("query = "+query);
							tupleQuery = conn.prepareTupleQuery(query);
							try (TupleQueryResult tupleQueryResult = tupleQuery.evaluate()) {
								while (tupleQueryResult.hasNext()) {
									BindingSet bindingSet = tupleQueryResult.next();
									Value resource = bindingSet.getValue("resource");
									String resourceIRI = resource.stringValue();
									String resourceLocalName = ((IRI)resource).getLocalName();
									String resourceType = bindingSet.getValue("resourceType").stringValue();
									String predicate = bindingSet.getValue("predicate").stringValue();
									String value = bindingSet.getValue("value").stringValue();
									String lang = bindingSet.getValue("lang").stringValue();
									String repId = getProject().getName();
									String role = getRoleFromResType(resourceType, resTypeToRoleMap);
									String type = LEXICALIZATION;

									String resourceIRI_repId = resourceIRI+"_"+repId;

									if(lexEntryInInConceptSet.contains(resourceIRI_repId)) {
										//this LexicalEntry has already being process during the previously query, so just
										// skip it
										continue;
									}
									//now add to the index the current element (the LexicalEntry)
									writer.addDocument(addResourceWithLabel(
											new ResourceWithLabel(resourceIRI, resourceLocalName, resourceType, lang,
													value, predicate, repId, type, role)));
								}
							}
						}
						//@formatter:on


						//Prepare the query for the skos:note (and all the subproperties)
						IRI modelType = getProject().getModel();
						if(modelType.equals(Project.SKOS_MODEL) || modelType.equals(Project.ONTOLEXLEMON_MODEL)) {
							//@formatter:off
							query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
									+ "\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>"
									+ "\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
									+ "\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
									+ "\nSELECT DISTINCT ?resource ?resourceType ?predicate ?value ?lang"
									+ "\nWHERE{"
									//do a subquery to get all the subproperties of skos:note
									+ "\n{SELECT ?predicate "
									+ "\nWHERE{ "
									+ "\n?predicate rdfs:subPropertyOf* skos:note ."
									+ "\n}}"
									//get both the plain notes and the reified one (consider only the property rdf:value)
									+ "\nGRAPH "+NTriplesUtil.toNTriplesString(getWorkingGraph()) + "{"
									+ "\n{"
									+ "\n?resource ?predicate ?value ."
									+ "\nFILTER(isLiteral(?value)) "
									+ "\nFILTER(isIRI(?resource)) "
									+ "\nBIND(lang(?value) AS ?lang)"
									+ "\n?resource a ?resourceType ."
									+ "\n}"
									+ "\nUNION"
									+ "\n{"
									+ "\n?resource ?predicate ?note ."
									+ "\n?note rdf:value ?value ."
									+ "\nFILTER(isLiteral(?value)) "
									+ "\nFILTER(isIRI(?resource)) "
									+ "\nBIND(lang(?value) AS ?lang)"
									+ "\n?resource a ?resourceType ."
									+ "\n}"

									+ "\n}"
									+ "\n}";
							//@formatter:on
							logger.debug("query = "+query);

							//add to the index the result of the query
							addDirectlyToIndex(query, conn, writer, NOTE, resTypeToRoleMap);

						}
					}
					taskCompleted = true;

				} catch (LockObtainFailedException e) {
					//the lock is taken so sleep for SLEEP_TIME_LOCK and then check again
					Thread.sleep(SLEEP_TIME_LOCK);
					++count;
				}
			} while (!taskCompleted && count<MAX_SLEEP_TENTATIVE);

		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}

		if(count>=MAX_SLEEP_TENTATIVE) {
			// there was a problem, since there were too many sleep tentatives, so
			throw new GlobalSearchIndexLockException();
		}
	}

	private void addDirectlyToIndex(String query, RepositoryConnection conn, IndexWriter writer, String type,
			Map<String, String> resTypeToRoleMap) throws IOException {
		logger.debug("query = "+query);
		TupleQuery tupleQuery = conn.prepareTupleQuery(query);
		try (TupleQueryResult tupleQueryResult = tupleQuery.evaluate()) {
			while (tupleQueryResult.hasNext()) {
				BindingSet bindingSet = tupleQueryResult.next();
				Value resource = bindingSet.getValue("resource");
				String resourceIRI = resource.stringValue();
				String resourceLocalName = ((IRI)resource).getLocalName();
				String resourceType = bindingSet.getValue("resourceType").stringValue();
				String predicate = bindingSet.getValue("predicate").stringValue();
				String value = bindingSet.getValue("value").stringValue();
				String lang = bindingSet.getValue("lang").stringValue();
				String repId = getProject().getName();
				String role = getRoleFromResType(resourceType, resTypeToRoleMap);
				
				/*Literal labelLiteral = ((Literal) bindingSet.getValue("label"));
				String label = labelLiteral.getLabel();
				String lang = ""; //if no language is present, the set for the lang the empty string
				if(labelLiteral.getLanguage().isPresent()) {
					lang = labelLiteral.getLanguage().get();
				}*/


				writer.addDocument(addResourceWithLabel(
						new ResourceWithLabel(resourceIRI, resourceLocalName, resourceType, lang,
								value, predicate, repId, type, role)));
			}
		}

	}


	private Map<String, String> computeAllRoles(RepositoryConnection conn){
		Map<String, String> resTypeToRoleMap = new HashMap<>();
		//based on computeRole in it.uniroma2.art.semanticturkey.data.role.RoleRecognitionOrchestrator 
		//@formatter:off
		String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
				+ "\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
				+ "\nPREFIX owl: <http://www.w3.org/2002/07/owl#>"
				+ "\nPREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
				+ "\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> "
				+ "\nPREFIX ontolex: <http://www.w3.org/ns/lemon/ontolex#> "
				+ "\nPREFIX lime: <http://www.w3.org/ns/lemon/lime#>"
				+ "\nSELECT ?type ?role "
				+ "\nWHERE { "

				+ "\n{"
				+ "\n?type rdfs:subClassOf* skos:Concept ."
				+ "\nBIND(\"concept\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* skos:ConceptScheme ."
				+ "\nBIND(\"conceptScheme\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* skosxl:Label ."
				+ "\nBIND(\"xLabel\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* skos:OrderedCollection ."
				+ "\nBIND(\"skosOrderedCollection\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* skos:Collection ."
				+ "\nBIND(\"skosCollection\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* rdfs:Datatype ."
				+ "\nBIND(\"dataRange\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* rdfs:Class ."
				+ "\nBIND(\"cls\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* owl:DatatypeProperty ."
				+ "\nBIND(\"datatypeProperty\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* owl:OntologyProperty ."
				+ "\nBIND(\"ontologyProperty\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* owl:AnnotationProperty ."
				+ "\nBIND(\"annotationProperty\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* owl:ObjectProperty ."
				+ "\nBIND(\"objectProperty\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* rdf:Property ."
				+ "\nBIND(\"property\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* owl:Ontology ."
				+ "\nBIND(\"ontology\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* ontolex:LexicalEntry ."
				+ "\nBIND(\"ontolexLexicalEntry\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* lime:Lexicon ."
				+ "\nBIND(\"limeLexicon\" AS ?role) "
				+ "\n}"
				+ "\nUNION"

				+ "\n{"
				+ "\n?type rdfs:subClassOf* ontolex:LexicalSense."
				+ "\nBIND(\"ontolexLexicalSense\" AS ?role) "
				+ "\n}"
				//+ "\nUNION"
				
				/*+ "\n{"
				+ "?type rdfs:subClassOf* owl:Thing ."
				+ "BIND(\"individual\" AS ?role) "
				+ "\n}"*/

				+ "\n}"
				;
		//@formatter:on

		TupleQuery tupleQuery = conn.prepareTupleQuery(query);
		try (TupleQueryResult tupleQueryResult = tupleQuery.evaluate()) {
			while (tupleQueryResult.hasNext()) {
				BindingSet bindingSet = tupleQueryResult.next();
				String type = bindingSet.getValue("type").stringValue();
				String role = bindingSet.getValue("role").stringValue();
				resTypeToRoleMap.put(type, role);
			}
		}

		return resTypeToRoleMap;
	}

	private String getRoleFromResType(String resType, Map<String, String> resTypeToRoleMap) {
		if(resTypeToRoleMap.containsKey(resType)) {
			return resTypeToRoleMap.get(resType);
		}
		return "unknown";
	}

	/**
	 * Remove from the Lucene index all the information of the current project
	 *
	 * @throws Exception
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorizedInProject('pm(project, index)', 'D', #projectName)")
	public void clearSpecificIndex(String projectName) throws Exception {
		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		int count=0;
		try {
			Directory directory = FSDirectory.open(getLuceneDir().toPath());
			boolean taskCompleted = false;

			do {
				try {
					try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new SimpleAnalyzer()))) {

						Builder builderBoolean = new BooleanQuery.Builder();
						builderBoolean.add(new TermQuery(new Term(INDEX_REPO_ID, projectName)), Occur.MUST);

						writer.deleteDocuments(builderBoolean.build());
						taskCompleted = true;
					}
				} catch (LockObtainFailedException e) {
					//the lock is taken so sleep for SLEEP_TIME_LOCK and then check again
					Thread.sleep(SLEEP_TIME_LOCK);
					++count;
				}
			} while (!taskCompleted && count<MAX_SLEEP_TENTATIVE);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}

		if(count>=MAX_SLEEP_TENTATIVE) {
			// there was a problem, since there were too many sleep tentatives, so
			throw new GlobalSearchIndexLockException();
		}
	}

	/**
	 * Remove from the Lucene index all the information about all projects
	 *
	 * @throws Exception
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void clearAllIndex() throws Exception {
		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		int count=0;
		try {
			Directory directory = FSDirectory.open(getLuceneDir().toPath());
			boolean taskCompleted = false;

			do {
				try {
					try (IndexWriter writer = new IndexWriter(directory,
							new IndexWriterConfig(new SimpleAnalyzer()))) {

						writer.deleteAll();
						taskCompleted=true;
					}
				} catch (LockObtainFailedException e) {
					//the lock is taken so sleep for SLEEP_TIME_LOCK and then check again
					Thread.sleep(SLEEP_TIME_LOCK);
					++count;
				}
			} while (!taskCompleted && count<MAX_SLEEP_TENTATIVE);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}

		if(count>=MAX_SLEEP_TENTATIVE) {
			// there was a problem, since there were too many sleep tentatives, so
			throw new GlobalSearchIndexLockException();
		}
	}

	/**
	 * Search in the Lucene index all resources/project matching the input string
	 *
	 * @param searchString
	 * @param langs
	 * @param maxResults the maximun number of results to have (0 means no limit)
	 * @param searchInLocalName true to search in the local name of resources as well (cannot be set to true while caseSensitive is true as well)
	 * @param caseSensitive true to perform a case sensitive search (cannot be set to true while searchInLocalName is true as well)
	 *
	 * @throws Exception
	 */
	@STServiceOperation
	// TODO decide the @PreAuthorize
	public JsonNode search(String searchString, @Optional List<String> langs,
			@Optional(defaultValue = "0") int maxResults,
			@Optional(defaultValue="false") boolean searchInLocalName,
			@Optional(defaultValue="false") boolean caseSensitive) throws Exception {

		if(caseSensitive && searchInLocalName) {
			throw new IllegalArgumentException("searchInLocalName and caseSensitive cannot be both true");
		}

		String searchStringLC = searchString.toLowerCase();

		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {

			Builder builderBooleanGlobal = new BooleanQuery.Builder();
			Map<String, String> nameValueSearchMap = new HashMap<String, String>();

			nameValueSearchMap.put(INDEX_VALUE, searchStringLC);

			// TODO decide what to do for the mode
			/*if (searchMode.equals(SearchMode.exact)) {
//				 nameValueSearchMap.put("label", "\"" + searchStringLC + "\"");
				nameValueSearchMap.put("label", searchStringLC);
			} else if (searchMode.equals(SearchMode.startsWith)) {
//				 nameValueSearchMap.put("label", "\"" + searchStringLC + "*\"");
				nameValueSearchMap.put("label", searchString + "*");
			} else {
				// this should not happen
				throw new IllegalArgumentException("searchMode should be " + SearchMode.exact + " or "
						+ SearchMode.startsWith + ", no other values are accepted");
			}*/

			if(langs!=null && langs.size()>0) {
				String langString = "";
				for(String lang : langs) {
					langString+=lang+" ";
				}
				nameValueSearchMap.put(INDEX_LANG, langString.trim());
			}

			//search for the resources matching the searchString (and inputLangs, if specified)
			for(String name : nameValueSearchMap.keySet()) {
				Query query = null;
				String value = nameValueSearchMap.get(name);
				//behave differently according to the field used for the search
				if(name.equals(INDEX_LANG)) {
					Builder builderBoolean = new BooleanQuery.Builder(); //  (0, BooleanClause.Occur.SHOULD);
					String[] valueArray = value.split(" ");
					for(String lang : valueArray) {
						builderBoolean.add(new TermQuery(new Term(name, lang)), Occur.SHOULD);
					}
					query = builderBoolean.build();
				} else if(name.equals(INDEX_VALUE)) {
					//split the value into single words
					String[] valueArray = value.split(" ");
					int count;
					//search in the label
					PhraseQuery.Builder builderTemp = new PhraseQuery.Builder();
					count = 0;
					for(String singleValue: valueArray) {
						builderTemp.add(new Term(name, singleValue), count++);
					}
					Query queryLabel  = builderTemp.build();
					//check if there should be search in the local name as well
					if(searchInLocalName) {
						count = 0;
						builderTemp = new PhraseQuery.Builder();
						for(String singleValue: valueArray) {
							builderTemp.add(new Term(INDEX_RESOURCE_LOCAL_NAME, singleValue), count++);
						}
						Query queryLocalName = builderTemp.build();
						//combine the query for label and the one for localName
						Builder builderBoolean = new BooleanQuery.Builder(); //  (0, BooleanClause.Occur.SHOULD);
						builderBoolean.add(queryLabel, Occur.SHOULD);
						builderBoolean.add(queryLocalName, Occur.SHOULD);
						query = builderBoolean.build();
					} else {
						//no need to search in the localName, so the query is only about the label
						query = queryLabel;
					}
				}

				builderBooleanGlobal.add(query, Occur.MUST);
			}

			BooleanQuery booleanQuery = builderBooleanGlobal.build();

			IndexSearcher searcher = createSearcher();

			if(maxResults<=0) {
				//maxResults = MAX_RESULTS;
				maxResults = searcher.getIndexReader().maxDoc()>0 ? searcher.getIndexReader().maxDoc() : MAX_RESULTS;
			}

			TopDocs hits = searcher.search(booleanQuery, maxResults);

			//combine the answers from lucene
			Map<String, List<ResourceWithLabel>> resToStructMap = combineResourcesForSearch(hits, searcher, caseSensitive, searchString);

			//prepare the JSON response
			return prepareResponseForSearch(resToStructMap);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}


	/**
	 * Provide the translations, using Lucene Indexes, for the input term in the desired languages
	 * @param searchString the input term for which to search for the translations
	 * @param searchLangs the list of languages where to search the input term. Optional
	 * @param transLangs the languages for the translations
	 * @param caseSensitive true to perform a case sensitive search. Optional, default value is false
	 * @param debug true to return more information
	 * @return
	 * @throws Exception
	 */
	@STServiceOperation
	// TODO decide the @PreAuthorize
	public JsonNode translation(String searchString, @NotEmpty List<String> searchLangs,
			@NotEmpty List<String> transLangs,
			@Optional(defaultValue="false") boolean caseSensitive,
			@Optional(defaultValue="false") boolean debug) throws Exception {

		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode response = jsonFactory.arrayNode();

		String searchStringLC = searchString.toLowerCase();
		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {

			Builder builderBooleanGlobal = new BooleanQuery.Builder();
			Map<String, String> nameValueSearchMap = new HashMap<String, String>();

			//search only in the LEXICALIZATION
			nameValueSearchMap.put(INDEX_TYPE, LEXICALIZATION);

			nameValueSearchMap.put(INDEX_VALUE, searchStringLC);

			if(searchLangs!=null && searchLangs.size()>0) {
				String langString = "";
				for(String lang : searchLangs) {
					langString+=lang+" ";
				}
				nameValueSearchMap.put(INDEX_LANG, langString.trim());
			}

			//search for the resources matching the searchString (and inputLangs, if specified)
			for(String name : nameValueSearchMap.keySet()) {
				Query query = null;
				String value = nameValueSearchMap.get(name);
				//behave differently according to the field used for the search
				if(name.equals(INDEX_LANG)) {
					Builder builderBoolean = new BooleanQuery.Builder(); //  (0, BooleanClause.Occur.SHOULD);
					String[] valueArray = value.split(" ");
					for(String lang : valueArray) {
						builderBoolean.add(new TermQuery(new Term(name, lang)), Occur.SHOULD);
					}
					query = builderBoolean.build();
				} else if(name.equals(INDEX_VALUE)) {
					//split the value into single words
					String[] valueArray = value.split(" ");
					int count;
					//search in the label
					PhraseQuery.Builder builderTemp = new PhraseQuery.Builder();
					count = 0;
					for(String singleValue: valueArray) {
						builderTemp.add(new Term(name, singleValue), count++);
					}
					query  = builderTemp.build();
				} else if(name.equals(INDEX_TYPE)){
					Builder builderBoolean = new BooleanQuery.Builder(); //
					builderBoolean.add(new TermQuery(new Term(name, LEXICALIZATION)), Occur.MUST);
					query = builderBoolean.build();
				}

				builderBooleanGlobal.add(query, Occur.MUST);
			}

			BooleanQuery booleanQuery = builderBooleanGlobal.build();

			IndexSearcher searcher = createSearcher();
			TopDocs hits = searcher.search(booleanQuery, MAX_RESULTS);

			//combine the answers from lucene
			Map<String, List<ResourceWithLabel>> resToStructMap = combineResourcesForSearch(hits, searcher, caseSensitive, searchString);

			//for every resource in the resToStructMap perform a query to get first all descriptions for the retrieve resource in the matched language and then
			// all translations in the specified transLangs
			for(String key : resToStructMap.keySet()){
				//get only the first value of the List, since in such list they are all about the same resource
				List<ResourceWithLabel> resToStructForMatchList = resToStructMap.get(key);
				ResourceWithLabel resourceWithLabel = resToStructForMatchList.get(0);
				String resource = resourceWithLabel.getResource();
				String repId = resourceWithLabel.getRepId();
				//String lang = resourceWithLabel.getLang();


				//get descriptions
				Builder builderBoolean = new BooleanQuery.Builder();
				Query query;

				//add the desired language for the translation
				Builder builderLangBoolean = new Builder();
				for(String searchLang : searchLangs) {
					builderLangBoolean.add(new TermQuery(new Term(INDEX_LANG, searchLang)), Occur.SHOULD);
				}
				query = builderLangBoolean.build();
				builderBoolean.add(query, Occur.MUST);

				//add the resource
				builderBoolean.add(new TermQuery(new Term(INDEX_RESOURCE, resource)), Occur.MUST);

				//add the repId
				builderBoolean.add(new TermQuery(new Term(INDEX_REPO_ID, repId)), Occur.MUST);

				//add the fact that is should be only lexicalization
				//builderBoolean.add(new TermQuery(new Term(INDEX_TYPE, LEXICALIZATION)), Occur.MUST);

				booleanQuery = builderBoolean.build();

				searcher = createSearcher();
				hits = searcher.search(booleanQuery, MAX_RESULTS);
				List<ResourceWithLabel> resToStructForDescList = combineResourcesForTranslation(hits, searcher);


				//get translations
				builderBoolean = new BooleanQuery.Builder();
				//add the desired language for the translation
				builderLangBoolean = new Builder();
				for(String tranLang : transLangs) {
					builderLangBoolean.add(new TermQuery(new Term(INDEX_LANG, tranLang)), Occur.SHOULD);
				}
				query = builderLangBoolean.build();
				builderBoolean.add(query, Occur.MUST);

				//add the resource
				builderBoolean.add(new TermQuery(new Term(INDEX_RESOURCE, resource)), Occur.MUST);

				//add the repId
				builderBoolean.add(new TermQuery(new Term(INDEX_REPO_ID, repId)), Occur.MUST);

				//add the fact that is should be only lexicalization
				//builderBoolean.add(new TermQuery(new Term(INDEX_TYPE, LEXICALIZATION)), Occur.MUST);

				booleanQuery = builderBoolean.build();

				searcher = createSearcher();
				hits = searcher.search(booleanQuery, MAX_RESULTS);
				List<ResourceWithLabel> resToStructForTransList = combineResourcesForTranslation(hits, searcher);
				prepareResponseForAlignment(response, resToStructForMatchList, resToStructForDescList,
						resToStructForTransList, jsonFactory, debug, searchLangs, transLangs);
			}


		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}

		return response;
	}



	private Map<String, List<ResourceWithLabel>> combineResourcesForSearch(TopDocs hits, IndexSearcher searcher, boolean caseSensitive, String searchString)
			throws IOException {
		Map<String, List<ResourceWithLabel>> resToStructMap = new HashMap<>();
		for(ScoreDoc sd : hits.scoreDocs) {
			Document doc = searcher.doc(sd.doc);

			String resource = doc.get(INDEX_RESOURCE);
			String resourceLocalName = doc.get(INDEX_RESOURCE_LOCAL_NAME);
			String resourceType = doc.get(INDEX_RESOURCE_TYPE);
			String lang = doc.get(INDEX_LANG);
			String value = doc.get(INDEX_VALUE);
			String predicate = doc.get(INDEX_PREDICATE);
			String repId = doc.get(INDEX_REPO_ID);
			String type = doc.get(INDEX_TYPE);
			String role = doc.get(INDEX_ROLE);

			if(caseSensitive) {
				if(!value.contains(searchString)){
					//since the search is case sensitive and the matched values does not contains the case sensitive searchString, skip this result
					continue;
				}
			}

			String resource_repId = resource+"_"+repId;
			if(!resToStructMap.containsKey(resource_repId)) {
				resToStructMap.put(resource_repId,  new ArrayList<>());
			}
			ResourceWithLabel resourceWithLabel = new ResourceWithLabel(resource, resourceLocalName, resourceType,
					lang, value, predicate, repId, type, role);
			resToStructMap.get(resource_repId).add(resourceWithLabel);
		}
		return resToStructMap;
	}

	private List<ResourceWithLabel> combineResourcesForTranslation(TopDocs hits, IndexSearcher searcher) throws IOException {
		List<ResourceWithLabel> resToStructList = new ArrayList<>();
		for(ScoreDoc sd : hits.scoreDocs) {
			Document doc = searcher.doc(sd.doc);

			String resource = doc.get(INDEX_RESOURCE);
			String resourceLocalName = doc.get(INDEX_RESOURCE_LOCAL_NAME);
			String resourceType = doc.get(INDEX_RESOURCE_TYPE);
			String lang = doc.get(INDEX_LANG);
			String value = doc.get(INDEX_VALUE);
			String predicate = doc.get(INDEX_PREDICATE);
			String repId = doc.get(INDEX_REPO_ID);
			String type = doc.get(INDEX_TYPE);
			String role = doc.get(INDEX_ROLE);

			ResourceWithLabel resourceWithLabel = new ResourceWithLabel(resource, resourceLocalName, resourceType,
					lang, value, predicate, repId, type, role);
			resToStructList.add(resourceWithLabel);
		}
		return resToStructList;
	}

	private JsonNode prepareResponseForSearch(Map<String, List<ResourceWithLabel>> resToStructMap) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode jsonExternalArray = jsonFactory.arrayNode();
		for(String resouce_repId : resToStructMap.keySet() ) {
			List<ResourceWithLabel> resourceWithLabelList = resToStructMap.get(resouce_repId);

			ResourceWithLabel resourceWithLabelFirst = resourceWithLabelList.get(0);

			ObjectNode jsonResource = jsonFactory.objectNode();
			jsonResource.set("resource", jsonFactory.textNode(resourceWithLabelFirst.getResource()));
			jsonResource.set("resourceLocalName", jsonFactory.textNode(resourceWithLabelFirst.getResourceLocalName()));
			jsonResource.set("resourceType", jsonFactory.textNode(resourceWithLabelFirst.getResourceType()));
			jsonResource.set("role", jsonFactory.textNode(resourceWithLabelFirst.getRole()));

			ObjectNode repoNode = jsonFactory.objectNode();
			String repoId = resourceWithLabelFirst.getRepId();
			repoNode.set("id", jsonFactory.textNode(repoId));
			repoNode.set("open", jsonFactory.booleanNode(ProjectManager.isOpen(repoId)));
			jsonResource.set("repository", repoNode);

			jsonExternalArray.add(jsonResource);

			ArrayNode jsonIntenalArray = jsonFactory.arrayNode();
			for(ResourceWithLabel resourceWithLabel : resourceWithLabelList) {
				ObjectNode json = jsonFactory.objectNode();
				json.set("matchedValue", jsonFactory.textNode(resourceWithLabel.getValue()));
				json.set("lang", jsonFactory.textNode(resourceWithLabel.getLang()));
				json.set("predicate", jsonFactory.textNode(resourceWithLabel.getPredicate()));
				json.set("type", jsonFactory.textNode(resourceWithLabel.getType()));
				jsonIntenalArray.add(json);
			}
			jsonResource.set("details", jsonIntenalArray);

		}
		return jsonExternalArray;
	}

	private void prepareResponseForAlignment(ArrayNode jsonNode, List<ResourceWithLabel> resToStructMatchList, List<ResourceWithLabel> resToStructDescList,
			List<ResourceWithLabel> resToStructTransList, JsonNodeFactory jsonFactory, boolean debug, List<String> searchLangList, List<String> transLangList){

		if(resToStructTransList.isEmpty() && !debug){
			//no translations were found and we are not in debug, so do not add anything
			return;
		}
		//get the first matched resource to get some element they all have in common
		ResourceWithLabel resourceWithLabel = resToStructMatchList.get(0);
		//create the result element for the search
		ObjectNode jsonSingleResultForTranlation = jsonFactory.objectNode();
		jsonNode.add(jsonSingleResultForTranlation);
		jsonSingleResultForTranlation.set("resource", jsonFactory.textNode(resourceWithLabel.getResource()));
		jsonSingleResultForTranlation.set("resourceLocalName", jsonFactory.textNode(resourceWithLabel.getResourceLocalName()));
		jsonSingleResultForTranlation.set("resourceType", jsonFactory.textNode(resourceWithLabel.getResourceType()));
		jsonSingleResultForTranlation.set("role", jsonFactory.textNode(resourceWithLabel.getRole()));

		ObjectNode repoNode = jsonFactory.objectNode();
		String repoId = resourceWithLabel.getRepId();
		repoNode.set("id", jsonFactory.textNode(repoId));
		repoNode.set("open", jsonFactory.booleanNode(ProjectManager.isOpen(repoId)));
		jsonSingleResultForTranlation.set("repository", repoNode);

		//add all matched values (grouped by searchLangList)
		ArrayNode matchedExternalArray = createArrayFromResourceWithLabelList(searchLangList, resToStructMatchList,
				jsonFactory);
		jsonSingleResultForTranlation.set("matches", matchedExternalArray);



		//add all descriptions in a the given language (grouped by searchLangList)
		ArrayNode descriptionExternalArray = createArrayFromResourceWithLabelList(searchLangList, resToStructDescList,
				jsonFactory);
		jsonSingleResultForTranlation.set("descriptions", descriptionExternalArray);


		//add all the translations (grouped by transLangs values)
		ArrayNode translationExternalArray = createArrayFromResourceWithLabelList(transLangList, resToStructTransList,
				jsonFactory);
		jsonSingleResultForTranlation.set("translations", translationExternalArray);
	}

	private ArrayNode createArrayFromResourceWithLabelList(List<String> langList,
			List<ResourceWithLabel> resToStructList,
			JsonNodeFactory jsonFactory){
		ArrayNode externalArray = jsonFactory.arrayNode();
		for(String lang : langList) {
			ObjectNode langAndMatchedNode = jsonFactory.objectNode();
			langAndMatchedNode.set("lang", jsonFactory.textNode(lang));
			ArrayNode internalArray = jsonFactory.arrayNode();
			for (ResourceWithLabel resourceWithLabel : resToStructList) {
				//check that language of resMatched is the same as lang
				if(!resourceWithLabel.getLang().toLowerCase().equals(lang.toLowerCase())){
					//different language, so do nothing
					continue;
				}
				ObjectNode objectNode = createObjectNodeForArray(resourceWithLabel, jsonFactory);
				internalArray.add(objectNode);
			}
			langAndMatchedNode.set("values", internalArray);
			externalArray.add(langAndMatchedNode);
		}
		return externalArray;
	}

	private ObjectNode createObjectNodeForArray(ResourceWithLabel resourceWithLabel, JsonNodeFactory jsonFactory){
		ObjectNode objectNode = jsonFactory.objectNode();
		objectNode.set("value", jsonFactory.textNode(resourceWithLabel.getValue()));
		objectNode.set("predicate", jsonFactory.textNode(resourceWithLabel.getPredicate()));
		objectNode.set("type", jsonFactory.textNode(resourceWithLabel.getType()));
		return objectNode;
	}
	
	private IndexSearcher createSearcher() throws IOException {
		Directory directory = FSDirectory.open(getLuceneDir().toPath());
		IndexReader reader = DirectoryReader.open(directory);
		return new IndexSearcher(reader);
	}


	private File getLuceneDir() {

		String mainIndexPath = Resources.getSemTurkeyDataDir()+File.separator+indexMainDir;
		File mainIndexDir = new File(mainIndexPath);
		if(!mainIndexDir.exists()){
			mainIndexDir.mkdir();
		}
		//String luceneIndexDirPath = Resources.getSemTurkeyDataDir()+File.separator+lucDirName;
		//File luceneIndexDir = new File(luceneIndexDirPath);
		File luceneIndexDir = new File(mainIndexDir, lucDirName);
		if(!luceneIndexDir.exists()) {
			luceneIndexDir.mkdir();
		}
		return luceneIndexDir;
	}
	
	private Document addResourceWithLabel(ResourceWithLabel resourceWithLabel) {
		Document doc = new Document();
		
		//@formatter:off
		/*
		doc.add(new TextField("concept", conceptWithLabel.getConcept(), Field.Store.YES));
		doc.add(new TextField("lang", conceptWithLabel.getLang(), Field.Store.YES));
		doc.add(new TextField("label", conceptWithLabel.getLabel(), Field.Store.YES));
		doc.add(new TextField("repId", conceptWithLabel.getRepId(), Field.Store.YES));
		doc.add(new TextField("labelType", conceptWithLabel.getLabelType(), Field.Store.YES));
		*/
		//@formatter:on
		
		doc.add(new StringField(INDEX_RESOURCE, resourceWithLabel.getResource(), Field.Store.YES));
		doc.add(new StringField(INDEX_RESOURCE_LOCAL_NAME, resourceWithLabel.getResourceLocalName(), Field.Store.YES));
		doc.add(new StringField(INDEX_RESOURCE_TYPE, resourceWithLabel.getResourceType(), Field.Store.YES));
		doc.add(new StringField(INDEX_LANG, resourceWithLabel.getLang(), Field.Store.YES));
		doc.add(new TextField(INDEX_VALUE, resourceWithLabel.getValue(), Field.Store.YES));
		doc.add(new StringField(INDEX_PREDICATE, resourceWithLabel.getPredicate(), Field.Store.YES));
		doc.add(new StringField(INDEX_REPO_ID, resourceWithLabel.getRepId(), Field.Store.YES));
		doc.add(new StringField(INDEX_TYPE, resourceWithLabel.getType(), Field.Store.YES));
		doc.add(new StringField(INDEX_ROLE, resourceWithLabel.getRole(), Field.Store.YES));
		
		return doc;
	}
	
	private class ResourceWithLabel{
		private String resource;
		private String resourceLocalName;
		private String resourceType;
		private String lang;
		private String value; // previously was matchedValue
		private String predicate;
		private String repId;
		private String type; // it should be LEXICALIZATION or NOTE
		private String role;
		
		public ResourceWithLabel(String resource, String resourceLocalName, String resourceType, String lang, 
				String value, String predicate, String repId, String type, String role) {
			this.resource = resource;
			this.resourceLocalName = resourceLocalName;
			this.resourceType = resourceType;
			this.lang = lang;
			this.value = value;
			this.predicate = predicate;
			this.repId = repId;
			this.type = type;
			this.role = role;
		}

		public String getResource() {
			return resource;
		}
		
		public String getResourceLocalName() {
			return resourceLocalName;
		}
		
		public String getResourceType() {
			return resourceType;
		}

		public String getLang() {
			return lang;
		}

		public String getValue() {
			return value;
		}

		public String getPredicate() {
			return predicate;
		}

		public String getRepId() {
			return repId;
		}
		
		public String getType() {
			return type;
		}
		
		public String getRole() {
			return role;
		}
		
	}
	
}
