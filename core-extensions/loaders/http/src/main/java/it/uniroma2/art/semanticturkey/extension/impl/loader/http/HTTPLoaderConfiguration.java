package it.uniroma2.art.semanticturkey.extension.impl.loader.http;

import java.net.URL;
import java.util.Map;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link HTTPLoaderFactory}.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class HTTPLoaderConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.loader.http.HTTPLoaderConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String htmlWarning = keyBase + ".htmlWarning";
		public static final String endpoint$description = keyBase + ".endpoint.description";
		public static final String endpoint$displayName = keyBase + ".endpoint.displayName";
		public static final String queryParameters$description = keyBase + ".queryParameters.description";
		public static final String queryParameters$displayName = keyBase + ".queryParameters.displayName";
		public static final String requestHeaders$description = keyBase + ".requestHeaders.description";
		public static final String requestHeaders$displayName = keyBase + ".requestHeaders.displayName";
		public static final String enableContentNegotiation$description = keyBase + ".enableContentNegotiation.description";
		public static final String enableContentNegotiation$displayName = keyBase + ".enableContentNegotiation.displayName";
		public static final String reportContentType$description = keyBase + ".reportContentType.description";
		public static final String reportContentType$displayName = keyBase + ".reportContentType.displayName";
		public static final String username$description = keyBase + ".username.description";
		public static final String username$displayName = keyBase + ".username.displayName";
		public static final String password$description = keyBase + ".password.description";
		public static final String password$displayName = keyBase + ".password.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@Override
	public String getHTMLWarning() {
		return "{" + MessageKeys.htmlWarning + "}";
	}

	@STProperty(description = "{" + MessageKeys.endpoint$description + "}", displayName = "{" + MessageKeys.endpoint$displayName + "}")
	@Required
	public URL endpoint;

	@STProperty(description = "{" + MessageKeys.queryParameters$description + "}", displayName = "{" + MessageKeys.queryParameters$displayName + "}")
	public Map<String, String> queryParameters;

	@STProperty(description = "{" + MessageKeys.requestHeaders$description + "}", displayName = "{" + MessageKeys.requestHeaders$displayName + "}")
	public Map<String, String> requestHeaders;

	@STProperty(description = "{" + MessageKeys.enableContentNegotiation$description + "}", displayName = "{" + MessageKeys.enableContentNegotiation$displayName + "}")
	@Required
	public Boolean enableContentNegotiation = true;

	@STProperty(description = "{" + MessageKeys.reportContentType$description + "}", displayName = "{" + MessageKeys.reportContentType$displayName + "}")
	@Required
	public Boolean reportContentType = true;

	@STProperty(description = "{" + MessageKeys.username$description + "}", displayName = "{" + MessageKeys.username$displayName + "}")
	public String username;

	@STProperty(description = "{" + MessageKeys.password$description + "}", displayName = "{" + MessageKeys.password$displayName + "}")
	public String password;

}
