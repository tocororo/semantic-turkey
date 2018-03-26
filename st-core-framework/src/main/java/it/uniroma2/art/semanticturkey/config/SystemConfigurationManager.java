package it.uniroma2.art.semanticturkey.config;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.config.impl.ConfigurationSupport;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;

/**
 * 
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <CONFTYPE>
 */
public interface SystemConfigurationManager<CONFTYPE extends Configuration>
		extends ConfigurationManager<CONFTYPE> {

	default Collection<String> getSystemConfigurationIdentifiers() {
		File folder = ConfigurationSupport.getConfigurationFolder(this);
		return ConfigurationSupport.listConfigurationIdentifiers(folder);
	}

	default CONFTYPE getSystemConfiguration(String identifier) throws IOException,
			ConfigurationNotFoundException, WrongPropertiesException, STPropertyAccessException {
		return ConfigurationSupport.loadConfiguration(this, ConfigurationSupport.getConfigurationFolder(this),
				identifier);
	}

	default void storeSystemConfiguration(String identifier, CONFTYPE configuration)
			throws IOException, WrongPropertiesException, STPropertyUpdateException {
		ConfigurationSupport.storeConfiguration(ConfigurationSupport.getConfigurationFolder(this), identifier,
				configuration);
	}

	default void deleteSystemConfiguration(String identifier) throws ConfigurationNotFoundException {
		ConfigurationSupport.deleteConfiguration(ConfigurationSupport.getConfigurationFolder(this),
				identifier);
	}

}
