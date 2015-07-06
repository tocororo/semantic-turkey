package it.uniroma2.art.semanticturkey.plugin.impls.rendering;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.SKOSRenderingEngineConfiguration;

import java.util.Arrays;
import java.util.Collection;

/**
 * Factory for the instantiation of {@link SKOSRenderingEngine}.
 */
public class SKOSRenderingEngineFactory implements PluginFactory<SKOSRenderingEngineConfiguration> {
	
	@Override
	public String getID() {
		return this.getClass().getName();
	}
	
	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration>asList(new SKOSRenderingEngineConfiguration());
	}

	@Override
	public SKOSRenderingEngineConfiguration createDefaultPluginConfiguration() {
		return new SKOSRenderingEngineConfiguration();
	}

	@Override
	public SKOSRenderingEngineConfiguration createPluginConfiguration(String configType) throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
	 ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);
		
		if (!SKOSRenderingEngineConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}
		
		try {
			return (SKOSRenderingEngineConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public SKOSRenderingEngine createInstance(PluginConfiguration config) {
		return new SKOSRenderingEngine((SKOSRenderingEngineConfiguration)config);
	}

}
