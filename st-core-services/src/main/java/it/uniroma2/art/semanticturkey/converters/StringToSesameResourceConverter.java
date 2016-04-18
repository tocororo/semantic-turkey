package it.uniroma2.art.semanticturkey.converters;

import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.rio.ntriples.NTriplesUtil;
import org.springframework.core.convert.converter.Converter;

/**
 * Converts the NT serialization of a resource (either an escaped IRI or a blank node) to an object implementing {@link Resource}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToSesameResourceConverter implements Converter<String, Resource>{
	private final ValueFactory vf = SimpleValueFactory.getInstance();
	
	@Override
	public Resource convert(String NTTerm) {
		return NTriplesUtil.parseResource(NTTerm, vf);
	}

}
