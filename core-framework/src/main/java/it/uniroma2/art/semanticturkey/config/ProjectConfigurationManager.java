package it.uniroma2.art.semanticturkey.config;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.config.impl.ConfigurationSupport;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <CONFTYPE>
 */
public interface ProjectConfigurationManager<CONFTYPE extends Configuration>
		extends ConfigurationManager<CONFTYPE> {

	default Collection<String> getProjectConfigurationIdentifiers(Project project) {
		File folder = ConfigurationSupport.getConfigurationFolder(this, project);
		return ConfigurationSupport.listConfigurationIdentifiers(folder);
	}

	default CONFTYPE getProjectConfiguration(Project project, String identifier)
			throws STPropertyAccessException {
		return ConfigurationSupport.loadConfiguration(this,
				ConfigurationSupport.getConfigurationFolder(this, project), identifier);
	}

	default void storeProjectConfiguration(Project project, String identifier, CONFTYPE configuration)
			throws IOException, WrongPropertiesException, STPropertyUpdateException {
		ConfigurationSupport.storeConfiguration(ConfigurationSupport.getConfigurationFolder(this, project),
				identifier, configuration);
	}

	default void deleteProjectConfiguration(Project project, String identifier)
			throws ConfigurationNotFoundException {
		ConfigurationSupport.deleteConfiguration(ConfigurationSupport.getConfigurationFolder(this, project),
				identifier);
	}

}
