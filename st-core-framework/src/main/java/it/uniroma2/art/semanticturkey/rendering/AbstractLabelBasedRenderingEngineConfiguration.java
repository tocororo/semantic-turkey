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
			+ STPropertiesManager.PREF_LANGUAGES + "} to depend on ST Properties")
	public String languages = "${" + STPropertiesManager.PREF_LANGUAGES + "}";

	@STProperty(description = "The template for the redering of resources", displayName = "template")
	public String template;

	@STProperty(description = "Definition of the variables that can be used inside the template", displayName = "variables")
	public String variables;

	@STProperty(description = "Tells whether the rendering engine should ignore the fact that validation is enabled", displayName = "ignore validation")
	public Boolean ignoreValidation;

}
