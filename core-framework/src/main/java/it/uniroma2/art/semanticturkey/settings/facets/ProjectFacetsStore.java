package it.uniroma2.art.semanticturkey.settings.facets;

import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;

/**
 * A storage for project facets.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ProjectFacetsStore implements ProjectSettingsManager<ProjectFacets> {

	@Override
	public String getId() {
		return ProjectFacetsStore.class.getName();
	}

}
