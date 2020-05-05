package it.uniroma2.art.semanticturkey.plugin.impls.rendering;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.OntoLexLemonRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.properties.STProperties;

/**
 * Factory for the instantiation of {@link OntoLexLemonRenderingEngine}.
 */
public class OntoLexLemonRenderingEngineFactory implements
		PluginFactory<OntoLexLemonRenderingEngineConfiguration, STProperties, STProperties, STProperties, STProperties> {

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public Collection<STProperties> getPluginConfigurations() {
		return Arrays.<STProperties>asList(new OntoLexLemonRenderingEngineConfiguration());
	}

	@Override
	public OntoLexLemonRenderingEngineConfiguration createDefaultPluginConfiguration() {
		return new OntoLexLemonRenderingEngineConfiguration();
	}

	@Override
	public OntoLexLemonRenderingEngineConfiguration createPluginConfiguration(String configType)
			throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
			ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);

		if (!OntoLexLemonRenderingEngineConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}

		try {
			return (OntoLexLemonRenderingEngineConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public OntoLexLemonRenderingEngine createInstance(STProperties config) {
		return new OntoLexLemonRenderingEngine((OntoLexLemonRenderingEngineConfiguration) config);
	}

}
