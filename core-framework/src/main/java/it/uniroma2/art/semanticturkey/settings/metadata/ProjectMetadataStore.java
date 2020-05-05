package it.uniroma2.art.semanticturkey.settings.metadata;

import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.SystemSettingsManager;

/**
 * A storage for project metadata
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ProjectMetadataStore implements SystemSettingsManager<StoredProjectMetadata>,
		ProjectSettingsManager<StoredProjectMetadata> {

	@Override
	public String getId() {
		return ProjectMetadataStore.class.getName();
	}

}
