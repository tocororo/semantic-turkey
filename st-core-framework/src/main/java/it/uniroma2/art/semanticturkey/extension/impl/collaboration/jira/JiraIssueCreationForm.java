package it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class JiraIssueCreationForm implements STProperties {


	
	@Override
	public String getShortName() {
		return "Jira Settings for the creation of an Issue";
	}
	
	
	@STProperty(description = "Summary for the new Issue", displayName = "Summary")
	@Required
	public String summary;
	
	@STProperty(description = "Description of the new Issue", displayName = "Description")
	public String description;
	
}
