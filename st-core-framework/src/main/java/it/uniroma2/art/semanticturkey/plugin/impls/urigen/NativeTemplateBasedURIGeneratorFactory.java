package it.uniroma2.art.semanticturkey.plugin.impls.urigen;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.NativeTemplateBasedURIGeneratorConfiguration;

import java.util.Arrays;
import java.util.Collection;

public class NativeTemplateBasedURIGeneratorFactory implements PluginFactory<NativeTemplateBasedURIGeneratorConfiguration> {

	@Override
	public String getID() {
		return this.getClass().getName();
	}
	
	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration>asList(new NativeTemplateBasedURIGeneratorConfiguration());
	}

	@Override
	public NativeTemplateBasedURIGeneratorConfiguration createDefaultPluginConfiguration() {
		return new NativeTemplateBasedURIGeneratorConfiguration();
	}

	@Override
	public NativeTemplateBasedURIGeneratorConfiguration createPluginConfiguration(String configType) throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
	ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);
		
		if (!NativeTemplateBasedURIGeneratorConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}
		
		try {
			return (NativeTemplateBasedURIGeneratorConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public NativeTemplateBasedURIGenerator createInstance(PluginConfiguration config) {
		return new NativeTemplateBasedURIGenerator((NativeTemplateBasedURIGeneratorConfiguration)config);
	}

}
