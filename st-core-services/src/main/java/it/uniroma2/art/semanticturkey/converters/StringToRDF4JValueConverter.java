package it.uniroma2.art.semanticturkey.converters;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.springframework.core.convert.converter.Converter;

/**
 * Converts the NT serialization of an RDF term to an object implementing {@link Value}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToRDF4JValueConverter implements Converter<String, Value>{
	private final ValueFactory vf = SimpleValueFactory.getInstance();
	
	@Override
	public Value convert(String NTTerm) {
		return NTriplesUtil.parseValue(NTTerm, vf);
	}

}
