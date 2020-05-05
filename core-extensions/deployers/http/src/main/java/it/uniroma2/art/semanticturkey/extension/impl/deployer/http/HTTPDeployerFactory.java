package it.uniroma2.art.semanticturkey.extension.impl.deployer.http;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * The {@link ExtensionFactory} for the the {@link HTTPDeployer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class HTTPDeployerFactory implements ExtensionFactory<HTTPDeployer>,
		ConfigurableExtensionFactory<HTTPDeployer, HTTPDeployerConfiguration>,
		PUScopedConfigurableComponent<HTTPDeployerConfiguration> {

	@Override
	public String getName() {
		return "HTTP Deployer";
	}

	@Override
	public String getDescription() {
		return "A deployer that uses the HTTP Protocol";
	}

	@Override
	public HTTPDeployer createInstance(HTTPDeployerConfiguration conf) {
		return new HTTPDeployer(conf);
	}

	@Override
	public Collection<HTTPDeployerConfiguration> getConfigurations() {
		return Arrays.asList(new HTTPDeployerConfiguration());
	}

}
