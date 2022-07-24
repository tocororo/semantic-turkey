package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal;

import it.uniroma2.art.semanticturkey.config.Configuration;

/**
 * Configuration class for {@link OntoPortalConnector} targeting
 * <a href="http://ecoportal.lifewatchitaly.eu/">EcoPortal</a>
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class EcoPortalConnectorConfiguration extends AbstractOntoPortalConnectorConfiguration
		implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.EcoPortalConnectorConfiguration";

		public static final String shortName = keyBase + ".shortName";
	}


	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

}
