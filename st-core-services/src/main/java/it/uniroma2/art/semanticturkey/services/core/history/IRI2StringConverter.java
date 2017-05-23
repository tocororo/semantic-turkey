package it.uniroma2.art.semanticturkey.services.core.history;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * A Jackson {@link Converter} that transforms an RDF4J {@link IRI} into a {@link String}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class IRI2StringConverter extends StdConverter<IRI, String> {

	@Override
	public String convert(IRI value) {
		return value.toString();
	}

}
