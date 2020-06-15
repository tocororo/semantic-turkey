package it.uniroma2.art.semanticturkey.config.resourcemetadata;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class ResourceMetadataAssociation implements Configuration {

	@Override
	public String getShortName() {
		return "Stored Resource Metadata Association";
	}

	@STProperty(description = "Resource role to be associated to a ResourceMetadataUpdater ", displayName = "Role")
	@Required
	public RDFResourceRole role;

	@STProperty(description = "Reference of the ResourceMetadataPattern that is invoked when a resource of the given role is created/updated/destroyed", displayName = "Resource Metadata Updater reference")
	@Required
	public String patternReference;

}
