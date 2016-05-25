package it.uniroma2.art.semanticturkey.converters;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.springframework.core.convert.converter.Converter;

/**
 * Converts the NT serialization of a literal to an object implementing {@link Literal}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToRDF4JLiteralConverter implements Converter<String, Literal>{
	private final ValueFactory vf = SimpleValueFactory.getInstance();
	
	@Override
	public Literal convert(String NTTerm) {
		return NTriplesUtil.parseLiteral(NTTerm, vf);
	}

}
