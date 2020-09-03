package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.customform.SpecialValue;
import it.uniroma2.art.semanticturkey.data.access.ResourceLocator;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.ontolexlemon.FormRenderer;
import it.uniroma2.art.semanticturkey.services.core.ontolexlemon.LexicalEntryRenderer;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static java.util.stream.Collectors.joining;

//import it.uniroma2.art.semanticturkey.utilities.SPARQLHelp;

@STService
public class Resources extends STServiceAdapter {

	private static final Logger logger = LoggerFactory.getLogger(Resources.class);

	@Autowired
	private ResourceLocator resourceLocator;

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#subject)+ ', values)', '{lang: [''' +@auth.langof(#value)+ ''', ''' +@auth.langof(#newValue)+ ''']}', 'U')")
	public void updateTriple(@Modified Resource subject, IRI property, Value value, Value newValue) {
		updateTripleImpl(subject, property, value, newValue);
	}


	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#subject)+ ', lexicalization)', '{lang: [''' +@auth.langof(#value)+ ''', ''' +@auth.langof(#newValue)+ ''']}', 'U')")
	public void updateLexicalization(@Modified Resource subject, IRI property, Literal value, Literal newValue) {
		logger.debug("request to update a lexicalization");
		updateTripleImpl(subject, property, value, newValue);
	}

	private void updateTripleImpl(Resource subject, IRI property, Value value, Value newValue) {
		logger.debug("request to update a triple");
		RepositoryConnection repoConnection = getManagedConnection();

		String query = "DELETE  {							\n"
				+ "		GRAPH ?g {							\n"
				+ "			?subject ?property ?value .		\n"
				+ "		}									\n"
				+ "}										\n"
				+ "INSERT  {							\n"
				+ "		GRAPH ?g {							\n"
				+ "			?subject ?property ?newValue .	\n"
				+ "		}									\n"
				+ "}										\n"
				+ "WHERE{									\n"
				+ "BIND(?g_input AS ?g )					\n"
				+ "BIND(?subject_input AS ?subject )		\n"
				+ "BIND(?property_input AS ?property )		\n"
				+ "BIND(?value_input AS ?value )			\n"
				+ "BIND(?newValue_input AS ?newValue )		\n" + "}";

		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("g_input", getWorkingGraph());
		update.setBinding("subject_input", subject);
		update.setBinding("property_input", property);
		update.setBinding("value_input", value);
		update.setBinding("newValue_input", newValue);
		update.execute();
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(resource, values)', '{lang: [''' +@auth.langof(#value)+ ''', ''' +@auth.langof(#newValue)+ ''']}', 'U')")
	public void updatePredicateObject(IRI property, Value value, Value newValue) {
		RepositoryConnection repoConnection = getManagedConnection();

		String ntGraph = NTriplesUtil.toNTriplesString(getWorkingGraph());
		String ntProperty = NTriplesUtil.toNTriplesString(property);
		String ntValue = NTriplesUtil.toNTriplesString(value);
		String ntNewValue = NTriplesUtil.toNTriplesString(newValue);

		String query =
				"DELETE { 														\n" +
				"	GRAPH " + ntGraph + " { 									\n" +
				"		?subject " + ntProperty + " " + ntValue + " .			\n" +
				"	} 															\n" +
				"} 																\n" +
				"INSERT  { 														\n" +
				"	GRAPH " + ntGraph + " { 									\n" +
				"		?subject " + ntProperty + " " + ntNewValue + " . 		\n" +
				"	}															\n" +
				"}																\n" +
				"WHERE { 														\n" +
				"	GRAPH " + ntGraph + " { 									\n" +
				"		?subject " + ntProperty + " " + ntValue + " . 			\n" +
				"	} 															\n" +
				"}";

		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(resource, values)', '{lang: ''' +@auth.langof(#value)+ '''}', 'D')")
	public void removePredicateObject(IRI property, Value value) {
		RepositoryConnection repoConnection = getManagedConnection();

		String ntGraph = NTriplesUtil.toNTriplesString(getWorkingGraph());
		String ntProperty = NTriplesUtil.toNTriplesString(property);
		String ntValue = NTriplesUtil.toNTriplesString(value);

		//this part is related to the modified date to be added to each modified subject
		String query =
				"DELETE  {														\n" +
				"		GRAPH " + ntGraph + " {									\n" +
				"			?subject " + ntProperty + " " + ntValue + " .		\n" +
				"		}														\n" +
				"}																\n" +
				"WHERE{															\n" +
				"		GRAPH " + ntGraph + " {									\n" +
				"			?subject " + ntProperty + " " + ntValue + " .	 	\n" +
				"		}														\n" +
				"}";

		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#subject)+ ', values)', '{lang: ''' +@auth.langof(#value)+ '''}', 'D')")
	public void removeValue(@LocallyDefined @Modified Resource subject, IRI property, Value value) {
		getManagedConnection().remove(subject, property, value, getWorkingGraph());
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#subject)+ ', values)', '{lang: ''' +@auth.langof(#value)+ '''}','C')")
	public void addValue(@LocallyDefined @Modified Resource subject, IRI property, SpecialValue value)
			throws CODAException {
		addValue(getManagedConnection(), subject, property, value);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resource)+ ')', 'U')")
	public void setDeprecated(@LocallyDefined @Modified IRI resource) {
		RepositoryConnection conn = getManagedConnection();
		Literal literalTrue = conn.getValueFactory().createLiteral("true", XMLSchema.BOOLEAN);
		conn.add(resource, OWL.DEPRECATED, literalTrue, getWorkingGraph());
	}

	/**
	 * Return the description of a resource, including show and nature
	 *
	 * @param resource
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf', 'R')")
	public AnnotatedValue<Resource> getResourceDescription(@LocallyDefined Resource resource) {
		QueryBuilder qb = createQueryBuilder(
		// @formatter:off
			" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>						\n" +
			" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>							\n" +
			" PREFIX owl: <http://www.w3.org/2002/07/owl#>									\n" +
			" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>							\n" +
			" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>							\n" +
			" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {				\n" +
			"    ?resource ?p ?o .															\n" +
			generateNatureSPARQLWherePart("?resource") +
			" }																				\n" +
			" GROUP BY ?resource 															\n"
			// @formatter:on
		);
		qb.setBinding("resource", resource);
		qb.processRendering();
		qb.processQName();
		qb.process(FormRenderer.INSTANCE_WITHOUT_FALLBACK, "resource", "attr_formRendering");
		qb.process(LexicalEntryRenderer.INSTANCE_WITHOUT_FALLBACK, "resource", "attr_lexicalEntryRendering");

		AnnotatedValue<Resource> annotatedResource = qb.runQuery().iterator().next();
		fixShowAttribute(annotatedResource);
		return annotatedResource;
	}

	/**
	 * Return the description of a list of resources, including show and nature
	 *
	 * @param resources
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> getResourcesInfo(@JsonSerialized IRI[] resources) {

		QueryBuilder qb;
		StringBuilder sb = new StringBuilder();
		sb.append(
		// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
				" prefix owl: <http://www.w3.org/2002/07/owl#>							\n" +
				" prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>					\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>					\n" +
				" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {		\n" +
				"     VALUES(?resource) {");
		sb.append(Arrays.stream(resources).map(iri -> "(" + RenderUtils.toSPARQL(iri) + ")").collect(joining()));
		sb.append("}													 				\n" +
				generateNatureSPARQLWherePart("?resource") +
				"} 																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb = createQueryBuilder(sb.toString());
		qb.processRendering();
		qb.processQName();
		qb.process(LexicalEntryRenderer.INSTANCE_WITHOUT_FALLBACK, "resource", "attr_lexicalEntryRendering");
		qb.process(FormRenderer.INSTANCE_WITHOUT_FALLBACK, "resource", "attr_formRendering");

		Collection<AnnotatedValue<Resource>> annotatedValues = qb.runQuery();
		Iterator<AnnotatedValue<Resource>> it = annotatedValues.iterator();
		while (it.hasNext()) {
			fixShowAttribute(it.next());
		}
		return annotatedValues;
	}

	/**
	 * Rendering of the lexical entry and ontolex form are computed by two dedicated query builders
	 * (not by processRendering()).
	 * This method replaces the show attribute for these kind of resources with the proper rendering
	 * @param annotatedResource
	 */
	private void fixShowAttribute(AnnotatedValue<Resource> annotatedResource) {
		Map<String, Value> attrs = annotatedResource.getAttributes();
		Literal lexicalEntryRendering = (Literal) attrs.remove("lexicalEntryRendering");
		Literal formRendering = (Literal) attrs.remove("formRendering");

		if (lexicalEntryRendering != null) {
			attrs.put("show", lexicalEntryRendering);
		} else {
			if (formRendering != null) {
				attrs.put("show", formRendering);
			}
		}
	}

	/**
	 * Return the position of a resource (local/remote/unknown)
	 *
	 * @param resource
	 * @return
	 * @throws ProjectAccessException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public String getResourcePosition(IRI resource) throws ProjectAccessException {
		return resourceLocator.locateResource(getProject(), getRepository(), resource).toString();
	}

	/**
	 * Returns a mapping between the provided resources and their position
	 * @param resources
	 * @return
	 * @throws ProjectAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Map<String, ResourcePosition> getResourcesPosition(@JsonSerialized IRI[] resources) throws ProjectAccessException {
		Map<String, ResourcePosition> positionMap = new HashMap<>();
		Repository repo = getRepository();
		Project project = getProject();
		for (IRI resource: resources) {
			ResourcePosition position = resourceLocator.locateResource(project, repo, resource);
			positionMap.put(resource.stringValue(), position);
		}
		return positionMap;
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(code)', 'R')")
	public String getOutgoingTriples(Resource resource, RDFFormat format) throws IOException {
		RepositoryConnection conn = getManagedConnection();
		GraphQueryResult res = getOutgoingTriplesQueryResult(conn, resource);

		Model model = new LinkedHashModel();
		while (res.hasNext()) {
			Statement stmt = res.next();
			model.add(stmt);
		}

		StringWriter sw = new StringWriter();
		RDFWriter writer = Rio.createWriter(format, sw);
		writer.set(BasicWriterSettings.PRETTY_PRINT, true);
		try {
			writer.startRDF();
			//write prefix mappings
			Map<String, String> prefixMappings = getProject().getNewOntologyManager().getNSPrefixMappings(false);
			for (String prefix: prefixMappings.keySet()) {
				writer.handleNamespace(prefix, prefixMappings.get(prefix));
			}
			//write statements
			for (Statement st: model) {
				writer.handleStatement(st);
			}
			writer.endRDF();
		} finally {
			sw.close();
		}

		return sw.toString();
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(code)', 'U')")
	public void updateResourceTriplesDescription(@LocallyDefined @Modified Resource resource, String triples, RDFFormat format) throws IOException {
		RepositoryConnection conn = getManagedConnection();

		InputStream triplesStream = new ByteArrayInputStream(triples.getBytes(StandardCharsets.UTF_8));

		Model oldDescriptionModel = new LinkedHashModel();
		Model newDescriptionModel = new LinkedHashModel();

		//get the statements of the updated description
		RDFParser rdfParser = Rio.createParser(format);
		rdfParser.getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
		rdfParser.setRDFHandler(new StatementCollector(newDescriptionModel));
		rdfParser.parse(triplesStream, getProject().getNewOntologyManager().getBaseURI());

		//if new description contains statements with a different subject throws an exception
		Statement diffSubjStmt = newDescriptionModel.stream()
				.filter(stmt -> !stmt.getSubject().equals(resource)).findAny().orElse(null);
		if (diffSubjStmt != null) {
			throw new IllegalArgumentException(
					"Invalid resource description: it includes statement about a different resource " +
							NTriplesUtil.toNTriplesString(diffSubjStmt.getSubject()));
		}

		//get the statements of the old description
		GraphQueryResult res = getOutgoingTriplesQueryResult(conn, resource);
		while (res.hasNext()) {
			oldDescriptionModel.add(res.next());
		}

		//Diff
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		//get the statements to add: those present in the new description and missing in the old one
		newDescriptionModel.stream()
				.filter(newStmt -> oldDescriptionModel.stream()
						.noneMatch(oldStmt ->
								newStmt.getSubject().equals(oldStmt.getSubject()) &&
								newStmt.getPredicate().equals(oldStmt.getPredicate()) &&
								newStmt.getObject().equals(oldStmt.getObject())))
				.forEach(modelAdditions::add);

		//get the statements to remove: those present in the old description and missing in the new one
		oldDescriptionModel.stream()
				.filter(oldStmt -> newDescriptionModel.stream()
						.noneMatch(newStmt ->
								oldStmt.getSubject().equals(newStmt.getSubject()) &&
								oldStmt.getPredicate().equals(newStmt.getPredicate()) &&
								oldStmt.getObject().equals(newStmt.getObject())))
				.forEach(modelRemovals::add);

		//prevent to delete the whole description
		if (modelRemovals.size() == oldDescriptionModel.size()) {
			throw new IllegalArgumentException("Cannot delete all the resource triples");
		}


		ValidationUtilities.executeWithoutValidation(
				ValidationUtilities.isValidationEnabled(stServiceContext), conn, (conn2) -> {
					try {
						conn.add(modelAdditions, getWorkingGraph());
						conn.remove(modelRemovals, getWorkingGraph());
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});


	}

	private GraphQueryResult getOutgoingTriplesQueryResult(RepositoryConnection conn, Resource resource) {
		String query = "CONSTRUCT {											\n" +
				"	?s ?p ?o .												\n" +
				"} WHERE {													\n" +
				"	GRAPH ?g {												\n" +
				"		?s ?p ?o											\n" +
				"	}														\n" +
				"}															\n" +
				"VALUES(?g) {												\n" +
				"	(" + RenderUtils.toSPARQL(getWorkingGraph()) + ")		\n" +
				"}";
		GraphQuery gq = conn.prepareGraphQuery(query);
		gq.setBinding("s", resource);
		return gq.evaluate();
	}

}
