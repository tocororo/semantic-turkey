package it.uniroma2.art.semanticturkey.properties.core;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesImpl;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class ProjectPreferences extends STPropertiesImpl implements STProperties {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.properties.core.ProjectPreferences";

		public static final String shortName = keyBase + ".shortName";
		public static final String languages$description = keyBase + ".languages.description";
		public static final String languages$displayName = keyBase + ".languages.displayName";
		public static final String show_flags$description = keyBase + ".show_flags.description";
		public static final String show_flags$displayName = keyBase + ".show_flags.displayName";
		public static final String show_instance_numbers$description = keyBase + ".show_instance_numbers.description";
		public static final String show_instance_numbers$displayName = keyBase + ".show_instance_numbers.displayName";
		public static final String active_scheme$description = keyBase + ".active_scheme.description";
		public static final String active_scheme$displayName = keyBase + ".active_scheme.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.languages$description + "}", displayName = "{" + MessageKeys.languages$displayName + "}")
	public String languages;

	@STProperty(description = "{" + MessageKeys.show_flags$description + "}", displayName = "{" + MessageKeys.show_flags$displayName + "}")
	public boolean show_flags;

	@STProperty(description = "{" + MessageKeys.show_instance_numbers$description + "}", displayName = "{" + MessageKeys.show_instance_numbers$displayName + "}")
	public boolean show_instance_numbers;

	@STProperty(description = "{" + MessageKeys.active_scheme$description + "}", displayName = "{" + MessageKeys.active_scheme$displayName + "}")
	public String active_scheme;
}
