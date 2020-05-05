package it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class JiraBackendProjectSettings implements Settings {

	@Override
	public String getShortName() {
		return "Jira Project Settings";
	}

	public String getHTMLDescription() {
		String message = "The manual configuration for JIRA requires that all of the following fields "
				+ "(including the non-mandatory ones) are filled up. Alternatively, insert only the mandatory "
				+ "field and, once the connection credentials have been provided, it will be possible to "
				+ "browse the list of JIRA projects and select one of them.";
		return message;
	}
	
	@STProperty(description = "Server URL", displayName = "Server URL")
	@Required
	public String serverURL;
	
	//@STProperty(description = "Jira Project Name", displayName = "Jira Project Name")
	//public String jiraPrjName;
	
	@STProperty(description = "Jira Project Key", displayName = "Jira Project Key")
	public String jiraPrjKey;
	
	@STProperty(description = "Jira Project Id", displayName = "Jira Project Id")
	public String jiraPrjId;
	
}
