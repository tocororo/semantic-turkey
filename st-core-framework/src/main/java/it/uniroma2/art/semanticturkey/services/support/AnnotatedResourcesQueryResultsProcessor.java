package it.uniroma2.art.semanticturkey.services.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;

import it.uniroma2.art.semanticturkey.services.AnnotatedValue;

/**
 * An object implementing this interface processed the results produced by a {@link QueryBuilder} generating
 * <code>AnnotatedValue&lt;Resource&gt;</code>s
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class AnnotatedResourcesQueryResultsProcessor
		implements QueryResultsProcessor<Collection<AnnotatedValue<Resource>>> {

	private String resourceVariableName;

	public AnnotatedResourcesQueryResultsProcessor() {
		this("resource");
	}

	public AnnotatedResourcesQueryResultsProcessor(String resourceVariableName) {
		this.resourceVariableName = resourceVariableName;
	}

	@Override
	public Collection<AnnotatedValue<Resource>> process(TupleQueryResult overallQueryResults,
			Map<Value, Map<String, Literal>> additionalColumns) {
		Collection<AnnotatedValue<Resource>> rv = new ArrayList<>();

		while (overallQueryResults.hasNext()) {

			BindingSet bindings = overallQueryResults.next();

			Value resource = bindings.getValue(resourceVariableName);

			if (resource == null) {
				throw new QueryBuilderException(
						"Variable ?" + resourceVariableName + " is unbound in binding set: " + bindings);
			}

			if (!(resource instanceof Resource)) {
				throw new QueryBuilderException(
						"The value bound to ?" + resourceVariableName + " is not a Resource: " + resource);
			}

			Map<String, Value> attributes = new HashMap<>();
			StreamSupport.stream(bindings.spliterator(), false)
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

			rv.add(new AnnotatedValue<Resource>((Resource) resource, attributes));
		}
		return rv;
	}

}
