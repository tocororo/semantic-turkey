package it.uniroma2.art.semanticturkey.config.resourceMetadata;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class ResourceMetadataPattern implements Configuration {

	@Override
	public String getShortName() {
		return "Stored Resource Metadata Pattern";
	}

	@STProperty(description = "Identifier of this ResourceMetadataPattern", displayName = "Id")
	@Required
	public String id;

	@STProperty(description = "A human friendly name of this ResourceMetadataPattern", displayName = "Name")
	@Required
	public String name;

	@STProperty(description = "A description of this ResourceMetadataPattern", displayName = "Description")
	public String description;

	@STProperty(description = "PEARL rule that is executed when a resource is created", displayName = "Construction")
	public String construction;

	@STProperty(description = "PEARL rule that is executed when a resource is updated", displayName = "Update")
	public String update;

	@STProperty(description = "PEARL rule that is executed when a resource is destroyed", displayName = "Destruction")
	public String destruction;

}
