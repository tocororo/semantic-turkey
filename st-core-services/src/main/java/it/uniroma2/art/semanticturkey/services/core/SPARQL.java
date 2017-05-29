package it.uniroma2.art.semanticturkey.services.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.sparql.Graph2TupleQueryResultAdapter;

/**
 * This class provides services for SPARQL queries/updates.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class SPARQL extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(SPARQL.class);

	/**
	 * Evaluates a query. The parameters controlling the dataset (i.e. {@code defaultGraphs} and
	 * {@code namedGraphs}) are not orthogonal: see {@link Dataset} for an explanation of their meaning.
	 * 
	 * @param query
	 *            the query
	 * @param ql
	 *            the query language (see {@link QueryLanguage}. Default value is {@code SPARQL}
	 * @param includeInferred
	 *            indicates whether inferred statements should be included in the evaluation of the query.
	 *            Default value is {@code true}
	 * @param bindings
	 *            variable to value bindings
	 * @param maxExecTime
	 *            maximum execution time measured in seconds (a zero or negative value indicates an unlimited
	 *            execution time). Default value is {@code 0}
	 * @param defaultGraphs
	 *            the graphs that constitute the default graph. The default value is the empty set.
	 * @param namedGraphs
	 *            the graphs that constitute the set of named graphs.
	 * 
	 * @return
	 * @throws IOException
	 * @throws JsonProcessingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	public JsonNode evaluateQuery(String query, @Optional(defaultValue = "SPARQL") QueryLanguage ql,
			@Optional(defaultValue = "true") boolean includeInferred,
			@Optional(defaultValue = "{}") Map<String, Value> bindings,
			@Optional(defaultValue = "0") int maxExecTime, @Optional(defaultValue = "") IRI[] defaultGraphs,
			@Optional(defaultValue = "") IRI[] namedGraphs) throws JsonProcessingException, IOException {
		RepositoryConnection conn = getManagedConnection();

		Query preparedQuery = conn.prepareQuery(ql, query);

		configureOperation(includeInferred, bindings, maxExecTime, defaultGraphs, namedGraphs, null, null,
				preparedQuery);

		if (preparedQuery instanceof BooleanQuery) {
			boolean result = ((BooleanQuery) preparedQuery).evaluate();
			ObjectNode sparqlObj = JsonNodeFactory.instance.objectNode();
			sparqlObj.set("head", JsonNodeFactory.instance.objectNode());
			sparqlObj.set("boolean", JsonNodeFactory.instance.booleanNode(result));

			ObjectNode wrapObj = JsonNodeFactory.instance.objectNode();
			wrapObj.set("resultType", JsonNodeFactory.instance.textNode("boolean"));
			wrapObj.set("sparql", sparqlObj);

			return wrapObj;
		} else if (preparedQuery instanceof TupleQuery) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			((TupleQuery) preparedQuery).evaluate(new SPARQLResultsJSONWriter(baos));
			ObjectMapper mapper = new ObjectMapper();
			JsonNode sparqlObj = mapper.readTree(baos.toString());

			ObjectNode wrapObj = JsonNodeFactory.instance.objectNode();
			wrapObj.set("resultType", JsonNodeFactory.instance.textNode("tuple"));
			wrapObj.set("sparql", sparqlObj);

			return wrapObj;
		} else { // must be (preparedQuery instanceof GraphQuery)
			JsonNode sparqlObj;
			try (TupleQueryResult queryResult = new IteratingTupleQueryResult(
					Arrays.asList("subj", "pred", "obj"),
					new Graph2TupleQueryResultAdapter(((GraphQuery) preparedQuery).evaluate()));) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				QueryResults.report(queryResult, new SPARQLResultsJSONWriter(baos));
				ObjectMapper mapper = new ObjectMapper();
				sparqlObj = mapper.readTree(baos.toString());
			}

			ObjectNode wrapObj = JsonNodeFactory.instance.objectNode();
			wrapObj.set("resultType", JsonNodeFactory.instance.textNode("graph"));
			wrapObj.set("sparql", sparqlObj);

			return wrapObj;
		}
	}

	/**
	 * Executes an update. The parameters controlling the dataset (i.e. {@code defaultGraphs},
	 * {@code namedGraphs}, {@code defaultInsertGraph} and {@code defaultRemoveGraphs} are not orthogonal):
	 * see {@link Dataset} for an explanation of their meaning.
	 * 
	 * @param query
	 *            the query
	 * @param ql
	 *            the query language (see {@link QueryLanguage}. Default value is {@code SPARQL}
	 * @param includeInferred
	 *            indicates whether inferred statements should be included in the evaluation of the query.
	 *            Default value is {@code true}
	 * @param bindings
	 *            variable to value bindings
	 * @param maxExecTime
	 *            maximum execution time measured in seconds (a zero or negative value indicates an unlimited
	 *            execution time). Default value is {@code 0}
	 * @param defaultGraphs
	 *            the graphs that constitute the default graph. The default value is the empty set.
	 * @param namedGraphs
	 *            the graphs that constitute the set of named graphs.
	 * @param defaultInsertGraph
	 *            the default insert graph to be used. The default value is {@code null}.
	 * @param defaultRemoveGraph
	 *            the default remove graphs.
	 * 
	 * 
	 * @return
	 * @throws IOException
	 * @throws JsonProcessingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	public void executeUpdate(String query, @Optional(defaultValue = "SPARQL") QueryLanguage ql,
			@Optional(defaultValue = "true") boolean includeInferred,
			@Optional(defaultValue = "{}") Map<String, Value> bindings,
			@Optional(defaultValue = "0") int maxExecTime, @Optional(defaultValue = "") IRI[] defaultGraphs,
			@Optional(defaultValue = "") IRI[] namedGraphs, @Optional IRI defaultInsertGraph,
			@Optional(defaultValue = "") IRI[] defaultRemoveGraphs)
			throws JsonProcessingException, IOException {

		RepositoryConnection conn = getManagedConnection();

		Update preparedUpdate = conn.prepareUpdate(ql, query);

		configureOperation(includeInferred, bindings, maxExecTime, defaultGraphs, namedGraphs,
				defaultInsertGraph, defaultRemoveGraphs, preparedUpdate);

		preparedUpdate.execute();
	}

	protected void configureOperation(boolean includeInferred, Map<String, Value> bindings, int maxExecTime,
			IRI[] defaultGraphs, IRI[] namedGraphs, @Nullable IRI defaultInsertGraph,
			@Nullable IRI[] defaultRemoveGraphs, Operation preparedUpdate) {
		preparedUpdate.setIncludeInferred(includeInferred);

		bindings.forEach((varName, varValue) -> preparedUpdate.setBinding(varName, varValue));

		SimpleDataset dataset = new SimpleDataset();
		Arrays.stream(defaultGraphs).forEach(dataset::addDefaultGraph);
		Arrays.stream(namedGraphs).forEach(dataset::addNamedGraph);

		if (defaultRemoveGraphs != null) {
			Arrays.stream(defaultRemoveGraphs).forEach(dataset::addDefaultRemoveGraph);
		}
		if (defaultInsertGraph != null) {
			dataset.setDefaultInsertGraph(defaultInsertGraph);
		}

		preparedUpdate.setDataset(dataset);

		preparedUpdate.setMaxExecutionTime(maxExecTime);
	}

}