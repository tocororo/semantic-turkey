package it.uniroma2.art.semanticturkey.converters;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.springframework.core.convert.converter.Converter;

import com.google.common.collect.Sets;

/**
 * Converts the string serialization of an RDF4J {@link RDFFormat}. The mapping is based on
 * {@link RDFParserRegistry#getKeys()} and {@link RDFWriterRegistry#getKeys()}.
 * 
 * @see {@link InputFormat}
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToRDF4JRDFormatConverter implements Converter<String, RDFFormat> {

	@Override
	public RDFFormat convert(String source) {
		return Sets
				.union(RDFParserRegistry.getInstance().getKeys(), RDFWriterRegistry.getInstance().getKeys())
				.stream().filter(f -> f.getName().equals(source)).findAny().orElseThrow(() -> {
					return new IllegalArgumentException("Unknown RDF Format: " + source);
				});
	}

}
