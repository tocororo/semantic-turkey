package it.uniroma2.art.semanticturkey.settings.project;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.RuntimeSTProperties;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class ProjectFacets extends RuntimeSTProperties implements Settings{

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.project.ProjectFacets";

		public static final String shortName = keyBase + ".shortName";
		public static final String customFacets$description = keyBase + ".customFacets.description";
		public static final String customFacets$displayName = keyBase + ".customFacets.displayName";
	}

	public ProjectFacets() {
		super("{" + MessageKeys.shortName + "}");
	}

	@STProperty(description = "{" + MessageKeys.customFacets$description + "}", displayName = "{" + MessageKeys.customFacets$displayName + "}")
	public STProperties customFacets;
}
