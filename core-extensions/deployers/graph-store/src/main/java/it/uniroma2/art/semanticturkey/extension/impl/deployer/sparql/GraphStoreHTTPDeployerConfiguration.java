package it.uniroma2.art.semanticturkey.extension.impl.deployer.sparql;

import java.net.URL;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link GraphStoreHTTPDeployerFactory}.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class GraphStoreHTTPDeployerConfiguration implements Configuration {
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.deployer.sparql.GraphStoreHTTPDeployerConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String htmlWarning = keyBase + ".htmlWarning";		
		public static final String graphStoreHTTPEndpoint$description = keyBase + ".graphStoreHTTPEndpoint.description";
		public static final String graphStoreHTTPEndpoint$displayName = keyBase + ".graphStoreHTTPEndpoint.displayName";
		public static final String destinationGraph$description = keyBase + ".destinationGraph.description";
		public static final String destinationGraph$displayName = keyBase + ".destinationGraph.displayName";
		public static final String clearFirst$description = keyBase + ".clearFirst.description";
		public static final String clearFirst$displayName = keyBase + ".clearFirst.displayName";
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

	@STProperty(description = "{" + MessageKeys.graphStoreHTTPEndpoint$description + "}", displayName = "{" + MessageKeys.graphStoreHTTPEndpoint$displayName + "}")
	public URL graphStoreHTTPEndpoint;

	@STProperty(description = "{" + MessageKeys.destinationGraph$description + "}", displayName = "{" + MessageKeys.destinationGraph$displayName + "}")
	public IRI destinationGraph;

	@STProperty(description = "{" + MessageKeys.clearFirst$description + "}", displayName = "{" + MessageKeys.clearFirst$displayName + "}")
	@Required
	public Boolean clearFirst;

	@STProperty(description = "{" + MessageKeys.username$description + "}", displayName = "{" + MessageKeys.username$displayName + "}")
	public String username;

	@STProperty(description = "{" + MessageKeys.password$description + "}", displayName = "{" + MessageKeys.password$displayName + "}")
	public String password;

}
