package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.zthesserializer;

import java.util.Set;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class ZthesSerializingExporterConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.zthesserializer.ZthesSerializingExporterConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String pivotLanguages$description = keyBase + ".pivotLanguages.description";
		public static final String pivotLanguages$displayName = keyBase + ".pivotLanguages.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}
	
	@STProperty(description = "{" + MessageKeys.pivotLanguages$description + "}", displayName = "{" + MessageKeys.pivotLanguages$displayName + "}")
	public Set<String> pivotLanguages;
	
}

