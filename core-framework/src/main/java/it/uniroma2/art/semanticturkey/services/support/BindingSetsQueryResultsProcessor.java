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
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.MapBindingSet;

import it.uniroma2.art.semanticturkey.services.AnnotatedValue;

/**
 * An object implementing this interface processed the results produced by a {@link QueryBuilder} generating
 * <code>BindingSet</code>s
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class BindingSetsQueryResultsProcessor
		implements QueryResultsProcessor<Collection<BindingSet>> {

	@Override
	public Collection<BindingSet> process(TupleQueryResult overallQueryResults,
			Map<Value, Map<String, Literal>> additionalColumns) {
		Collection<BindingSet> rv = new ArrayList<>();

		while (overallQueryResults.hasNext()) {
			BindingSet bindings = overallQueryResults.next();

			Value resource = bindings.getValue("resource");

			if (resource == null) {
				throw new QueryBuilderException("Variable ?resource is unbound in binding set: " + bindings);
			}

			if (!(resource instanceof Resource)) {
				throw new QueryBuilderException(
						"The value bound to ?resource is not a Resource: " + resource);
			}

			MapBindingSet bs = new MapBindingSet();
			bindings.forEach(bs::addBinding);

			Map<String, Literal> row = additionalColumns.get(resource);

			if (row != null) {
				row.forEach((varName, boundValue) -> {
					bs.addBinding(varName, boundValue);
				});
			}

			rv.add(bs);
		}
		return rv;
	}

}
