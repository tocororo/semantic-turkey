package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class RemoteTripleStoreSettings implements STProperties {
    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.RemoteTripleStoreSettings";

        public static final String shortName = keyBase + ".shortName";

        public static final String serverURL$description = keyBase + ".serverURL.description";
        public static final String serverURL$displayName = keyBase + ".serverURL.displayName";
        public static final String username$description = keyBase + ".username.description";
        public static final String username$displayName = keyBase + ".username.displayName";
        public static final String password$description = keyBase + ".password.description";
        public static final String password$displayName = keyBase + ".password.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.serverURL$description + "}", displayName = "{"
            + MessageKeys.serverURL$displayName + "}")
    public String serverURL;

    @STProperty(description = "{" + MessageKeys.username$description + "}", displayName = "{"
            + MessageKeys.username$displayName + "}")
    public String username;

    @STProperty(description = "{" + MessageKeys.password$description + "}", displayName = "{"
            + MessageKeys.password$displayName + "}")
    public String password;

}
