package it.uniroma2.art.semanticturkey.plugin.extpts.impls.repositoryimplconfigurer.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfigurationParameter;

public abstract class RDF4JSailConfigurerConfiguration extends AbstractPluginConfiguration
		implements PredefinedRepositoryImplConfigurerConfiguration {

	@PluginConfigurationParameter(description = "true if the RDF4J repository has to support directType inference; defaults to true")
	public boolean directTypeInference = false;

	@PluginConfigurationParameter(description = "true if the RDF4J repository has to support RDFS inferencing; defaults to true")
	public boolean rdfsInference = false;

}
