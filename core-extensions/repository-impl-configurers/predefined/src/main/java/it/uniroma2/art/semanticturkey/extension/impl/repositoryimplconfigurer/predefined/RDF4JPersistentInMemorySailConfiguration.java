package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

import it.uniroma2.art.semanticturkey.properties.STProperty;

public class RDF4JPersistentInMemorySailConfiguration extends RDF4JInMemorySailConfiguration {

	@STProperty(description = "time in milliseconds before model is persisted; default is 1000 ms")
	public long syncDelay = 1000L;

	public RDF4JPersistentInMemorySailConfiguration() {
		super();
	}

	public String getShortName() {
		return "in memory / persistent";
	}

	public boolean isPersistent() {
		return true;
	}

}
