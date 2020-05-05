package it.uniroma2.art.semanticturkey.plugin;

import java.util.Collection;
import java.util.Map;

import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * A factory responsible for the instantiation of a plugin.
 * 
 * @param <T>
 */
public interface PluginFactory<T extends STProperties, R extends STProperties, Q extends STProperties, P extends STProperties, S extends STProperties> {

	/**
	 * Returns the factory identifier.
	 * 
	 * @return
	 */
	String getID();

	/**
	 * Returns allowed configurations for this factory.
	 * 
	 * @return
	 */
	Collection<STProperties> getPluginConfigurations();

	/**
	 * Returns the default configuration.
	 * 
	 * @return
	 */
	T createDefaultPluginConfiguration();

	/**
	 * Instantiates a configuration object given the configuration class name.
	 * 
	 * @param confType
	 * @return
	 * @throws UnsupportedPluginConfigurationException
	 * @throws UnloadablePluginConfigurationException
	 * @throws ClassNotFoundException
	 */
	T createPluginConfiguration(String configType) throws UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException, ClassNotFoundException;

	/**
	 * Instantiates a plugin based on the given configuration object.
	 * 
	 * @param conf
	 * @return
	 */
	Object createInstance(STProperties conf);

	/**
	 * Returns project settings for the implemented extension point. The returned {@link STProperties} is
	 * filled with information already stored in the project.
	 * 
	 * @param project
	 * @return
	 * @throws STPropertyAccessException
	 */
	default R getExtensonPointProjectSettings(Project project) throws STPropertyAccessException {
		return null;
	}

	/**
	 * Stores the provided project settings for the implemented extension point.
	 * 
	 * @param project
	 * @param settings
	 * @throws STPropertyUpdateException
	 */
	default void storeExtensonPointProjectSettings(Project project, STProperties settings)
			throws STPropertyUpdateException {
	}

	/**
	 * Stores the provided project settings for the implemented extension point.
	 * 
	 * @param project
	 * @param settings
	 * @throws STPropertyUpdateException
	 * @throws STPropertyAccessException
	 */
	default void storeExtensonPointProjectSettings(Project project, Map<String, Object> settings)
			throws STPropertyUpdateException, STPropertyAccessException {
	}

	/**
	 * Returns class-level project settings for this plugin. The returned {@link STProperties} is filled with
	 * information already stored in the project.
	 * 
	 * @param project
	 * @return
	 * @throws STPropertyAccessException
	 */
	default Q getProjectSettings(Project project) throws STPropertyAccessException {
		return null;
	}

	/**
	 * Stores the provided class-level project settings.
	 * 
	 * @param project
	 * @param settings
	 * @throws STPropertyUpdateException
	 */
	default void storeProjectSettings(Project project, STProperties settings)
			throws STPropertyUpdateException {
	}

	/**
	 * Stores the provided class-level project settings.
	 * 
	 * @param project
	 * @param settings
	 * @throws STPropertyUpdateException
	 * @throws STPropertyAccessException
	 */
	default void storeProjectSettings(Project project, Map<String, Object> settings)
			throws STPropertyUpdateException, STPropertyAccessException {
	}

	/**
	 * Returns preferences for the implemented extension point. The returned {@link STProperties} is filled
	 * with information already stored in the project.
	 * 
	 * @param project
	 * @return
	 * @throws STPropertyAccessException
	 */
	default P getExtensonPointProjectPreferences(Project project) throws STPropertyAccessException {
		return null;
	}

	/**
	 * Stores the provided project settings for the implemented extension point.
	 * 
	 * @param project
	 * @param settings
	 * @throws STPropertyUpdateException
	 */
	default void storeExtensonPointProjectPreferencences(Project project, STProperties settings)
			throws STPropertyUpdateException {
	}

	/**
	 * Stores the provided project settings for the implemented extension point.
	 * 
	 * @param project
	 * @param settings
	 * @throws STPropertyUpdateException
	 * @throws STPropertyAccessException
	 */
	default void storeExtensonPointProjectPreferences(Project project, Map<String, Object> settings)
			throws STPropertyUpdateException, STPropertyAccessException {
	}

	/**
	 * Returns class-level project preferences for this plugin. The returned {@link STProperties} is filled
	 * with information already stored in the project.
	 * 
	 * @param project
	 * @param user 
	 * @return
	 * @throws STPropertyAccessException
	 */
	default S getProjectPreferences(Project project, STUser user) throws STPropertyAccessException {
		return null;
	}

	/**
	 * Stores the provided class-level project preferences.
	 * 
	 * @param project
	 * @param user
	 * @param settings
	 * @throws STPropertyUpdateException
	 */
	default void storeProjectPreferences(Project project, STUser user, STProperties settings)
			throws STPropertyUpdateException {
	}

	/**
	 * Stores the provided class-level project preferences.
	 * 
	 * @param project
	 * @param user
	 * @param settings
	 * @throws STPropertyUpdateException
	 * @throws STPropertyAccessException
	 */
	default void storeProjectPreferences(Project project, STUser user, Map<String, Object> settings)
			throws STPropertyUpdateException, STPropertyAccessException {
	}

}
