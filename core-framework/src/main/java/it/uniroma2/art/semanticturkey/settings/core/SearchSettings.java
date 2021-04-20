package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.List;

public class SearchSettings implements STProperties {
    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.SearchSettings";

        public static final String shortName = keyBase + ".shortName";
        public static final String restrictLang$description = keyBase + ".restrictLang.description";
        public static final String restrictLang$displayName = keyBase + ".restrictLang.displayName";
        public static final String languages$description = keyBase + ".languages.description";
        public static final String languages$displayName = keyBase + ".languages.displayName";
        public static final String includeLocales$description = keyBase + ".includeLocales.description";
        public static final String includeLocales$displayName = keyBase + ".includeLocales.displayName";
        public static final String useAutocompletion$description = keyBase + ".useAutocompletion.description";
        public static final String useAutocompletion$displayName = keyBase + ".useAutocompletion.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.restrictLang$description
            + "}", displayName = "{" + MessageKeys.restrictLang$displayName + "}")
    public boolean restrictLang;
    
    @STProperty(description = "{" + MessageKeys.languages$description
            + "}", displayName = "{" + MessageKeys.languages$displayName + "}")
    public List<String> languages;

    @STProperty(description = "{" + MessageKeys.includeLocales$description
            + "}", displayName = "{" + MessageKeys.includeLocales$displayName + "}")
    public boolean includeLocales;
    
    @STProperty(description = "{" + MessageKeys.useAutocompletion$description
            + "}", displayName = "{" + MessageKeys.useAutocompletion$displayName + "}")
    public boolean useAutocompletion;

}