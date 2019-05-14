package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;

@STService
public class GlobalSearch extends STServiceAdapter {

	protected static Logger logger = LoggerFactory.getLogger(GlobalSearch.class);
	
	private final String lucDirName = "luceneIndex";
	
	private final int MAX_RESULTS = 100;

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
	// TODO decide the @PreAuthorize
	public void createIndex() throws Exception {
		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {
			Directory directory = FSDirectory.open(getLuceneDir().toPath());

			SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
			IndexWriterConfig config = new IndexWriterConfig(simpleAnalyzer);
			RepositoryConnection conn = getManagedConnection();
			Map<String, String> resTypeToRoleMap = computeAllRoles(conn);
			try (IndexWriter writer = new IndexWriter(directory, config)) {

				//@formatter:off
				String query = "";
				IRI lexModel = getProject().getLexicalizationModel();
				//prepare the query for the part associated to the LexicalizationModel
				if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) { //SKOS-XL
					query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
							+ "\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>"
							+ "\nSELECT ?resource ?resourceType ?predicate ?matchedValue ?lang" 
							+ "\nWHERE{" 
							+ "\nGRAPH "+NTriplesUtil.toNTriplesString(getWorkingGraph()) +"{"
							+ "\n?xlabel skosxl:literalForm ?matchedValue ." 
							+ "\nFILTER(isLiteral(?matchedValue)) "
							+ "\nBIND(lang(?matchedValue) AS ?lang)"
							+ "\n?resource ?predicate ?xlabel ." 
							+ "\nFILTER(isIRI(?resource)) "
							+ "\n?resource a ?resourceType ."
							+ "\n}"
							+ "\n}";
					//add to the index the result of the query
					addDirectlyToIndex(query, conn, writer, "label", resTypeToRoleMap);
				} else if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL)) { // SKOS
					String labelsProp = "( skos:prefLabel, skos:altLabel, skos:hiddenLabel )";
					query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
							+ "\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>"
							+ "\nSELECT ?resource ?resourceType ?predicate ?matchedValue ?lang" 
							+ "\nWHERE{" 
							+ "\nGRAPH "+NTriplesUtil.toNTriplesString(getWorkingGraph()) + "{"
							+ "\n?resource ?predicate ?matchedValue ." 
							+ "\nFILTER(isLiteral(?matchedValue)) "
							+ "\nBIND(lang(?matchedValue) AS ?lang)"
							+ "\nFILTER( ?predicate IN "+labelsProp+")" 
							+ "\nFILTER(isIRI(?resource)) "
							+ "\n?resource a ?resourceType ."
							+ "\n}"
							+ "\n}";
					//add to the index the result of the query
					addDirectlyToIndex(query, conn, writer, "label", resTypeToRoleMap);
				} else if (lexModel.equals(Project.RDFS_LEXICALIZATION_MODEL)){ // RDFS
					String labelsProp = "( rdfs:label )";
					query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
							+ "\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>"
							+ "\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
							+ "\nSELECT ?resource ?resourceType ?predicate ?matchedValue ?lang" 
							+ "\nWHERE{" 
							+ "\nGRAPH "+NTriplesUtil.toNTriplesString(getWorkingGraph()) + "{"
							+ "\n?resource ?predicate ?matchedValue ." 
							+ "\nFILTER(isLiteral(?matchedValue)) "
							+ "\nBIND(lang(?matchedValue) AS ?lang)"
							+ "\nFILTER( ?predicate IN "+labelsProp+")" 
							+ "\nFILTER(isIRI(?resource)) "
							+ "\n?resource a ?resourceType ."
							+ "\n}"
							+ "\n}";
					//add to the index the result of the query
					addDirectlyToIndex(query, conn, writer, "label", resTypeToRoleMap);
				} else { //ONTOLEX
					//see for more details https://www.w3.org/2016/05/ontolex/#core
					
					//first get all the LexicalConcept (or Ontology Entity) connected to the LexicalEntry that are 
					// then connected to the form (the Literal). Take also the LexicalEntry and store them in a List.
					// Then get just the LexicalEntry and index only those not present in the List constructued in 
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
							+ "\nSELECT ?resource ?resourceType ?predicate ?matchedValue ?lang ?lexicalEntry" 
							+ "\nWHERE{" 
							+ "\nGRAPH "+NTriplesUtil.toNTriplesString(getWorkingGraph()) + "{"
							+ "\n?lexicalEntry a "+NTriplesUtil.toNTriplesString(ONTOLEX.LEXICAL_ENTRY)+ "." 
							+ "\n?resource"+allResToLexicalEntry+"?lexicalEntry ."
							+ "\n?lexicalEntry "+canonicalFormOrOtherForm+" ?ontoForm ." 
							+ "\n?lexicalEntry ?predicate ?ontoForm ." 
							+ "\n?ontoForm "+NTriplesUtil.toNTriplesString(ONTOLEX.WRITTEN_REP)+" ?matchedValue ." 
							+ "\nFILTER(isLiteral(?matchedValue)) "
							+ "\nBIND(lang(?matchedValue) AS ?lang)"
							+ "\nFILTER(isIRI(?resource)) "
							+ "\n?resource a ?resourceType ."
							+ "\n}"
							+ "\n}";
					logger.debug("query = "+query);
					TupleQuery tupleQuery = conn.prepareTupleQuery(query);
					List<String> lexEntryInLexConceptList = new ArrayList<>();
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
							String matchedValue = bindingSet.getValue("matchedValue").stringValue();
							String lang = bindingSet.getValue("lang").stringValue();
							String repId = getProject().getName();
							String role = getRoleFromResType(resourceType, resTypeToRoleMap);
							String type = "label";
							
							String resourceIRI_repId = resourceIRI+"_"+repId;
							String lexEntryIRI_repId = lexEntryIRI+"_"+repId;
							
							if(!lexEntryInLexConceptList.contains(lexEntryIRI_repId)) {
								lexEntryInLexConceptList.add(lexEntryIRI_repId);
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
											matchedValue, predicate, repId, type, role)));
						}
					}
					
					//now create a SPARQL query to get all LexicalEntry and check those not already found in the 
					//previous query
					query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
							+ "\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>"
							+ "\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
							+ "\nSELECT ?resource ?resourceType ?predicate ?matchedValue ?lang" 
							+ "\nWHERE{" 
							+ "\nGRAPH "+NTriplesUtil.toNTriplesString(getWorkingGraph()) + "{"
							+ "\n?resource a "+NTriplesUtil.toNTriplesString(ONTOLEX.LEXICAL_ENTRY)+ "." 
							+ "\n?resource "+canonicalFormOrOtherForm+" ?ontoForm ." 
							+ "\n?resource ?predicate ?ontoForm ." 
							+ "\n?ontoForm "+NTriplesUtil.toNTriplesString(ONTOLEX.WRITTEN_REP)+" ?matchedValue ." 
							+ "\nFILTER(isLiteral(?matchedValue)) "
							+ "\nBIND(lang(?matchedValue) AS ?lang)"
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
							String matchedValue = bindingSet.getValue("matchedValue").stringValue();
							String lang = bindingSet.getValue("lang").stringValue();
							String repId = getProject().getName();
							String role = getRoleFromResType(resourceType, resTypeToRoleMap);
							String type = "label";
							
							String resourceIRI_repId = resourceIRI+"_"+repId;
							
							if(lexEntryInLexConceptList.contains(resourceIRI_repId)) {
								//this LexicalEntry has already being process during the previously query, so just 
								// skip it
								continue;
							}
							//now add to the index the current element (the LexicalEntry)
							writer.addDocument(addResourceWithLabel(
									new ResourceWithLabel(resourceIRI, resourceLocalName, resourceType, lang, 
											matchedValue, predicate, repId, type, role)));
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
							+ "\nSELECT ?resource ?resourceType ?predicate ?matchedValue ?lang"
							+ "\nWHERE{" 
							//do a subquery to get all the subproperties of skos:note 
							+ "\n{SELECT ?predicate "
							+ "\nWHERE{ "
							+ "\n?predicate rdfs:subPropertyOf* skos:note ."
							+ "\n}}"
							//get both the plain notes and the reified one (consider only the property rdf:value)
							+ "\nGRAPH "+NTriplesUtil.toNTriplesString(getWorkingGraph()) + "{"
							+ "\n{"
							+ "\n?resource ?predicate ?matchedValue ."
							+ "\nFILTER(isLiteral(?matchedValue)) "
							+ "\nFILTER(isIRI(?resource)) "
							+ "\nBIND(lang(?matchedValue) AS ?lang)"
							+ "\n?resource a ?resourceType ."
							+ "\n}"
							+ "\nUNION"
							+ "\n{"
							+ "\n?resource ?predicate ?note ."
							+ "\n?note rdf:value ?matchedValue ."
							+ "\nFILTER(isLiteral(?matchedValue)) "
							+ "\nFILTER(isIRI(?resource)) "
							+ "\nBIND(lang(?matchedValue) AS ?lang)"
							+ "\n?resource a ?resourceType ."
							+ "\n}"
							
							+ "\n}"
							+ "\n}";
					//@formatter:on
					
					//add to the index the result of the query
					addDirectlyToIndex(query, conn, writer, "note", resTypeToRoleMap);
				}
			}

		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}

	private void addDirectlyToIndex(String query, RepositoryConnection conn, IndexWriter writer, String type, 
			Map<String, String> resTypeToRoleMap) throws IOException {
		//System.out.println("query = "+query); // DEBUG
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
				String matchedValue = bindingSet.getValue("matchedValue").stringValue();
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
								matchedValue, predicate, repId, type, role)));
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
	//@STServiceOperation
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO decide the @PreAuthorize
	public void clearSpecificIndex() throws Exception {
		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {
			Directory directory = FSDirectory.open(getLuceneDir().toPath());
			try (IndexWriter writer = new IndexWriter(directory,
					new IndexWriterConfig(new SimpleAnalyzer()))) {
				
				
				
				Builder builderBoolean = new BooleanQuery.Builder();
				builderBoolean.add(new TermQuery(new Term("repId", getProject().getName())), Occur.MUST);

//				Map<String, String> nameValueSearchMap = new HashMap<String, String>();
//				nameValueSearchMap.put("repId", getProject().getName());
//				Builder builder = new BooleanQuery.Builder();
//				for (String name : nameValueSearchMap.keySet()) {
//					String value = nameValueSearchMap.get(name);
//
//					builderBoolean.add(new TermQuery(new Term(name, lang)), Occur.SHOULD);
//					
//					QueryParser qp = new QueryParser(name, new SimpleAnalyzer());
//					Query query = qp.parse("\"" + value + "\"");
//
//					// builder.add(new BooleanClause(query, Occur.MUST));
//					builder.add(query, Occur.MUST);
//				}

				writer.deleteDocuments(builderBoolean.build());
			}
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}
	
	/**
	 * Remove from the Lucene index all the information about all projects
	 * 
	 * @throws Exception
	 */
	@STServiceOperation(method = RequestMethod.POST)
	//@STServiceOperation
	@Write
	// TODO decide the @PreAuthorize
	public void clearAllIndex() throws Exception {
		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {
			Directory directory = FSDirectory.open(getLuceneDir().toPath());
			try (IndexWriter writer = new IndexWriter(directory,
					new IndexWriterConfig(new SimpleAnalyzer()))) {
				
				writer.deleteAll();
				
			}
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}

	/**
	 * Search in the Lucene index all resources/project matching the input string
	 * 
	 * @param searchString
	 * @param lang
	 * @throws Exception
	 */
	@STServiceOperation
	//@Read
	// TODO decide the @PreAuthorize
	public JsonNode search(String searchString, @Optional List<String> langs,
			@Optional(defaultValue = "0") int maxResults, 
			@Optional(defaultValue="false") boolean searchInLocalName) throws Exception {
		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {

			Builder builderBooleanGlobal = new BooleanQuery.Builder();
			Map<String, String> nameValueSearchMap = new HashMap<String, String>();
			
			nameValueSearchMap.put("matchedValue", searchString);
			
			// TODO decide what to do for the mode
			/*if (searchMode.equals(SearchMode.exact)) {
//				 nameValueSearchMap.put("label", "\"" + searchString + "\"");
				nameValueSearchMap.put("label", searchString);
			} else if (searchMode.equals(SearchMode.startsWith)) {
//				 nameValueSearchMap.put("label", "\"" + searchString + "*\"");
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
			nameValueSearchMap.put("lang", langString.trim());
		}
		
		for(String name : nameValueSearchMap.keySet()) {
			Query query = null;
			String value = nameValueSearchMap.get(name);
			//behave differently according to the field used for the search
			if(name.equals("lang")) {
				Builder builderBoolean = new BooleanQuery.Builder(); //  (0, BooleanClause.Occur.SHOULD);
				String[] valueArray = value.split(" ");
				for(String lang : valueArray) {
					builderBoolean.add(new TermQuery(new Term(name, lang)), Occur.SHOULD);
				}
				query = builderBoolean.build();
			} else if(name.equals("matchedValue")) {
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
						builderTemp.add(new Term("resourceLocalName", singleValue), count++);
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
		//System.out.println("query = "+booleanQuery.toString()); // DEBUG
		if(maxResults==0) {
			maxResults = MAX_RESULTS;
		}
		
		IndexSearcher searcher = createSearcher();
		TopDocs hits = searcher.search(booleanQuery, maxResults);

		//combine the answer from lucene
		Map<String, List<ResourceWithLabel>> resToStructMap = combineResoruces(hits, searcher);
		
		//prepare the JSON response
		JsonNode responseJson = prepareResponse(resToStructMap);
		
		return responseJson;
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}
	
	
	

	private Map<String, List<ResourceWithLabel>> combineResoruces(TopDocs hits, IndexSearcher searcher) 
			throws IOException {
		Map<String, List<ResourceWithLabel>> resToStructMap = new HashMap<>();
		for(ScoreDoc sd : hits.scoreDocs) {
			Document doc = searcher.doc(sd.doc);
			
			String resource = doc.get("resource");
			String resourceLocalName = doc.get("resourceLocalName");
			String resourceType = doc.get("resourceType");
			String lang = doc.get("lang");
			String matchedValue = doc.get("matchedValue");
			String predicate = doc.get("predicate");
			String repId = doc.get("repId");
			String type = doc.get("type");
			String role = doc.get("role");
			
			String resource_repId = resource+"_"+repId;
			if(!resToStructMap.containsKey(resource_repId)) {
				resToStructMap.put(resource_repId,  new ArrayList<>());
			}
			ResourceWithLabel resourceWithLabel = new ResourceWithLabel(resource, resourceLocalName, resourceType,
					lang, matchedValue, predicate, repId, type, role);
			resToStructMap.get(resource_repId).add(resourceWithLabel);
		}
		return resToStructMap;
	}
	
	private JsonNode prepareResponse(Map<String, List<ResourceWithLabel>> resToStructMap) {
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
				json.set("matchedValue", jsonFactory.textNode(resourceWithLabel.getMatchedValue()));
				json.set("lang", jsonFactory.textNode(resourceWithLabel.getLang()));
				json.set("predicate", jsonFactory.textNode(resourceWithLabel.getPredicate()));
				json.set("type", jsonFactory.textNode(resourceWithLabel.getType()));
				jsonIntenalArray.add(json);
			}
			jsonResource.set("details", jsonIntenalArray);
			
		}
		return jsonExternalArray;
	}
	
	private IndexSearcher createSearcher() throws IOException {
		Directory directory = FSDirectory.open(getLuceneDir().toPath());
		IndexReader reader = DirectoryReader.open(directory);
		IndexSearcher searcher = new IndexSearcher(reader);
		return searcher;
	}


	private File getLuceneDir() {
		String luceneIndexDirPath = Resources.getSemTurkeyDataDir()+File.separator+lucDirName;
		File luceneIndexDir = new File(luceneIndexDirPath);
		if(!luceneIndexDir.exists()) {
			luceneIndexDir.mkdir();
		}
		return luceneIndexDir;
	}
	
	private Document addResourceWithLabel(ResourceWithLabel resourceWithLabel) throws IOException {
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
		
		doc.add(new StringField("resource", resourceWithLabel.getResource(), Field.Store.YES));
		doc.add(new StringField("resourceLocalName", resourceWithLabel.getResourceLocalName(), Field.Store.YES));
		doc.add(new StringField("resourceType", resourceWithLabel.getResourceType(), Field.Store.YES));
		doc.add(new StringField("lang", resourceWithLabel.getLang(), Field.Store.YES));
		doc.add(new TextField("matchedValue", resourceWithLabel.getMatchedValue(), Field.Store.YES));
		doc.add(new StringField("predicate", resourceWithLabel.getPredicate(), Field.Store.YES));
		doc.add(new StringField("repId", resourceWithLabel.getRepId(), Field.Store.YES));
		doc.add(new StringField("type", resourceWithLabel.getType(), Field.Store.YES));
		doc.add(new StringField("role", resourceWithLabel.getRole(), Field.Store.YES));
		
		return doc;
	}
	
	private class ResourceWithLabel{
		private String resource;
		private String resourceLocalName;
		private String resourceType;
		private String lang;
		private String matchedValue;
		private String predicate;
		private String repId;
		private String type; // it should be note or label
		private String role;
		
		public ResourceWithLabel(String resource, String resourceLocalName, String resourceType, String lang, 
				String matchedValue, String predicate, String repId, String type, String role) {
			this.resource = resource;
			this.resourceLocalName = resourceLocalName;
			this.resourceType = resourceType;
			this.lang = lang;
			this.matchedValue = matchedValue;
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

		public String getMatchedValue() {
			return matchedValue;
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
