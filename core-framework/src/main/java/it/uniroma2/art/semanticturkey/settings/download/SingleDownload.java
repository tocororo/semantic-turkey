package it.uniroma2.art.semanticturkey.settings.download;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.util.Map;

/**
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class SingleDownload implements Settings {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.download.SingleDownload";

        public static final String shortName = keyBase + ".shortName";
        public static final String fileName$description = keyBase
                + ".fileName.description";
        public static final String fileName$displayName = keyBase
                + ".fileName.displayName";
        public static final String langToLocalizedMap$description = keyBase
                + ".langToLocalizedMap.description";
        public static final String langToLocalizedMap$displayName = keyBase
                + ".langToLocalizedMap.displayName";
        public static final String timestamp$description = keyBase
                + ".timestamp.description";
        public static final String timestamp$displayName = keyBase
                + ".timestamp.displayName";
        public static final String format$description = keyBase
                + ".format.description";
        public static final String format$displayName = keyBase
                + ".format.displayName";

    }

    @Override
    public String getShortName() {
        return "{" + SingleDownload.MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + SingleDownload.MessageKeys.fileName$description
            + "}", displayName = "{" + SingleDownload.MessageKeys.fileName$displayName + "}")
    public String fileName;


    @STProperty(description = "{" + SingleDownload.MessageKeys.langToLocalizedMap$description
            + "}", displayName = "{" + SingleDownload.MessageKeys.langToLocalizedMap$displayName + "}")
    public Map<String, String> langToLocalizedMap;


    @STProperty(description = "{" + SingleDownload.MessageKeys.timestamp$description
            + "}", displayName = "{" + SingleDownload.MessageKeys.timestamp$displayName + "}")
    public String timestamp;


    @STProperty(description = "{" + SingleDownload.MessageKeys.format$description
            + "}", displayName = "{" + SingleDownload.MessageKeys.format$displayName + "}")
    public String format;

}
