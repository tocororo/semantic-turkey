package it.uniroma2.art.semanticturkey.config.contribution;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class StoredStableResourceContributionConfiguration extends StoredMetadataContributionConfiguration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.contribution.StoredStableResourceContributionConfiguration";
		
		public static final String shortName = "Stored RDF dataset Contribution";
		public static final String homepage$description = keyBase + ".homepage.description";
		public static final String homepage$displayName = keyBase + ".homepage.displayName";
		public static final String description$description = keyBase + ".description.description";
		public static final String description$displayName = keyBase + ".description.displayName";
		public static final String isOwner$description = keyBase + ".isOwner.description";
		public static final String isOwner$displayName = keyBase + ".isOwner.displayName";
		public static final String model$description = keyBase + ".model.description";
		public static final String model$displayName = keyBase + ".model.displayName";
		public static final String lexicalizationModel$description = keyBase + ".lexicalizationModel.description";
		public static final String lexicalizationModel$displayName = keyBase + ".lexicalizationModel.displayName";
	}

	public String getContributionTypeLabel() {
		return "Stable";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.homepage$description + "}", displayName = "{" + MessageKeys.homepage$displayName + "}")
	public String homepage;

	@STProperty(description = "{" + MessageKeys.description$description + "}", displayName = "{" + MessageKeys.description$displayName + "}")
	@Required
	public String description;

	@STProperty(description = "{" + MessageKeys.isOwner$description + "}", displayName = "{" + MessageKeys.isOwner$displayName + "}")
	public boolean isOwner;

	@STProperty(description = "{" + MessageKeys.model$description + "}", displayName = "{" + MessageKeys.model$displayName + "}")
	@Required
	public IRI model;

	@STProperty(description = "{" + MessageKeys.lexicalizationModel$description + "}", displayName = "{" + MessageKeys.lexicalizationModel$displayName + "}")
	@Required
	public IRI lexicalizationModel;

}
