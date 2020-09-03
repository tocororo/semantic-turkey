package it.uniroma2.art.semanticturkey.converters;

import java.util.Collections;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.ValidatingValueFactory;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

import it.uniroma2.art.semanticturkey.services.annotations.SkipTermValidation;

/**
 * Converts the NT serialization of an RDF term to an object implementing {@link Value}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class AbstractStringToRDF4JValueConverter<T extends Value> implements GenericConverter {
	private final ValueFactory vf = SimpleValueFactory.getInstance();
	private final ValueFactory vvf = new ValidatingValueFactory(vf);
	private final Class<T> expectedType;

	public AbstractStringToRDF4JValueConverter(Class<T> expectedType) {
		this.expectedType = expectedType;
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, expectedType));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		Value value = NTriplesUtil.parseValue((String) source,
				targetType.hasAnnotation(SkipTermValidation.class) ? vf : vvf);

		Class<? extends Value> actualType = value.getClass();

		if (expectedType.isAssignableFrom(actualType)) {
			return expectedType.cast(value);
		} else {
			throw new WrongRDFTermException(actualType, expectedType);
		}
	}

}
