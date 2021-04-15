package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.STProperty;

public class STARTTLSSmtpMailSettings {
    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.STARTTLSSmtpMailSettings";

        public static final String shortName = keyBase + ".shortName";
        public static final String enable$description = keyBase + ".enable.description";
        public static final String enable$displayName = keyBase + ".enable.displayName";
    }

    @STProperty(description = "{" + MessageKeys.enable$description + "}", displayName = "{"
            + MessageKeys.enable$displayName + "}")
    public Boolean enable;

}
