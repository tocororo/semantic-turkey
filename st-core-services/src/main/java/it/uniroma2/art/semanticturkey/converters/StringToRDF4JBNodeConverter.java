package it.uniroma2.art.semanticturkey.converters;

import org.eclipse.rdf4j.model.BNode;

/**
 * Converts the NT serialization of a blank node to an object implementing {@link BNode}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToRDF4JBNodeConverter extends AbstractStringToRDF4JValueConverter<BNode> {

	public StringToRDF4JBNodeConverter() {
		super(BNode.class);
	}

}
