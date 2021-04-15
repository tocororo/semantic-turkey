package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.List;
import java.util.Map;

public class ClassTreeFilter implements STProperties {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.ClassTreeFilter";

        public static final String shortName = keyBase + ".shortName";
        public static final String enabled$description = keyBase + ".enabled.description";
        public static final String enabled$displayName = keyBase + ".enabled.displayName";
        public static final String map$displayName = keyBase + ".map.displayName";
        public static final String map$description = keyBase + ".map.description";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.enabled$description
            + "}", displayName = "{" + MessageKeys.enabled$displayName + "}")
    public boolean enabled;

    @STProperty(description = "{" + MessageKeys.map$description
            + "}", displayName = "{" + MessageKeys.map$displayName + "}")
    public Map<String, List<String>> map;

}
