package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.List;
import java.util.Map;

public class ResourceViewProjectSettings implements STProperties {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.ResourceViewProjectSettings";

        public static final String shortName = keyBase + ".shortName";
        public static final String customSections$description = keyBase + ".customSections.description";
        public static final String customSections$displayName = keyBase + ".customSections.displayName";
        public static final String templates$description = keyBase + ".templates.description";
        public static final String templates$displayName = keyBase + ".templates.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.customSections$description + "}", displayName = "{" + MessageKeys.customSections$displayName +"}")
    public Map<String, ResourceViewCustomSectionSettings> customSections;

    @STProperty(description = "{" + MessageKeys.templates$description + "}", displayName = "{" + MessageKeys.templates$displayName +"}")
    public Map<RDFResourceRole, List<String>> templates;

}
