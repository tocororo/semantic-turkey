package it.uniroma2.art.semanticturkey.rendering;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfigurationParameter;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;

/**
 * Base configuration class for {@link RenderingEngine}s which are based on natural language labels.
 */
public abstract class AbstractLabelBasedRenderingEngineConfiguration extends AbstractPluginConfiguration {

	@PluginConfigurationParameter(description = "A comma-separated list of language tags. Use ${"
			+ STPropertiesManager.PROP_LANGUAGES + "} to depend on ST Properties")
	public String languages = "${" + STPropertiesManager.PROP_LANGUAGES + "}";

}
