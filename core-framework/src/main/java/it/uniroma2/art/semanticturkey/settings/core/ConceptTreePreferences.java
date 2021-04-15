package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

import java.util.List;

public class ConceptTreePreferences implements STProperties {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.ConceptTreePreferences";

        public static final String shortName = keyBase + ".shortName";
        public static final String baseBroaderProp$description = keyBase + ".baseBroaderProp.description";
        public static final String baseBroaderProp$displayName = keyBase + ".baseBroaderProp.displayName";
        public static final String broaderProps$description = keyBase + ".broaderProps.description";
        public static final String broaderProps$displayName = keyBase + ".broaderProps.displayName";
        public static final String narrowerProps$description = keyBase + ".narrowerProps.description";
        public static final String narrowerProps$displayName = keyBase + ".narrowerProps.displayName";
        public static final String includeSubProps$description = keyBase + ".includeSubProps.description";
        public static final String includeSubProps$displayName = keyBase + ".includeSubProps.displayName";
        public static final String syncInverse$displayName = keyBase + ".syncInverse.displayName";
        public static final String syncInverse$description = keyBase + ".syncInverse.description";
        public static final String visualization$displayName = keyBase + ".visualization.displayName";
        public static final String visualization$description = keyBase + ".visualization.description";
        public static final String multischemeMode$displayName = keyBase + ".multischemeMode.displayName";
        public static final String multischemeMode$description = keyBase + ".multischemeMode.description";
        public static final String safeToGoLimit$displayName = keyBase + ".safeToGoLimit.displayName";
        public static final String safeToGoLimit$description = keyBase + ".safeToGoLimit.description";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.baseBroaderProp$description
            + "}", displayName = "{" + MessageKeys.baseBroaderProp$displayName + "}")
    public String baseBroaderProp;

    @STProperty(description = "{" + MessageKeys.broaderProps$description
            + "}", displayName = "{" + MessageKeys.broaderProps$displayName + "}")
    public List<IRI> broaderProps;

    @STProperty(description = "{" + MessageKeys.narrowerProps$description
            + "}", displayName = "{" + MessageKeys.narrowerProps$displayName + "}")
    public List<IRI> narrowerProps;

    @STProperty(description = "{" + MessageKeys.includeSubProps$description
            + "}", displayName = "{" + MessageKeys.includeSubProps$displayName + "}")
    public Boolean includeSubProps;

    @STProperty(description = "{" + MessageKeys.syncInverse$description
            + "}", displayName = "{" + MessageKeys.syncInverse$displayName + "}")
    public Boolean syncInverse;

    @STProperty(description = "{" + MessageKeys.visualization$description
            + "}", displayName = "{" + MessageKeys.visualization$displayName + "}")
    @Enumeration({"hierarchyBased", "searchBased"})
    public String visualization;

    @STProperty(description = "{" + MessageKeys.multischemeMode$description
            + "}", displayName = "{" + MessageKeys.multischemeMode$displayName + "}")
    @Enumeration({"or", "and"})
    public String multischemeMode;

    @STProperty(description = "{" + MessageKeys.safeToGoLimit$description
            + "}", displayName = "{" + MessageKeys.safeToGoLimit$displayName + "}")
    public Integer safeToGoLimit;


}
