package it.uniroma2.art.semanticturkey.extension.impl.collaboration.freedcamp;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class FreedcampIssueCreationForm implements STProperties {


	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.collaboration.freedcamp.FreedcampIssueCreationForm";

		public static final String shortName = keyBase + ".shortName";
		public static final String title$description = keyBase + ".title.description";
		public static final String title$displayName = keyBase + ".title.displayName";
	}
	
	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.title$description + "}", displayName = "{"
			+ MessageKeys.title$displayName + "}")
	@Required
	public String title;

	
}
