package it.uniroma2.art.semanticturkey.services.support;

import java.util.Map;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.TupleQueryResult;


/**
 * An object implementing this interface can be used to process the (raw) results produced by a
 * {@link QueryBuilder}, in order to generate the final results.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 * @param <T>
 */
public interface QueryResultsProcessor<T> {
	T process(TupleQueryResult overallQueryResults, Map<Value, Map<String, Literal>> additionalColumns);
}
