package it.uniroma2.art.semanticturkey.extension.impl.deployer.sparql;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * The {@link ExtensionFactory} for the the {@link GraphStoreHTTPDeployer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class GraphStoreHTTPDeployerFactory implements ExtensionFactory<GraphStoreHTTPDeployer>,
		ConfigurableExtensionFactory<GraphStoreHTTPDeployer, GraphStoreHTTPDeployerConfiguration>,
		PUScopedConfigurableComponent<GraphStoreHTTPDeployerConfiguration> {

	@Override
	public String getName() {
		return "Graph Store HTTP Deployer";
	}

	@Override
	public String getDescription() {
		return "A deployer that uses the SPARQL 1.1 Graph Store HTTP Protocol";
	}

	@Override
	public GraphStoreHTTPDeployer createInstance(GraphStoreHTTPDeployerConfiguration conf) {
		return new GraphStoreHTTPDeployer(conf);
	}

	@Override
	public Collection<GraphStoreHTTPDeployerConfiguration> getConfigurations() {
		return Arrays.asList(new GraphStoreHTTPDeployerConfiguration());
	}

}
