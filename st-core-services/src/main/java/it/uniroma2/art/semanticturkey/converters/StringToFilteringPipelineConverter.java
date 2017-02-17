package it.uniroma2.art.semanticturkey.converters;

import java.io.IOException;

import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.uniroma2.art.semanticturkey.services.core.export.FilteringPipeline;

/**
 * Converts a (JSON) string to a {@link FilteringPipeline}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToFilteringPipelineConverter implements Converter<String, FilteringPipeline> {

	private final ObjectMapper mapper;

	public StringToFilteringPipelineConverter() {
		this.mapper = new ObjectMapper();
	}

	@Override
	public FilteringPipeline convert(String source) {
		try {
			return mapper.readValue(source, FilteringPipeline.class);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
