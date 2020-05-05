package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public abstract class RDF4JSailConfiguration extends AbstractPluginConfiguration
		implements PredefinedConfiguration {

	public static final String SCHEMA_CACHING_RDFS_INFERENCER = "schema-caching-rdfs-inferencer";
	public static final String FORWARD_CHAINING_RDFS_INFERENCER = "forward-chaining-rdfs-inferencer";
	public static final String NONE_INFERENCER = "none";

	@STProperty(description = "true if the RDF4J repository has to support directType inference; defaults to false")
	public boolean directTypeInference = false;

	@STProperty(description = "inferencer to use; defaults to none")
	@Enumeration({ NONE_INFERENCER, FORWARD_CHAINING_RDFS_INFERENCER, SCHEMA_CACHING_RDFS_INFERENCER })
	public String inferencer = NONE_INFERENCER;
}
