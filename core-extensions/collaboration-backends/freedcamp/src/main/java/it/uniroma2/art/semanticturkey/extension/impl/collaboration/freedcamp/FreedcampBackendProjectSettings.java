package it.uniroma2.art.semanticturkey.extension.impl.collaboration.freedcamp;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class FreedcampBackendProjectSettings implements Settings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.collaboration.freedcamp.FreedcampBackendProjectSettings";

		public static final String shortName = keyBase + ".shortName";
		public static final String htmlDescription = keyBase + ".htmlDescription";
		public static final String serverURL$description = keyBase + ".serverURL.description";
		public static final String serverURL$displayName = keyBase + ".serverURL.displayName";
		public static final String freedcampPrjId$description = keyBase + ".freedcampPrjId.description";
		public static final String freedcampPrjId$displayName = keyBase + ".freedcampPrjId.displayName";
		public static final String freedcampTaskListId$description = keyBase + ".freedcampTaskListId.description";
		public static final String freedcampTaskListId$displayName = keyBase + ".freedcampTaskListId.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	public String getHTMLDescription() {
		return "{" + MessageKeys.htmlDescription + "}";
	}
	
	@STProperty(description = "{" + MessageKeys.serverURL$description + "}", displayName = "{"+ MessageKeys.serverURL$displayName + "}")
	@Required
	public String serverURL;
	
	//@STProperty(description = "Jira Project Name", displayName = "Jira Project Name")
	//public String jiraPrjName;

	@STProperty(description = "{" + MessageKeys.freedcampPrjId$description + "}", displayName = "{" + MessageKeys.freedcampPrjId$displayName + "}")
	public String freedcampPrjId;

	@STProperty(description = "{" + MessageKeys.freedcampTaskListId$description + "}", displayName = "{" + MessageKeys.freedcampTaskListId$displayName + "}")
	public String freedcampTaskListId;
}
