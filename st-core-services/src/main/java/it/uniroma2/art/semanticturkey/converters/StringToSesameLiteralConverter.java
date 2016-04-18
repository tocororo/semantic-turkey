package it.uniroma2.art.semanticturkey.converters;

import org.openrdf.model.Literal;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.rio.ntriples.NTriplesUtil;
import org.springframework.core.convert.converter.Converter;

/**
 * Converts the NT serialization of a literal to an object implementing {@link Literal}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToSesameLiteralConverter implements Converter<String, Literal>{
	private final ValueFactory vf = SimpleValueFactory.getInstance();
	
	@Override
	public Literal convert(String NTTerm) {
		return NTriplesUtil.parseLiteral(NTTerm, vf);
	}

}
