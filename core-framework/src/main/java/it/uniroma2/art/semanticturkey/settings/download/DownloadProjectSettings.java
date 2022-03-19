package it.uniroma2.art.semanticturkey.settings.download;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.Map;

/**
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class DownloadProjectSettings implements Settings {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.download.DonwloadProjectSettings";

        public static final String shortName = keyBase + ".shortName";
        public static final String fileNameToSingleDownloadMap$description = keyBase
                + ".fileNameToSingleDownloadMap.description";
        public static final String fileNameToSingleDownloadMap$displayName = keyBase
                + ".fileNameToSingleDownloadMap.displayName";

    }

    @Override
    public String getShortName() {
        return "{" + DownloadProjectSettings.MessageKeys.shortName + "}";
    }

    /*
    @STProperty(description = "{" + DownloadProjectSettings.MessageKeys.fileName$description
            + "}", displayName = "{" + DownloadProjectSettings.MessageKeys.fileName$displayName + "}")
    public String fileName;
    */

    @STProperty(description = "{" + DownloadProjectSettings.MessageKeys.fileNameToSingleDownloadMap$description
            + "}", displayName = "{" + DownloadProjectSettings.MessageKeys.fileNameToSingleDownloadMap$displayName + "}")
    public Map<String, SingleDownload> fileNameToSingleDownloadMap;


}
