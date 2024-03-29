package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import it.uniroma2.art.semanticturkey.constraints.SubPropertyOf;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.RemoteResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourceLocator;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.data.nature.NatureRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.data.nature.TripleScopes;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedLexicalizationModelException;
import it.uniroma2.art.semanticturkey.mdr.core.DatasetMetadata;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;
import it.uniroma2.art.semanticturkey.project.RepositoryLocation;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.ontolexlemon.FormRenderer;
import it.uniroma2.art.semanticturkey.services.core.ontolexlemon.LexicalEntryRenderer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractStatementConsumer;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@STService
public class ICV extends STServiceAdapter {
	
	@Autowired
	private ResourceLocator resourceLocator;

	private ThreadLocal<Map<Project, RepositoryConnection>> projectConnectionHolder = ThreadLocal
			.withInitial(HashMap::new);
	
	protected static Logger logger = LoggerFactory.getLogger(ICV.class);

	//-----ICV ON LOGICAL CONSISTENCY -----
	private static final Pattern CONSISTENCY_VIOLATION_PATTERN = Pattern.compile("Consistency check (?<conditionName>.+?) failed:\\n(?<inconsistentTriples>.+?)((?=Consistency check)|$)", Pattern.DOTALL);

	public static class ConsistencyViolation {

		public String conditionName;
		public List<Triple> inconsistentTriples;

	}

	public static class InferenceExplanation  {
		public String ruleName;
		public List<Triple> premises;
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf', 'R')")
	public InferenceExplanation explain(Resource subject, IRI predicate, Value object) {
		RepositoryConnection conn = getManagedConnection();
		ValueFactory vf = SimpleValueFactory.getInstance();

		// Executes the query to obtain an explanation for an inferred triple
		TupleQuery explainQuery = conn.prepareTupleQuery("PREFIX proof: <http://www.ontotext.com/proof/>\n" +
				"\n" +
				"SELECT ?rule ?s ?p ?o ?context WHERE {\n" +
				"    ?ctx proof:explain (?subject ?predicate ?object) .\n" +
				"    ?ctx proof:rule ?rule .\n" +
				"    ?ctx proof:subject ?s .\n" +
				"    ?ctx proof:predicate ?p .\n" +
				"    ?ctx proof:object ?o .\n" +
				"    ?ctx proof:context ?context .\n" +
				"}");
		explainQuery.setBinding("subject", subject);
		explainQuery.setBinding("predicate", predicate);
		explainQuery.setBinding("object", object);
		List<BindingSet> explanationBindingSets = QueryResults.asList(explainQuery.evaluate());

		if (explanationBindingSets.isEmpty()) {
			throw new RuntimeException("Could not explain triple: " + NTriplesUtil.toNTriplesString(subject) + " " + NTriplesUtil.toNTriplesString(predicate) + " " + NTriplesUtil.toNTriplesString(object) + " .");
		}

		// Gets the rule name
		String ruleName = Literals.getLabel(explanationBindingSets.iterator().next().getValue("rule"), "");

		// Gets tge raw triples that are the conditions
		List<Statement> rawTriples = explanationBindingSets.stream().map(bs -> vf.createStatement((Resource)bs.getValue("s"), (IRI)bs.getValue("p"),bs.getValue("o"))).collect(Collectors.toList());

		List<Triple> processedTriples = getProcessedTriples(conn, rawTriples);
		InferenceExplanation explanation = new InferenceExplanation();
		explanation.ruleName = ruleName;
		explanation.premises = processedTriples;

		return explanation;
	}

	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('rdf', 'R')")
	public List<ConsistencyViolation> listConsistencyViolations() {
		try (RepositoryConnection conn = getRepository().getConnection()) {
			// Determines the current ruleset. If none, throws an exception
			TupleQuery rulesetQuery = conn.prepareTupleQuery(
					"PREFIX sys: <http://www.ontotext.com/owlim/system#>                  \n" +
					"SELECT ?state ?ruleset {                                             \n" +
					"    ?state sys:listRulesets ?ruleset                                 \n" +
					"}");
			String ruleset = QueryResults.stream(rulesetQuery.evaluate())
					.filter(bs -> conn.getValueFactory().createIRI("http://www.ontotext.com/owlim/system#currentRuleset")
							.equals(bs.getValue("state")))
					.map(bs -> bs.getValue("ruleset"))
					.filter(Objects::nonNull)
					.findAny()
					.map(Value::stringValue)
					.orElseThrow(() -> new IllegalStateException("No ruleset is currently defined"));

			// The actual consistency check
			Update consistencyCheck = conn.prepareUpdate(
				"PREFIX sys: <http://www.ontotext.com/owlim/system#>                               \n" +
				"INSERT DATA {                                                                     \n" +
				"    _:b sys:consistencyCheckAgainstRuleset \"" + RenderUtils.escape(ruleset) + "\"\n" +
				"}");
			try {
				consistencyCheck.execute();
			} catch (UpdateExecutionException e) {
				List<ConsistencyViolation> violations = new ArrayList<>();

				// Parses (what should be) the consistency violations report
				String msg = e.getMessage();
				Matcher matcher = CONSISTENCY_VIOLATION_PATTERN.matcher(msg);
				ValueFactory vf = SimpleValueFactory.getInstance();

				while (matcher.find()) {
					String conditionName = matcher.group("conditionName");
					String triplesRaw = matcher.group("inconsistentTriples");
					List<Statement> rawTriples = new ArrayList<>();
					Arrays.stream(triplesRaw.split("\n")).map(String::trim).map(triple -> {
						int subjectBegin = 0;
						int subjectEnd = triple.indexOf(" ");

						int predicateBegin = subjectEnd + 1;
						int predicateEnd = triple.indexOf(" ", predicateBegin);

						int objectBegin = predicateEnd + 1;
						int objectEnd = triple.length();

						String subjectStr = triple.substring(subjectBegin, subjectEnd);
						String predicateStr = triple.substring(predicateBegin, predicateEnd);
						String objectStr = triple.substring(objectBegin, objectEnd);

						Resource subject = subjectStr.startsWith("_:") ? vf.createBNode(subjectStr.substring(2)) : vf.createIRI(subjectStr);
						IRI predicate = vf.createIRI(predicateStr);
						Value object;

						if (objectStr.startsWith("_:")) {
							object = vf.createBNode(objectStr.substring(2));
						} else if (objectStr.startsWith("\"")) {
							int langTagBegin = objectStr.lastIndexOf("\"@");
							if (langTagBegin != -1) {
								object = vf.createLiteral(objectStr.substring(1, langTagBegin), objectStr.substring(langTagBegin + 2));
							} else {
								int dtBegin = objectStr.lastIndexOf("\"^^<");
								if (dtBegin != -1) {
									object = vf.createLiteral(objectStr.substring(1, dtBegin), vf.createIRI(objectStr.substring(dtBegin + 4, objectStr.length() - 1)));
								} else {
									object = vf.createLiteral(objectStr.substring(1, objectStr.length() - 1));
								}
							}
						} else {
							object = vf.createIRI(objectStr);
						}

						return vf.createStatement(subject, predicate, object);
					}).forEach(rawTriples::add);

					List<Triple> processedTriples = getProcessedTriples(conn, rawTriples);

					ConsistencyViolation aViolation = new ConsistencyViolation();
					aViolation.conditionName = conditionName;
					aViolation.inconsistentTriples =  processedTriples;

					violations.add(aViolation);
				}

				// If no violation has been found, it means that the exception was related to something else
				if (violations.isEmpty()) {
					throw e;
				}

				return violations;
			}
		}

		return Collections.emptyList();
	}

	private List<Triple> getProcessedTriples(RepositoryConnection conn, List<Statement> rawTriples) {
		// Transforms the raw list of triples into a model for faster lookup
		Model triplesAsModel = new LinkedHashModel(rawTriples);

		// Deletes bnodes which are objects of other triples in the model. For simplicity, we assume acyclic
		// graph wrt bnodes
		triplesAsModel.removeIf(s -> s.getSubject().isBNode() && triplesAsModel.contains(null, null, s.getObject()));

		// Processes the remaining triples
		List<Triple> processedTriples = new ArrayList<>(triplesAsModel.size());

		// Determines the graphs to which each triple belongs to
		TupleQuery graphQuery = conn.prepareTupleQuery("SELECT ?s ?p ?o ?g { GRAPH ?g { ?s ?p ?o } }");
		BooleanQuery askExplicitNullCtxQuery = conn.prepareBooleanQuery("ASK { GRAPH <http://rdf4j.org/schema/rdf4j#nil> { ?s ?p ?o } }");
		askExplicitNullCtxQuery.setIncludeInferred(false);

		// Statements used for rendering blank nodes
		Model statements = new LinkedHashModel();


		// Performs a describe over blank nodes
		List<BNode> blankNodes = Sets.union(triplesAsModel.subjects(), triplesAsModel.objects()).stream().filter(BNode.class::isInstance).map(BNode.class::cast).collect(Collectors.toList());
		if (!blankNodes.isEmpty()) {
			GraphQuery describeQuery = conn.prepareGraphQuery("DESCRIBE " + IntStream.range(0, blankNodes.size()).mapToObj(i -> "?x" + i).collect(Collectors.joining(" ")));
			Streams.mapWithIndex(blankNodes.stream(), Pair::of).forEach(p->describeQuery.setBinding("x" + p.getValue(), p.getKey()));
			QueryResults.stream(describeQuery.evaluate()).forEach(statements::add);
		}

		// Get attributes for resources
		List<Resource> resources = Streams.concat(triplesAsModel.subjects().stream(), triplesAsModel.predicates().stream(), triplesAsModel.objects().stream()).filter(Resource.class::isInstance).map(Resource.class::cast).distinct().collect(Collectors.toList());

		Collection<AnnotatedValue<Resource>> annotatedResources;
		if (!resources.isEmpty()) {
			QueryBuilder qb;
			StringBuilder sb = new StringBuilder();
			sb.append(
				// @formatter:off
				" SELECT ?resource WHERE {		\n" +
				IntStream.range(0, resources.size()).mapToObj(i -> "{ BIND(?x" + i + " as ?resource) }").collect(Collectors.joining("\nUNION\n")) +
				"} GROUP BY ?resource													\n"
				// @formatter:on
			);
			qb = createQueryBuilder(sb.toString());
			qb.processStandardAttributes();
			qb.process(LexicalEntryRenderer.INSTANCE_WITHOUT_FALLBACK, "resource", "attr_lexicalEntryRendering");
			qb.process(FormRenderer.INSTANCE_WITHOUT_FALLBACK, "resource", "attr_formRendering");

			Streams.mapWithIndex(resources.stream(), Pair::of).forEach(p -> qb.setBinding("x" + p.getValue(), p.getKey()));

			annotatedResources = qb.runQuery();
		} else {
			annotatedResources = Collections.emptyList();
		}

		// Stuff for the computation of resource attributes
		Map<Resource, Map<String, Value>> resource2attributes = annotatedResources.stream().collect(Collectors.toMap(ar -> ar.getValue(), ar -> ar.getAttributes()));
		Map<IRI, Map<Resource, Literal>> predicate2creShow = new HashMap<>();

		// Process the statements that survived after bnode inlining
		for (Statement st : rawTriples) {
			if (!triplesAsModel.contains(st)) continue; // skips triples that have been deletes (e.g. by bnode inlining)

			graphQuery.setBinding("s", st.getSubject());
			graphQuery.setBinding("p", st.getPredicate());
			graphQuery.setBinding("o", st.getObject());

			askExplicitNullCtxQuery.setBinding("s", st.getSubject());
			askExplicitNullCtxQuery.setBinding("p", st.getPredicate());
			askExplicitNullCtxQuery.setBinding("o", st.getObject());

			Set<Resource> graphSet = QueryResults.stream(graphQuery.evaluate()).map(bs -> (Resource)bs.getValue("g")).collect(Collectors.toSet());
			if (askExplicitNullCtxQuery.evaluate()) { // explicitly in the null context
				graphSet.add(null);
			} else if(graphSet.isEmpty()) { // if the triple is not in any graph nor is it in the null context, then it is inferred
				graphSet.add(NatureRecognitionOrchestrator.INFERENCE_GRAPH);
			}
			String graphsAttribute = AbstractStatementConsumer.computeGraphs(graphSet);
			TripleScopes tripleScopeAttribute = AbstractStatementConsumer.computeTripleScope(graphSet, getWorkingGraph());

			AnnotatedValue<Resource> annotatedSubject = new AnnotatedValue<>(st.getSubject());
			AnnotatedValue<IRI> annotatedPredicate = new AnnotatedValue<>(st.getPredicate());
			AnnotatedValue<Value> annotatedObject = new AnnotatedValue<>(st.getObject());

			// Add annotations to resources
			for (AnnotatedValue<?> av : ImmutableList.of(annotatedSubject, annotatedPredicate, annotatedObject)) {
				if (av.getValue().isResource()) {
					AnnotatedValue<Resource> av2 = (AnnotatedValue<Resource>)(Object)av;
					AbstractStatementConsumer.addShowViaDedicatedOrGenericRendering(av2, resource2attributes, predicate2creShow, st.getPredicate(), statements, true);
					AbstractStatementConsumer.addNature(av2, resource2attributes);
					AbstractStatementConsumer.addQName(av2, resource2attributes);
				}
			}

			Triple aProcessedTriple = new Triple(annotatedSubject, annotatedPredicate, annotatedObject, graphsAttribute, tripleScopeAttribute);
			processedTriples.add(aProcessedTriple);
		}
		return processedTriples;
	}

	//-----ICV ON CONCEPTS STRUCTURE-----
	
	//ST-87
	/**
	 * Returns a list of concepts, where each concept is a dangling concept in the given
	 * scheme
	 * @param scheme scheme where the concepts are dangling
	 * @return a list of concepts, where each concept is a dangling concept in the given
	 * scheme
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Collection<AnnotatedValue<Resource>> listDanglingConcepts(IRI scheme)  {
		String q = "SELECT ?resource WHERE { \n"
				+ "BIND(" + NTriplesUtil.toNTriplesString(scheme) + " as ?scheme) \n"
				+ "?resource a " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT) + ". \n"
				+ "?resource " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?resource " + NTriplesUtil.toNTriplesString(SKOS.TOP_CONCEPT_OF) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.HAS_TOP_CONCEPT) + "  ?scheme \n"
				+ "} \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?resource " + NTriplesUtil.toNTriplesString(SKOS.BROADER) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.NARROWER) + "  ?broader . \n"
				+ "?broader " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "} \n } GROUP BY ?resource";
		logger.debug("query [listDanglingConcepts]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	//ST-1273
	/**
	 * Returns a list of concepts and search in all schemes 
	 * @return a list of concepts and search in all schemes
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Collection<AnnotatedValue<Resource>> listDanglingConceptsForAllSchemes()  {
		String q = "SELECT ?resource ?attr_dangScheme WHERE { \n"
				//+ "BIND(" + NTriplesUtil.toNTriplesString(scheme) + " as ?scheme) \n"
				+ "?resource a " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT) + ". \n"
				+ "?resource " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?attr_dangScheme . \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?resource " + NTriplesUtil.toNTriplesString(SKOS.TOP_CONCEPT_OF) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.HAS_TOP_CONCEPT) + "  ?attr_dangScheme \n"
				+ "} \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?resource " + NTriplesUtil.toNTriplesString(SKOS.BROADER) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.NARROWER) + "  ?broader . \n"
				+ "?broader " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?attr_dangScheme . \n"
				+ "} \n } GROUP BY ?resource ?attr_dangScheme";
		logger.debug("query [listDanglingConcepts]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
//	/**
//	 * Detects cyclic hierarchical relations. Returns a list of records top, n1, n2 where 
//	 * top is likely the cause of the cycle, n1 and n2 are vertex that belong to the cycle
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
//	public Response listCyclicConcepts()  {
//		RepositoryConnection conn = getManagedConnection();
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT DISTINCT ?top ?n1 ?n2 WHERE{\n"
//				+ "{?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?n1 .\n"
//				+ "?n1 (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">) ?n2 .\n"
//				+ "?n2 (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?top .\n"
//				+ "}UNION{\n"
//				+ "?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">) ?n1 .\n"
//				+ "?n1 (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">) ?top .\n"
//				+ "bind(?top as ?n2)\n"
//				+ "} {\n" 
//				+ "?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?cyclicConcept .\n"
//				+ "?cyclicConcept (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?top .\n"
//				+ "?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">) ?broader .\n"
//				+ "FILTER NOT EXISTS {\n"
//				+ "?broader (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?top }\n"
//				+ "} UNION {\n"
//				+ "?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?cyclicConcept .\n"
//				+ "?cyclicConcept (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?top .\n"
//				+ "?top (<" + SKOS.TOP_CONCEPT_OF + "> | ^ <" + SKOS.HAS_TOP_CONCEPT + ">)+ ?scheme .} }";
//		logger.debug("query [listCyclicConcepts]:\n" + q);
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			String topCyclicConcept = tb.getBinding("top").getValue().stringValue();
//			String node1 = tb.getBinding("n1").getValue().stringValue();
//			String node2 = tb.getBinding("n2").getValue().stringValue();
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			recordElem.setAttribute("topCyclicConcept", topCyclicConcept);
//			recordElem.setAttribute("node1", node1);
//			recordElem.setAttribute("node2", node2);
//		}
//		return response;
//	}
	
	/**
	 * Returns a list of schemes that have no top concept
	 * @return a list of schemes that have no top concept
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(conceptScheme)', 'R')")
	public Collection<AnnotatedValue<Resource>> listConceptSchemesWithNoTopConcept() {
		String q = "SELECT ?resource WHERE {\n"
				+ "?resource a <" + SKOS.CONCEPT_SCHEME + "> .\n"
				+ "FILTER NOT EXISTS { {\n"
				+ "?resource <" + SKOS.HAS_TOP_CONCEPT + "> ?topConcept .\n"
				+ "} UNION {\n"
				+ "?topConcept <" + SKOS.TOP_CONCEPT_OF + "> ?resource . } } }\n"
				+ "GROUP BY ?resource";
		logger.debug("query [listConceptSchemesWithNoTopConcept]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Returns a list of concepts that don't belong to any scheme 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Collection<AnnotatedValue<Resource>> listConceptsWithNoScheme(){
		String q = "SELECT ?resource WHERE { \n"
				+ "?resource a <" + SKOS.CONCEPT + "> .\n"
				+ "FILTER NOT EXISTS { ?resource <" + SKOS.IN_SCHEME + "> ?scheme . } }\n"
				+ "GROUP BY ?resource";
		logger.debug("query [listConceptsWithNoScheme]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Returns a list of concepts that are topConcept but have a broader in the same scheme 
	 * @return a list of concepts that are topConcept but have a broader in the same scheme
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public JsonNode listTopConceptsWithBroader(){
		String q = "SELECT DISTINCT ?concept ?scheme WHERE {\n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?broader <" + SKOS.IN_SCHEME + "> | <" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + "> ?scheme . }";
		logger.debug("query [listTopConceptsWithBroader]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode recordArrayNode = jsonFactory.arrayNode();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String concept = tb.getBinding("concept").getValue().stringValue();
			String scheme = tb.getBinding("scheme").getValue().stringValue();
			ObjectNode recordNode = jsonFactory.objectNode();
			recordNode.set("concept", jsonFactory.textNode(concept));
			recordNode.set("scheme", jsonFactory.textNode(scheme));
			recordArrayNode.add(recordNode);
		}
		return recordArrayNode;
	}
	
//	/**
//	 * Returns a list of skos:Concept that have redundant hierarchical relations
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
//	public Response listHierarchicallyRedundantConcepts(){
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT DISTINCT ?narrower ?broader WHERE{\n"
//				+ "?narrower <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
//				+ "?narrower (<" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + ">)+ ?middle .\n"
//				+ "?middle <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
//				+ "FILTER(?narrower != ?middle)\n}";
//		logger.debug("query [listHierarchicallyRedundantConcepts]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			String narrower = tb.getBinding("narrower").getValue().stringValue();
//			String broader = tb.getBinding("broader").getValue().stringValue();
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			recordElem.setAttribute("broader", broader);
//			recordElem.setAttribute("narrower", narrower);
//		}
//		return response;
//	}
//	
//	//-----ICV ON LABELS-----
//	
//	/**
//	 * Returns a list of records concept1-concept2-label-lang, of concepts that have the same skos:prefLabel
//	 * in the same language
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
//	public Response listConceptsWithSameSKOSPrefLabel() {
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT ?concept1 ?concept2 ?label ?lang WHERE {\n"
//				+ "?concept1 a <" + SKOS.CONCEPT + "> .\n"
//				+ "?concept2 a <" + SKOS.CONCEPT + "> .\n"
//				+ "?concept1 <" + SKOS.PREF_LABEL + "> ?label .\n"
//				+ "?concept2 <" + SKOS.PREF_LABEL + "> ?label .\n"
//				+ "bind(lang(?label) as ?lang)\n"
//				+ "FILTER (str(?concept1) < str(?concept2)) }";
//		logger.debug("query [listConceptsWithSameSKOSPrefLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			String concept1 = tb.getBinding("concept1").getValue().stringValue();
//			String concept2 = tb.getBinding("concept2").getValue().stringValue();
//			String label = tb.getBinding("label").getValue().stringValue();
//			String lang = tb.getBinding("lang").getValue().stringValue();
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			recordElem.setAttribute("concept1", concept1);
//			recordElem.setAttribute("concept2", concept2);
//			recordElem.setAttribute("label", label);
//			recordElem.setAttribute("lang", lang);
//		}
//		return response;
//	}
//	
//	/**
//	 * Returns a list of records concept1-concept2-label-lang, of concepts that have the same skosxl:prefLabel
//	 * in the same language
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
//	public Response listConceptsWithSameSKOSXLPrefLabel() {
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT ?concept1 ?concept2 ?label1 ?lang WHERE {\n"
//				+ "?concept1 a <" + SKOS.CONCEPT + "> .\n"
//				+ "?concept2 a <" + SKOS.CONCEPT + "> .\n"
//				+ "?concept1 <" + SKOSXL.PREF_LABEL + "> ?xlabel1 .\n"
//				+ "?concept2 <" + SKOSXL.PREF_LABEL + "> ?xlabel2 .\n"
//				+ "?xlabel1 <" + SKOSXL.LITERAL_FORM + "> ?label1 .\n"
//				+ "?xlabel2 <" + SKOSXL.LITERAL_FORM + "> ?label2 .\n"
//				+ "FILTER (?label1 = ?label2)\n"
//				+ "FILTER (str(?concept1) < str(?concept2))\n"
//				+ "bind(lang(?label1) as ?lang) }";
//		logger.debug("query [listConceptsWithSameSKOSXLPrefLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			String concept1 = tb.getBinding("concept1").getValue().stringValue();
//			String concept2 = tb.getBinding("concept2").getValue().stringValue();
//			String label = tb.getBinding("label1").getValue().stringValue();
//			String lang = tb.getBinding("lang").getValue().stringValue();
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			recordElem.setAttribute("concept1", concept1);
//			recordElem.setAttribute("concept2", concept2);
//			recordElem.setAttribute("label", label);
//			recordElem.setAttribute("lang", lang);
//		}
//		return response;
//	}
//	
	
	/**
	 * Returns a list of concepts or schemes that have no skos:prefLabel
	 * @return a list of concepts or schemes that have no skos:prefLabel
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithNoSKOSPrefLabel() {
		String q = "SELECT ?resource WHERE {\n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPT_SCHEME + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.COLLECTION + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.ORDERED_COLLECTION + "> . }\n"
				+ "FILTER NOT EXISTS {\n"
				+ "?resource <" + SKOS.PREF_LABEL + "> ?prefLabel .\n"
				+ "} } GROUP BY ?resource";
		logger.debug("query [listResourcesWithNoSKOSPrefLabel]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Returns a list of concepts/schemes/collections that have no skosxl:prefLabel
	 * @return a list of concepts/schemes/collections that have no skosxl:prefLabel
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithNoSKOSXLPrefLabel()  {
		String q = "SELECT ?resource WHERE {\n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPT_SCHEME + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.COLLECTION + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.ORDERED_COLLECTION + "> . }\n"
				+ "FILTER NOT EXISTS {\n"
				+ "?resource <" + SKOSXL.PREF_LABEL + "> ?prefLabel .\n"
				+ "} } GROUP BY ?resource";
		logger.debug("query [listResourcesWithNoSKOSXLPrefLabel]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
//	
//	/**
//	 * Returns a list of records resource-label-lang. A record like that means that the concept ?concept has 
//	 * the same skos:prefLabel and skos:altLabel ?label in language ?lang
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
//	public Response listResourcesWithOverlappedSKOSLabel()  {
//		String q = "SELECT ?resource ?label ?lang ?type WHERE {\n"
//				+ "?resource a ?type .\n"
//				+ "?resource <" + SKOS.PREF_LABEL + "> ?label .\n"
//				+ "?resource <" + SKOS.ALT_LABEL + "> ?label .\n"
//				+ "bind(lang(?label) as ?lang) . }";
//		logger.debug("query [listResourcesWithOverlappedSKOSLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			IRI resource = (IRI) tb.getValue("resource");
//			String label = tb.getValue("label").stringValue();
//			String lang = tb.getValue("lang").stringValue();
//			String type = tb.getValue("type").stringValue();
//			RDFResourceRole role = RDFResourceRole.concept;
//			if (type.equals(SKOS.CONCEPT)) {
//				role = RDFResourceRole.concept;
//			} else if (type.equals(SKOS.CONCEPT_SCHEME)) {
//				role = RDFResourceRole.conceptScheme;
//			}
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			addResourceToElement(recordElem,resource, role, resource.stringValue());
//			
//			addLiteralToElement(recordElem, conn.getValueFactory().createLiteral(label, lang));
//		}
//		return response;
//	}
//	
//	/**
//	 * Returns a list of records concept-label-lang. A record like that means that the concept ?concept has 
//	 * the same skosxl:prefLabel and skosxl:altLabel ?label in language ?lang
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
//	public Response listResourcesWithOverlappedSKOSXLLabel()  {
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT ?resource ?type ?prefLabel ?altLabel ?literalForm ?lang WHERE {\n"
//				+ "?resource a ?type .\n"
//				+ "?resource <" + SKOSXL.PREF_LABEL + "> ?prefLabel .\n"
//				+ "?resource <" + SKOSXL.ALT_LABEL + "> ?altLabel .\n"
//				+ "?prefLabel <" + SKOSXL.LITERAL_FORM + "> ?literalForm .\n"
//				+ "?altLabel <" + SKOSXL.LITERAL_FORM + "> ?literalForm .\n"
//				+ "bind(lang(?literalForm) as ?lang) . }";
//		logger.debug("query [listResourcesWithOverlappedSKOSXLLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			IRI resource = (IRI) tb.getValue("resource");
//			IRI type = (IRI) tb.getValue("type");
//			Resource prefLabel = (Resource) tb.getValue("prefLabel");
//			Resource altLabel = (Resource) tb.getValue("altLabel");
//			String literalForm = tb.getValue("literalForm").stringValue();
//			String lang = tb.getValue("lang").stringValue();
//			
//			RDFResourceRole role = RDFResourceRole.concept;
//			if (type.equals(SKOS.CONCEPT)) {
//				role = RDFResourceRole.concept;
//			} else if (type.equals(SKOS.CONCEPT_SCHEME)) {
//				role = RDFResourceRole.conceptScheme;
//			} else if (type.equals(SKOS.COLLECTION)) {
//				role = RDFResourceRole.skosCollection;
//			} else if (type.equals(SKOS.ORDERED_COLLECTION)) {
//				role = RDFResourceRole.skosOrderedCollection;
//			}
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			addResourceToElement(recordElem, resource, role, resource.stringValue());
//
//			
//			Element prefLabelElem = XMLHelp.newElement(recordElem, "prefLabel");
//			Element resPrefLabelElem = addResourceToElement(prefLabelElem, prefLabel, RDFResourceRole.xLabel, literalForm);
//			resPrefLabelElem.setAttribute("lang", lang);
//			
//			Element altLabelElem = XMLHelp.newElement(recordElem, "altLabel");
//			Element resAltLabelElem = addResourceToElement(altLabelElem, altLabel, RDFResourceRole.xLabel, literalForm);
//			resAltLabelElem.setAttribute("lang", lang);
//		}
//		return response;
//	}
//	
//	/**
//	 * Returns a list of records concept-labelPred-label-lang. A record like that means that
//	 * that the concept ?concept has the skos label ?label in language ?lang for the predicates ?labelPred that
//	 * contains some extra whitespace (at the begin, at the end or multiple whitespace between two words)
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
//	public Response listConceptsWithExtraWhitespaceInSKOSLabel() {
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT ?concept ?labelPred ?label ?lang WHERE {\n"
//				+ "{ bind(<" + SKOS.PREF_LABEL + "> as ?labelPred)}\n"
//				+ "UNION\n"
//				+ "{bind(<" + SKOS.ALT_LABEL + "> as ?labelPred)}\n"
//				+ "?concept ?labelPred ?skoslabel .\n"
//				+ "bind(str(?skoslabel) as ?label)\n"
//				+ "FILTER (regex (?label, '^ +') || regex (?label, ' +$') || regex(?label, ' {2,}?'))\n"
//				+ "bind(lang(?skoslabel) as ?lang) }";
//		logger.debug("query [listConceptsWithExtraWhitespaceInSKOSLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			String concept = tb.getBinding("concept").getValue().stringValue();
//			String labelPred = tb.getBinding("labelPred").getValue().stringValue();
//			String label = tb.getBinding("label").getValue().stringValue();
//			String lang = tb.getBinding("lang").getValue().stringValue();
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			recordElem.setAttribute("concept", concept);
//			recordElem.setAttribute("labelPred", labelPred);
//			recordElem.setAttribute("label", label);
//			recordElem.setAttribute("lang", lang);
//		}
//		return response;
//	}
//	
//	/**
//	 * Returns a list of records concept-labelPred-label-lang. A record like that means that
//	 * that the concept ?concept has the skosxl label ?label in language ?lang for the predicates ?labelPred that
//	 * contains some extra whitespace (at the begin, at the end or multiple whitespace between two words)
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
//	public Response listConceptsWithExtraWhitespaceInSKOSXLLabel() {
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT ?concept ?labelPred ?label ?lang WHERE {\n"
//				+ "{ bind(<" + SKOSXL.PREF_LABEL + "> as ?labelPred)}\n"
//				+ "UNION\n"
//				+ "{bind(<" + SKOSXL.ALT_LABEL + "> as ?labelPred)}\n"
//				+ "?concept ?labelPred ?xlabel .\n"
//				+ "?xlabel <" + SKOSXL.LITERAL_FORM + "> ?litForm .\n"
//				+ "bind(str(?litForm) as ?label)\n"
//				+ "FILTER (regex (?label, '^ +') || regex (?label, ' +$') || regex(?label, ' {2,}?'))\n"
//				+ "bind(lang(?litForm) as ?lang) }";
//		logger.debug("query [listConceptsWithExtraWhitespaceInSKOSXLLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			String concept = tb.getBinding("concept").getValue().stringValue();
//			String labelPred = tb.getBinding("labelPred").getValue().stringValue();
//			String label = tb.getBinding("label").getValue().stringValue();
//			String lang = tb.getBinding("lang").getValue().stringValue();
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			recordElem.setAttribute("concept", concept);
//			recordElem.setAttribute("labelPred", labelPred);
//			recordElem.setAttribute("label", label);
//			recordElem.setAttribute("lang", lang);
//		}
//		return response;
//	}
	
	//ST-264
	/**
	 * Returns a list of dangling labels, namely the labels not linked with any concept
	 * @return a list of dangling labels, namely the labels not linked with any concept
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(xLabel)', 'R')")
	public Collection<AnnotatedValue<Resource>> listDanglingXLabels() {
		String q = "SELECT ?resource WHERE {\n"
				+ "?subLabel <" + RDFS.SUBCLASSOF + ">* <" + SKOSXL.LABEL + "> .\n" 
				+ "?resource a ?subLabel .\n"
				+ "FILTER NOT EXISTS {\n" 
				+ "?concept <" + SKOSXL.PREF_LABEL + "> ?resource.\n"
				+ "}\n"
				+ "FILTER NOT EXISTS {\n" 
				+ "?concept <" + SKOSXL.ALT_LABEL + "> ?resource.\n"
				+ "}\n"
				+ "FILTER NOT EXISTS {\n" 
				+ "?concept <" + SKOSXL.HIDDEN_LABEL + "> ?resource.\n"
				+ "}\n"
				+ "} GROUP BY ?resource";
		logger.debug("query [listDanglingXLabels]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
	//ST-101 FR-O-112
	/**
	 * Return a list of resources with skos:altLabel(s) (or skosxl:altLabel) but not a corresponding 
	 * skos:prefLabel (or skos:prefLabel) for the same language locale. 
	 * @param rolesArray an array containing all the roles to which the desired resource should belong to 
	 * @return a list of resources with skos:altLabel(s) (or skosxl:altLabel) but not a corresponding 
	 * skos:prefLabel (or skos:prefLabel) for the same language locale. 
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithAltNoPrefLabel(RDFResourceRole[] rolesArray) 
			throws UnsupportedLexicalizationModelException  {
		IRI lexModel = getProject().getLexicalizationModel();
		
		if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}
		String q = "SELECT DISTINCT ?resource (GROUP_CONCAT(DISTINCT ?lang; separator=\",\") AS ?attr_missingLang)\n"
				+ "WHERE {\n";
		
		q += rolePartForQuery(rolesArray, "?resource", true);
		
		if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
			q+= "?resource " + NTriplesUtil.toNTriplesString(SKOSXL.ALT_LABEL) +" ?altLabel . \n" 
				+ "?altLabel "+ NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM) + " ?altTerm . \n";
		} else {
			q+= "?resource " + NTriplesUtil.toNTriplesString(SKOS.ALT_LABEL) +" ?altTerm . \n"; 
		}
			q+= "bind (lang(?altTerm) as ?lang) .\n"
				+ "FILTER NOT EXISTS { \n";
			
		if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
			q+=  "?resource " + NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL) +" ?prefLabel . \n" 
				+ "?prefLabel "+ NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM) + " ?prefTerm . \n";
		} else {
			q+=  "?resource " + NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL) +" ?prefTerm . \n";
		}
		q += "FILTER(lang(?prefTerm) = ?lang)"
				+ "}\n"
				+ "}\n"
				+"GROUP BY ?resource ";
				
				
		logger.debug("query [listConceptNoSkosxlPrefLang]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
	//ST-834
	/**
	 * Return a list of resources with no lexicalization (rdfs:label, skos:prefLabel or skosxl:prefLabel)
	 *  in one or more input languages
	 * @param rolesArray an array containing all the roles to which the desired resource should belong to
	 * @param languagesArray an array containing all languages in which to look for no lexicalization 
	 * @return a list of resources with no lexicalization (rdfs:label, skos:prefLabel or skosxl:prefLabel)
	 *  in one or more input languages
 	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesNoLexicalization(RDFResourceRole[] rolesArray, 
			String[] languagesArray)  {
		
		String query = "SELECT DISTINCT ?resource (GROUP_CONCAT(DISTINCT ?lang; separator=\",\") AS ?attr_missingLang)\n"
				+ "WHERE {\n";
		
		//now look for the roles
		query+=rolePartForQuery(rolesArray, "?resource", true);
		
		//now add the part that, using the lexicalization model, search for resources not having a language
		IRI lexModel = getProject().getLexicalizationModel();
		boolean first = true;
		String union = "";
		for(String lang : languagesArray) {
			if(!first) {
				union = "UNION\n";
			}
			first=false;
			if(lexModel.equals(Project.RDFS_LEXICALIZATION_MODEL)) {
				query+=union+"{ \n"
						+"?resource a ?fakeType .\n" // otherwise the FILTER NOT EXISTS does not work
						+"BIND('"+lang+"' as ?lang)\n"
						+ "FILTER NOT EXISTS { \n"
						+"?resource "+NTriplesUtil.toNTriplesString(RDFS.LABEL)+" ?label .\n"
						+"FILTER(lang(?label) = ?lang)\n"
						+ "}\n}";
			} else if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL)) {
				query+=union+"{ \n"
						+"?resource a ?fakeType .\n" // otherwise the FILTER NOT EXISTS does not work
						+"BIND('"+lang+"' as ?lang)\n"
						+ "FILTER NOT EXISTS { \n"
						+"?resource "+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+" ?label .\n"
						+"FILTER(lang(?label) = ?lang)\n"
						+ "}\n}";
			} else if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
				query+=union+"{ \n"
						+"?resource a ?fakeType .\n" // otherwise the FILTER NOT EXISTS does not work
						+"BIND('"+lang+"' as ?lang)\n"
						+ "FILTER NOT EXISTS { \n"
						+"?resource "+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+" ?xlabel .\n"
						+"?xlabel "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?label .\n"
						+"FILTER(lang(?label) = ?lang)\n"
						+ "}\n}";
			} 
		}
		
		query+="}\n"
				+ "GROUP BY ?resource ";
				
		logger.debug("query [listResourcesNoLexicalization]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
	/**
	 * Return a list of concepts mapped to each other using both skos:exactMatch and one of skos:broadMatch 
	 * or skos:relatedMatch mapping properties as the exactMatch relation is disjoint with both broadMatch 
	 * and relatedMatch
	 * @return a list of concepts mapped to each other using both skos:exactMatch and one of skos:broadMatch 
	 * or skos:relatedMatch mapping properties as the exactMatch relation is disjoint with both broadMatch 
	 * and relatedMatch
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Collection<AnnotatedValue<Resource>> listConceptsExactMatchDisjoint()  {
		String query = "SELECT DISTINCT ?resource \n"
				+ "WHERE {\n"
				+ "?resource a "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT) +" . \n"
				+ "?concept2 a "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT) +" . \n"
				+ " ?resource " +NTriplesUtil.toNTriplesString(SKOS.EXACT_MATCH) +" ?concept2 .\n"
				
				+ "{?resource "+NTriplesUtil.toNTriplesString(SKOS.BROAD_MATCH) +" ?concept2 . }\n"
				+ "UNION \n"
				+ "{?resource "+NTriplesUtil.toNTriplesString(SKOS.RELATED_MATCH) +" ?concept2 . }\n"
				+ "UNION \n"
				+ "{?concept2 "+NTriplesUtil.toNTriplesString(SKOS.BROAD_MATCH) +" ?resource . }\n"
				+ "UNION \n"
				+ "{?concept2 "+NTriplesUtil.toNTriplesString(SKOS.RELATED_MATCH) +" ?resource . }\n";
				
		
		query+="}\n"
				+ "GROUP BY ?resource ";
		
		logger.debug("query [listConceptsExactMatchDisjoint]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Return a list of concepts connected to each other with both the skos:related and the 
	 * skos:broaderTransitive as the skos:related relation is disjoint with skos:broaderTransitive
	 * (it consider also all their subproperties with the transitive closure)
	 * @return a list of concepts connected to each other with both the skos:related and the 
	 * skos:broaderTransitive as the related relation is disjoint with broaderTransitive
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Collection<AnnotatedValue<Resource>> listConceptsRelatedDisjoint()  {
		String query = "SELECT DISTINCT ?resource "
				+ "\nWHERE {"

				// get the subProperties of skos:related
				+"\n{SELECT ?relProp "
				+"\nWHERE {"
				+"\n?relProp "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+NTriplesUtil.toNTriplesString(SKOS.RELATED)+" ."
				+"\n}"
				+"\n}"

				//get the subProperties of skos:broaderTransitive
				+"\n{SELECT ?broadProp "
				+"\nWHERE {"
				+"\n?broadProp "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+NTriplesUtil.toNTriplesString(SKOS.BROADER_TRANSITIVE)+" ."
				+"\n}"
				+"\n}"

				//get all the subclasses of skos:Concept (twice)
				+"\n{SELECT ?conceptClass1 "
				+"\nWHERE {"
				+"\n?conceptClass1 "+NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+"* "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT)+" ."
				+"\n}"
				+"\n}"

				+"\n{SELECT ?conceptClass2 "
				+"\nWHERE {"
				+"\n?conceptClass2 "+NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+"* "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT)+" ."
				+"\n}"
				+"\n}"


				+ "\n?resource a ?conceptClass1 . "
				+ "\n?concept2 a ?conceptClass2 . "

				+ "\n?resource ?relProp ?concept2 ."
				
				+ "\n{?resource ?broadProp ?concept2 . }"
				+ "\nUNION "
				+ "\n{?concept2 ?broadProp ?resource . }";
				
		
		query+="\n}"
				+ "\nGROUP BY ?resource ";

		logger.debug("query [listConceptsRelatedDisjoint]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.setIncludeInferred(true);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
	//ST-735 FR-O-114
	/**
	 * Return a list of resources that have more than one skosxl:prefLabel for the same language locale
	 * @param rolesArray an array containing all the roles to which the desired resource should belong to
	 * @return a list of resources that have more than one skosxl:prefLabel for the same language locale
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithMorePrefLabelSameLang(RDFResourceRole[] rolesArray) 
			throws UnsupportedLexicalizationModelException  {
		IRI lexModel = getProject().getLexicalizationModel();
		if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}
		
		String query = "SELECT DISTINCT ?resource (GROUP_CONCAT(DISTINCT ?lang; separator=\",\") AS ?attr_duplicateLang)\n"
				+ "WHERE {\n";
		
		query+=rolePartForQuery(rolesArray, "?resource", true);
		
		if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)){
			query += "?resource "+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+" ?xlabel1 .\n"
					+ "?resource "+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+" ?xlabel2 .\n"
					+ "FILTER(?xlabel1 != ?xlabel2) \n"
					+ "?xlabel1 "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?label1 .\n"
					+ "?xlabel2 "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?label2 .\n";
		} else {
			query += "?resource "+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+" ?label1 .\n"
					+ "?resource "+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+" ?label2 .\n"
					+ "FILTER(?label1 != ?label2) \n";
		}
		query += "FILTER(lang(?label1) = lang(?label2)) \n"
				+ "BIND(lang(?label1) AS ?lang) \n"
				+"}\n"
				+ "GROUP BY ?resource ";
		
		logger.debug("query [listResourcesWithMorePrefLabelSameLang]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
	//ST-739 FR-New-ICV.1
	/**
	 * Return a list of resources that have a SKOS/SKOSXL label without any language tag 
	 * @param rolesArray an array containing all the roles to which the desired resource should belong to
	 * @return a list of resources that have a SKOS/SKOSXL label without any language tag 
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithNoLanguageTagForLabel(RDFResourceRole[] rolesArray) {
		IRI lexModel = getProject().getLexicalizationModel();
		/*if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}*/
		
		String query = "SELECT DISTINCT ?resource ?attr_xlabel ?attr_label \n"
				+ "WHERE {\n";
		
		query+=rolePartForQuery(rolesArray, "?resource", true);
		
		if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)){
			query += "?resource ("+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+"|"+
						NTriplesUtil.toNTriplesString(SKOS.ALT_LABEL)+") ?attr_xlabel .\n"
					+ "?attr_xlabel "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?attr_label .\n";
		} else if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL)) {
			query += "?resource ("+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+"|"+
						NTriplesUtil.toNTriplesString(SKOS.ALT_LABEL)+") ?attr_label .\n";
		} else {
			query += "?resource "+NTriplesUtil.toNTriplesString(RDFS.LABEL)+" ?attr_label .\n";
		}
		query += "FILTER(lang(?attr_label) = '') \n"
				+"}\n"
				+ "GROUP BY ?resource ?attr_xlabel ?attr_label";
		
		logger.debug("query [listResourcesWithNoLanguageTagForLabel]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Return a list of resources with extra whitespace(s) in skos(xl):label(s) annotation properties
	 * @param rolesArray an array containing all the roles to which the desired resource should belong to
	 * @return a list of resources with extra whitespace(s) in skos(xl):label(s) annotation properties
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithExtraSpacesInLabel(RDFResourceRole[] rolesArray) {
		IRI lexModel = getProject().getLexicalizationModel();
		
		/*if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}*/
		
		String query = "SELECT ?resource ?attr_xlabel ?attr_label \n"
				+ "WHERE {\n";
		
		query+=rolePartForQuery(rolesArray, "?resource", true);
		
		if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)){
			query += "?resource ("+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+"|"+
						NTriplesUtil.toNTriplesString(SKOSXL.ALT_LABEL)+") ?attr_xlabel .\n"
					+ "?attr_xlabel "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?attr_label .\n";
		} else if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL) ){
			query += "?resource ("+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+"|"+
						NTriplesUtil.toNTriplesString(SKOS.ALT_LABEL)+") ?attr_label .\n";
		} else {
			query += "?resource "+NTriplesUtil.toNTriplesString(RDFS.LABEL)+" ?attr_label .\n";
		}
		query += "FILTER (regex (?attr_label, '^ +') || regex (?attr_label, ' +$') || regex(?attr_label, '  '))\n"
				+"}\n"
				+ "GROUP BY ?resource ?attr_xlabel ?attr_label";
		
		logger.debug("query [listResourcesWithExtraSpacesInLabel]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Return a list of different resources where each resource belong to the same scheme as another resource
	 * and these two resources have the same skos:prefLabel or skosxl:prefLabel/skosxl:literalForm
	 * @param rolesArray an array containing all the roles to which the desired resource should belong to
	 * @return a list of different resources where each resource belong to the same scheme as another resource
	 * and these two resources have the same skos:prefLabel or skosxl:prefLabel/skosxl:literalForm
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithSameLabels(RDFResourceRole[] rolesArray) 
			throws UnsupportedLexicalizationModelException  {
		IRI lexModel = getProject().getLexicalizationModel();
		
		//pass one role at a time
		Collection<AnnotatedValue<Resource>> annValueList = new ArrayList<>();
		for(RDFResourceRole role : rolesArray){
			RDFResourceRole[] tempRolesArray = {role};
			annValueList.addAll(listResourcesWithSameLabels(tempRolesArray, lexModel));
		}
		return annValueList;
	}
	
	private Collection<AnnotatedValue<Resource>> listResourcesWithSameLabels(RDFResourceRole[] rolesArray, IRI lexModel) 
			throws UnsupportedLexicalizationModelException  {
		if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}
		String query = "SELECT DISTINCT ?resource ?attr_xlabel ?attr_label"
				+ "\nWHERE {";
		
		query+=rolePartForQuery(rolesArray, "?resource", true);
		//query+=rolePartForQuery(rolesArray, "?resource2"); // old
		query+="\n?propScheme "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
				NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME)+" .";
		if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)){
			query += "\n?resource "+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+" ?attr_xlabel ."
					+ "\n?attr_xlabel "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?attr_label ."
					+ "\n?attr_xlabel2 "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?attr_label ."
					+ "\n?resource2 "+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+" ?attr_xlabel2 ."
					+ rolePartForQuery(rolesArray, "?resource2", false);
			if(rolesArray!=null && rolesArray.length==1 && rolesArray[0].equals(RDFResourceRole.concept)) {
					query+= "\n?resource ?propScheme ?scheme . "
							+ "\n?resource2 ?propScheme ?scheme . ";
			}
		} else { //if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL) ){
			query += "\n?resource "+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+" ?attr_label ."
					+ "\n?resource2 "+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+" ?attr_label ."
					+ rolePartForQuery(rolesArray, "?resource2", false);
			if(rolesArray!=null && rolesArray.length==1 && rolesArray[0].equals(RDFResourceRole.concept)) {
				query+= "\n?resource ?propScheme ?scheme .  "
					+ "\n?resource2 ?propScheme ?scheme . ";
			}
		}
		
		query += "\nFILTER(?resource != ?resource2)"
				+"\n}"
				+ "\nGROUP BY ?resource ?attr_xlabel ?attr_label";
		
		logger.debug("query [listResourcesWithSameLabels]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
	/**
	 * Return a list of resources with overlapped lexicalization (resource with same label multiple times)
	 * @param rolesArray an array containing all the roles to which the desired resource should belong to
	 * @return a list of resources with overlapped lexicalization (resource with same label multiple times)
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithOverlappedLabels(RDFResourceRole[] rolesArray) 
			throws UnsupportedLexicalizationModelException  {
		IRI lexModel = getProject().getLexicalizationModel();
		
		if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}
		String query = "SELECT DISTINCT ?resource ?attr_xlabel ?attr_label \n"
				+ "WHERE {\n";
		query+=rolePartForQuery(rolesArray, "?resource", true);
		if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)){
			query += "?resource "+getSkosxlPrefOrAltOrHidden()+" ?attr_xlabel .\n"
					+ "?attr_xlabel "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?attr_label .\n"
					+ "?resource "+getSkosxlPrefOrAltOrHidden()+" ?attr_xlabel2 .\n"
					+ "?attr_xlabel2 "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?attr_label .\n"
					+ "FILTER(?attr_xlabel != ?attr_xlabel2)";
		} else if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL) ){
			query += "?resource "+getSkosPrefOrAltOrHidden()+" ?attr_label .\n"
					+ "?resource ?propLex1 ?attr_label .\n"
					+ "?resource ?propLex2 ?attr_label .\n"
					+ "FILTER(?propLex1 != ?propLex2)";
		}
		
		query += "}\n"
				+ "GROUP BY ?resource ?attr_xlabel ?attr_label";
		
		logger.debug("query [listResourcesWithOverlappedLabels]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
	/**
	 * Return a list of resources not having the property skos:definition for the given languages
	 * @param rolesArray an array containing all the roles to which the desired resource should belong to
	 * @param languagesArray an array containing all languages in which to look for the definitions
	 * @return a list of resources not having the property skos:definition for the given languages
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesNoDef(RDFResourceRole[] rolesArray,
			String[] languagesArray) {
		//IRI lexModel = getProject().getLexicalizationModel();
		
		/*if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}*/
		
		String query = "SELECT ?resource (GROUP_CONCAT(DISTINCT ?lang; separator=\",\") AS ?attr_missingLang) \n"
				+ "WHERE {\n";
		
		query+=rolePartForQuery(rolesArray, "?resource", true);
		
		
		boolean first = true;
		String union = "";
		for(String lang : languagesArray) {
			if(!first) {
				union = "UNION\n";
			}
			first=false;
			query+=union+"{ \n"
					+"?resource a ?fakeType .\n" // otherwise the FILTER NOT EXISTS does not work
					+"BIND('"+lang+"' as ?lang)\n"
					+ "FILTER NOT EXISTS { \n"
					+ "{ ?resource "+NTriplesUtil.toNTriplesString(SKOS.DEFINITION)+ "?definition . } \n"
					+" UNION\n"
					+ "{ ?resource "+NTriplesUtil.toNTriplesString(SKOS.DEFINITION)+ "?definitionRef . "
					+ "?definitionRef "+NTriplesUtil.toNTriplesString(RDF.VALUE)+"?definition . } \n"
					+"FILTER(lang(?definition) = ?lang)\n"
					+ "}\n}";
		}
		query += "}\n"
				+ "GROUP BY ?resource ?attr_xlabel ?attr_label";
		
		logger.debug("query [listResourcesNoDef]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
	/**
	 * Return a list of concepts belong to  hierarchical cyclic
	 * @return a list of concepts belong to  hierarchical cyclic
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	//public Collection<AnnotatedValue<Resource>> listConceptsHierarchicalCycles() {
	public JsonNode listConceptsHierarchicalCycles() {
		//IRI lexModel = getProject().getLexicalizationModel();
		
		/*if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}*/
		
		String query = "SELECT ?resource ?attr_broader_concept \n"
				+ "WHERE {\n"
				+ "?resource a "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT)+" .\n"
				+ "?resource "+broaderOrInverseNarrower()+" ?attr_broader_concept .\n"
				+ "?attr_broader_concept "+broaderOrInverseNarrower()+"* ?resource .\n"
				+ "} \n"
				+ "GROUP BY ?resource ?attr_broader_concept";
		logger.debug("query [listConceptsHierarchicalCycles]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		//return qb.runQuery();
		
		Collection<AnnotatedValue<Resource>> annotatedValueList = qb.runQuery();
		
		//now do a post process of the results, to find the cycles
		
		//first of all, read all the result and prepare the two map containing all the relevant information
		Map<String, List<String>> conceptToBroadersConceptMap = new HashMap<>();
		Map<String, AnnotatedValue<Resource>> conceptToAnnotatedValue = new HashMap<>();
		for(AnnotatedValue<Resource>annotatedValue : annotatedValueList) {
			String concept = annotatedValue.getStringValue();
			//add the AnnotatedValue to the map
			conceptToAnnotatedValue.put(concept, annotatedValue);
			//add the concept and the broader to the map
			String broaderConcept = annotatedValue.getAttributes().get("broader_concept").stringValue();
			//remove the attribute "broader_concept" from the AnnotatedValue
			annotatedValue.getAttributes().remove("broader_concept");
			if(!conceptToBroadersConceptMap.containsKey(concept)) {
				conceptToBroadersConceptMap.put(concept, new ArrayList<>());
			}
			conceptToBroadersConceptMap.get(concept).add(broaderConcept);
		}
		
		//now iterate over conceptToBroadersConceptMap to extract the cycles (if any)
		List<List<String>> cyclesList = new ArrayList<>();
		for( String concept : conceptToBroadersConceptMap.keySet()) {
				calculateCycle(concept, new ArrayList<>(), conceptToBroadersConceptMap,
						cyclesList );
		}
		
		//remove the duplicate cycles
		List<List<String>> cyclesReduxList = removeDuplicateCycles(cyclesList);
		
		//now the duplicates cycles have been removed, so construct the answer
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		
		ArrayNode cycles = jsonFactory.arrayNode();
		for(List<String> conceptList: cyclesReduxList) {
			ArrayNode singleCycle = jsonFactory.arrayNode();
			for(String concept : conceptList) {
				singleCycle.addPOJO(conceptToAnnotatedValue.get(concept));
			}
			cycles.add(singleCycle);
		}
		
		return cycles;
	}
	
	private void calculateCycle(String concept, ArrayList<String> currentCycle,
			Map<String, List<String>> conceptToBroadersConceptMap,
			List<List<String>> cyclesList) {
		//check if the current concept is the first element of the currentCycle, in this case a cycle was 
		// found, so add it to the cyclesToConceptsInCycleList
		if(currentCycle.size()>0 && currentCycle.get(0).equals(concept)) {
			cyclesList.add(currentCycle);
			return;
		}
		
		//check if the current concept is present in the currentCycle,in this case an inner cycle was found,
		// return without doing nothing (this cycle will be found by starting from this concept)
		if(currentCycle.contains(concept)) {
			return;
		}
		
		//add concept to currentCycle
		currentCycle.add(concept);
		
		//the cycle is not completed, so get the broader of the current concept
		List<String> broaderList = conceptToBroadersConceptMap.get(concept);
		for(String broader : broaderList) {
			//clone the currentCycle, then add the concept and call calculateCycle
			calculateCycle(broader, new ArrayList<>(currentCycle), conceptToBroadersConceptMap, 
					cyclesList);
		}
	}

	private List<List<String>> removeDuplicateCycles(List<List<String>> cyclesList) {
		List<List<String>> cyclesReduxList = new ArrayList<>();
		for(int i=0; i<cyclesList.size(); ++i) {
			boolean toBeAdded = true;
			for(int k=i+1; k<cyclesList.size(); ++k) {
				//check if cycles i and k contain the same elements or not
				if(compareCycles(cyclesList.get(i), cyclesList.get(k))) {
					toBeAdded=false;
					break;
				}
			}
			if(toBeAdded) {
				cyclesReduxList.add(cyclesList.get(i));
			}
		}
		return cyclesReduxList;
	}
	
	private boolean compareCycles(List<String> cycle1, List<String> cycle2) {
		if(cycle1.size() != cycle2.size()) {
			//since they have different sizes, they are different
			return false;
		}
		for (String s : cycle1) {
			if (!cycle2.contains(s)) {
				// the i element of cycle1 is not contained in cycle2, so they do not contain the same
				// elements
				return false;
			}
		}
		//the cycles contains the same elements
		return true;
	}
	
	//ST-194
	/**
	 * Return a list of triples that are redundant from the hierarchical point of view
	 * @param sameScheme true to look only on the same scheme (optional value, its default value is true), 
	 * false otherwise
	 * @return a list of triples that are redundant from the hierarchical point of view
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	//public Collection<AnnotatedValue<Resource>> listConceptsHierarchicalCycles() {
	public JsonNode listConceptsHierarchicalRedundancies(@Optional(defaultValue="true") boolean sameScheme) 
			throws UnsupportedLexicalizationModelException {
		IRI lexModel = getProject().getLexicalizationModel();
		
		if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}
		
		String query = "SELECT ?resource ?attr_concept ?attr_other_concept ?attr_predicate \n"
				+ "WHERE {\n"
				+ "?attr_concept a "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT)+" .\n"
				+ "?attr_concept "+broaderOrInverseNarrower()+" ?broader_concept .\n"
				+ "?broader_concept "+broaderOrInverseNarrower()+"+ ?attr_other_concept .\n"
				+ "FILTER(?broader_concept != ?attr_other_concept)\n"
				+ "?attr_concept "+broaderOrInverseNarrower()+" ?attr_other_concept .\n";
		if(sameScheme) {
			query += "?attr_concept "+NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME)+" ?scheme .\n"
					+"?broader_concept "+NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME)+" ?scheme .\n"
					+"?attr_other_concept "+NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME)+" ?scheme .\n";
		}
		//now check if the used property is BROADER or NARROWER
		query+= "{?attr_concept "+NTriplesUtil.toNTriplesString(SKOS.BROADER)+" ?attr_other_concept .\n"
				+ "BIND( "+NTriplesUtil.toNTriplesString(SKOS.BROADER)+"AS ?attr_predicate)}\n"
				+ "UNION\n"
				+ "{?attr_concept ^"+NTriplesUtil.toNTriplesString(SKOS.NARROWER)+" ?attr_other_concept .\n"
				+ "BIND( "+NTriplesUtil.toNTriplesString(SKOS.NARROWER)+"AS ?attr_predicate)}\n"
		//now bind the three elements (?attr_concept, ?attr_predicate and ?attr_other_concept )
				+ "{BIND(?attr_concept AS ?resource)\n"
				+ "?resource "+broaderOrInverseNarrower()+" ?attr_other_concept .}\n"
				+ "UNION\n"
				+ "{BIND(?attr_predicate AS ?resource)\n"
				+ "?resource ?useless_prep ?useless_obj}\n"
				+ "UNION\n"
				+ "{BIND(?attr_other_concept AS ?resource)\n"
				+ "?attr_concept "+broaderOrInverseNarrower()+" ?resource .}\n"

				+ "} \n"
				+ "GROUP BY ?resource ?attr_concept ?attr_other_concept ?attr_predicate ";
		logger.debug("query [listConceptsHierarchicalRedundancies]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		//return qb.runQuery();
		
		Collection<AnnotatedValue<Resource>> annotatedValueList = qb.runQuery();
		//iterate over the response to construct the structure which will be used for the answer
		Map<String, TripleForAnnotatedValue> tripleForRedundancyMap = new HashMap<>();
		for(AnnotatedValue<Resource> annotatedValue: annotatedValueList) {
			String concept = annotatedValue.getAttributes().get("concept").stringValue();
			annotatedValue.getAttributes().remove("concept");
			String predicate = annotatedValue.getAttributes().get("predicate").stringValue();
			annotatedValue.getAttributes().remove("predicate");
			String other_concept = annotatedValue.getAttributes().get("other_concept").stringValue();
			annotatedValue.getAttributes().remove("other_concept");
			String key = concept+predicate+other_concept; 
			if(!tripleForRedundancyMap.containsKey(key)) {
				tripleForRedundancyMap.put(key, new TripleForAnnotatedValue());
			}
			TripleForAnnotatedValue tripleForRedundancy = tripleForRedundancyMap.get(key);
			//check the AnnotatedValue to which of its "elements"refer to
			String value = annotatedValue.getValue().stringValue();
			boolean invert = predicate.equals(NTriplesUtil.toNTriplesString(SKOS.NARROWER));
			if (value.equals(concept)) {
				if(invert) {
					tripleForRedundancy.setObject(annotatedValue);
				} else {
					tripleForRedundancy.setSubject(annotatedValue);
				}
			} else if (value.equals(predicate)) {
				tripleForRedundancy.setPredicate(annotatedValue);
			} else { //value.equals(other_concept)
				if (invert) {
					tripleForRedundancy.setSubject(annotatedValue);
				} else {
					tripleForRedundancy.setObject(annotatedValue);
				}
			}
		}
		
		//now construct the response
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode redundancies = jsonFactory.arrayNode();
		for(TripleForAnnotatedValue tripleForRedundancy : tripleForRedundancyMap.values()) {
			ObjectNode singleRedundancy = jsonFactory.objectNode();
			singleRedundancy.putPOJO("subject", tripleForRedundancy.getSubject());
			singleRedundancy.putPOJO("predicate", tripleForRedundancy.getPredicate());
			singleRedundancy.putPOJO("object", tripleForRedundancy.getObject());
			redundancies.add(singleRedundancy);
		}
		return redundancies;
	}

	private class TripleForAnnotatedValue {
		AnnotatedValue<Resource> subject;
		AnnotatedValue<Resource> predicate;
		AnnotatedValue<Resource> object;
		
		public AnnotatedValue<Resource> getSubject() {
			return subject;
		}
		public void setSubject(AnnotatedValue<Resource> subject) {
			this.subject = subject;
		}
		public AnnotatedValue<Resource> getPredicate() {
			return predicate;
		}
		public void setPredicate(AnnotatedValue<Resource> predicate) {
			this.predicate = predicate;
		}
		public AnnotatedValue<Resource> getObject() {
			return object;
		}
		public void setObject(AnnotatedValue<Resource> object) {
			this.object = object;
		}
	}
	
	
	//ST-776 FR-O-98
	/**
	 * Return a list of namespaces of alignments concepts with the number of alignments per namespace
	 * @param rolesArray an array containing all the roles to which the desired resource should belong to
	 * @return a list of namespaces of alignments concepts with the number of alignments per namespace
	 * @throws ProjectAccessException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public JsonNode listAlignedNamespaces(RDFResourceRole[] rolesArray) throws ProjectAccessException {
		//IRI lexModel = getProject().getLexicalizationModel();
		
		/*if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}*/
		boolean first = true;
		String query = "SELECT ?namespace (count(?namespace) as ?count) \n"
				+ "WHERE {\n";
		
		boolean alreadyAddedMappingRel = false;
		String union = "";
		for(RDFResourceRole role : rolesArray) {
			if(!first) {
				union = "UNION\n";
			}
			first = false;
			if(role.equals(RDFResourceRole.concept) || role.equals(RDFResourceRole.conceptScheme) ||
					role.equals(RDFResourceRole.skosCollection)) {
				if(!alreadyAddedMappingRel) {
					query += union
						// ?propMapping rdfs:subPropertyOf skos:mappingRelation
						+"{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
							NTriplesUtil.toNTriplesString(SKOS.MAPPING_RELATION)+" . } \n";
				}
				alreadyAddedMappingRel=true;
			} else if(role.equals(RDFResourceRole.cls)) {
				query += union
						// ?propMapping rdfs:subPropertyOf owl:equivalentClass
						+ "{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.EQUIVALENTCLASS)+" . } \n"
						+ " UNION \n"
						// ?propMapping rdfs:subPropertyOf owl:disjointWith
						+ "{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.DISJOINTWITH)+" . } \n"
						+ " UNION \n"
						// ?propMapping rdfs:subPropertyOf rdfs:subClassOf
						+ "{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+" . } \n";
			} else if(role.equals(RDFResourceRole.property)) {
				query += union
						// ?propMapping rdfs:subPropertyOf owl:equivalentProperty
						+ "{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.EQUIVALENTPROPERTY)+" . } \n"
						//+ " UNION \n"
						//NTriplesUtil.toNTriplesString(OWL.PROPERTYDISJOINTWITH)+" . } \n"
						// ?propMapping rdfs:subPropertyOf owl:equivalentProperty
						+ " UNION \n"
						// ?propMapping rdfs:subPropertyOf rdfs.subPropertyOfy
						+ "{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+" . } \n";
			} else if(role.equals(RDFResourceRole.individual)) {
				query += union
						// ?propMapping rdfs:subPropertyOf owl:sameAs
						+ "{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.SAMEAS)+" . } \n"
						+ " UNION \n"
						// ?propMapping rdfs:subPropertyOf owl:differentFrom
						+ "{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.DIFFERENTFROM)+" . } \n";
			}
		}
		
		query += "?resource ?propMapping ?resource2 .\n";
		
		//now check the type of the ?resource
		query += getTypesFromRoles("?resource", rolesArray);
		
		query += "BIND(REPLACE(str(?resource2), '[^(#|/)]+$', \"\") AS ?namespace)\n"
				+ "}\n"
				+ "GROUP BY ?namespace";
		
		
		logger.debug("query [listAlignedNamespaces]:\n" + query);
		TupleQuery tupleQuery = getManagedConnection().prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		
		//now iterate over the result to create the response
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode responce = jsonFactory.arrayNode();
		SimpleValueFactory simpleValueFactory = SimpleValueFactory.getInstance();
		while(tupleQueryResult.hasNext()) {
			BindingSet bindingSet = tupleQueryResult.next();
			if(bindingSet.hasBinding("namespace")){
				//add the namespace and the count
				String namespace = bindingSet.getBinding("namespace").getValue().stringValue();
				String count = bindingSet.getBinding("count").getValue().stringValue();
				ObjectNode objectNode = jsonFactory.objectNode();
				objectNode.put("namespace", namespace);
				objectNode.put("count", count);
				//get all the associated location for the given namespace
				ArrayNode locationsNode = jsonFactory.arrayNode();
				List<ResourcePosition> resourcePositionList = 
						resourceLocator.listResourceLocations(getProject(), getRepository(), simpleValueFactory.createIRI(namespace));
				//iterate over the list of location to construct the response
				for(ResourcePosition resourcePosition : resourcePositionList) {
					ObjectNode locationNode = jsonFactory.objectNode();
					if(resourcePosition instanceof LocalResourcePosition) {
						locationNode.put("type", RepositoryLocation.Location.local.toString());
						LocalResourcePosition localResourcePosition = (LocalResourcePosition) resourcePosition;
						locationNode.put("name",localResourcePosition.getProject().getName());
					} else if (resourcePosition instanceof RemoteResourcePosition) {
						locationNode.put("type", RepositoryLocation.Location.remote.toString());
						RemoteResourcePosition remoteResourcePosition = (RemoteResourcePosition) resourcePosition;
						DatasetMetadata datasetMetadata = remoteResourcePosition.getDatasetMetadata();
						locationNode.put("title", datasetMetadata.getTitle().orElse(null));
						locationNode.put("sparqlEndpoint", datasetMetadata.getSparqlEndpoint().map(Value::stringValue).orElse(null));
						locationNode.put("dereferenceable", datasetMetadata.getDereferenciationSystem().map(ds -> true).orElse(false));
					}
					locationsNode.add(locationNode);
				}
				objectNode.set("locations", locationsNode);
				responce.add(objectNode);
			}
		}
		
		return responce;
	}
	
	private String getTypesFromRoles(String resource, RDFResourceRole[] rolesArray) {
		String query = resource+" a ?type . \n"
				+"FILTER(";
		boolean first = true;
		boolean isIndividualPresent = false;
		for(RDFResourceRole role : rolesArray) {
			if(role.equals(RDFResourceRole.individual)) {
				isIndividualPresent = true;
				continue;
			}
			if(!first) {
					query += " || ";
			}
			first = false;
			if(role.equals(RDFResourceRole.concept)) {
				query +="?type = "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT);
			} else if(role.equals(RDFResourceRole.conceptScheme)) {
				query +="?type = "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT_SCHEME);
			} else if(role.equals(RDFResourceRole.skosCollection)) {
				query +="?type = "+NTriplesUtil.toNTriplesString(SKOS.COLLECTION) + " || "
						+ "?type = "+NTriplesUtil.toNTriplesString(SKOS.ORDERED_COLLECTION);
			} else if(role.equals(RDFResourceRole.cls)) {
				query +="?type = "+NTriplesUtil.toNTriplesString(OWL.CLASS) + " || "
						+ "?type = "+NTriplesUtil.toNTriplesString(RDFS.CLASS);
			} else if(role.equals(RDFResourceRole.property)) {
				query += "?type = "+NTriplesUtil.toNTriplesString(RDF.PROPERTY)+" || "+
						"?type = "+NTriplesUtil.toNTriplesString(OWL.OBJECTPROPERTY)+" || "+
						"?type = "+NTriplesUtil.toNTriplesString(OWL.DATATYPEPROPERTY)+" || "+
						"?type = "+NTriplesUtil.toNTriplesString(OWL.ANNOTATIONPROPERTY)+" || " +
						"?type = "+NTriplesUtil.toNTriplesString(OWL.ONTOLOGYPROPERTY)+" ";
			} 
		}
		query += ")\n";
		if(isIndividualPresent) {
			String queryforInd = resource+" a ?type . \n"
					+"?type a "+NTriplesUtil.toNTriplesString(OWL.CLASS)+" .\n";
			
			if(rolesArray.length>1) {
			query = "{\n"+query+"}\n"
					+ "UNION \n"
					+ "{\n"
					+ queryforInd 
					+ "}\n";
			} else {
				query = queryforInd;
			}
		} 
		return query;
	}

	//ST-776 FR-O-98
	/**
	 * Return a list of triples of broken alignments
	 * @param nsToLocationMap a map to link namespace to location
	 * @param rolesArray an array containing all the roles to which the desired resource should belong to
	 * @return a list of triples of broken alignments
	 * @throws ProjectAccessException 
	 * @throws IOException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public JsonNode listBrokenAlignments(Map<String, String> nsToLocationMap, RDFResourceRole[] rolesArray) 
			throws ProjectAccessException {
		//the values of the map are one of the above:
		// - local:PROJECT_NAME
		// - remote:dereference
		// - remote:SPARQL_ENDPOINT
		
		//do a spqarql query to obtain all the resources in a mapping relations and consider just the resources
		boolean first = true;
		String query = "SELECT ?resource ?attr_subj ?attr_propMapping ?attr_obj\n"
				+ "WHERE {\n";
		boolean alreadyAddedMappingRel = false;
		String union="";
		for(RDFResourceRole role : rolesArray) {
			if(!first) {
				union = "UNION\n";
				
			}
			first = false;
			if(role.equals(RDFResourceRole.concept) || role.equals(RDFResourceRole.conceptScheme) ||
					role.equals(RDFResourceRole.skosCollection)) {
				if(!alreadyAddedMappingRel) {
					query += union
						// ?attr_propMapping rdfs:subPropertyOf skos:mappingRelation
						+ "{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
							NTriplesUtil.toNTriplesString(SKOS.MAPPING_RELATION)+" . } \n";
				}
				alreadyAddedMappingRel=true;
			} else if(role.equals(RDFResourceRole.cls)) {
				query += union
						// ?attr_propMapping rdfs:subPropertyOf owl:equivalentClass
						+ "{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.EQUIVALENTCLASS)+" . } \n"
						+ " UNION \n"
						// ?attr_propMapping rdfs:subPropertyOf owl:disjointWith
						+ "{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.DISJOINTWITH)+" . } \n"
						+ " UNION \n"
						// ?attr_propMapping rdfs:subPropertyOf rdfs:subClassOf
						+ "{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+" . } \n";
			} else if(role.equals(RDFResourceRole.property)) {
				query += union
						// ?attr_propMapping rdfs:subPropertyOf owl:equivalentProperty
						+ "{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.EQUIVALENTPROPERTY)+" . } \n"
						//+ " UNION \n"
						//NTriplesUtil.toNTriplesString(OWL.PROPERTYDISJOINTWITH)+" . } \n"
						// ?attr_propMapping rdfs:subPropertyOf owl:equivalentProperty
						+ " UNION \n"
						// ?attr_propMapping rdfs:subPropertyOf rdfs.subPropertyOfy
						+ "{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+" . } \n";
			} else if(role.equals(RDFResourceRole.individual)) {
				query += union
						// ?attr_propMapping rdfs:subPropertyOf owl:sameAs
						+ "{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.SAMEAS)+" . } \n"
						+ " UNION \n"
						// ?attr_propMapping rdfs:subPropertyOf owl:differentFrom
						+ "{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.DIFFERENTFROM)+" . } \n";
			}
		}
		
		query += "?attr_subj ?attr_propMapping ?attr_obj .\n"
				+ "FILTER isIRI(?attr_obj) \n"
				+ getTypesFromRoles("?attr_subj", rolesArray)
		
		//put ?attr_subj ?attr_propMapping ?obj in ?resource (so the will be in the annotated value)
				+ "{?attr_subj ?attr_propMapping ?attr_obj .\n" //added to have some results
				+ "BIND(?attr_subj AS ?resource)}\n"
				+ "UNION\n"
				+ "{?attr_subj ?attr_propMapping ?attr_obj .\n"//added to have some results
				+ "BIND(?attr_propMapping AS ?resource)}\n"
				+ "UNION\n"
				+ "{?attr_subj ?attr_propMapping ?attr_obj .\n" //added to have some results
				+ "BIND(?attr_obj AS ?resource)}\n"		
				
				+ "}\n"
				+ "GROUP BY ?resource ?attr_subj ?attr_propMapping ?attr_obj ";
		
		
		logger.debug("query [listBrokenAlignments]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		
		Collection<AnnotatedValue<Resource>> annotatedValueList = qb.runQuery();
		
		//iterate over the results of the query to filter out those triples which have an object not starting with 
		// one of the namespaces (or their redux versions). Those triples that pass the test, are placed in the 
		// data structure which will be use to check the alignments
		Map <String, TripleForAnnotatedValue> tripleForAnnotatedValueMap = new HashMap<>();
		for(AnnotatedValue<Resource> annotatedValue : annotatedValueList) {
			String subj = annotatedValue.getAttributes().get("subj").stringValue();
			annotatedValue.getAttributes().remove("subj");
			String propMapping = annotatedValue.getAttributes().get("propMapping").stringValue();
			annotatedValue.getAttributes().remove("propMapping");
			String obj = annotatedValue.getAttributes().get("obj").stringValue();
			annotatedValue.getAttributes().remove("obj");
			
			//check that the obj belong to one of the desired namespaces
			boolean found = false;
			for(String namespace : nsToLocationMap.keySet()) {
				if(obj.startsWith(namespace)) {
					found = true;
					break;
				}
			}
			if(!found) {
				//the obj does not belong to any desired namespace, so do not consider this triple (singular element)
				continue;
			}
			//the obj belongs to any desired namespace, so add this triple (singular element) to the map
			String key = subj+propMapping+obj; 
			if(!tripleForAnnotatedValueMap.containsKey(key)) {
				tripleForAnnotatedValueMap.put(key, new TripleForAnnotatedValue());
			}
			TripleForAnnotatedValue tripleForAnnotatedValue = tripleForAnnotatedValueMap.get(key);
			//check the AnnotatedValue to see which of its "elements"refer to
			String value = annotatedValue.getValue().stringValue();
			if(value.equals(subj)) {
				tripleForAnnotatedValue.setSubject(annotatedValue);
			} else if(value.equals(propMapping)) {
				tripleForAnnotatedValue.setPredicate(annotatedValue);
			} else { //value.equals(obj)
				tripleForAnnotatedValue.setObject(annotatedValue);
			}
		}
		
		
		//use the just created tripleForAnnotatedValueMap to construct a map linking the namespace (or redux version)
		// of the objct of the list of triple of annotatedValue (all the Triple having that namespace 
		// in their object)
		Map<String, List<TripleForAnnotatedValue>> namespaceToTripleMap = new HashMap<>();
		//first of all, create a map having the desired keys (the namespaces or redux versions)
		for(String namespace : nsToLocationMap.keySet()) {
			namespaceToTripleMap.put(namespace, new ArrayList<>());
		}
		//now fill the just created empty map namespaceToTripleMap
		for(TripleForAnnotatedValue tripleForAnnotatedValue : tripleForAnnotatedValueMap.values()) {
			for(String namespace : namespaceToTripleMap.keySet()) {
				if(tripleForAnnotatedValue.getObject().getValue().toString().startsWith(namespace)) {
					namespaceToTripleMap.get(namespace).add(tripleForAnnotatedValue);
					//now focus on the next triple
					break;
				}
			}
		}
		
		//prepare the empty response, which will be fill everytime a broken alignment is found
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode response = jsonFactory.arrayNode();
		
		//now iterate over the map namespaceToTripleMap, get the value of the location from the input map 
		// nsToLocationMap, and check every resource associated to that namespace
		//
		try {
			for(String namespace : namespaceToTripleMap.keySet()) {
				RepositoryConnection connectionToOtherRepository = null;
				String typeAndLocation = nsToLocationMap.get(namespace);
				if(typeAndLocation.startsWith(RepositoryLocation.Location.local.toString())) {
					String projName = typeAndLocation.split(RepositoryLocation.Location.local +":")[1];
					connectionToOtherRepository = acquireManagedConnectionToProject(getProject(), ProjectManager.getProject(projName));
				} else { // it is a remote alignments
					if(!typeAndLocation.equals("remote:dereference")) { // it is a SPARQL endpoint
						String sparqlEndPoint = typeAndLocation.split(
								RepositoryLocation.Location.remote +":")[1];
						if(sparqlEndPoint != null) {
							Repository sparqlRepository = new SPARQLRepository(sparqlEndPoint);
							sparqlRepository.init();
							connectionToOtherRepository = sparqlRepository.getConnection();
						}
					}
				}
				//now check each resource associated to the current namespace
				for( TripleForAnnotatedValue triple : namespaceToTripleMap.get(namespace)) {
					Resource obj = triple.getObject().getValue();
					
					if(connectionToOtherRepository != null) {
						query = "SELECT ?deprecated ?hasType"
								+" WHERE {\n"
								//check if the resource is deprecated
								+ "{ "+NTriplesUtil.toNTriplesString(obj)+ " "+
									NTriplesUtil.toNTriplesString(OWL.DEPRECATED)+" \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> .\n"
								+ "BIND(true AS ?deprecated )\n"
								+ "}\n"
								+ "UNION\n"
								//check if the resource has a type
								+ "{ "+NTriplesUtil.toNTriplesString(obj) + " a ?type .\n"  
								+ "BIND(true AS ?hasType)\n "
								+ "}\n"
								+ "}";
						logger.debug("query [listBrokenAlignments2]:\n" + query);
						boolean hasType = false;
						boolean isDeprecated = false;
						TupleQuery tupleQuery = connectionToOtherRepository.prepareTupleQuery(query);
						tupleQuery.setIncludeInferred(false);
						TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
						//analyze the response of the query to see if the resource has a type and/or is deprecated
						if(tupleQueryResult.hasNext()) {
							//it has at least a type or it is deprecated
							BindingSet bindingSet = tupleQueryResult.next();
							if(bindingSet.hasBinding("hasType")) {
								hasType = true;
							}
							if(bindingSet.hasBinding("deprecated")) {
								isDeprecated = true;
							}
						}
						//if the resource has no type or is deprecated, then return it (the triple from which the 
						// resource was taken)
						if(!hasType || isDeprecated) {
							ObjectNode singleBrokenAlign = jsonFactory.objectNode();
							singleBrokenAlign.putPOJO("subject", triple.getSubject());
							singleBrokenAlign.putPOJO("predicate", triple.getPredicate());
							triple.getObject().setAttribute("deprecated", isDeprecated);
							singleBrokenAlign.putPOJO("object", triple.getObject());
							response.add(singleBrokenAlign);
						}
					} else {
						//the connection to the repository (local or remote) is not possible, so use the 
						// HTTP connection
						//do an httpRequest to see if the which IRIs are associated to an existing web page
						IRI resource = (IRI) triple.getObject().getValue();
						boolean toAdd = false;
						HttpURLConnection con = null;
						try {
							URL url = new URL(resource.stringValue());
							con = (HttpURLConnection) url.openConnection();
							con.setRequestMethod("GET");
							con.setRequestProperty("Content-Type", "application/json");
							con.setConnectTimeout(5000);
							con.setReadTimeout(5000);
							//connect to the remote site
							con.connect();
							int code = con.getResponseCode();
							if(code!=200) {
								//the resource was not found, so this triple will be returned
								toAdd = true;
							}
						} catch (IOException e) {
							//the connection was not possible, so this triple will be returned
							toAdd = true;
						} finally {
							if(toAdd) {
								//if the page cannot be found, then the resource does not exist, so return it
								ObjectNode singleBrokenAlign = jsonFactory.objectNode();
								singleBrokenAlign.putPOJO("subject", triple.getSubject());
								singleBrokenAlign.putPOJO("predicate", triple.getPredicate());
								singleBrokenAlign.putPOJO("object", triple.getObject());
								response.add(singleBrokenAlign);
							}
							//close the connection with the remote site
							con.disconnect();
						}
						
					}
				}
			} 
			
		} finally {
			//check all the connections used and close them
			for (RepositoryConnection otherConn : projectConnectionHolder.get().values()) {
				try {
					otherConn.close();
				} catch (RepositoryException e) {
					logger.debug("Exception closing additional project", e);
				}
			}
			projectConnectionHolder.remove();
		}
		
		return response;
	}
	
	
	/**
	 * Return a list of triples of broken definitions (definition that are not defined in the local repository nor 
	 * can be resolved with a remote one using http dereferenciation )
	 * @param rolesArray an array containing all the roles to which the desired resource should belong to
	 * @param property the subProperty of skos:note to consider
	 * @return a list of triples of broken definitions (definition that are not defined in the local repository nor 
	 * can be resolved with a remote one using http dereferenciation )
	 * @throws ProjectAccessException 
	 * @throws IOException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public JsonNode listBrokenDefinitions(RDFResourceRole[] rolesArray,
			@SubPropertyOf(superPropertyIRI = "http://www.w3.org/2004/02/skos/core#note") IRI property) {
		
		//do a spqarql query to obtain all the resources linked with the desired property (or one of its 
		// subproperties)
		String query = "SELECT ?resource ?attr_subj ?attr_prop ?attr_obj\n"
				+ "WHERE {\n"
				+ "?attr_prop "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
					NTriplesUtil.toNTriplesString(property)+" .\n"
				+ "?attr_subj ?attr_prop ?attr_obj ."
				+ "FILTER isIRI(?attr_obj) \n"
				
				//exclude the obj defined in this ontology
				+ "MINUS{?attr_obj a ?type} \n"
				
				//filter the subject according to the input rolesArray
				+ getTypesFromRoles("?attr_subj", rolesArray)
				
				//put ?attr_subj ?attr_propMapping ?obj in ?resource (so the will be in the annotated value)
				+ "{?attr_subj ?attr_propMapping ?attr_obj .\n" //added to have some results
				+ "BIND(?attr_subj AS ?resource)}\n"
				+ "UNION\n"
				+ "{?attr_subj ?attr_propMapping ?attr_obj .\n"//added to have some results
				+ "BIND(?attr_propMapping AS ?resource)}\n"
				+ "UNION\n"
				+ "{?attr_subj ?attr_propMapping ?attr_obj .\n" //added to have some results
				+ "BIND(?attr_obj AS ?resource)}\n"	
				
				+ "} "
				+ "GROUP BY ?resource ?attr_subj ?attr_prop ?attr_obj ";
		
		logger.debug("query [listBrokenDefinition]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		
		//execute the query
		Collection<AnnotatedValue<Resource>> annotatedValueList = qb.runQuery();
		
		//now iterate over the response of the SPARQL query to construct the structure which will contain the
		// the triples with the definition
		Map <String, TripleForAnnotatedValue> tripleForAnnotatedValueMap = new HashMap<>();
		for(AnnotatedValue<Resource> annotatedValue : annotatedValueList) {
			String subj = annotatedValue.getAttributes().get("subj").stringValue();
			annotatedValue.getAttributes().remove("subj");
			String propMapping = annotatedValue.getAttributes().get("prop").stringValue();
			annotatedValue.getAttributes().remove("prop");
			String obj = annotatedValue.getAttributes().get("obj").stringValue();
			annotatedValue.getAttributes().remove("obj");
			String key = subj+propMapping+obj; 
			if(!tripleForAnnotatedValueMap.containsKey(key)) {
				tripleForAnnotatedValueMap.put(key, new TripleForAnnotatedValue());
			}
			TripleForAnnotatedValue tripleForAnnotatedValue = tripleForAnnotatedValueMap.get(key);
			//check the AnnotatedValue to see which of its "elements"refer to
			String value = annotatedValue.getValue().stringValue();
			if(value.equals(subj)) {
				tripleForAnnotatedValue.setSubject(annotatedValue);
			} else if(value.equals(propMapping)) {
				tripleForAnnotatedValue.setPredicate(annotatedValue);
			} else { //value.equals(obj)
				tripleForAnnotatedValue.setObject(annotatedValue);
			}
		}
		
		
		//prepare the empty response, which will be fill every time a broken alignment is found
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode response = jsonFactory.arrayNode();
		
		String namespace = getProject().getDefaultNamespace();
		
		//now iterate over the structure containg all the returned triples from the SPARQL query
		for(TripleForAnnotatedValue tripleForAnnotatedValue : tripleForAnnotatedValueMap.values()) {
			//check tha the obj does not belong to the current project
			if(!(tripleForAnnotatedValue.getObject().getValue() instanceof IRI)) {
				//the obj is not an IRI, this could not be possible, since in the SPARQL query I filter the
				// non-IRI value, but better to be safe than sorry
				continue;
			}
			IRI obj = (IRI)tripleForAnnotatedValue.getObject().getValue();
			if(obj.stringValue().startsWith(namespace)) {
				//the obj belong to the current project, so do not consider it
				continue;
			}
			//now try the to HTTP deference
			IRI resource = (IRI) tripleForAnnotatedValue.getObject().getValue();
			boolean toAdd = false;
			HttpURLConnection con = null;
			try {
				URL url = new URL(resource.stringValue());
				con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("GET");
				con.setRequestProperty("Content-Type", "application/json");
				con.setConnectTimeout(5000);
				con.setReadTimeout(5000);
				//connect to the remote site
				con.connect();
				int code = con.getResponseCode();
				if(code!=200) {
					//the resource was not found, so this triple will be returned
					toAdd = true;
				}
			} catch (IOException e) {
				//the connection was not possible, so this triple will be returned
				toAdd = true;
			} finally {
				if(toAdd) {
					//if the page cannot be found, then the resource does not exist, so return it
					ObjectNode singleBrokenAlign = jsonFactory.objectNode();
					singleBrokenAlign.putPOJO("subject", tripleForAnnotatedValue.getSubject());
					singleBrokenAlign.putPOJO("predicate", tripleForAnnotatedValue.getPredicate());
					singleBrokenAlign.putPOJO("object", tripleForAnnotatedValue.getObject());
					response.add(singleBrokenAlign);
				}
				//close the connection with the remote site
				con.disconnect();
			}
		
		}
		
		return response;
	}
	
	
	//ST-779 FR-New-ICVS.1
	/**
	 * Return a list of resources having an invalid URI according to complex regex 
	 * @return a list of resources having an invalid URI according to complex regex
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listLocalInvalidURIs() {
		
		String query = "SELECT DISTINCT ?resource \n" 
				+ "WHERE{ \n"
				+ "?resource a ?type .\n"
				+ getFilterForCheckingUris("?resource")
				+ "}\n"
				+ "GROUP BY ?resource ";
		
		logger.debug("query [listInvalidURIs]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
	private String getFilterForCheckingUris(String resource) {
		//this complex regex was copied from: 
		//	http://snipplr.com/view/6889/regular-expressions-for-uri-validationparsing/
				
		String regex = "^([a-z0-9+.-]+):(?://(?:((?:[a-z0-9-._~!$&'()*+,;=:]|%[0-9A-F]{2})*)@)?"
				+ "((?:[a-z0-9-._~!$&'()*+,;=]|%[0-9A-F]{2})*)(?::(\\\\d*))?"
				+ "(/(?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?|"
				+ "(/?(?:[a-z0-9-._~!$&'()*+,;=:@]|%[0-9A-F]{2})+"
				+ "(?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?)"
				+ "(?:\\\\?((?:[a-z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*))?"
				+ "(?:#((?:[a-z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*))?";

		String queryPart ="FILTER ( (REGEX( str(?resource), \" \")) "
				+ "|| !REGEX( str(?resource), \""+regex+"\", \"i\") ) \n";

		return queryPart;
	}
	
	//-----GENERICS-----
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesURIWithSpace()  {
		String q = "SELECT ?resource WHERE { \n"+
				"{?resource ?p1 ?o1} \n"+
				"UNION \n"+
				"{?s1 ?p2 ?resource} \n"+
				"UNION \n"+
				"{?s2 ?resource ?o2} \n"+
				"bind(str(?resource) as ?uri) \n"+
				"FILTER (regex(?uri, ' +?')) \n"+ //uri has 1+ space
				"FILTER (isURI(?resource)) \n } GROUP BY ?resource";
		logger.debug("query [listResourcesURIWithSpace]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	//########### QUICK FIXES #################
	
	/**
	 * Quick fix for dangling concepts. Set all dangling concepts as topConceptOf the given scheme
	 * @param scheme the scheme to which all dangling concept will set as top concept of
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'C')")
	public void setAllDanglingAsTopConcept(IRI scheme) {
		String q = "INSERT {\n"
				+ "GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + "\n"
				+ "{ ?concept " + NTriplesUtil.toNTriplesString(SKOS.TOP_CONCEPT_OF) + " " + NTriplesUtil.toNTriplesString(scheme) + " }\n"
				+ "} WHERE {\n"
				+ "BIND(" + NTriplesUtil.toNTriplesString(scheme) + " as ?scheme) \n"
				+ "?concept a " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT) + ". \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.TOP_CONCEPT_OF) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.HAS_TOP_CONCEPT) + "  ?scheme \n"
				+ "} \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.BROADER) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.NARROWER) + "  ?broader . \n"
				+ "?broader " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "} \n}";
		logger.debug("query [setAllDanglingAsTopConcept]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for dangling concepts. Set the given broader for all dangling concepts in the given scheme 
	 * @param scheme the scheme to which all dangling concept will be associated to
	 * @param broader the concept to which all dangling concept will be set as broader of
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'C')")
	public void setBroaderForAllDangling(IRI scheme, IRI broader) {
		String q = "INSERT {\n"
				+ "GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " \n"
				+ "{ ?concept " + NTriplesUtil.toNTriplesString(SKOS.BROADER) + " " + NTriplesUtil.toNTriplesString(broader) + " }\n"
				+ "} WHERE {\n"
				+ "BIND(" + NTriplesUtil.toNTriplesString(scheme) + " as ?scheme) \n"
				+ "?concept a " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT) + ". \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.TOP_CONCEPT_OF) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.HAS_TOP_CONCEPT) + "  ?scheme \n"
				+ "} \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.BROADER) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.NARROWER) + "  ?broader . \n"
				+ "?broader " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "} \n}";
		logger.debug("query [setBroaderForAllDangling]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for dangling concepts. Removes all dangling concepts from the given scheme
	 * @param scheme the scheme which will be removed from all dangling concept
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, schemes)', 'D')")
	public void removeAllDanglingFromScheme(IRI scheme) {
		String q = "DELETE { \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " " + NTriplesUtil.toNTriplesString(scheme) + " }\n"
				+ "WHERE {\n"
				+ "BIND(" + NTriplesUtil.toNTriplesString(scheme) + " as ?scheme) \n"
				+ "?concept a " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT) + ". \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.TOP_CONCEPT_OF) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.HAS_TOP_CONCEPT) + "  ?scheme \n"
				+ "} \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.BROADER) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.NARROWER) + "  ?broader . \n"
				+ "?broader " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "} \n }";
		logger.debug("query [removeAllDanglingFromScheme]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for dangling concepts. Delete all the dangling concepts of the given scheme
	 * @param scheme the scheme from which all dangling concept will be be deleted from the ontology  
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'D')")
	public void deleteAllDanglingConcepts(IRI scheme) {
		String q = "DELETE { "
				+ "?concept ?p1 ?o .\n"
				+ "?s ?p2 ?concept \n"
				+ "} WHERE {\n"
				+ "BIND(<" + scheme.stringValue() + "> as ?scheme)\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.TOP_CONCEPT_OF + "> ?scheme}\n"
				+ "FILTER NOT EXISTS {?scheme <" + SKOS.HAS_TOP_CONCEPT + "> ?concept }\n"
				+ "OPTIONAL { ?concept ?p1 ?o . }\n"
				+ "OPTIONAL { ?s ?p2 ?concept . }\n"
				+ "{ ?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1 . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> ?broaderConcept1 .\n"
				+ "FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.IN_SCHEME + "> ?scheme  . }\n"
				+ "} {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "?broaderConcept2 <" + SKOS.NARROWER + "> ?concept .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.IN_SCHEME + "> ?scheme . }\n"
				+ "}\n}";
		logger.debug("query [deleteAllDanglingConcepts]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for concepts in no scheme. Add all concepts without scheme to the given scheme
	 * @param scheme the scheme to which all concepts having no scheme will be added to 
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, scheme)', 'C')")
	public void addAllConceptsToScheme(IRI scheme) {
		String q = "INSERT {\n"
				+ "GRAPH <" + getWorkingGraph().stringValue() + ">\n"
				+ "{ ?concept <" + SKOS.IN_SCHEME + "> <" + scheme.stringValue() + "> }\n"
				+ "} WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "FILTER NOT EXISTS { ?concept <" + SKOS.IN_SCHEME + "> ?scheme . } }";
		logger.debug("query [addAllConceptsToScheme]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Fix for topConcept with broader. Remove all the broader relation in the given scheme of the given concept.
	 * @param concept the concept which will be no longer the broader of the top concept of the given scheme
	 * @param scheme
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'D')")
	public void removeBroadersToConcept(IRI concept, IRI scheme) {
		String q = "DELETE {\n"
				+ "?concept <" + SKOS.BROADER + "> ?broader .\n"
				+ "?broader <" + SKOS.NARROWER + "> ?concept .\n"
				+ "} WHERE {\n"
				+ "BIND (<" + concept.stringValue() + "> as ?concept) \n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?broader <" + SKOS.IN_SCHEME + "> ?scheme . }";
		logger.debug("query [removeBroadersToConcept]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for topConcept with broader. Remove all the broader (or narrower) relation in the 
	 * of top concepts with broader (in the same scheme).
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'D')")
	public void removeBroadersToAllConcepts() {
		String q = "DELETE {\n"
				+ "?concept <" + SKOS.BROADER + "> ?broader .\n"
				+ "?broader <" + SKOS.NARROWER + "> ?concept .\n"
				+ "} WHERE {\n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?broader <" + SKOS.IN_SCHEME + "> ?scheme . }";
		logger.debug("query [removeBroadersToAllConcepts]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for topConcept with broader. Remove as topConceptOf all the topConcept with broader.
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'D')")
	public void removeAllAsTopConceptsWithBroader() {
		String q = "DELETE {\n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> ?scheme .\n"
				+ "?scheme <" + SKOS.HAS_TOP_CONCEPT + "> ?concept .\n"
				+ "} WHERE {\n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + "> ?scheme .\n"
   				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
   				+ "?broader <" + SKOS.IN_SCHEME + "> ?scheme . }";
		logger.debug("query [removeAllAsTopConceptsWithBroader]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for hierarchical redundancy. Remove narrower/broader redundant relations.
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'D')")
	public void removeAllHierarchicalRedundancy() {
		String q = "DELETE {\n"
				+ "?narrower <" + SKOS.BROADER + "> ?broader .\n"
				+ "?broader <" + SKOS.NARROWER + "> ?narrower .\n"
				+ "} WHERE {\n"
				+ "?narrower <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?narrower (<" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + ">)+ ?middle .\n"
				+ "?middle <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "FILTER(?narrower != ?middle) }";
		logger.debug("query [removeAllHierarchicalRedundancy]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for dangling xLabel. Deletes all triples that involve the dangling xLabel(s)
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(xLabel)', 'D')")
	public void deleteAllDanglingXLabel() {
		String q = "DELETE {\n"
				+ "?s ?p1 ?xlabel .\n"
				+ "?xlabel ?p2 ?o .\n"
				+ "} WHERE {\n"
				+ "?subLabel <" + RDFS.SUBCLASSOF + ">* <" + SKOSXL.LABEL + "> .\n" 
				+ "?xlabel a ?subLabel .\n"
				+ "OPTIONAL { ?s ?p1 ?xlabel . }\n"
				+ "OPTIONAL { ?xlabel ?p2 ?o . }\n"
				+ "FILTER NOT EXISTS {\n"
				+ "?concept <" + SKOSXL.PREF_LABEL + "> | <" + SKOSXL.ALT_LABEL + "> | <" + SKOSXL.HIDDEN_LABEL + "> ?xlabel.\n"
				+ "} }";
		logger.debug("query [deleteAllDanglingXLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Fix for dangling xLabel. Links the dangling xLabel to the given concept through the given predicate 
	 * @param concept the concept which will be linked with the given xlabelPred to the input xlabel
	 * @param xlabelPred the property which will be used to link the given concept to the input xlabel
	 * @param xlabel the xlabel which will be linked to the given concept using the input xlabelPred
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(resource, lexicalization)', 'C')")
	public void setDanglingXLabel(IRI concept, IRI xlabelPred, Resource xlabel) {
		String q = "";
		if (xlabelPred.equals(SKOSXL.PREF_LABEL)) {
			q = "DELETE {\n"
					+ "<" + concept.stringValue() + "> <" + SKOSXL.PREF_LABEL + "> ?oldPrefLabel\n"
					+ "} INSERT {\n"
					+ "GRAPH <" + getWorkingGraph() + "> {\n"
					+ "<" + concept.stringValue() + "> <" + SKOSXL.ALT_LABEL + "> ?oldPrefLabel .\n"
					+ "<" + concept.stringValue() + "> <" + SKOSXL.PREF_LABEL + "> <" + xlabel.stringValue() + "> . }\n"
					+ "} WHERE {\nOPTIONAL {\n"
					+ "<" + concept.stringValue() + "> <" + SKOSXL.PREF_LABEL + "> ?oldPrefLabel \n"
					+ "} }";
		} else { //altLabel or hiddenLabel
			q = "INSERT DATA {\n"
					+ "GRAPH <" + getWorkingGraph() + "> {\n"
					+ "<" + concept.stringValue() + "> <" + xlabelPred.stringValue() + "> <" + xlabel.stringValue() + "> \n"
					+ "}\n}";
		}
		logger.debug("query [setDanglingXLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}

	
//	private Element addResourceToElement(Element parent, Resource resource, RDFResourceRole role, String show){
//		Element nodeElement;
//		if(resource instanceof IRI){
//			nodeElement = XMLHelp.newElement(parent, "uri");
//		} else { // (node.isBlank())
//			nodeElement = XMLHelp.newElement(parent, "bnode");
//		}
//		nodeElement.setTextContent(resource.stringValue());
//		if (role != null)
//			nodeElement.setAttribute("role", role.toString());
//		if (show != null)
//			nodeElement.setAttribute("show", show);
//		//explicit is set to true
//		nodeElement.setAttribute("explicit", Boolean.toString(true));
//		
//		//OLD
//		//serializeMap(nodeElement, node);
//		
//		return nodeElement;
//	}
//	
//	private Element addLiteralToElement(Element parent, Literal literal){
//		Element nodeElement;
//		if(literal.getLanguage().isPresent()){
//			nodeElement = XMLHelp.newElement(parent, "plainLiteral");
//			nodeElement.setAttribute("lang", literal.getLanguage().get());
//		} else if(literal.getDatatype()==null){
//			nodeElement = XMLHelp.newElement(parent, "plainLiteral");
//		} else{
//			nodeElement = XMLHelp.newElement(parent, "typedLiteral");
//			nodeElement.setAttribute("type", literal.getDatatype().stringValue());
//		}
//		nodeElement.setTextContent(literal.stringValue());
//		//explicit is set to true
//		nodeElement.setAttribute("explicit", Boolean.toString(true));
//		
//		//OLD
//		//serializeMap(nodeElement, node);
//
//		return nodeElement;
//	}
//	
//	private static Map<String, String> ns2PrefixMapping(RepositoryConnection conn){
//		return QueryResults.stream(conn.getNamespaces()).collect(
//				toMap(Namespace::getName, Namespace::getPrefix, (v1, v2) -> v1 != null ? v1 : v2));
//	}
//	
	
	private String rolePartForQuery(RDFResourceRole[] rolesArray, String var, boolean useSubQuery) {
		String query = "";
		String union = "";
		boolean first = true;
		if (useSubQuery) {
			query += "\n{SELECT " + var + " "
					+ "\nWHERE {";
		}
		for (RDFResourceRole role : rolesArray) {
			if (!first) {
				union = "\nUNION";
			}

			if (role.equals(RDFResourceRole.concept)) {
				query += union + "\n{ " + var + " "
						+ NTriplesUtil.toNTriplesString(RDF.TYPE) + "/" + NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* "
						+ NTriplesUtil.toNTriplesString(SKOS.CONCEPT) + " . } ";
				first = false;
			} else if (role.equals(RDFResourceRole.cls)) {
				query += union + "\n{ " + var
						+ NTriplesUtil.toNTriplesString(RDF.TYPE) + "/" + NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* "
						+ "?type .  "
						+ "\nFILTER(?type = " + NTriplesUtil.toNTriplesString(OWL.CLASS) + " || "
						+ "?type = " + NTriplesUtil.toNTriplesString(RDFS.CLASS) + " ) } \n";
				first = false;
			} else if (role.equals(RDFResourceRole.property)) {
				query += union + "\n{ " + var
						+ NTriplesUtil.toNTriplesString(RDF.TYPE) + "/" + NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* "
						+ "?type .  "
						+ "\nFILTER(?type = " + NTriplesUtil.toNTriplesString(RDF.PROPERTY) + " || "
						+ "?type = " + NTriplesUtil.toNTriplesString(OWL.OBJECTPROPERTY) + " || "
						+ "?type = " + NTriplesUtil.toNTriplesString(OWL.DATATYPEPROPERTY) + " || "
						+ "?type = " + NTriplesUtil.toNTriplesString(OWL.ANNOTATIONPROPERTY) + " || "
						+ "?type = " + NTriplesUtil.toNTriplesString(OWL.ONTOLOGYPROPERTY) + " )" +
						"\n}";
				first = false;
			} else if (role.equals(RDFResourceRole.conceptScheme)) {
				query += union + "\n{ " + var + " "
						+ NTriplesUtil.toNTriplesString(RDF.TYPE) + "/" + NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* "
						+ NTriplesUtil.toNTriplesString(SKOS.CONCEPT_SCHEME) + " . } ";
				first = false;
			} else if (role.equals(RDFResourceRole.skosCollection)) {
				query += union + "\n{ " + var
						+ NTriplesUtil.toNTriplesString(RDF.TYPE) + "/" + NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* "
						+ "?type .  "
						+ "\nFILTER(?type = " + NTriplesUtil.toNTriplesString(SKOS.COLLECTION) + " || "
						+ "?type = " + NTriplesUtil.toNTriplesString(SKOS.ORDERED_COLLECTION) + " ) }";
				first = false;
			} else if (role.equals(RDFResourceRole.individual)) {
				query += union + "\n{ " + var + " a ?type .  "
						+ "\n?type a ?classType . "
						+ "\nFILTER(?classType = " + NTriplesUtil.toNTriplesString(OWL.CLASS) + " || "
						+ "?classType = " + NTriplesUtil.toNTriplesString(RDFS.CLASS) + " ) } \n";
				first = false;
			}
		}

		if (useSubQuery) {
			query += "\n}\n}";
		}
		return query;
	}
	
	private String getSkosxlPrefOrAltOrHidden() {
		String or = "("+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+" | " +
				NTriplesUtil.toNTriplesString(SKOSXL.ALT_LABEL)+" | " +
				NTriplesUtil.toNTriplesString(SKOSXL.HIDDEN_LABEL)+" )";
		return or;
	}
	
	private String getSkosPrefOrAltOrHidden() {
		String or = "("+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+" | " +
				NTriplesUtil.toNTriplesString(SKOS.ALT_LABEL)+" | " +
				NTriplesUtil.toNTriplesString(SKOS.HIDDEN_LABEL)+" )";
		return or;
	}
	
	private String broaderOrInverseNarrower() {
		String broaderOrInverceNarrower = "("+NTriplesUtil.toNTriplesString(SKOS.BROADER)+" | ^" +
				NTriplesUtil.toNTriplesString(SKOS.NARROWER)+" )";
		return broaderOrInverceNarrower;
	}
	
	private RepositoryConnection acquireManagedConnectionToProject(ProjectConsumer consumer,
			Project resourceHoldingProject) throws ProjectAccessException {
		if (consumer.equals(resourceHoldingProject)) {
			return getManagedConnection();
		} else {
			AccessResponse accessResponse = ProjectManager.checkAccessibility(consumer,
					resourceHoldingProject, AccessLevel.R, LockLevel.NO);

			if (!accessResponse.isAffirmative()) {
				throw new ProjectAccessException(accessResponse.getMsg());
			}

			return projectConnectionHolder.get().computeIfAbsent(resourceHoldingProject,
					p -> RDF4JRepositoryUtils.wrapReadOnlyConnection(p.getRepository().getConnection()));
		}
	}
}
