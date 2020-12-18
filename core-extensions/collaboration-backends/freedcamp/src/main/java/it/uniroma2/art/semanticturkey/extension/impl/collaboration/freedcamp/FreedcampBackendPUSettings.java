package it.uniroma2.art.semanticturkey.extension.impl.collaboration.freedcamp;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class FreedcampBackendPUSettings implements Settings {

	@Override
	public String getShortName() {
		return "JIRA User Information";
	}
	
	@Override
	public String getHTMLWarning() {
		return "The credentials are stored without encryption on the server. " + 
				"Be aware that the system administration could be able to see them. "+
				"<br/>To generate the API Key, please refer to this "+
				"<a target='_blank' href='https://freedcamp.com//Mobile_7Yh//iOS_application_6zp//wiki//wiki_public//view//DFaab'>page</a>. " +
				"<br/>If only the API Key is provided, it will be assumed that is a Not Secured Key. To use a Secured Key, please provide the "+
				"API Secret as well.";
	}

	@STProperty(description = "API Key", displayName = "API Key")
	@Required
	public String apiKey;

	@STProperty(description = "API Secret", displayName = "API Secret")
	public String apiSecret;

}
