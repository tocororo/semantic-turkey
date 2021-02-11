package it.uniroma2.art.semanticturkey.settings.facets;

import javax.validation.Valid;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import it.uniroma2.art.semanticturkey.properties.Schema;

/**
 * Project facets.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ProjectFacets implements Settings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.project.ProjectFacets";

		public static final String shortName = keyBase + ".shortName";
		public static final String customFacets$description = keyBase + ".customFacets.description";
		public static final String customFacets$displayName = keyBase + ".customFacets.displayName";
	}

	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "Category", displayName = "Category")
	public String category;

	@STProperty(description = "Organization", displayName = "Organization")
	public String organization;

	@STProperty(description = "{" + MessageKeys.customFacets$description + "}", displayName = "{"
			+ MessageKeys.customFacets$displayName + "}")
	@Schema(settingsManager = CustomProjectFacetsSchemaStore.class)
	@Valid
	public STProperties customFacets;
}
