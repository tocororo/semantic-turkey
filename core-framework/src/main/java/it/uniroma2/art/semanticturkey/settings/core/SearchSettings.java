package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.List;

public class SearchSettings implements STProperties {
    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.SearchSettings";

        public static final String shortName = keyBase + ".shortName";
        public static final String stringMatchMode$description = keyBase + ".stringMatchMode.description";
        public static final String stringMatchMode$displayName = keyBase + ".stringMatchMode.displayName";
        public static final String useURI$description = keyBase + ".useURI.description";
        public static final String useURI$displayName = keyBase + ".useURI.displayName";
        public static final String useLocalName$description = keyBase + ".useLocalName.description";
        public static final String useLocalName$displayName = keyBase + ".useLocalName.displayName";
        public static final String useNotes$description = keyBase + ".useNotes.description";
        public static final String useNotes$displayName = keyBase + ".useNotes.displayName";
        public static final String restrictLang$description = keyBase + ".restrictLang.description";
        public static final String restrictLang$displayName = keyBase + ".restrictLang.displayName";
        public static final String languages$description = keyBase + ".languages.description";
        public static final String languages$displayName = keyBase + ".languages.displayName";
        public static final String includeLocales$description = keyBase + ".includeLocales.description";
        public static final String includeLocales$displayName = keyBase + ".includeLocales.displayName";
        public static final String useAutocompletion$description = keyBase + ".useAutocompletion.description";
        public static final String useAutocompletion$displayName = keyBase + ".useAutocompletion.displayName";
        public static final String restrictActiveScheme$description = keyBase + ".restrictActiveScheme.description";
        public static final String restrictActiveScheme$displayName = keyBase + ".restrictActiveScheme.displayName";
        public static final String extendToAllIndividuals$description = keyBase + ".extendToAllIndividuals.description";
        public static final String extendToAllIndividuals$displayName = keyBase + ".extendToAllIndividuals.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    /*
    @STProperty(description = "{" + MessageKeys.stringMatchMode$description
            + "}", displayName = "{" + MessageKeys.stringMatchMode$displayName + "}")
    @Enumeration({"startsWith", "contains", "endsWith", "exact", "fuzzy"})
    public String stringMatchMode;
     */

    /*
    @STProperty(description = "{" + MessageKeys.useURI$description
            + "}", displayName = "{" + MessageKeys.useURI$displayName + "}")
    public boolean useURI;
    */

    /*
    @STProperty(description = "{" + MessageKeys.useLocalName$description
            + "}", displayName = "{" + MessageKeys.useLocalName$displayName + "}")
    public boolean useLocalName;
    */

    /*
    @STProperty(description = "{" + MessageKeys.useNotes$description
            + "}", displayName = "{" + MessageKeys.useNotes$displayName + "}")
    public boolean useNotes;
    */

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

    /*
    @STProperty(description = "{" + MessageKeys.restrictActiveScheme$description
            + "}", displayName = "{" + MessageKeys.restrictActiveScheme$displayName + "}")
    public boolean restrictActiveScheme;
    */

    /*
    @STProperty(description = "{" + MessageKeys.extendToAllIndividuals$description
            + "}", displayName = "{" + MessageKeys.extendToAllIndividuals$displayName + "}")
    public boolean extendToAllIndividuals;
    */
}