package it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * The {@link ExtensionFactory} for the the {@link OntoPortalDeployer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class OntoPortalDeployerFactory implements ExtensionFactory<OntoPortalDeployer>,
		ConfigurableExtensionFactory<OntoPortalDeployer, AbstractOntoPortalDeployerConfiguration>,
		PUScopedConfigurableComponent<AbstractOntoPortalDeployerConfiguration> {
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.OntoPortalDeployerFactory";
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
	public OntoPortalDeployer createInstance(AbstractOntoPortalDeployerConfiguration conf) {
		return new OntoPortalDeployer(conf);
	}

	@Override
	public Collection<AbstractOntoPortalDeployerConfiguration> getConfigurations() {
		return Arrays.asList(new OntoPortalDeployerConfiguration(), new EcoPortalDeployerConfiguration());
	}

}
