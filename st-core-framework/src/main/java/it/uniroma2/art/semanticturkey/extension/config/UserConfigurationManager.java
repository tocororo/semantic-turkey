package it.uniroma2.art.semanticturkey.extension.config;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.config.impl.ConfigurationSupport;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <CONFTYPE>
 */
public interface UserConfigurationManager<CONFTYPE extends Configuration>
		extends ConfigurationManager<CONFTYPE> {

	default Collection<String> getUserConfigurationIdentifiers(STUser user) {
		File folder = ConfigurationSupport.getConfigurationFolder(this, user);
		return ConfigurationSupport.listConfigurationIdentifiers(folder);
	}

	default CONFTYPE getUserConfiguration(STUser user, String identifier)
			throws IOException, ConfigurationNotFoundException, WrongPropertiesException {
		return ConfigurationSupport.loadConfiguration(this,
				ConfigurationSupport.getConfigurationFolder(this, user), identifier);
	}

	default void storeUserConfiguration(STUser user, String identifier, CONFTYPE configuration)
			throws IOException, WrongPropertiesException {
		ConfigurationSupport.storeConfiguration(ConfigurationSupport.getConfigurationFolder(this, user),
				identifier, configuration);
	}

}
