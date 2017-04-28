package it.uniroma2.art.semanticturkey.plugin.extpts.impls.repositoryimplconfigurer;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.repositoryimplconfigurer.conf.GraphDBFreeConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.repositoryimplconfigurer.conf.PredefinedRepositoryImplConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.repositoryimplconfigurer.conf.RDF4JNativeSailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.repositoryimplconfigurer.conf.RDF4JNonPersistentInMemorySailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.repositoryimplconfigurer.conf.RDF4JPersistentInMemorySailConfigurerConfiguration;

/**
 * Factory for the instantiation of {@link PredefinedRepositoryImplConfigurer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PredefinedRepositoryImplConfigurerFactory
		implements PluginFactory<PredefinedRepositoryImplConfigurerConfiguration> {

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration>asList(new RDF4JPersistentInMemorySailConfigurerConfiguration(),
				new RDF4JNonPersistentInMemorySailConfigurerConfiguration(),
				new RDF4JNativeSailConfigurerConfiguration(), new GraphDBFreeConfigurerConfiguration());
	}

	@Override
	public PredefinedRepositoryImplConfigurerConfiguration createDefaultPluginConfiguration() {
		return new RDF4JNativeSailConfigurerConfiguration();
	}

	@Override
	public PredefinedRepositoryImplConfigurerConfiguration createPluginConfiguration(String configType)
			throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
			ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);

		if (!PredefinedRepositoryImplConfigurerConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}

		try {
			return (PredefinedRepositoryImplConfigurerConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public PredefinedRepositoryImplConfigurer createInstance(PluginConfiguration config) {
		return new PredefinedRepositoryImplConfigurer((PredefinedRepositoryImplConfigurerConfiguration) config);
	}

}
