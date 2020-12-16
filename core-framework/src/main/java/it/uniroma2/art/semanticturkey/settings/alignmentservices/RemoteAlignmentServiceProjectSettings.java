package it.uniroma2.art.semanticturkey.settings.alignmentservices;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class RemoteAlignmentServiceProjectSettings implements Settings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.alignmentservices.RemoteAlignmentServiceProjectSettings";

		public static final String shortName = keyBase + ".shortName";
		public static final String configID$description = keyBase + ".configID.description";
		public static final String configID$displayName = keyBase + ".configID.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.configID$description + "}", displayName = "{"
			+ MessageKeys.configID$displayName + "}")
	public String configID;

}
