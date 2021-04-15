package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class PreloadSettings implements STProperties {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.PreloadSettings";

		public static final String shortName = keyBase + ".shortName";

		public static final String profiler$description = keyBase + ".profiler.description";
		public static final String profiler$displayName = keyBase + ".profiler.displayName";

	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.profiler$description + "}", displayName = "{"
			+ MessageKeys.profiler$displayName + "}")
	public PreloadProfilerSettings profiler;

}
