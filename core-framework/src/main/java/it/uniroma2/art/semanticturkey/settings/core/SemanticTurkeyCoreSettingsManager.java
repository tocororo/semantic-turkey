package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.SystemSettingsManager;
import it.uniroma2.art.semanticturkey.settings.PGSettingsManager;

/**
 * A settings manager for Semantic Turkey core settings.
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SemanticTurkeyCoreSettingsManager implements SystemSettingsManager<CoreSystemSettings>,
        ProjectSettingsManager<CoreProjectSettings>, PUSettingsManager<CorePUSettings>, PGSettingsManager<CorePGSettings> {

    @Override
    public String getId() {
        return SemanticTurkeyCoreSettingsManager.class.getName();
    }

}
