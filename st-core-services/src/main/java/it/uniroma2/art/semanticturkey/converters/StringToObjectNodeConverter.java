package it.uniroma2.art.semanticturkey.converters;

import java.io.IOException;

import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;

/**
 * Converts a string to an {@link ObjectNode}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToObjectNodeConverter implements Converter<String, ObjectNode> {

	private final ObjectMapper mapper;

	public StringToObjectNodeConverter() {
		this.mapper = new ObjectMapper();
	}

	@Override
	public ObjectNode convert(String source) {
		try {
			JsonNode rv = mapper.readTree(source);
			if (!(rv instanceof ObjectNode)) {
				throw new IllegalArgumentException(
						"ObjectNode expected, but " + rv.getClass().getSimpleName() + " given");
			}
			return (ObjectNode) rv;
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
