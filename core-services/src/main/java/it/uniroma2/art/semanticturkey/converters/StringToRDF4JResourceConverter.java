package it.uniroma2.art.semanticturkey.converters;

import org.eclipse.rdf4j.model.Resource;

/**
 * Converts the NT serialization of a resource (either an escaped IRI or a blank node) to an object
 * implementing {@link Resource}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToRDF4JResourceConverter extends AbstractStringToRDF4JValueConverter<Resource> {
	public StringToRDF4JResourceConverter() {
		super(Resource.class);
	}
}
