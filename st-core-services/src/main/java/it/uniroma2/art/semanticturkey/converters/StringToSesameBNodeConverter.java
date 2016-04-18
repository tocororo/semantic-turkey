package it.uniroma2.art.semanticturkey.converters;

import org.openrdf.model.BNode;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.rio.ntriples.NTriplesUtil;
import org.springframework.core.convert.converter.Converter;

/**
 * Converts the NT serialization of a blank node to an object implementing {@link BNode}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToSesameBNodeConverter implements Converter<String, BNode>{
	private final ValueFactory vf = SimpleValueFactory.getInstance();
	
	@Override
	public BNode convert(String NTTerm) {
		return NTriplesUtil.parseBNode(NTTerm, vf);
	}

}
