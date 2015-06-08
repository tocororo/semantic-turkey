package it.uniroma2.art.semanticturkey.plugin.impls.urigen;

import it.uniroma2.art.semanticturkey.customrange.CODACoreProvider;
import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.CODABasedAnyURIGeneratorConfiguration;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.CODABasedTemplatedURIGeneratorConfiguration;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.CODABasedURIGeneratorConfiguration;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory for the instantiation of {@link CODABasedURIGenerator}.
 */
public class CODABasedURIGeneratorFactory implements PluginFactory<CODABasedURIGeneratorConfiguration> {

	@Autowired
	private ObjectFactory<CODACoreProvider> codaCoreProviderFactory;
	
	@Override
	public String getID() {
		return this.getClass().getName();
	}
	
	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration>asList(new CODABasedTemplatedURIGeneratorConfiguration(), new CODABasedAnyURIGeneratorConfiguration());
	}

	@Override
	public CODABasedURIGeneratorConfiguration createDefaultPluginConfiguration() {
		return new CODABasedTemplatedURIGeneratorConfiguration();
	}

	@Override
	public CODABasedURIGeneratorConfiguration createPluginConfiguration(String configType) throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
	 ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);
		
		if (!CODABasedTemplatedURIGeneratorConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}
		
		try {
			return (CODABasedURIGeneratorConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public CODABasedURIGenerator createInstance(PluginConfiguration config) {
		
		return new CODABasedURIGenerator((CODABasedURIGeneratorConfiguration)config, codaCoreProviderFactory);
	}

}
