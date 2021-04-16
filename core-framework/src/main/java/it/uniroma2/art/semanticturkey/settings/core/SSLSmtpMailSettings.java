package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.STProperty;

public class SSLSmtpMailSettings {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.SSLSmtpMailSettings";

        public static final String shortName = keyBase + ".shortName";
        public static final String enable$description = keyBase + ".enable.description";
        public static final String enable$displayName = keyBase + ".enable.displayName";
    }

    @STProperty(description = "{" + SSLSmtpMailSettings.MessageKeys.enable$description + "}", displayName = "{"
            + SSLSmtpMailSettings.MessageKeys.enable$displayName + "}")
    public Boolean enable;

}
