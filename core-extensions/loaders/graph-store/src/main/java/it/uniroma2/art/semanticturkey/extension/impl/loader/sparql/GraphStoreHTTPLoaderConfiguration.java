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

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.loader.sparql.GraphStoreHTTPLoaderConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String htmlWarning = keyBase + ".htmlWarning";
		public static final String graphStoreHTTPEndpoint$description = keyBase + ".graphStoreHTTPEndpoint.description";
		public static final String graphStoreHTTPEndpoint$displayName = keyBase + ".graphStoreHTTPEndpoint.displayName";
		public static final String sourceGraph$description = keyBase + ".sourceGraph.description";
		public static final String sourceGraph$displayName = keyBase + ".sourceGraph.displayName";
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

	@STProperty(description = "{" + MessageKeys.sourceGraph$description + "}", displayName = "{" + MessageKeys.sourceGraph$displayName + "}")
	public IRI sourceGraph;

	@STProperty(description = "{" + MessageKeys.username$description + "}", displayName = "{" + MessageKeys.username$displayName + "}")
	public String username;

	@STProperty(description = "{" + MessageKeys.password$description + "}", displayName = "{" + MessageKeys.password$displayName + "}")
	public String password;

}
