package it.uniroma2.art.semanticturkey.properties.yaml;

import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * /** A {@link JsonDeserializer} for RDF4J {@link Value}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RDF4JValueDeserializer extends AbstractRDF4JValueDeserializer<Value> {
	private static final long serialVersionUID = 1432940744647617486L;

	public RDF4JValueDeserializer() {
		super(Value.class);
	}

}
