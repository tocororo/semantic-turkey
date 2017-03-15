package it.uniroma2.art.semanticturkey.converters;

import java.io.IOException;

import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.uniroma2.art.semanticturkey.project.RepositoryAccess;

/**
 * Converts a JSON string to a {@link RepositoryAccess} describing different access options.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToRepositoryAccessConverter implements Converter<String, RepositoryAccess> {

	private final ObjectMapper mapper;

	public StringToRepositoryAccessConverter() {
		this.mapper = new ObjectMapper();
	}

	@Override
	public RepositoryAccess convert(String source) {
		try {
			return mapper.readValue(source, RepositoryAccess.class);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
