package it.uniroma2.art.semanticturkey.converters;

import org.eclipse.rdf4j.model.Literal;

/**
 * Converts the NT serialization of a literal to an object implementing {@link Literal}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToRDF4JLiteralConverter extends AbstractStringToRDF4JValueConverter<Literal> {

	public StringToRDF4JLiteralConverter() {
		super(Literal.class);
	}
	
}
