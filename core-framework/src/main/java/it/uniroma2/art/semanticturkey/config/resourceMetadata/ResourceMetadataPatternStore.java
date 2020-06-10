package it.uniroma2.art.semanticturkey.config.resourceMetadata;

import it.uniroma2.art.semanticturkey.extension.ProjectScopedConfigurableComponent;

public class ResourceMetadataPatternStore implements ProjectScopedConfigurableComponent<ResourceMetadataPattern> {

	@Override
	public String getId() {
		return ResourceMetadataPatternStore.class.getName();
	}

}
