package it.uniroma2.art.semanticturkey.utilities;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * A Jackson {@link Converter} that transforms a {@link String} into an RDF4J {@link IRI}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class String2IRIConverter extends StdConverter<String, IRI> {

	@Override
	public IRI convert(String value) {
		return SimpleValueFactory.getInstance().createIRI(value);
	}

}
