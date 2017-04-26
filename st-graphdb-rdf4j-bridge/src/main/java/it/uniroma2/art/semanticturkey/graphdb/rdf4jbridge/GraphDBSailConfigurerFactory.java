package it.uniroma2.art.semanticturkey.graphdb.rdf4jbridge;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.graphdb.rdf4jbridge.conf.GraphDBFreeSailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.graphdb.rdf4jbridge.conf.GraphDBSailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;

public class GraphDBSailConfigurerFactory implements PluginFactory<GraphDBSailConfigurerConfiguration>{

	@Override
	public String getID() {
		return GraphDBSailConfigurerFactory.class.getName();
	}

	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration> asList(new GraphDBFreeSailConfigurerConfiguration());
	}

	@Override
	public GraphDBFreeSailConfigurerConfiguration createDefaultPluginConfiguration() {
		return new GraphDBFreeSailConfigurerConfiguration();
	}

	@Override
	public GraphDBSailConfigurerConfiguration createPluginConfiguration(String configType)
			throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
			ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);

		if (!GraphDBSailConfigurerConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}

		try {
			return (GraphDBSailConfigurerConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public Object createInstance(PluginConfiguration conf) {
		if (conf instanceof GraphDBFreeSailConfigurerConfiguration) {
			return new GraphDBFreeSailConfigurer((GraphDBFreeSailConfigurerConfiguration)conf);
		} else {
			throw new IllegalArgumentException("Unsupported configuration type: " + conf.getClass().getName());
		}
		
	}
}
