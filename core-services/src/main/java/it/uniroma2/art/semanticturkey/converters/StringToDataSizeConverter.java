package it.uniroma2.art.semanticturkey.converters;

import it.uniroma2.art.semanticturkey.properties.DataSize;
import org.springframework.core.convert.converter.Converter;

/**
 * Constructs a {@link DataSize} object from its string serialization (e.g. 2 MiB)
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToDataSizeConverter implements Converter<String, DataSize> {
	public StringToDataSizeConverter() {
	}

	@Override
	public DataSize convert(String s) {
		return DataSize.valueOf(s);
	}
}
