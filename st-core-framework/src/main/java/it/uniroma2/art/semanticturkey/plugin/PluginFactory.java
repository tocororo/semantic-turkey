package it.uniroma2.art.semanticturkey.plugin;

import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;

import java.util.Collection;

/**
 * A factory responsible for the instantiation of a plugin.
 * @param <T>
 */
public interface PluginFactory<T extends PluginConfiguration> {

	/**
	 * Returns the factory identifier.
	 * @return
	 */
	String getID();
	
	/**
	 * Returns allowed configurations for this factory.
	 * @return
	 */
	Collection<PluginConfiguration> getPluginConfigurations();

	/**
	 * Returns the default configuration.
	 * @return
	 */
	T createDefaultPluginConfiguration();
	
	/**
	 * Instantiates a configuration object given the configuration class name.
	 * @param confType
	 * @return
	 * @throws UnsupportedPluginConfigurationException
	 * @throws UnloadablePluginConfigurationException
	 * @throws ClassNotFoundException
	 */
	T createPluginConfiguration(String configType)
			throws UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException, ClassNotFoundException;

	/**
	 * Instantiates a plugin based on the given configuration object.
	 * @param conf
	 * @return
	 */
	Object createInstance(PluginConfiguration conf);

}
