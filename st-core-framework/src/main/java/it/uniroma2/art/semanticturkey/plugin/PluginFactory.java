package it.uniroma2.art.semanticturkey.plugin;

import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;

import java.util.Collection;

public interface PluginFactory<T extends PluginConfiguration> {

	Collection<PluginConfiguration> getPluginConfigurations();

	T createDefaultPluginConfiguration();
	
	T createPluginConfiguration(String confType)
			throws UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException, ClassNotFoundException;

	Object createInstance(PluginConfiguration conf);

}
