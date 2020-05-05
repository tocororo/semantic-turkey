package it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class JiraBackendPUSettings implements Settings {

	@Override
	public String getShortName() {
		return "JIRA User Information";
	}
	
	@Override
	public String getHTMLWarning() {
		return "The credentials are stored without encryption on the server. " + 
				"Be aware that the system administration could be able to see them. "+
				"To generate the token, please refer to this "+
				"<a target='_blank' href='https://id.atlassian.com/manage/api-tokens'>page</a>";
	}

	@STProperty(description = "Username", displayName = "Username")
	@Required
	public String username;

	@STProperty(description = "Token", displayName = "Token")
	@Required
	public String password;

}
