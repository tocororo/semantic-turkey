package it.uniroma2.art.semanticturkey.utilities;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.AbstractValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * A {@link ValueFactory} that tries to recover from errors such an {@code rdf:langString} literal without
 * language tag.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ErrorRecoveringValueFactory extends AbstractValueFactory {

	private static ErrorRecoveringValueFactory instance = new ErrorRecoveringValueFactory();

	public static ErrorRecoveringValueFactory getInstance() {
		return instance;
	}

	protected ErrorRecoveringValueFactory() {
	}

	@Override
	public Literal createLiteral(String value, IRI datatype) {
		if (RDF.LANGSTRING.equals(datatype)) {
			return super.createLiteral(value, "und");
		} else {
			return super.createLiteral(value, datatype);
		}
	}
}
