package it.uniroma2.art.semanticturkey.rendering;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Base configuration class for {@link RenderingEngine}s which are based on natural language labels.
 */
public abstract class AbstractLabelBasedRenderingEngineConfiguration extends AbstractPluginConfiguration {

	@STProperty(description = "A comma-separated list of language tags. Use ${"
			+ STPropertiesManager.PROP_LANGUAGES + "} to depend on ST Properties")
	public String languages = "${" + STPropertiesManager.PROP_LANGUAGES + "}";

}
