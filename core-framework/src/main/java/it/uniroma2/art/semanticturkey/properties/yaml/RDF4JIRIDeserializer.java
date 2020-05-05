package it.uniroma2.art.semanticturkey.properties.yaml;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * A {@link JsonDeserializer} for RDF4J {@link IRI}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RDF4JIRIDeserializer extends AbstractRDF4JValueDeserializer<IRI> {

	private static final long serialVersionUID = -4312939126047661986L;

	public RDF4JIRIDeserializer() {
		super(IRI.class);
	}

}
