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

	@Override
	public String getShortName() {
		return "HTTP Loader Configuration";
	}

	@Override
	public String getHTMLWarning() {
		return "The credentials are stored wihtout encryption on the server. "
				+ "Be aware that the system administration could be able to see them.";
	}

	@STProperty(description = "The address of the endpoint where data will be loaded from", displayName = "Endpoint")
	@Required
	public URL endpoint;

	@STProperty(description = "Additional query parameters", displayName = "Query Parameters")
	public Map<String, String> queryParameters;

	@STProperty(description = "Additional request headers", displayName = "Request Headers")
	public Map<String, String> requestHeaders;

	@STProperty(description = "Enables content negotiation based on the user-supplied data format", displayName = "Enable Content Negotiation")
	@Required
	public Boolean enableContentNegotiation = true;

	@STProperty(description = "Tells the loader to report the content-type received by the server", displayName = "Report Content-Type")
	@Required
	public Boolean reportContentType = true;

	@STProperty(description = "Username", displayName = "Username")
	public String username;

	@STProperty(description = "Password", displayName = "Password")
	public String password;

}
