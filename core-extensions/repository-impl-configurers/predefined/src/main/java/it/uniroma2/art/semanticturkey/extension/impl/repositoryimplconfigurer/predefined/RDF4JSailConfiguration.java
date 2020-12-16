package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public abstract class RDF4JSailConfiguration extends AbstractPluginConfiguration
		implements PredefinedConfiguration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.RDF4JSailConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String directTypeInference$description = keyBase + ".directTypeInference.description";
		public static final String directTypeInference$displayName = keyBase + ".directTypeInference.displayName";
		public static final String inferencer$description = keyBase + ".inferencer.description";
		public static final String inferencer$displayName = keyBase + ".inferencer.displayName";
	}

	public static final String SCHEMA_CACHING_RDFS_INFERENCER = "schema-caching-rdfs-inferencer";
	public static final String FORWARD_CHAINING_RDFS_INFERENCER = "forward-chaining-rdfs-inferencer";
	public static final String NONE_INFERENCER = "none";

	@STProperty(description = "{" + MessageKeys.directTypeInference$description + "}", displayName = "{" + MessageKeys.directTypeInference$displayName + "}")
	public boolean directTypeInference = false;

	@STProperty(description = "{" + MessageKeys.inferencer$description + "}", displayName = "{" + MessageKeys.inferencer$displayName + "}")
	@Enumeration({ NONE_INFERENCER, FORWARD_CHAINING_RDFS_INFERENCER, SCHEMA_CACHING_RDFS_INFERENCER })
	public String inferencer = NONE_INFERENCER;
}
