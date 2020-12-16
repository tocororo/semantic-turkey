package it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class JiraIssueCreationForm implements STProperties {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira.JiraIssueCreationForm";

		public static final String shortName = keyBase + ".shortName";
		public static final String summary$description = keyBase + ".summary.description";
		public static final String summary$displayName = keyBase + ".summary.displayName";
		public static final String description$description = keyBase + ".description.description";
		public static final String description$displayName = keyBase + ".description.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.summary$description + "}", displayName = "{"
			+ MessageKeys.summary$displayName + "}")
	@Required
	public String summary;

	@STProperty(description = "{" + MessageKeys.description$description + "}", displayName = "{"
			+ MessageKeys.description$displayName + "}")
	public String description;

}
