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
public class AbstractStringToRDF4JValueConverter<T extends Value> implements Converter<String, T>{
	private final ValueFactory vf = SimpleValueFactory.getInstance();
	private final Class<T> expectedType;
	
	public AbstractStringToRDF4JValueConverter(Class<T> expectedType) {
		this.expectedType = expectedType;
	}
	
	@Override
	public T convert(String NTTerm) {
		Value value = NTriplesUtil.parseValue(NTTerm, vf);
		
		Class<? extends Value> actualType = value.getClass();
		
		if (expectedType.isAssignableFrom(actualType)) {
			return expectedType.cast(value);
		} else {
			throw new WrongRDFTermException(actualType, expectedType);
		}
	}

}
