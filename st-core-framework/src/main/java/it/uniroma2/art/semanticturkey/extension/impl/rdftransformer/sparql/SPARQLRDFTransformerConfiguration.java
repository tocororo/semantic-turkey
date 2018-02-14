package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.sparql;

import it.uniroma2.art.semanticturkey.extension.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SPARQLRDFTransformerConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "SPARQL Update RDF Transformer";
	}
	
	@STProperty(description="SPARQL Update implementing the filter")
	@Required
	public String filter;
	
	@STProperty(description="Executes the SPARQL query on each graph separately")
	public boolean sliced = true;
}
