package it.uniroma2.art.semanticturkey.settings.core;

import java.util.List;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class AdminMailSettings implements STProperties {
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.AdminMailSettings";

		public static final String shortName = keyBase + ".shortName";

		public static final String address$description = keyBase + ".addres.description";
		public static final String address$displayName = keyBase + ".addres.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.address$description + "}", displayName = "{"
			+ MessageKeys.address$displayName + "}")
	public List<String> address;

}
