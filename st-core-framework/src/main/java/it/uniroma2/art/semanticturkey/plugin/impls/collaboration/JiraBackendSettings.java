package it.uniroma2.art.semanticturkey.plugin.impls.collaboration;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesImpl;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class JiraBackendSettings extends STPropertiesImpl implements STProperties {

	@Override
	public String getShortName() {
		return "Jira Settings";
	}

	@STProperty(description = "Server URL")
	@Required
	public String serverURL;
	
	@STProperty(description = "Jira Project Name")
	public String jiraPrjName;
	
	@STProperty(description = "Jira Project Key")
	public String jiraPrjKey;
	
	@STProperty(description = "Jira Project Id")
	public String jiraPrjId;
	
}
