package it.uniroma2.art.semanticturkey.utilities;

import java.util.Optional;

import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * A Jackson {@link Converter} that transforms an {@link Optional} to a possibly {@code null} object.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class Optional2StringConverter<T> extends StdConverter<Optional<T>, String> {

	@Override
	public String convert(Optional<T> value) {
		return value.map(Object::toString).orElse(null);
	}

}
