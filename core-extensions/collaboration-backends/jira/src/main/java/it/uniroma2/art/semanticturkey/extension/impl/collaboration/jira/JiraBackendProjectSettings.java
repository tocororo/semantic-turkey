package it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class JiraBackendProjectSettings implements Settings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira.JiraBackendProjectSettings";

		public static final String shortName = keyBase + ".shortName";
		public static final String htmlDescription = keyBase + ".htmlDescription";
		public static final String serverURL$description = keyBase + ".serverURL.description";
		public static final String serverURL$displayName = keyBase + ".serverURL.displayName";
		public static final String jiraPrjKey$description = keyBase + ".jiraPrjKey.description";
		public static final String jiraPrjKey$displayName = keyBase + ".jiraPrjKey.displayName";
		public static final String jiraPrjId$description = keyBase + ".jiraPrjId.description";
		public static final String jiraPrjId$displayName = keyBase + ".jiraPrjId.displayName";


	}
	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	public String getHTMLDescription() {
		return "{" + MessageKeys.htmlDescription + "}";
	}
	
	@STProperty(description = "{" + MessageKeys.serverURL$description + "}", displayName = MessageKeys.serverURL$displayName)
	@Required
	public String serverURL;
	
	//@STProperty(description = "Jira Project Name", displayName = "Jira Project Name")
	//public String jiraPrjName;
	
	@STProperty(description = "{" + MessageKeys.jiraPrjKey$description + "}", displayName = MessageKeys.jiraPrjKey$displayName)
	public String jiraPrjKey;
	
	@STProperty(description = "{" + MessageKeys.jiraPrjId$description + "}", displayName = MessageKeys.jiraPrjId$displayName)
	public String jiraPrjId;
	
}
