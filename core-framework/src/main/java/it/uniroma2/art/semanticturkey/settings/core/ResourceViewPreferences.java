package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.List;
import java.util.Map;

public class ResourceViewPreferences implements STProperties {
    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.ResourceViewPreferences";

        public static final String shortName = keyBase + ".shortName";
        public static final String defaultConceptType$description = keyBase + ".defaultConceptType.description";
        public static final String defaultConceptType$displayName = keyBase + ".defaultConceptType.displayName";
        public static final String defaultLexEntryType$description = keyBase + ".defaultLexEntryType.description";
        public static final String defaultLexEntryType$displayName = keyBase + ".defaultLexEntryType.displayName";
        public static final String resViewPartitionFilter$description = keyBase + ".resViewPartitionFilter.description";
        public static final String resViewPartitionFilter$displayName = keyBase + ".resViewPartitionFilter.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.defaultConceptType$description
            + "}", displayName = "{" + MessageKeys.defaultConceptType$displayName + "}")
    @Enumeration({"resourceView", "termView", "code"})
    public String defaultConceptType;

    @STProperty(description = "{" + MessageKeys.defaultLexEntryType$description
            + "}", displayName = "{" + MessageKeys.defaultLexEntryType$displayName + "}")
    @Enumeration({"resourceView", "lexicographerView", "code"})
    public String defaultLexEntryType;

    @STProperty(description = "{" + MessageKeys.resViewPartitionFilter$description
            + "}", displayName = "{" + MessageKeys.resViewPartitionFilter$displayName + "}")
    public Map<String, List<String>> resViewPartitionFilter;

}
