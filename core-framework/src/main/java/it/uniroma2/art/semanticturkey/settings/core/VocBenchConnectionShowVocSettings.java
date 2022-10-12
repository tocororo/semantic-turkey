package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class VocBenchConnectionShowVocSettings implements STProperties {
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.VocBenchConnectionShowVocSettings";

		public static final String shortName = keyBase + ".shortName";

		public static final String vbURL$description = keyBase + ".vbURL.description";
		public static final String vbURL$displayName = keyBase + ".vbURL.displayName";
		public static final String stHost$description = keyBase + ".stHost.description";
		public static final String stHost$displayName = keyBase + ".stHost.displayName";
		public static final String adminEmail$description = keyBase + ".adminEmail.description";
		public static final String adminEmail$displayName = keyBase + ".adminEmail.displayName";
		public static final String adminPassword$description = keyBase + ".adminPassword.description";
		public static final String adminPassword$displayName = keyBase + ".adminPassword.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.vbURL$description + "}", displayName = "{"
			+ MessageKeys.vbURL$displayName + "}")
	public String vbURL;

	@STProperty(description = "{" + MessageKeys.stHost$description + "}", displayName = "{"
			+ MessageKeys.stHost$displayName + "}")
	public String stHost;

	@STProperty(description = "{" + MessageKeys.adminEmail$description + "}", displayName = "{"
			+ MessageKeys.adminEmail$displayName + "}")
	public String adminEmail;

	@STProperty(description = "{" + MessageKeys.adminPassword$description + "}", displayName = "{"
			+ MessageKeys.adminPassword$displayName + "}")
	public String adminPassword;

}
