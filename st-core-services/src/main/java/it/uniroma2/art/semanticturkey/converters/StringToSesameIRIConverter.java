package it.uniroma2.art.semanticturkey.converters;

import org.openrdf.model.IRI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.rio.ntriples.NTriplesUtil;
import org.springframework.core.convert.converter.Converter;

/**
 * Converts the NT serialization of an (escaped) IRI to an object implementing {@link IRI}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToSesameIRIConverter implements Converter<String, IRI>{
	private final ValueFactory vf = SimpleValueFactory.getInstance();
	
	@Override
	public IRI convert(String NTTerm) {
		return NTriplesUtil.parseURI(NTTerm, vf);
	}

}
