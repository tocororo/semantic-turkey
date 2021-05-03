package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import javax.validation.constraints.Min;

public class LexEntryListPreferences implements STProperties {
    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.LexEntryListPreferences";

        public static final String shortName = keyBase + ".shortName";
        public static final String visualization$description = keyBase + ".visualization.description";
        public static final String visualization$displayName = keyBase + ".visualization.displayName";
        public static final String allowVisualizationChange$description = keyBase + ".allowVisualizationChange.description";
        public static final String allowVisualizationChange$displayName = keyBase + ".allowVisualizationChange.displayName";
        public static final String indexLength$description = keyBase + ".indexLength.description";
        public static final String indexLength$displayName = keyBase + ".indexLength.displayName";
        public static final String safeToGoLimit$description = keyBase + ".safeToGoLimit.description";
        public static final String safeToGoLimit$displayName = keyBase + ".safeToGoLimit.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.visualization$description
            + "}", displayName = "{" + MessageKeys.visualization$displayName + "}")
    @Enumeration({"indexBased", "searchBased"})
    public String visualization;

    @STProperty(description = "{" + MessageKeys.allowVisualizationChange$description
            + "}", displayName = "{" + MessageKeys.allowVisualizationChange$displayName + "}")
    public Boolean allowVisualizationChange;

    @STProperty(description = "{" + MessageKeys.indexLength$description
            + "}", displayName = "{" + MessageKeys.indexLength$displayName + "}")
    @Min(1)
    public Integer indexLength;

    @STProperty(description = "{" + MessageKeys.safeToGoLimit$description
            + "}", displayName = "{" + MessageKeys.safeToGoLimit$displayName + "}")
    public Integer safeToGoLimit;

}
