package it.uniroma2.art.semanticturkey.extension.impl.loader.sparql;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * The {@link ExtensionFactory} for the the {@link GraphStoreHTTPLoader}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class GraphStoreHTTPLoaderFactory implements ExtensionFactory<GraphStoreHTTPLoader>,
		ConfigurableExtensionFactory<GraphStoreHTTPLoader, GraphStoreHTTPLoaderConfiguration>,
		PUScopedConfigurableComponent<GraphStoreHTTPLoaderConfiguration> {

	@Override
	public String getName() {
		return "Graph Store HTTP Deployer";
	}

	@Override
	public String getDescription() {
		return "A deployer that uses the SPARQL 1.1 Graph Store HTTP Protocol";
	}

	@Override
	public GraphStoreHTTPLoader createInstance(GraphStoreHTTPLoaderConfiguration conf) {
		return new GraphStoreHTTPLoader(conf);
	}

	@Override
	public Collection<GraphStoreHTTPLoaderConfiguration> getConfigurations() {
		return Arrays.asList(new GraphStoreHTTPLoaderConfiguration());
	}

}
