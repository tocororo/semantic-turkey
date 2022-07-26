package it.uniroma2.art.semanticturkey.settings.uri2projectresolution;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.Map;

public class Uri2ProjectResolutionSettings implements Settings {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.uri2projectresolution.Uri2ProjectResolutionSettings";

        public static final String shortName = keyBase + ".shortName";
        public static final String uri2ProjectMap$description = keyBase
                + ".uri2ProjectMap.description";
        public static final String uri2ProjectMap$displayName = keyBase
                + ".uri2ProjectMap.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.uri2ProjectMap$description + "}", displayName = "{"
            + MessageKeys.uri2ProjectMap$displayName + "}")
    public Map<String, String> uri2ProjectMap;

}