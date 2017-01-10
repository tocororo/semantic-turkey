package it.uniroma2.art.semanticturkey.services.support;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import it.uniroma2.art.semanticturkey.data.role.RoleRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.ProjectedBindingSet;
import it.uniroma2.art.semanticturkey.sparql.QueryBuildOutput;
import it.uniroma2.art.semanticturkey.sparql.SPARQLShallowParser;
import it.uniroma2.art.semanticturkey.sparql.TupleQueryShallowModel;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * A {@code QueryBuilder} supports the composition and evaluation of the SPARQL query underpinning an Semantic
 * Turkey service, by taking care of cross-cutting concerns such as rendering and role retrieval.
 */
public class QueryBuilder {
	private static final Logger logger = LoggerFactory.getLogger(QueryBuilder.class);

	private final STServiceContext serviceContext;
	private final String resourceQuery;
	private RenderingEngine renderingEngine;
	private RoleRecognitionOrchestrator roleRecognitionOrchestrator;
	private final Set<QueryBuilderProcessor> attachedProcessors;
	private final BiMap<QueryBuilderProcessor, GraphPatternBinding> attachedProcessorsGraphPatternBinding;
	private final Map<String, Value> bindingSet;

	/**
	 * Constructs a {@code QueryBuilder} for a service backed by a given (tuple) query.
	 * 
	 * @param serviceContext
	 * @param resourceQuery
	 */
	public QueryBuilder(STServiceContext serviceContext, String resourceQuery) {
		this.serviceContext = serviceContext;
		this.resourceQuery = resourceQuery;
		this.attachedProcessors = new HashSet<>();
		this.bindingSet = new HashMap<>();
		this.roleRecognitionOrchestrator = null;
		this.attachedProcessorsGraphPatternBinding = HashBiMap.create();
	}

	/**
	 * Attaches the rendering of the retrieved resources.
	 * 
	 * @throws QueryBuilderException
	 */
	public void processRendering() throws QueryBuilderException {
		if (renderingEngine != null) {
			throw new QueryBuilderException("Rendering engine already configured");
		}
		renderingEngine = serviceContext.getProject().getRenderingEngine();
		process(renderingEngine, "resource", "attr_show");
	}

	/**
	 * Attaches the role retrieval of the retrieved resources.
	 * 
	 * @throws QueryBuilderException
	 */
	public void processRole() throws QueryBuilderException {
		if (roleRecognitionOrchestrator != null) {
			throw new QueryBuilderException("Role recognizer already configured");
		}
		roleRecognitionOrchestrator = RoleRecognitionOrchestrator.getInstance();
		process(roleRecognitionOrchestrator, "resource", "attr_role");
	}

	/**
	 * Attaches a given processor.
	 * 
	 * @param processor
	 * @param bindingVariable
	 * @param outputVariable
	 */
	public void process(QueryBuilderProcessor processor, String bindingVariable, String outputVariable) {
		if (attachedProcessors.contains(processor)) {
			throw new QueryBuilderException("Processors already attached: " + processor);
		}

		if (attachedProcessorsGraphPatternBinding.containsValue(outputVariable)) {
			throw new QueryBuilderException("Variable " + outputVariable + " already bound to a processor");
		}
		this.attachedProcessors.add(processor);
		this.attachedProcessorsGraphPatternBinding.put(processor,
				new GraphPatternBinding(bindingVariable, outputVariable));
	}

	/**
	 * Binds a variable to a given value.
	 * 
	 * @param name
	 * @param value
	 * @throws QueryBuilderException
	 */
	public void setBinding(String name, Value value) throws QueryBuilderException {
		if (bindingSet.containsKey(name)) {
			throw new QueryBuilderException("Variable " + name + " already bound to a value");
		}
		bindingSet.put(name, value);
	}

	/**
	 * Evaluates the (possibly enriched) query and returns the retrieved resources.
	 * 
	 * @return
	 * @throws QueryEvaluationException
	 */
	public Collection<AnnotatedValue<Resource>> runQuery()
			throws QueryBuilderException, QueryEvaluationException {
		Repository repo = serviceContext.getProject().getRepository();
		RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repo);
		try {
			return runQuery(conn);
		} finally {
			RDF4JRepositoryUtils.releaseConnection(conn, repo);
		}
	}

	/**
	 * Evaluates the (possibly enriched) query on the given repository connection and returns the retrieved
	 * resources.
	 * 
	 * @return
	 * @throws QueryEvaluationException
	 */
	public Collection<AnnotatedValue<Resource>> runQuery(RepositoryConnection conn) {
		QueryBuildOutput queryBuildOutput = computeEnrichedQuery();
		TupleQueryShallowModel enrichedQuery = queryBuildOutput.queryModel;

		String enrichedQueryString = enrichedQuery.linearize();
		Map<QueryBuilderProcessor, BiMap<String, String>> processorVariableBinding = queryBuildOutput.mangled2processorSpecificVariableMapping;

		logger.debug(enrichedQueryString);

		TupleQuery query = conn.prepareTupleQuery(enrichedQueryString);
		query.setIncludeInferred(false);
		
		bindingSet.forEach(query::setBinding);

		logger.debug("query binding set = {}", bindingSet);

		List<String> bindingNames;
		List<BindingSet> bindings;

		try (TupleQueryResult queryResults = query.evaluate()) {
			bindingNames = queryResults.getBindingNames();
			bindings = QueryResults.asList(queryResults).stream()
					.filter(bs -> bs.getValue("resource") != null).collect(toList());
		}

		logger.debug("binding count = {}", bindings.size());
		
		List<String> initialQueryVariables = enrichedQuery.getInitialQueryVariables();

		BiMap<String, String> projected2baseVarMapping = HashBiMap.create();
		initialQueryVariables.forEach(varName -> projected2baseVarMapping.put(varName, varName));

		TupleQueryResult projectedOverallResults = projectResults(bindings, projected2baseVarMapping);

		Map<Value, Map<String, Literal>> additionalColumns = new HashMap<>();

		for (Map.Entry<QueryBuilderProcessor, BiMap<String, String>> entry : processorVariableBinding
				.entrySet()) {
			QueryBuilderProcessor proc = entry.getKey();
			BiMap<String, String> projected2BaseVariableBinding = entry.getValue();

			List<BindingSet> projectedResults = QueryResults
					.asList(projectResults(bindings, projected2BaseVariableBinding));

			Map<Value, Literal> processorResults = proc.processBindings(serviceContext.getProject(),
					projectedResults);

			GraphPatternBinding graphPatternBinding = attachedProcessorsGraphPatternBinding.get(proc);

			if (processorResults != null) {

				for (Map.Entry<Value, Literal> individualResult : processorResults.entrySet()) {
					Map<String, Literal> row = additionalColumns.get(individualResult.getKey());
					if (row == null) {
						row = new HashMap<>();
						additionalColumns.put(individualResult.getKey(), row);
					}
					row.put(graphPatternBinding.getProjectedOutputVariable(), individualResult.getValue());
				}

			} else {

				String targetVariable = proc.getGraphPattern().getProjectionElement().getTargetVariable();
				String outputVariable = graphPatternBinding.getProjectedOutputVariable();

				for (BindingSet projectedResultsEntry : projectedResults) {
					Value resource = projectedResultsEntry.getValue(proc.getBindingVariable());

					Literal value = (Literal) projectedResultsEntry.getValue(targetVariable);

					if (value == null)
						continue; // Skip unbound variables

					Map<String, Literal> row = additionalColumns.get(resource);
					if (row == null) {
						row = new HashMap<>();
						additionalColumns.put(resource, row);
					}
					row.put(outputVariable, value);

				}
			}

		}

		return QueryResults.stream(projectedOverallResults)
				.map(bs -> bindingSet2annotatedResource(bs, additionalColumns)).collect(toList());
	}

	private static TupleQueryResult projectResults(List<BindingSet> queryResults,
			BiMap<String, String> projected2baseVarMapping) {
		return QueryResults.distinctResults(
				new IteratingTupleQueryResult(new ArrayList<String>(projected2baseVarMapping.values()),
						queryResults.stream().map(
								bindingSet -> new ProjectedBindingSet(bindingSet, projected2baseVarMapping))
						.collect(toList())));
	}

	private QueryBuildOutput computeEnrichedQuery() throws QueryBuilderException {

		TupleQueryShallowModel queryShallowModel = SPARQLShallowParser.getInstance()
				.parseTupleQuery(resourceQuery);

		QueryBuildOutput out = new QueryBuildOutput();
		out.queryModel = queryShallowModel;
		out.mangled2processorSpecificVariableMapping = new HashMap<QueryBuilderProcessor, BiMap<String, String>>();

		int counter = 0;
		for (QueryBuilderProcessor proc : attachedProcessors) {
			GraphPatternBinding graphPatternBinding = this.attachedProcessorsGraphPatternBinding.get(proc);
			GraphPattern gp = proc.getGraphPattern();

			counter++;
			final int tCounter = counter;

			BiMap<String, String> inverseVariableMapping = HashBiMap.create();
			inverseVariableMapping.put(graphPatternBinding.getProjectedBindingVariable(),
					proc.getBindingVariable());
			GraphPattern renamedGp = gp.renamed(varName -> "proc_" + tCounter + "_" + varName,
					inverseVariableMapping);

			inverseVariableMapping.values().removeIf(varName -> !varName.equals(proc.getBindingVariable())
					&& !varName.equals(gp.getProjectionElement().getTargetVariable()));

			try {
				queryShallowModel.appendGraphPattern(renamedGp);
			} catch (IllegalArgumentException e) {
				throw new QueryBuilderException(e);
			}

			out.mangled2processorSpecificVariableMapping.put(proc, inverseVariableMapping);
		}

		return out;
	}

	private static AnnotatedValue<Resource> bindingSet2annotatedResource(BindingSet bindingSet,
			Map<Value, Map<String, Literal>> additionalColumns) throws QueryBuilderException {
		Value resource = bindingSet.getValue("resource");

		if (resource == null) {
			throw new QueryBuilderException("Variable ?resource is unbound in binding set: " + bindingSet);
		}

		if (!(resource instanceof Resource)) {
			throw new QueryBuilderException("The value bound to ?resource is not a Resource: " + resource);
		}

		Map<String, Value> attributes = new HashMap<>();
		StreamSupport.stream(bindingSet.spliterator(), false)
				.filter(binding -> binding.getName().startsWith("attr_"))
				.forEach(binding -> attributes.put(binding.getName().substring(5), binding.getValue()));

		Map<String, Literal> row = additionalColumns.get(resource);

		if (row != null) {
			row.forEach((varName, boundValue) -> {
				if (varName.startsWith("attr_")) {
					attributes.put(varName.substring(5), boundValue);
				}
			});
		}

		AnnotatedValue<Resource> rv = new AnnotatedValue<Resource>((Resource) resource, attributes);

		return rv;
	}

}

class GraphPatternBinding {
	private String projectedBindingVariable;
	private String projectedOutputVariable;

	public GraphPatternBinding(String projectedBindingVariable, String projectedOutputVariable) {
		this.projectedBindingVariable = projectedBindingVariable;
		this.projectedOutputVariable = projectedOutputVariable;
	}

	public String getProjectedBindingVariable() {
		return projectedBindingVariable;
	}

	public String getProjectedOutputVariable() {
		return projectedOutputVariable;
	}
}