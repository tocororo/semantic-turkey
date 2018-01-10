package it.uniroma2.art.semanticturkey.plugin;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonProperty;

import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;

/**
 * Holds a pluginFactoryId, its configuration type (if not the default) and any configuration parameter
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PluginSpecification {
	private String factoryId;
	private Optional<String> configTypeHolder;
	private Properties properties;

	public PluginSpecification(@JsonProperty("factoryId") String factoryId,
			@JsonProperty("configType") String configType,
			@JsonProperty("properties") Properties properties) {
		this.factoryId = factoryId;
		this.configTypeHolder = Optional.ofNullable(configType);
		this.properties = properties;
	}

	public PluginSpecification() {

	}

	public String getFactoryId() {
		return factoryId;
	}

	public String getConfigType() {
		return configTypeHolder.orElse(null);
	}

	public Properties getProperties() {
		return properties;
	}

	public Object instatiatePlugin() throws ClassNotFoundException, UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException, WrongPropertiesException {
		PluginFactory<?, ?, ?, ?, ?> pluginFactory = PluginManager.getPluginFactory(factoryId);

		STProperties config;
		if (configTypeHolder.isPresent()) {
			config = pluginFactory.createPluginConfiguration(configTypeHolder.get());
		} else {
			config = pluginFactory.createDefaultPluginConfiguration();
		}

		if (!properties.isEmpty()) {
			config.setProperties(properties);
		}

		return pluginFactory.createInstance(config);
	}

	public void expandDefaults() throws ClassNotFoundException, UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException {
		PluginFactory<?, ?, ?, ?, ?> pluginFactory = PluginManager.getPluginFactory(factoryId);

		if (!configTypeHolder.isPresent()) {
			configTypeHolder = Optional
					.of(pluginFactory.createDefaultPluginConfiguration().getClass().getName());
		}

		if (properties == null || properties.isEmpty()) {
			properties = new Properties();
			try {
				pluginFactory.createPluginConfiguration(configTypeHolder.get()).storeProperties(properties);
			} catch (IOException | WrongPropertiesException e) {
				throw new UnloadablePluginConfigurationException(e);
			}
		}
	}

}
