package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

import it.uniroma2.art.semanticturkey.properties.STProperty;

public class RDF4JNativeSailConfiguration extends RDF4JSailConfiguration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.RDF4JNativeSailConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String forceSync$description = keyBase + ".forceSync.description";
		public static final String forceSync$displayName = keyBase + ".forceSync.displayName";
		public static final String tripleIndexes$description = keyBase + ".tripleIndexes.description";
		public static final String tripleIndexes$displayName = keyBase + ".tripleIndexes.displayName";
	}

	@STProperty(description = "{" + MessageKeys.forceSync$description + "}", displayName = "{" + MessageKeys.forceSync$displayName+ "}")
	public Boolean forceSync = false;

	@STProperty(description = "{" + MessageKeys.tripleIndexes$description + "}", displayName = "{" + MessageKeys.tripleIndexes$displayName+ "}")
	public String tripleIndexes = "spoc, posc";

	public RDF4JNativeSailConfiguration() {
		super();
	}

	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	public boolean isPersistent() {
		return true;
	}
}
