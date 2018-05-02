package it.uniroma2.art.semanticturkey.extension.impl.deployer.http;

import java.net.URL;
import java.util.Map;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link HTTPDeployerFactory}.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class HTTPDeployerConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "HTTP Deployer Configuration";
	}

	@Override
	public String getHTMLWarning() {
		return "The credentials are stored wihtout encryption on the server. "
				+ "Be aware that the system administration could be able to see them.";
	}

	@STProperty(description = "The address of the endpoint where data will be deployed onto", displayName = "Endpoint")
	@Required
	public URL endpoint;

	@STProperty(description = "HTTP verb to use", displayName = "HTTP Verb")
	@Enumeration({ "PUT", "POST" })
	@Required
	public String httpVerb;

	@STProperty(description = "Additional query parameters", displayName = "Query Parameters")
	public Map<String, String> queryParameters;

	@STProperty(description = "Additional request headers", displayName = "Request Headers")
	public Map<String, String> requestHeaders;

	@STProperty(description = "Username", displayName = "Username")
	public String username;

	@STProperty(description = "Password", displayName = "Password")
	public String password;

}
