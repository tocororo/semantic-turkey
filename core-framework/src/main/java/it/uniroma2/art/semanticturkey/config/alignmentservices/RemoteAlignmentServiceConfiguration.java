package it.uniroma2.art.semanticturkey.config.alignmentservices;

import java.net.URL;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class RemoteAlignmentServiceConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "Remote alignment service";
	}

	@STProperty(description = "URL of the alignment service", displayName = "Server URL")
	@Required
	public URL serverURL;

	@STProperty(description = "Username used for accessing the alignment service", displayName = "Username")
	public String username;

	@STProperty(description = "Password used for accessing the alignment service", displayName = "Password")
	public String password;
}
