package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.DataSize;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class PreloadProfilerSettings implements STProperties {
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.PreloadProfilerSettings";

		public static final String shortName = keyBase + ".shortName";

		public static final String threshold$description = keyBase + ".threshold.description";
		public static final String threshold$displayName = keyBase + ".threshold.displayName";

	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.threshold$description + "}", displayName = "{"
			+ MessageKeys.threshold$displayName + "}")
	public DataSize threshold;

}
