package it.uniroma2.art.semanticturkey.services.support;

import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;

import java.util.List;
import java.util.Map;

/**
 * A @code {@link QueryBuilderProcessor} allows to customize the query performed via a {@link QueryBuilder}.
 */
public interface QueryBuilderProcessor {
	GraphPattern getGraphPattern(STServiceContext context);
	boolean introducesDuplicates();
	default boolean requiresOptionalWrapper() { return true; }
	Map<Value, Literal> processBindings(STServiceContext context, List<BindingSet> resultTable);
	default void processBindings(List<BindingSet> resultTable) {}
	String getBindingVariable();
}
