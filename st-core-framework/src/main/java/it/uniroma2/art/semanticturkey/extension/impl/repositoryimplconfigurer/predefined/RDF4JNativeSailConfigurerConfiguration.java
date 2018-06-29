package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

import it.uniroma2.art.semanticturkey.properties.STProperty;

public class RDF4JNativeSailConfigurerConfiguration extends RDF4JSailConfigurerConfiguration {

	@STProperty(description = "Specifies whether updates should be synced to disk forcefully; defaults to false")
	public Boolean forceSync = false;

	@STProperty(description = "specifies the triple indexes to be created for optimizing"
			+ "query resolution; see: http://rdf4j.org/doc/programming-with-rdf4j/the-repository-api/#Creating_a_Native_RDF_Repository; defaults to spoc, posc")
	public String tripleIndexes = "spoc, posc";

	public RDF4JNativeSailConfigurerConfiguration() {
		super();
	}

	public String getShortName() {
		return "native store / persistent";
	}

	public boolean isPersistent() {
		return true;
	}
}
