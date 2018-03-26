package it.uniroma2.art.semanticturkey.config;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.config.impl.ConfigurationSupport;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <CONFTYPE>
 */
public interface PUConfigurationManager<CONFTYPE extends Configuration>
		extends ConfigurationManager<CONFTYPE> {

	default Collection<String> getProjectConfigurationIdentifiers(Project project, STUser user) {
		File folder = ConfigurationSupport.getConfigurationFolder(this, project, user);
		return ConfigurationSupport.listConfigurationIdentifiers(folder);
	}

	default CONFTYPE getProjectConfiguration(Project project, STUser user, String identifier)
			throws IOException, ConfigurationNotFoundException, WrongPropertiesException,
			STPropertyAccessException {
		return ConfigurationSupport.loadConfiguration(this,
				ConfigurationSupport.getConfigurationFolder(this, project, user), identifier);
	}

	default void storeProjectConfiguration(Project project, STUser user, String identifier,
			CONFTYPE configuration) throws IOException, WrongPropertiesException, STPropertyUpdateException {
		ConfigurationSupport.storeConfiguration(
				ConfigurationSupport.getConfigurationFolder(this, project, user), identifier, configuration);
	}

	default void deleteProjectConfiguration(Project project, STUser user, String identifier)
			throws ConfigurationNotFoundException {
		ConfigurationSupport.deleteConfiguration(
				ConfigurationSupport.getConfigurationFolder(this, project, user), identifier);
	}

}
