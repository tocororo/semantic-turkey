package it.uniroma2.art.semanticturkey.converters;

import org.eclipse.rdf4j.model.Value;

/**
 * An exception occurring when a legal RDF term is given but an incompatible type of term is expected: for
 * example, a literal is given, but a resource (either IRI or bnode) is expected.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class WrongRDFTermException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;
	private Class<? extends Value> actualType;
	private Class<? extends Value> expectedType;

	public WrongRDFTermException(Class<? extends Value> actualType, Class<? extends Value> expectedType) {
		this.actualType = actualType;
		this.expectedType = expectedType;
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder();

		sb.append(actualType.getSimpleName()).append(" given, but ").append(expectedType.getSimpleName())
				.append(" expected");

		return sb.toString();
	}
}
