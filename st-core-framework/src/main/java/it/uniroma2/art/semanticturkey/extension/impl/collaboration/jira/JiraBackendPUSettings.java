package it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class JiraBackendPUSettings implements Settings {

	@Override
	public String getShortName() {
		return "Jira Backened Project User Settings";
	}
	
	@Override
	public String getHTMLWarning() {
		return "Warning: the credentials are stored wihtout encryption on the server. " + 
				"Be aware that the system administration could be able to see them.";
	}

	@STProperty(description = "Username", displayName = "Username")
	@Required
	public String username;

	@STProperty(description = "Password", displayName = "Password")
	@Required
	public String password;

}
