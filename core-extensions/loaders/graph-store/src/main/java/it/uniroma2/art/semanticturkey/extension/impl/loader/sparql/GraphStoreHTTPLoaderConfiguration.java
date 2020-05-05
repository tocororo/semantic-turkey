package it.uniroma2.art.semanticturkey.extension.impl.loader.sparql;

import java.net.URL;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link GraphStoreHTTPLoaderFactory}.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class GraphStoreHTTPLoaderConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "Graph Store HTTP Loader Configuration";
	}

	@Override
	public String getHTMLWarning() {
		return "Either &quot;<em>graphStoreHTTPEndpoint</em>&quot; or &quot;<em>sourceGraph</em>&quot; shall be set."
				+ "The credentials are stored wihtout encryption on the server. "
				+ "Be aware that the system administration could be able to see them.";
	}

	@STProperty(description = "The address of the endpoint conforming to the HTTP Graph Store protocol. If not provided, it is assumed that the source graph is identified directly", displayName = "Graph Store HTTP endpoint")
	public URL graphStoreHTTPEndpoint;

	@STProperty(description = "The graph where data should be loaded from. If not provided, the default graph is used", displayName = "Source graph")
	public IRI sourceGraph;

	@STProperty(description = "Username", displayName = "Username")
	public String username;

	@STProperty(description = "Password", displayName = "Password")
	public String password;

}
