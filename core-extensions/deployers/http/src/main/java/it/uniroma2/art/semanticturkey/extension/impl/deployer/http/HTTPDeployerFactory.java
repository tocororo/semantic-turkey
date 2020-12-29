package it.uniroma2.art.semanticturkey.extension.impl.deployer.http;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * The {@link ExtensionFactory} for the the {@link HTTPDeployer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class HTTPDeployerFactory implements ExtensionFactory<HTTPDeployer>,
		ConfigurableExtensionFactory<HTTPDeployer, HTTPDeployerConfiguration>,
		PUScopedConfigurableComponent<HTTPDeployerConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.deployer.http.HTTPDeployerFactory";
		private static final String name = keyBase + ".name";
		private static final String description = keyBase + ".description";
	}

	@Override
	public String getName() {
		return STMessageSource.getMessage(MessageKeys.name);
	}

	@Override
	public String getDescription() {
		return STMessageSource.getMessage(MessageKeys.description);
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
