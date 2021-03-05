package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A configuration for the {@link SpreadsheetSerializingExporter}.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 *
 */
public class SpreadsheetSerializingExporterConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.SpreadsheetSerializingExporterConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String language$description = keyBase + ".language.description";
		public static final String language$displayName = keyBase + ".language.displayName";
		public static final String reifiedNotes$description = keyBase + ".reifiedNotes.description";
		public static final String reifiedNotes$displayName = keyBase + ".reifiedNotes.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.language$description + "}", displayName = "{" + MessageKeys.language$displayName + "}")
	public String language;

	@STProperty(description = "{" + MessageKeys.reifiedNotes$description + "}", displayName = "{" + MessageKeys.reifiedNotes$displayName + "}")
	public Boolean reifiedNotes;

}
