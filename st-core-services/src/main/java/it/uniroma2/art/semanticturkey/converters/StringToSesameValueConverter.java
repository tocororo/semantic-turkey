package it.uniroma2.art.semanticturkey.converters;

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.rio.ntriples.NTriplesUtil;
import org.springframework.core.convert.converter.Converter;

/**
 * Converts the NT serialization of an RDF term to an object implementing {@link Value}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToSesameValueConverter implements Converter<String, Value>{
	private final ValueFactory vf = SimpleValueFactory.getInstance();
	
	@Override
	public Value convert(String NTTerm) {
		return NTriplesUtil.parseValue(NTTerm, vf);
	}

}
