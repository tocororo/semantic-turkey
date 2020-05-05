package it.uniroma2.art.semanticturkey.extension.impl.customservice.sparql;

import it.uniroma2.art.semanticturkey.config.customservice.Operation;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link SPARQLCustomServiceBackendFactory}.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SPARQLOperation extends Operation {

	@Override
	public String getShortName() {
		return "SPARQL Custom Service Operation";
	}

	@STProperty(description = "Implementation of a custom service operation in SPARQL", displayName = "SPARQL")
	@Required
	public String sparql;

}
