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

}
