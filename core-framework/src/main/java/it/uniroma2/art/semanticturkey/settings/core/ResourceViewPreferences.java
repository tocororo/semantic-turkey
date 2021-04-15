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
        public static final String mode$description = keyBase + ".mode.description";
        public static final String mode$displayName = keyBase + ".mode.displayName";
        public static final String syncTabs$description = keyBase + ".syncTabs.description";
        public static final String syncTabs$displayName = keyBase + ".syncTabs.displayName";
        public static final String defaultConceptType$description = keyBase + ".defaultConceptType.description";
        public static final String defaultConceptType$displayName = keyBase + ".defaultConceptType.displayName";
        public static final String lastConceptType$description = keyBase + ".lastConceptType.description";
        public static final String lastConceptType$displayName = keyBase + ".lastConceptType.displayName";
        public static final String defaultLexEntryType$description = keyBase + ".defaultLexEntryType.description";
        public static final String defaultLexEntryType$displayName = keyBase + ".defaultLexEntryType.displayName";
        public static final String lastLexEntryType$description = keyBase + ".lastLexEntryType.description";
        public static final String lastLexEntryType$displayName = keyBase + ".lastLexEntryType.displayName";
        public static final String displayImages$description = keyBase + ".displayImages.description";
        public static final String displayImages$displayName = keyBase + ".displayImages.displayName";
        public static final String resViewPartitionFilter$description = keyBase + ".resViewPartitionFilter.description";
        public static final String resViewPartitionFilter$displayName = keyBase + ".resViewPartitionFilter.displayName";
        public static final String rendering$description = keyBase + ".rendering.description";
        public static final String rendering$displayName = keyBase + ".rendering.displayName";
        public static final String inference$description = keyBase + ".inference.description";
        public static final String inference$displayName = keyBase + ".inference.displayName";
        public static final String showDeprecated$description = keyBase + ".showDeprecated.description";
        public static final String showDeprecated$displayName = keyBase + ".showDeprecated.displayName";
        public static final String showDatatypeBadge$description = keyBase + ".showDatatypeBadge.description";
        public static final String showDatatypeBadge$displayName = keyBase + ".showDatatypeBadge.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    /*
    @STProperty(description = "{" + MessageKeys.mode$description
            + "}", displayName = "{" + MessageKeys.mode$displayName + "}")
    @Enumeration({"tabbed", "splitted"})
    public String mode;
    */

    /*
    @STProperty(description = "{" + MessageKeys.syncTabs$description
            + "}", displayName = "{" + MessageKeys.syncTabs$displayName + "}")
    public Boolean syncTabs;
    */

    @STProperty(description = "{" + MessageKeys.defaultConceptType$description
            + "}", displayName = "{" + MessageKeys.defaultConceptType$displayName + "}")
    @Enumeration({"resourceView", "termView", "code"})
    public String defaultConceptType;

    /*
    @STProperty(description = "{" + MessageKeys.lastConceptType$description
            + "}", displayName = "{" + MessageKeys.lastConceptType$displayName + "}")
    @Enumeration({"resourceView", "termView", "code"})
    public String lastConceptType;
    */

    @STProperty(description = "{" + MessageKeys.defaultLexEntryType$description
            + "}", displayName = "{" + MessageKeys.defaultLexEntryType$displayName + "}")
    @Enumeration({"resourceView", "lexicographerView", "code"})
    public String defaultLexEntryType;

    /*
    @STProperty(description = "{" + MessageKeys.lastLexEntryType$description
            + "}", displayName = "{" + MessageKeys.lastLexEntryType$displayName + "}")
    @Enumeration({"resourceView", "lexicographerView", "code"})
    public String lastLexEntryType;
    */

    /*
    @STProperty(description = "{" + MessageKeys.displayImages$description
            + "}", displayName = "{" + MessageKeys.displayImages$displayName + "}")
    public boolean displayImages;
    */

    @STProperty(description = "{" + MessageKeys.resViewPartitionFilter$description
            + "}", displayName = "{" + MessageKeys.resViewPartitionFilter$displayName + "}")
    public Map<String, List<String>> resViewPartitionFilter;

    /*
    @STProperty(description = "{" + MessageKeys.rendering$description
            + "}", displayName = "{" + MessageKeys.rendering$displayName + "}")
    public boolean rendering;
    */

    /*
    @STProperty(description = "{" + MessageKeys.inference$description
            + "}", displayName = "{" + MessageKeys.inference$displayName + "}")
    public boolean inference;
    */

    /*
    @STProperty(description = "{" + MessageKeys.showDeprecated$description
            + "}", displayName = "{" + MessageKeys.showDeprecated$displayName + "}")
    public boolean showDeprecated;
    */

    /*
    @STProperty(description = "{" + MessageKeys.showDatatypeBadge$description
            + "}", displayName = "{" + MessageKeys.showDatatypeBadge$displayName + "}")
    public boolean showDatatypeBadge;
    */

}
