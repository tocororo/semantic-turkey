package it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class JiraBackendPUSettings implements Settings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira.JiraBackendPUSettings";

		public static final String shortName = keyBase + ".shortName";
		public static final String htmlWarning = keyBase + ".htmlWarning";
		public static final String username$description = keyBase + ".username.description";
		public static final String username$displayName = keyBase + ".username.displayName";
		public static final String password$description = keyBase + ".password.description";
		public static final String password$displayName = keyBase + ".password.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}
	
	@Override
	public String getHTMLWarning() {
		return "{" + MessageKeys.htmlWarning + "}";
	}

	@STProperty(description = "{" + MessageKeys.username$description + "}", displayName = "{" + MessageKeys.username$displayName + "}")
	@Required
	public String username;

	@STProperty(description = "{" + MessageKeys.password$description + "}", displayName = "{" + MessageKeys.password$displayName + "}")
	@Required
	public String password;

}
