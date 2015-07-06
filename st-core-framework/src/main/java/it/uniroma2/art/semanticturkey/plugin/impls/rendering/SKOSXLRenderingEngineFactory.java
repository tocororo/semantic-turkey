package it.uniroma2.art.semanticturkey.plugin.impls.rendering;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.SKOSXLRenderingEngineConfiguration;

import java.util.Arrays;
import java.util.Collection;

/**
 * Factory for the instantiation of {@link SKOSXLRenderingEngine}.
 */
public class SKOSXLRenderingEngineFactory implements PluginFactory<SKOSXLRenderingEngineConfiguration> {
	
	@Override
	public String getID() {
		return this.getClass().getName();
	}
	
	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration>asList(new SKOSXLRenderingEngineConfiguration());
	}

	@Override
	public SKOSXLRenderingEngineConfiguration createDefaultPluginConfiguration() {
		return new SKOSXLRenderingEngineConfiguration();
	}

	@Override
	public SKOSXLRenderingEngineConfiguration createPluginConfiguration(String configType) throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
	 ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);
		
		if (!SKOSXLRenderingEngineConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}
		
		try {
			return (SKOSXLRenderingEngineConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public SKOSXLRenderingEngine createInstance(PluginConfiguration config) {
		return new SKOSXLRenderingEngine((SKOSXLRenderingEngineConfiguration)config);
	}

}
