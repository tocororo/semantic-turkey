package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

import it.uniroma2.art.semanticturkey.properties.STProperty;

public class RDF4JPersistentInMemorySailConfiguration extends RDF4JInMemorySailConfiguration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.RDF4JPersistentInMemorySailConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String syncDelay$description = keyBase + ".syncDelay.description";
		public static final String syncDelay$displayName = keyBase + ".syncDelay.displayName";
	}

	@STProperty(description = "{" + MessageKeys.syncDelay$description + "}", displayName = "{" + MessageKeys.syncDelay$displayName + "}")
	public long syncDelay = 1000L;

	public RDF4JPersistentInMemorySailConfiguration() {
		super();
	}

	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	public boolean isPersistent() {
		return true;
	}

}
