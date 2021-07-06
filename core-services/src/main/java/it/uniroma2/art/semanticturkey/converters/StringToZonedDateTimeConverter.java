package it.uniroma2.art.semanticturkey.converters;

import it.uniroma2.art.semanticturkey.properties.DataSize;
import org.springframework.core.convert.converter.Converter;

import java.time.ZonedDateTime;

/**
 * Constructs a {@link ZonedDateTime} object from a ISO-like date-time format, such as '2011-12-03T10:15:30+01:00[Europe/Paris]'.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToZonedDateTimeConverter implements Converter<String, ZonedDateTime> {
	public StringToZonedDateTimeConverter() {
	}

	@Override
	public ZonedDateTime convert(String s) {
		return ZonedDateTime.parse(s);
	}

	public static void main(String[] args) {
		new StringToZonedDateTimeConverter().convert("2011-12-03T10:15:30+01:00");
	}
}
