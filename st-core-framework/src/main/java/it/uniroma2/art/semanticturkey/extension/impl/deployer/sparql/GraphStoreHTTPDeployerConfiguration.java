package it.uniroma2.art.semanticturkey.extension.impl.deployer.sparql;

import java.net.URL;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link GraphStoreHTTPDeployerFactory}.
 */
public class GraphStoreHTTPDeployerConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "Graph Store HTTP Deployer Configuration";
	}

	@Override
	public String getHTMLWarning() {
		return "Either &quot;<em>graphStoreHTTPEndpoint</em>&quot; or &quot;<em>destinationGraph</em>&quot; shall be set."
				+ "The credentials are stored wihtout encryption on the server. "
				+ "Be aware that the system administration could be able to see them.";
	}

	@STProperty(description = "The address of the endpoint conforming to the HTTP Graph Store protocol. If not provided, it is assumed that the destination graph is identified directly", displayName = "Graph Store HTTP endpoint")
	public URL graphStoreHTTPEndpoint;

	@STProperty(description = "The graph where data should be deployed. If not provided, the default graph is used", displayName = "Destination graph")
	public IRI destinationGraph;

	@STProperty(description = "Tells if already existing data should be cleared fist", displayName = "Clear first")
	@Required
	public Boolean clearFirst;

	@STProperty(description = "Username", displayName = "Username")
	public String username;

	@STProperty(description = "Password", displayName = "Password")
	public String password;

}
