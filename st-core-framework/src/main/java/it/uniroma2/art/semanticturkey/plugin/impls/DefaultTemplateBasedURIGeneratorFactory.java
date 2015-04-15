package it.uniroma2.art.semanticturkey.plugin.impls;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.DefaultTemplateBasedURIGenerator.Configuration;

import java.util.Arrays;
import java.util.Collection;

public class DefaultTemplateBasedURIGeneratorFactory implements PluginFactory<DefaultTemplateBasedURIGenerator.Configuration> {

	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration>asList(new DefaultTemplateBasedURIGenerator.Configuration());
	}

	@Override
	public Configuration createDefaultPluginConfiguration() {
		return new DefaultTemplateBasedURIGenerator.Configuration();
	}

	@Override
	public DefaultTemplateBasedURIGenerator.Configuration createPluginConfiguration(String configType) throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
	ClassNotFoundException {
		if (!configType.equals(DefaultTemplateBasedURIGenerator.Configuration.class.getName())) {
			throw new UnsupportedPluginConfigurationException();
		}
		
		return new DefaultTemplateBasedURIGenerator.Configuration();
	}

	@Override
	public DefaultTemplateBasedURIGenerator createInstance(PluginConfiguration config) {
		return new DefaultTemplateBasedURIGenerator((DefaultTemplateBasedURIGenerator.Configuration)config);
	}

}
