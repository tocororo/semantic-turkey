package it.uniroma2.art.semanticturkey.converters;

import java.io.IOException;

import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;

/**
 * Converts a string to a {@link PluginSpecification}, containing a plugin factoryId and its configuration
 * parameter
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToPluginSpecificationConverter implements Converter<String, PluginSpecification> {

	private final ObjectMapper mapper;

	public StringToPluginSpecificationConverter() {
		this.mapper = new ObjectMapper();
	}

	@Override
	public PluginSpecification convert(String source) {
		try {
			return mapper.readValue(source, PluginSpecification.class);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
