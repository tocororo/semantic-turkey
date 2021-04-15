package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class FromMailSettings implements STProperties {
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.FromMailSettings";

		public static final String shortName = keyBase + ".shortName";

		public static final String address$description = keyBase + ".address.description";
		public static final String address$displayName = keyBase + ".address.displayName";
		public static final String password$description = keyBase + ".password.description";
		public static final String password$displayName = keyBase + ".password.displayName";
		public static final String alias$description = keyBase + ".alias.description";
		public static final String alias$displayName = keyBase + ".alias.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.address$description + "}", displayName = "{"
			+ MessageKeys.address$displayName + "}")
	public String address;


	@STProperty(description = "{" + MessageKeys.password$description + "}", displayName = "{"
			+ MessageKeys.password$displayName + "}")
	public String password;

	@STProperty(description = "{" + MessageKeys.alias$description + "}", displayName = "{"
			+ MessageKeys.alias$displayName + "}")
	public String alias;
}
