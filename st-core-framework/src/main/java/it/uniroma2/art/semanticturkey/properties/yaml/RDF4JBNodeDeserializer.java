package it.uniroma2.art.semanticturkey.properties.yaml;

import org.eclipse.rdf4j.model.BNode;

import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * A {@link JsonDeserializer} for RDF4J {@link BNode}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RDF4JBNodeDeserializer extends AbstractRDF4JValueDeserializer<BNode> {

	private static final long serialVersionUID = 4066658323579113069L;

	public RDF4JBNodeDeserializer() {
		super(BNode.class);
	}

}
