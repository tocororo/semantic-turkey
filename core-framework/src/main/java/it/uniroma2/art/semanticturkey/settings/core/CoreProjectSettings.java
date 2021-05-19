package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.Language;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.List;

public class CoreProjectSettings implements Settings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.CoreProjectSettings";

		public static final String shortName = keyBase + ".shortName";
		public static final String languages$description = keyBase
				+ ".languages.description";
		public static final String languages$displayName = keyBase
				+ ".languages.displayName";
		public static final String labelClashMode$description = keyBase
				+ ".labelClashMode.description";
		public static final String labelClashMode$displayName = keyBase
				+ ".labelClashMode.displayName";
		public static final String resourceView$description = keyBase
				+ ".resourceView.description";
		public static final String resourceView$displayName = keyBase
				+ ".resourceView.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.languages$description
			+ "}", displayName = "{" + MessageKeys.languages$displayName + "}")
	public List<Language> languages;

	@STProperty(description = "{" + MessageKeys.labelClashMode$description
			+ "}", displayName = "{" + MessageKeys.labelClashMode$displayName + "}")
	@Enumeration({"forbid", "warning", "allow"})
	public String labelClashMode;

	@STProperty(description = "{" + MessageKeys.resourceView$description
			+ "}", displayName = "{" + MessageKeys.resourceView$displayName + "}")
	public ResourceViewProjectSettings resourceView;

}
