package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.showvoc;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link ShowVocConnector}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ShowVocConnectorConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.showvoc.ShowVocConnectorConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String apiBaseURL$description = keyBase + ".apiBaseURL.description";
		public static final String apiBaseURL$displayName = keyBase + ".apiBaseURL.displayName";
		public static final String frontendBaseURL$description = keyBase + ".frontendBaseURL.description";
		public static final String frontendBaseURL$displayName = keyBase + ".frontendBaseURL.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.apiBaseURL$description + "}", displayName = "{" + MessageKeys.apiBaseURL$displayName + "}")
	@Required
	public String apiBaseURL;

	@STProperty(description = "{" + MessageKeys.frontendBaseURL$description + "}", displayName = "{" + MessageKeys.frontendBaseURL$displayName + "}")
	public String frontendBaseURL;

}
