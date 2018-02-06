package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.properties.ContentType;
import it.uniroma2.art.semanticturkey.properties.ContentTypeVocabulary;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public abstract class RDF4JSailConfigurerConfiguration extends AbstractPluginConfiguration
		implements PredefinedRepositoryImplConfigurerConfiguration {

	@STProperty(description = "true if the RDF4J repository has to support directType inference; defaults to true")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	public boolean directTypeInference = false;

	@STProperty(description = "true if the RDF4J repository has to support RDFS inferencing; defaults to true")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	public boolean rdfsInference = false;

}
