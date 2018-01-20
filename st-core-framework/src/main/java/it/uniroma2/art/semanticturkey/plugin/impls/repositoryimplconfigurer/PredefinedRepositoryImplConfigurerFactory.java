package it.uniroma2.art.semanticturkey.plugin.impls.repositoryimplconfigurer;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.repositoryimplconfigurer.conf.GraphDBFreeConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.impls.repositoryimplconfigurer.conf.GraphDBSEConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.impls.repositoryimplconfigurer.conf.PredefinedRepositoryImplConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.impls.repositoryimplconfigurer.conf.RDF4JNativeSailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.impls.repositoryimplconfigurer.conf.RDF4JPersistentInMemorySailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.properties.STProperties;

/**
 * Factory for the instantiation of {@link PredefinedRepositoryImplConfigurer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PredefinedRepositoryImplConfigurerFactory implements
		PluginFactory<PredefinedRepositoryImplConfigurerConfiguration, STProperties, STProperties, STProperties, STProperties> {

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public Collection<STProperties> getPluginConfigurations() {
		return Arrays.<STProperties>asList(new RDF4JPersistentInMemorySailConfigurerConfiguration(),
				new RDF4JNativeSailConfigurerConfiguration(), new GraphDBFreeConfigurerConfiguration(),
				new GraphDBSEConfigurerConfiguration());
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
	public PredefinedRepositoryImplConfigurer createInstance(STProperties config) {
		return new PredefinedRepositoryImplConfigurer(
				(PredefinedRepositoryImplConfigurerConfiguration) config);
	}

}
