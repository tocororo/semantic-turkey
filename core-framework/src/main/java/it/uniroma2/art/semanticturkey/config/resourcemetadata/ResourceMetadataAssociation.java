package it.uniroma2.art.semanticturkey.config.resourcemetadata;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class ResourceMetadataAssociation implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.resourcemetadata.ResourceMetadataAssociation";

		public static final String shortName = keyBase + ".shortName";
		public static final String role$description = keyBase + ".role.description";
		public static final String role$displayName = keyBase + ".role.displayName";
		public static final String patternReference$description = keyBase + ".patternReference.description";
		public static final String patternReference$displayName = keyBase + ".patternReference.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.role$description + "}", displayName = "{" + MessageKeys.role$displayName + "}")
	@Required
	public RDFResourceRole role;

	@STProperty(description = "{" + MessageKeys.patternReference$description + "}", displayName = "{" + MessageKeys.patternReference$displayName + "}")
	@Required
	public String patternReference;

}
