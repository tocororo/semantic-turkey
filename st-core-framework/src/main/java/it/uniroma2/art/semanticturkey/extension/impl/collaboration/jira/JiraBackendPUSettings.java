package it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class JiraBackendPUSettings implements Settings {

	@Override
	public String getShortName() {
		return "Jira Backened Project User Settings";
	}

	@STProperty(description = "Username")
	@Required
	public String username;

	@STProperty(description = "Password")
	@Required
	public String password;

}
