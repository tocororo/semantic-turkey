package it.uniroma2.art.semanticturkey.plugin;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.google.gson.JsonObject;

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
	private ObjectNode configuration;

	public PluginSpecification(@JsonProperty("factoryId") String factoryId,
			@JsonProperty("configType") String configType, @JsonProperty("properties") Properties properties,
			@JsonProperty("configuration") ObjectNode configuration) {
		this.factoryId = factoryId;
		this.configTypeHolder = Optional.ofNullable(configType);
		this.properties = properties;
		this.configuration = configuration;

		if (properties == null && configuration != null) {
			this.properties = new Properties();
			configuration.fields().forEachRemaining(entry -> {
				String k = entry.getKey();
				JsonNode v = entry.getValue();
				if (v instanceof ValueNode) {
					this.properties.setProperty(k, v.asText());
				}
			});
		}
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

	public ObjectNode getConfiguration() {
		return this.configuration;
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
