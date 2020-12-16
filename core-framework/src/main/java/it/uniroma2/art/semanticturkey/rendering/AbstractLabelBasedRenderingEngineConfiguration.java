package it.uniroma2.art.semanticturkey.rendering;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Base configuration class for {@link RenderingEngine}s which are based on natural language labels.
 */
public abstract class AbstractLabelBasedRenderingEngineConfiguration extends AbstractPluginConfiguration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.rendering.AbstractLabelBasedRenderingEngineConfiguration";
		
		public static final String shortName = keyBase + ".shortName";
		public static final String languages$description = keyBase + ".languages.description";
		public static final String languages$displayName = keyBase + ".languages.displayName";
		public static final String template$description = keyBase + ".template.description";
		public static final String template$displayName = keyBase + ".template.displayName";
		public static final String variables$description = keyBase + ".variables.description";
		public static final String variables$displayName = keyBase + ".variables.displayName";
		public static final String ignoreValidation$description = keyBase + ".ignoreValidation.description";
		public static final String ignoreValidation$displayName = keyBase + ".ignoreValidation.displayName";
	}

	@STProperty(description = MessageKeys.languages$description, displayName = MessageKeys.languages$displayName)
	public String languages = "${" + STPropertiesManager.PREF_LANGUAGES + "}";

	@STProperty(description = MessageKeys.template$description, displayName = MessageKeys.template$displayName)
	public String template;

	@STProperty(description = MessageKeys.variables$description, displayName = MessageKeys.variables$displayName)
	public String variables;

	@STProperty(description = MessageKeys.ignoreValidation$description, displayName = MessageKeys.ignoreValidation$displayName)
	public Boolean ignoreValidation;

}
