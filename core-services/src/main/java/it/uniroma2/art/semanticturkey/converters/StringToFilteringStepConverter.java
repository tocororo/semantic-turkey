package it.uniroma2.art.semanticturkey.converters;

import java.io.IOException;

import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.uniroma2.art.semanticturkey.services.core.export.TransformationStep;

/**
 * Converts a (JSON) string to a {@link TransformationStep}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToFilteringStepConverter implements Converter<String, TransformationStep> {

	private final ObjectMapper mapper;

	public StringToFilteringStepConverter() {
		this.mapper = new ObjectMapper();
	}

	@Override
	public TransformationStep convert(String source) {
		try {
			return mapper.readValue(source, TransformationStep.class);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
