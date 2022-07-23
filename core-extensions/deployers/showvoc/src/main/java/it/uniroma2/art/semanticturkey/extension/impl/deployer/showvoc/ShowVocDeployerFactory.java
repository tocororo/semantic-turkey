package it.uniroma2.art.semanticturkey.extension.impl.deployer.showvoc;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

import java.util.Arrays;
import java.util.Collection;

/**
 * The {@link ExtensionFactory} for the the {@link ShowVocDeployer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ShowVocDeployerFactory implements ExtensionFactory<ShowVocDeployer>,
		ConfigurableExtensionFactory<ShowVocDeployer, ShowVocDeployerConfiguration>,
		PUScopedConfigurableComponent<ShowVocDeployerConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.deployer.showvoc.ShowVocDeployerFactory";
		private static final String name = keyBase + ".name";
		private static final String description = keyBase + ".description";
	}

	private ExtensionPointManager exptMgr;

	public ShowVocDeployerFactory(ExtensionPointManager exptMgr) {
		this.exptMgr = exptMgr;
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
	public ShowVocDeployer createInstance(ShowVocDeployerConfiguration conf) throws InvalidConfigurationException {
		return new ShowVocDeployer(exptMgr, conf);
	}

	@Override
	public Collection<ShowVocDeployerConfiguration> getConfigurations() {
		return Arrays.asList(new ExistingProjectShowVocDeployerConfiguration(), new NewProjectShowVocDeployerConfiguration());
	}


}
