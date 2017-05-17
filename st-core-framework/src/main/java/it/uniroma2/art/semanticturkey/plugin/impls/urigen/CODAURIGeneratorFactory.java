package it.uniroma2.art.semanticturkey.plugin.impls.urigen;

import it.uniroma2.art.semanticturkey.customform.CODACoreProvider;
import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.CODAAnyURIGeneratorConfiguration;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.CODATemplateBasedURIGeneratorConfiguration;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.CODAURIGeneratorConfiguration;
import it.uniroma2.art.semanticturkey.properties.STProperties;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory for the instantiation of {@link CODAURIGenerator}.
 */
public class CODAURIGeneratorFactory
		implements PluginFactory<CODAURIGeneratorConfiguration, STProperties, STProperties> {

	@Autowired
	private ObjectFactory<CODACoreProvider> codaCoreProviderFactory;

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration>asList(new CODATemplateBasedURIGeneratorConfiguration(),
				new CODAAnyURIGeneratorConfiguration());
	}

	@Override
	public CODAURIGeneratorConfiguration createDefaultPluginConfiguration() {
		return new CODATemplateBasedURIGeneratorConfiguration();
	}

	@Override
	public CODAURIGeneratorConfiguration createPluginConfiguration(String configType)
			throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
			ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);

		if (!CODATemplateBasedURIGeneratorConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}

		try {
			return (CODAURIGeneratorConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public CODAURIGenerator createInstance(PluginConfiguration config) {

		return new CODAURIGenerator((CODAURIGeneratorConfiguration) config, codaCoreProviderFactory);
	}

}
