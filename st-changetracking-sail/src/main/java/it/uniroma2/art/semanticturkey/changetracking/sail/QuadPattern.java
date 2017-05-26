package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

/**
 * A quadruple pattern.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class QuadPattern {
	private Resource subject;
	private IRI predicate;
	private Value object;
	private Resource context;

	public QuadPattern(Resource subject, IRI predicate, Value object, Resource context) {
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.context = context;
	}

	public Resource getSubject() {
		return subject;
	}

	public IRI getPredicate() {
		return predicate;
	}

	public Value getObject() {
		return object;
	}

	public Resource getContext() {
		return context;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof QuadPattern))
			return false;

		QuadPattern objQuad = (QuadPattern) obj;

		return Objects.equals(subject, objQuad.subject) && Objects.equals(predicate, objQuad.predicate)
				&& Objects.equals(object, objQuad.object) && Objects.equals(context, objQuad.context);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, predicate, object, context);
	}
}
