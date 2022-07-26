package it.uniroma2.art.semanticturkey.settings.uri2projectresolution;

import it.uniroma2.art.semanticturkey.extension.settings.SystemSettingsManager;

public class Uri2ProjectResolutionManager implements SystemSettingsManager<Uri2ProjectResolutionSettings> {

    @Override
    public String getId() {
        return Uri2ProjectResolutionManager.class.getName();
    }

}
