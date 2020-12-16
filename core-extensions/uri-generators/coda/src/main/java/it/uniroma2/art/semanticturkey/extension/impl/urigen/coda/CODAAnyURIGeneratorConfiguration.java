package it.uniroma2.art.semanticturkey.extension.impl.urigen.coda;

import it.uniroma2.art.semanticturkey.plugin.impls.urigen.CODAURIGeneratorFactory;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link CODAURIGeneratorFactory} that supports the use of any CODA converter
 * compliant with the <code>coda:randIdGen</code>.
 *
 */
public class CODAAnyURIGeneratorConfiguration extends CODAURIGeneratorConfiguration {
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.urigen.coda.CODAAnyURIGeneratorConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String converter$description = keyBase + ".converter.description";
		public static final String converter$displayName = keyBase + ".converter.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.converter$description + "}", displayName = "{"
			+ MessageKeys.converter$displayName + "}")
	public String converter = "http://art.uniroma2.it/coda/converters/templateBasedRandIdGen";

}
