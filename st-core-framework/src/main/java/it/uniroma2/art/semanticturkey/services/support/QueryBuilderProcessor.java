package it.uniroma2.art.semanticturkey.services.support;

import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;

/**
 * A @code {@link QueryBuilderProcessor} allows to customize the query performed via a {@link QueryBuilder}.
 */
public interface QueryBuilderProcessor {
	GraphPattern getGraphPattern(Project<?> currentProject);
	boolean introducesDuplicates();
	Map<Value, Literal> processBindings(Project<?> currentProject, List<BindingSet> resultTable);
	default void processBindings(List<BindingSet> resultTable) {}
	String getBindingVariable();
}
