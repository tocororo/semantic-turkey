package it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.PredefinedSailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.RDF4JNativeSailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.RDF4JNonPersistentInMemorySailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.RDF4JPersistentInMemorySailConfigurerConfiguration;

/**
 * Factory for the instantiation of {@link PredefinedSailConfigurer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PredefinedSailConfigurerFactory implements PluginFactory<PredefinedSailConfigurerConfiguration> {

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration>asList(new RDF4JPersistentInMemorySailConfigurerConfiguration(),
				new RDF4JNonPersistentInMemorySailConfigurerConfiguration(), new RDF4JNativeSailConfigurerConfiguration());
	}

	@Override
	public PredefinedSailConfigurerConfiguration createDefaultPluginConfiguration() {
		return new RDF4JNativeSailConfigurerConfiguration();
	}

	@Override
	public PredefinedSailConfigurerConfiguration createPluginConfiguration(String configType)
			throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
			ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);

		if (!PredefinedSailConfigurerConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}

		try {
			return (PredefinedSailConfigurerConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public PredefinedSailConfigurer createInstance(PluginConfiguration config) {
		return new PredefinedSailConfigurer((PredefinedSailConfigurerConfiguration) config);
	}

}
