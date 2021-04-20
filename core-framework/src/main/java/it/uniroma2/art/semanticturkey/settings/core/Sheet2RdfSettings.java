package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.List;

public class Sheet2RdfSettings implements STProperties {
    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.Sheet2RdfSettings";

        public static final String shortName = keyBase + ".shortName";
        public static final String useHeaders$description = keyBase + ".useHeaders.description";
        public static final String useHeaders$displayName = keyBase + ".useHeaders.displayName";
        public static final String namingStrategy$description = keyBase + ".namingStrategy.description";
        public static final String namingStrategy$displayName = keyBase + ".namingStrategy.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.useHeaders$description
            + "}", displayName = "{" + MessageKeys.useHeaders$displayName + "}")
    public boolean useHeaders;
    
    @STProperty(description = "{" + MessageKeys.namingStrategy$description
            + "}", displayName = "{" + MessageKeys.namingStrategy$displayName + "}")
    @Enumeration({"columnAlphabeticIndex", "columnNumericIndex", "normalizedHeaderName"})
    public String namingStrategy;


}