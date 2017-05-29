package it.uniroma2.art.semanticturkey.converters;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StringToValueMapConverter implements Converter<String, Map<String, Value>> {

	private final ObjectMapper mapper;

	public StringToValueMapConverter() {
		this.mapper = new ObjectMapper();
	}

	@Override
	public Map<String, Value> convert(String source) {
		try {
			Map<String, String> rawMap = mapper.readValue(source, new TypeReference<Map<String, String>>() {
			});

			return rawMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
					e -> NTriplesUtil.parseValue(e.getValue(), SimpleValueFactory.getInstance())));
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
