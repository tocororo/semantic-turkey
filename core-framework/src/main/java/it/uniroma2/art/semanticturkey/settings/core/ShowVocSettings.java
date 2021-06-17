package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class ShowVocSettings implements STProperties {
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.ShowVocSettings";

		public static final String shortName = keyBase + ".shortName";

		public static final String disableContributions$description = keyBase + ".disableContributions.description";
		public static final String disableContributions$displayName = keyBase + ".disableContributions.displayName";

		public static final String vbConnectionConfig$description = keyBase + ".vbConnectionConfig.description";
		public static final String vbConnectionConfig$displayName = keyBase + ".vbConnectionConfig.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.disableContributions$description + "}", displayName = "{"
			+ MessageKeys.disableContributions$displayName + "}")
	public Boolean disableContributions;

	@STProperty(description = "{" + MessageKeys.vbConnectionConfig$description + "}", displayName = "{"
			+ MessageKeys.vbConnectionConfig$displayName + "}")
	public VocBenchConnectionShowVocSettings vbConnectionConfig;

}
