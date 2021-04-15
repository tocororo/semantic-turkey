package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

public class ClassTreePreferences implements STProperties {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.ClassTreePreferences";

        public static final String shortName = keyBase + ".shortName";
        public static final String rootClass$description = keyBase + ".rootClass.description";
        public static final String rootClass$displayName = keyBase + ".rootClass.displayName";
        public static final String filter$description = keyBase + ".filter.description";
        public static final String filter$displayName = keyBase + ".filter.displayName";
        public static final String showInstancesNumber$description = keyBase + ".showInstancesNumber.description";
        public static final String showInstancesNumber$displayName = keyBase + ".showInstancesNumber.displayName";

    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.rootClass$description
            + "}", displayName = "{" + MessageKeys.rootClass$displayName + "}")
    public IRI rootClass;

    @STProperty(description = "{" + MessageKeys.filter$description
            + "}", displayName = "{" + MessageKeys.filter$displayName + "}")
    public ClassTreeFilter filter;

    @STProperty(description = "{" + MessageKeys.showInstancesNumber$description
            + "}", displayName = "{" + MessageKeys.showInstancesNumber$displayName + "}")
    public Boolean showInstancesNumber;

}
