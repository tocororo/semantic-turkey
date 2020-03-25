package it.uniroma2.art.semanticturkey.extension.impl.customservice.sparql;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link SPARQLCustomServiceBackendFactory}.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SPARQLCustomServiceBackendConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "SPARQL Custom Service Configuration";
	}

	@STProperty(description = "Implementation of a custom service operation in SPARQL", displayName = "SPARQL")
	@Required
	public String sparql;

}
