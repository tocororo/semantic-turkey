package it.uniroma2.art.semanticturkey.properties.yaml;

import org.eclipse.rdf4j.model.Literal;

import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * A {@link JsonDeserializer} for RDF4J {@link Literal}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RDF4JLiteralDeserializer extends AbstractRDF4JValueDeserializer<Literal> {

	private static final long serialVersionUID = 4930795995987715093L;

	public RDF4JLiteralDeserializer() {
		super(Literal.class);
	}

}
