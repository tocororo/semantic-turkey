package it.uniroma2.art.semanticturkey.settings.download;

import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;


/**
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class DownloadSettingsManager implements ProjectSettingsManager<DownloadProjectSettings> {

    @Override
    public String getId() {
        return DownloadSettingsManager.class.getName();
    }
}
