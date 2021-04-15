package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.List;

public class ProjectCreationSettings implements STProperties {
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.ProjectCreationSettings";

		public static final String shortName = keyBase + ".shortName";

		public static final String aclUniversalAccessDefault$description = keyBase + ".aclUniversalAccessDefault.description";
		public static final String aclUniversalAccessDefault$displayName = keyBase + ".aclUniversalAccessDefault.displayName";
		public static final String openAtStartUpDefault$description = keyBase + ".openAtStartUpDefault.description";
		public static final String openAtStartUpDefault$displayName = keyBase + ".openAtStartUpDefault.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.aclUniversalAccessDefault$description + "}", displayName = "{"
			+ MessageKeys.aclUniversalAccessDefault$displayName + "}")
	public Boolean aclUniversalAccessDefault;

	@STProperty(description = "{" + MessageKeys.openAtStartUpDefault$description + "}", displayName = "{"
			+ MessageKeys.openAtStartUpDefault$displayName + "}")
	public Boolean openAtStartUpDefault;

}
