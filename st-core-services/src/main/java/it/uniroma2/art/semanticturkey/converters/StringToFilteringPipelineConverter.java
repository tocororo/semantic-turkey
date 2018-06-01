package it.uniroma2.art.semanticturkey.converters;

import java.io.IOException;

import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.uniroma2.art.semanticturkey.services.core.export.TransformationPipeline;

/**
 * Converts a (JSON) string to a {@link TransformationPipeline}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToFilteringPipelineConverter implements Converter<String, TransformationPipeline> {

	private final ObjectMapper mapper;

	public StringToFilteringPipelineConverter() {
		this.mapper = new ObjectMapper();
	}

	@Override
	public TransformationPipeline convert(String source) {
		try {
			return mapper.readValue(source, TransformationPipeline.class);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
