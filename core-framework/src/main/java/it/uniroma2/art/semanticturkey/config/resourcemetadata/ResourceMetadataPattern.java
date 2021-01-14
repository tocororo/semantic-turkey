package it.uniroma2.art.semanticturkey.config.resourcemetadata;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class ResourceMetadataPattern implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.resourcemetadata.ResourceMetadataPattern";

		public static final String shortName = keyBase + ".shortName";
		public static final String description$description = keyBase + ".description.description";
		public static final String description$displayName = keyBase + ".description.displayName";
		public static final String construction$description = keyBase + ".construction.description";
		public static final String construction$displayName = keyBase + ".construction.displayName";
		public static final String update$description = keyBase + ".update.description";
		public static final String update$displayName = keyBase + ".update.displayName";
		public static final String destruction$description = keyBase + ".destruction.description";
		public static final String destruction$displayName = keyBase + ".destruction.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.description$description + "}", displayName = "{" + MessageKeys.description$displayName + "}")
	public String description;

	@STProperty(description = "{" + MessageKeys.construction$description + "}", displayName = "{" + MessageKeys.construction$displayName + "}")
	public String construction;

	@STProperty(description = "{" + MessageKeys.update$description + "}", displayName = "{" + MessageKeys.update$displayName + "}")
	public String update;

	@STProperty(description = "{" + MessageKeys.destruction$description + "}", displayName = "{" + MessageKeys.destruction$displayName + "}")
	public String destruction;

}
