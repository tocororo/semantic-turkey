package it.uniroma2.art.semanticturkey.converters;

import java.io.IOException;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class StringToMapConverter implements Converter<String, Map<String, Object>> {
	
	private final ObjectMapper mapper;

	public StringToMapConverter() {
		this.mapper = new ObjectMapper();
	}

	@Override
	public Map<String, Object> convert(String source) {
		try {
			return mapper.readValue(source, new TypeReference<Map<String, Object>>(){});
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}
	

}
