package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Abstract base class of configuration classes for the {@link OntoPortalConnectorFactory}. The parameter
 * {@link #apiBaseURL} is optional, as long as concrete subclasses assume a sensible default or tighten its
 * definition making it mandatory.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class AbstractOntoPortalConnectorConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.AbstractOntoPortalConnectorConfiguration";

		public static final String htmlWarning = keyBase + ".htmlWarning";
		public static final String frontendBaseURL$description = keyBase + ".frontendBaseURL.description";
		public static final String frontendBaseURL$displayName = keyBase + ".frontendBaseURL.displayName";
		public static final String apiBaseURL$description = keyBase + ".apiBaseURL.description";
		public static final String apiBaseURL$displayName = keyBase + ".apiBaseURL.displayName";
		public static final String apiKey$description = keyBase + ".apiKey.description";
		public static final String apiKey$displayName = keyBase + ".apiKey.displayName";
		public static final String retrieveSubmissions$description = keyBase + ".retrieveSubmissions.description";
		public static final String retrieveSubmissions$displayName = keyBase + ".retrieveSubmissions.displayName";
	}

	@Override
	public String getHTMLWarning() {
		return "{" + MessageKeys.htmlWarning + "}";
	}

	@STProperty(description = "{" + MessageKeys.frontendBaseURL$description+ "}", displayName = "{" + MessageKeys.frontendBaseURL$displayName + "}")
	public String frontendBaseURL;

	@STProperty(description = "{" + MessageKeys.apiBaseURL$description+ "}", displayName = "{" + MessageKeys.apiBaseURL$displayName + "}")
	public String apiBaseURL;

	@STProperty(description = "{" + MessageKeys.apiKey$description+ "}", displayName = "{" + MessageKeys.apiKey$displayName + "}")
	public String apiKey;

	@STProperty(description = "{" + MessageKeys.retrieveSubmissions$description + "}", displayName = "{" + MessageKeys.retrieveSubmissions$displayName + "}")
	public Boolean retrieveSubmissions;
}
