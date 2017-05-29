package it.uniroma2.art.semanticturkey.services.core.sparql;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.MapBindingSet;

/**
 * Adapts a {@link GraphQueryResult} to a {@link TupleQueryResult}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class Graph2TupleQueryResultAdapter
		implements CloseableIteration<BindingSet, QueryEvaluationException> {

	private GraphQueryResult delegate;

	public Graph2TupleQueryResultAdapter(GraphQueryResult queryResult) {
		this.delegate = queryResult;
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		return delegate.hasNext();
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		Statement st = delegate.next();

		MapBindingSet bs = new MapBindingSet();
		bs.addBinding("subj", st.getSubject());
		bs.addBinding("pred", st.getPredicate());
		bs.addBinding("obj", st.getObject());

		return bs;
	}

	@Override
	public void remove() throws QueryEvaluationException {
		delegate.remove();
	}

	@Override
	public void close() throws QueryEvaluationException {
		delegate.close();
	}

}