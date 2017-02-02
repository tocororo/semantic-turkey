package it.uniroma2.art.semanticturkey.converters;

import java.io.IOException;

import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.uniroma2.art.semanticturkey.services.core.export.FilteringStep;

/**
 * Converts a (JSON) string to a {@link FilteringStep}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToFilteringStepConverter implements Converter<String, FilteringStep> {

	private final ObjectMapper mapper;

	public StringToFilteringStepConverter() {
		this.mapper = new ObjectMapper();
	}

	@Override
	public FilteringStep convert(String source) {
		try {
			return mapper.readValue(source, FilteringStep.class);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
