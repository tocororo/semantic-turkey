package it.uniroma2.art.semanticturkey.extension.extpts.rendering;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class RenderingEnginePUSettings implements Settings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEnginePUSettings";

		public static final String shortName = keyBase + ".shortName";
		public static final String languages$description = keyBase + ".languages.description";
		public static final String languages$displayName = keyBase + ".languages.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.languages$description + "}", displayName = "{" + MessageKeys.languages$displayName + "}")
	@Required
	public String languages = "*";
}
