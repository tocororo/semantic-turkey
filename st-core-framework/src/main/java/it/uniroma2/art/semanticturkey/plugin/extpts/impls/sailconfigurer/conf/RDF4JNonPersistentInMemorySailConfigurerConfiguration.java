package it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf;

public class RDF4JNonPersistentInMemorySailConfigurerConfiguration extends RDF4JInMemorySailConfigurerConfiguration {

	public RDF4JNonPersistentInMemorySailConfigurerConfiguration() {
		super();
	}

	public String getShortName() {
		return "in memory / no persist";
	}

}
