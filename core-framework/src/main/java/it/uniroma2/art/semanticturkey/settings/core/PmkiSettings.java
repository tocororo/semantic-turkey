package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class PmkiSettings implements STProperties {
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.PmkiSettings";

		public static final String shortName = keyBase + ".shortName";

		public static final String vbConnectionConfig$description = keyBase + ".vbConnectionConfig.description";
		public static final String vbConnectionConfig$displayName = keyBase + ".vbConnectionConfig.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.vbConnectionConfig$description + "}", displayName = "{"
			+ MessageKeys.vbConnectionConfig$displayName + "}")
	public VocBenchConnectionPmkiSettings vbConnectionConfig;

}
