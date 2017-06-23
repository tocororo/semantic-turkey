package it.uniroma2.art.semanticturkey.plugin.impls.repositoryimplconfigurer.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public abstract class RDF4JSailConfigurerConfiguration extends AbstractPluginConfiguration
		implements PredefinedRepositoryImplConfigurerConfiguration {

	@STProperty(description = "true if the RDF4J repository has to support directType inference; defaults to true")
	public boolean directTypeInference = false;

	@STProperty(description = "true if the RDF4J repository has to support RDFS inferencing; defaults to true")
	public boolean rdfsInference = false;

}
