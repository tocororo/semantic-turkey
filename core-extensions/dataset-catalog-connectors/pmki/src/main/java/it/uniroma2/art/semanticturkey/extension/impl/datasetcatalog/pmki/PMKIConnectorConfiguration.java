package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.pmki;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link PMKIConnector}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PMKIConnectorConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "PMKI Connector";
	}

	@STProperty(description = "The base URL of an implementation of the PMKI REST API", displayName = "API Base URL")
	@Required
	public String apiBaseURL;

	@STProperty(description = "The base URL of the frontend of a PMKI instance", displayName = "Frontend Base URL")
	public String frontendBaseURL;

}
