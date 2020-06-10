package it.uniroma2.art.semanticturkey.config.resourceMetadata;

import it.uniroma2.art.semanticturkey.extension.ProjectScopedConfigurableComponent;

public class ResourceMetadataAssociationStore implements ProjectScopedConfigurableComponent<ResourceMetadataAssociation> {

	@Override
	public String getId() {
		return ResourceMetadataAssociationStore.class.getName();
	}

}