package it.uniroma2.art.semanticturkey.plugin.impls.rendering;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.RDFSRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.properties.STProperties;

import java.util.Arrays;
import java.util.Collection;

/**
 * Factory for the instantiation of {@link RDFSRenderingEngine}.
 */
public class RDFSRenderingEngineFactory
		implements PluginFactory<RDFSRenderingEngineConfiguration, STProperties, STProperties> {

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration>asList(new RDFSRenderingEngineConfiguration());
	}

	@Override
	public RDFSRenderingEngineConfiguration createDefaultPluginConfiguration() {
		return new RDFSRenderingEngineConfiguration();
	}

	@Override
	public RDFSRenderingEngineConfiguration createPluginConfiguration(String configType)
			throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
			ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);

		if (!RDFSRenderingEngineConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}

		try {
			return (RDFSRenderingEngineConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public RDFSRenderingEngine createInstance(PluginConfiguration config) {
		return new RDFSRenderingEngine((RDFSRenderingEngineConfiguration) config);
	}

}
