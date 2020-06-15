package it.uniroma2.art.semanticturkey.settings.alignmentservices;

import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;

/**
 * A settings manager for the association of a project with a remote alignment service.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class RemoteAlignmentServiceProjectSettingsManager
		implements ProjectSettingsManager<RemoteAlignmentServiceProjectSettings> {

	@Override
	public String getId() {
		return RemoteAlignmentServiceProjectSettingsManager.class.getName();
	}

}
