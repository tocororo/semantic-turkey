package it.uniroma2.art.semanticturkey.plugin.impls.collaboration;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesImpl;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class JiraBackendPreferences extends STPropertiesImpl implements STProperties {

	@Override
	public String getShortName() {
		return "Jira Backened Preferences";
	}

	@STProperty(description = "Username")
	@Required
	public String username;

	@STProperty(description = "Password")
	@Required
	public String password;

}
