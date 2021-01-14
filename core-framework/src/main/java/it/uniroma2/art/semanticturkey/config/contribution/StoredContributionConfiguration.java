package it.uniroma2.art.semanticturkey.config.contribution;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

public abstract class StoredContributionConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.contribution.StoredContributionConfiguration";

		public static final String contributorName$description = keyBase + ".contributorName.description";
		public static final String contributorName$displayName = keyBase + ".contributorName.displayName";
		public static final String contributorLastName$description = keyBase + ".contributorLastName.description";
		public static final String contributorLastName$displayName = keyBase + ".contributorLastName.displayName";
		public static final String contributorEmail$description = keyBase + ".contributorEmail.description";
		public static final String contributorEmail$displayName = keyBase + ".contributorEmail.displayName";
		public static final String contributorOrganization$description = keyBase + ".contributorOrganization.description";
		public static final String contributorOrganization$displayName = keyBase + ".contributorOrganization.displayName";
		public static final String resourceName$description = keyBase + ".resourceName.description";
		public static final String resourceName$displayName = keyBase + ".resourceName.displayName";
		public static final String baseURI$description = keyBase + ".baseURI.description";
		public static final String baseURI$displayName = keyBase + ".baseURI.displayName";
	}

	@STProperty(description = "{" + MessageKeys.contributorName$description + "}", displayName = "{" + MessageKeys.contributorName$displayName+ "}")
	@Required
	public String contributorName;

	@STProperty(description = "{" + MessageKeys.contributorLastName$description + "}", displayName = "{" + MessageKeys.contributorLastName$displayName+ "}")
	@Required
	public String contributorLastName;

	@STProperty(description = "{" + MessageKeys.contributorEmail$description + "}", displayName = "{" + MessageKeys.contributorEmail$displayName+ "}")
	@Required
	public String contributorEmail;

	@STProperty(description = "{" + MessageKeys.contributorOrganization$description + "}", displayName = "{" + MessageKeys.contributorOrganization$displayName+ "}")
	public String contributorOrganization;

	@STProperty(description = "{" + MessageKeys.resourceName$description + "}", displayName = "{" + MessageKeys.resourceName$displayName+ "}")
	@Required
	public String resourceName;

	@STProperty(description = "{" + MessageKeys.baseURI$description + "}", displayName = "{" + MessageKeys.baseURI$displayName+ "}")
	@Required
	public IRI baseURI;

	public abstract String getContributionTypeLabel();

}
