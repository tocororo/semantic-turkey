package it.uniroma2.art.semanticturkey.converters;

import org.eclipse.rdf4j.model.Value;

/**
 * Converts the NT serialization of an RDF term to an object implementing {@link Value}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToRDF4JValueConverter extends AbstractStringToRDF4JValueConverter<Value>{
	public StringToRDF4JValueConverter() {
		super(Value.class);
	}
}
