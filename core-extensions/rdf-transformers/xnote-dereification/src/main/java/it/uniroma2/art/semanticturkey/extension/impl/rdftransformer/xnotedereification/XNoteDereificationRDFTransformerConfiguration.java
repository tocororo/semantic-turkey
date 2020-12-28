package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xnotedereification;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class XNoteDereificationRDFTransformerConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xnotedereification.XNoteDereificationRDFTransformerConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String preserveReifiedNotes$description = keyBase + ".preserveReifiedNotes.description";
		public static final String preserveReifiedNotes$displayName = keyBase + ".preserveReifiedNotes.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.preserveReifiedNotes$description + "}", displayName = "{" + MessageKeys.preserveReifiedNotes$displayName + "}")
	public boolean preserveReifiedNotes = true;
}
