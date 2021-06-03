package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.List;

public class CoreSystemSettings implements Settings {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.CoreSystemSettings";

        public static final String shortName = keyBase + ".shortName";

        public static final String adminList$description = keyBase + ".adminList.description";
        public static final String adminList$displayName = keyBase + ".adminList.displayName";
        public static final String remoteConfigs$description = keyBase
                + ".remoteConfigs.description";
        public static final String remoteConfigs$displayName = keyBase
                + ".remoteConfigs.displayName";
        public static final String experimentalFeaturesEnabled$description = keyBase
                + ".experimentalFeaturesEnabled.description";
        public static final String experimentalFeaturesEnabled$displayName = keyBase
                + ".experimentalFeaturesEnabled.displayName";
        public static final String showFlags$description = keyBase + ".showFlags.description";
        public static final String showFlags$displayName = keyBase + ".showFlags.displayName";
        public static final String emailVerification$description = keyBase + ".emailVerification.description";
        public static final String emailVerification$displayName = keyBase + ".emailVerification.displayName";
        public static final String homeContent$description = keyBase + ".homeContent.description";
        public static final String homeContent$displayName = keyBase + ".homeContent.displayName";
        public static final String projectCreation$description = keyBase + ".projectCreation.description";
        public static final String projectCreation$displayName = keyBase + ".projectCreation.displayName";
        public static final String preload$description = keyBase + ".preload.description";
        public static final String preload$displayName = keyBase + ".preload.displayName";
        public static final String stDataVersion$description = keyBase + ".stDataVersion.description";
        public static final String stDataVersion$displayName = keyBase + ".stDataVersion.displayName";
        public static final String mail$description = keyBase + ".mail.description";
        public static final String mail$displayName = keyBase + ".mail.displayName";
        public static final String showvoc$description = keyBase + ".showvoc.description";
        public static final String showvoc$displayName = keyBase + ".showvoc.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.adminList$description + "}", displayName = "{"
            + MessageKeys.adminList$displayName + "}")
    public List<String> adminList;

    @STProperty(description = "{" + MessageKeys.remoteConfigs$description
            + "}", displayName = "{" + MessageKeys.remoteConfigs$displayName + "}")
    public List<RemoteTripleStoreSettings> remoteConfigs;

    @STProperty(description = "{" + MessageKeys.experimentalFeaturesEnabled$description
            + "}", displayName = "{" + MessageKeys.experimentalFeaturesEnabled$displayName + "}")
    public Boolean experimentalFeaturesEnabled = false;

    @STProperty(description = "{" + MessageKeys.showFlags$description + "}", displayName = "{"
            + MessageKeys.showFlags$displayName + "}")
    public Boolean showFlags = true;

    @STProperty(description = "{" + MessageKeys.emailVerification$description + "}", displayName = "{"
            + MessageKeys.emailVerification$displayName + "}")
    public Boolean emailVerification = false;

    @STProperty(description = "{" + MessageKeys.homeContent$description + "}", displayName = "{"
            + MessageKeys.homeContent$displayName + "}")
    public String homeContent;

    @STProperty(description = "{" + MessageKeys.projectCreation$description
            + "}", displayName = "{" + MessageKeys.projectCreation$displayName
            + "}")
    public ProjectCreationSettings projectCreation;

    @STProperty(description = "{" + MessageKeys.preload$description + "}", displayName = "{"
            + MessageKeys.preload$displayName + "}")
    public PreloadSettings preload;

    @STProperty(description = "{" + MessageKeys.stDataVersion$description + "}", displayName = "{"
            + MessageKeys.stDataVersion$displayName + "}")
    public String stDataVersion;

    @STProperty(description = "{" + MessageKeys.mail$description + "}", displayName = "{"
            + MessageKeys.mail$displayName + "}")
    public MailSettings mail;

    @STProperty(description = "{" + MessageKeys.showvoc$description + "}", displayName = "{"
            + MessageKeys.showvoc$displayName + "}")
    public ShowVocSettings showvoc;
}
