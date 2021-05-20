package it.uniroma2.art.semanticturkey.extension.impl.rendering;

import java.util.Map;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public abstract class AbstractLabelBasedRenderingEngineConfiguration implements Configuration {

	public static final String USER_LANGS_VAR = "userLangs";

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rendering.AbstractLabelBasedRenderingEngineConfiguration";

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

	@STProperty(description = "{" + MessageKeys.languages$description + "}", displayName = "{"
			+ MessageKeys.languages$displayName + "}")
	public String languages = "${" + USER_LANGS_VAR + "}";

	@STProperty(description = "{" + MessageKeys.template$description + "}", displayName = "{"
			+ MessageKeys.template$displayName + "}")
	public String template;

	@STProperty(description = "{" + MessageKeys.variables$description + "}", displayName = "{"
			+ MessageKeys.variables$displayName + "}")
	public Map<String, VariableDefinition> variables;

	@STProperty(description = "{" + MessageKeys.ignoreValidation$description + "}", displayName = "{"
			+ MessageKeys.ignoreValidation$displayName + "}")
	public Boolean ignoreValidation;

}
