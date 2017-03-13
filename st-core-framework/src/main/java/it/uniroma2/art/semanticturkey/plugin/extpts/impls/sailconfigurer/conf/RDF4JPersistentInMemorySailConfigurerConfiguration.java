package it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfigurationParameter;

public class RDF4JPersistentInMemorySailConfigurerConfiguration extends RDF4JInMemorySailConfigurerConfiguration {

	@PluginConfigurationParameter(description = "time in milliseconds before model is persisted; default is 1000 ms")
	public long syncDelay = 1000L;

	public RDF4JPersistentInMemorySailConfigurerConfiguration() {
		super();
	}

	public String getShortName() {
		return "in memory / persistent";
	}

	public boolean isPersistent() {
		return true;
	}

}
