package it.uniroma2.art.semanticturkey.settings.facets;

import it.uniroma2.art.semanticturkey.extension.settings.SystemSettingsManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesSchema;

/**
 * A store for the schema of custom project facets.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>.
 *
 */
public class ProjectFacetsSchemaStore implements SystemSettingsManager<STPropertiesSchema> {

	@Override
	public String getId() {
		return ProjectFacetsSchemaStore.class.getName();
	}

}
