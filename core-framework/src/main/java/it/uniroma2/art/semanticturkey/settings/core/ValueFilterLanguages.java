package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.List;

public class ValueFilterLanguages implements STProperties {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.ValueFilterLanguages";

        public static final String shortName = keyBase + ".shortName";
        public static final String languages$description = keyBase + ".languages.description";
        public static final String languages$displayName = keyBase + ".languages.displayName";
        public static final String enabled$description = keyBase + ".enabled.description";
        public static final String enabled$displayName = keyBase + ".enabled.displayName";

    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.languages$description
            + "}", displayName = "{" + MessageKeys.languages$displayName + "}")
    public List<String> languages;

    @STProperty(description = "{" + MessageKeys.enabled$description
            + "}", displayName = "{" + MessageKeys.enabled$displayName + "}")
    public boolean enabled;

}
