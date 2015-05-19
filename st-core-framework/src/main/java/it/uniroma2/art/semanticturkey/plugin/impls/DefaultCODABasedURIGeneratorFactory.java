package it.uniroma2.art.semanticturkey.plugin.impls;

import it.uniroma2.art.semanticturkey.customrange.CODACoreProvider;
import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory for the instantiation of {@link DefaultCODABasedURIGenerator}.
 */
public class DefaultCODABasedURIGeneratorFactory implements PluginFactory<DefaultCODABasedURIGenerator.Configuration> {

	@Autowired
	private ObjectFactory<CODACoreProvider> codaCoreProviderFactory;
	
	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration>asList(new DefaultCODABasedURIGenerator.Configuration());
	}

	@Override
	public DefaultCODABasedURIGenerator.Configuration createDefaultPluginConfiguration() {
		return new DefaultCODABasedURIGenerator.Configuration();
	}

	@Override
	public DefaultCODABasedURIGenerator.Configuration createPluginConfiguration(String configType) throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
	ClassNotFoundException {
		if (!configType.equals(DefaultCODABasedURIGenerator.Configuration.class.getName())) {
			throw new UnsupportedPluginConfigurationException();
		}
		
		return new DefaultCODABasedURIGenerator.Configuration();
	}

	@Override
	public DefaultCODABasedURIGenerator createInstance(PluginConfiguration config) {
		return new DefaultCODABasedURIGenerator((DefaultCODABasedURIGenerator.Configuration)config, codaCoreProviderFactory);
	}

}
