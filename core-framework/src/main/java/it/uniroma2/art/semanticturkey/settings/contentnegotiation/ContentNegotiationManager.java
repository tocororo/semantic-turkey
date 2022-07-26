package it.uniroma2.art.semanticturkey.settings.contentnegotiation;

import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;

public class ContentNegotiationManager implements ProjectSettingsManager<ContentNegotiationSettings> {

    @Override
    public String getId() {
        return ContentNegotiationManager.class.getName();
    }



}
