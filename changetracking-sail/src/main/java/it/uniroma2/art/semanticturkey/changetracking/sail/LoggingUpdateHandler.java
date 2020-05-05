package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.UpdateContext;

/**
 * An {@link UpdateHandler} logging requested operations.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class LoggingUpdateHandler extends BaseUpdateHandler {
	private List<QuadPattern> additions;
	private List<QuadPattern> removals;

	public LoggingUpdateHandler() {
		additions = new LinkedList<>();
		removals = new LinkedList<>();
	}

	@Override
	public void addStatement(Resource subj, IRI pred, Value obj, Resource[] newContexts) {
		for (Resource c : newContexts) {
			additions.add(new QuadPattern(subj, pred, obj, c));
		}
	}

	@Override
	public void addStatement(UpdateContext modify, Resource subj, IRI pred, Value obj,
			Resource[] newContexts) {
		for (Resource c : newContexts) {
			additions.add(new QuadPattern(subj, pred, obj, c));
		}
	}

	@Override
	public void removeStatements(Resource subj, IRI pred, Value obj, Resource[] newContexts) {
		for (Resource c : newContexts) {
			removals.add(new QuadPattern(subj, pred, obj, c));
		}
	}

	@Override
	public void removeStatement(UpdateContext modify, Resource subj, IRI pred, Value obj,
			Resource[] newContexts) {
		for (Resource c : newContexts) {
			removals.add(new QuadPattern(subj, pred, obj, c));
		}
	}

	@Override
	public void clear(Resource[] contexts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearNamespaces() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeNamespace(String prefix) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isReadOnly() {
		return additions.isEmpty() && removals.isEmpty();
	}

	@Override
	public void clearHandler(IRI... contexts) {
		if (contexts.length == 0) {
			additions.clear();
			removals.clear();
		} else {
			for (IRI ctx : contexts) {
				additions.removeIf(qp -> Objects.equals(qp.getContext(), ctx));
				removals.removeIf(qp -> Objects.equals(qp.getContext(), ctx));
			}
		}
	}

	public Iterable<QuadPattern> getAdditions() {
		return Collections.unmodifiableList(additions);
	}

	public Iterable<QuadPattern> getRemovals() {
		return Collections.unmodifiableList(removals);
	}

}
