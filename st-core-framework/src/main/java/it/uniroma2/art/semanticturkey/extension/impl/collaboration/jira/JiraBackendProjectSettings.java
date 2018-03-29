package it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class JiraBackendProjectSettings implements Settings {

	@Override
	public String getShortName() {
		return "Jira Project Settings";
	}

	@STProperty(description = "Server URL", displayName = "Server URL")
	@Required
	public String serverURL;
	
	@STProperty(description = "Jira Project Name", displayName = "Jira Project Name")
	public String jiraPrjName;
	
	@STProperty(description = "Jira Project Key", displayName = "Jira Project Key")
	public String jiraPrjKey;
	
	@STProperty(description = "Jira Project Id", displayName = "Jira Project Id")
	public String jiraPrjId;
	
}
