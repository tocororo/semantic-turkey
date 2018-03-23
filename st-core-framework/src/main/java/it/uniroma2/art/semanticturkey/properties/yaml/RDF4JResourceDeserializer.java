package it.uniroma2.art.semanticturkey.properties.yaml;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Resource;

import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * A {@link JsonDeserializer} for RDF4J {@link BNode}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RDF4JResourceDeserializer extends AbstractRDF4JValueDeserializer<Resource> {

	private static final long serialVersionUID = 5889016958439131104L;

	public RDF4JResourceDeserializer() {
		super(Resource.class);
	}
}
