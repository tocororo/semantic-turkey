package it.uniroma2.art.semanticturkey.converters;

import java.io.IOException;

import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.uniroma2.art.semanticturkey.customform.CustomFormValue;

public class StringToCustomFormValueConverter implements Converter<String, CustomFormValue> {
	
	private final ObjectMapper mapper;

	public StringToCustomFormValueConverter() {
		this.mapper = new ObjectMapper();
	}

	@Override
	public CustomFormValue convert(String source) {
		try {
			return mapper.readValue(source, CustomFormValue.class);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
