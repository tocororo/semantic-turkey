package it.uniroma2.art.semanticturkey.config.alignmentservices;

import java.net.URL;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class RemoteAlignmentServiceConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.alignmentservices.RemoteAlignmentServiceConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String htmlWarning = keyBase + ".htmlWarning";
		public static final String serverURL$description = keyBase + ".serverURL.description";
		public static final String serverURL$displayName = keyBase + ".serverURL.displayName";
		public static final String username$description = keyBase + ".username.description";
		public static final String username$displayName = keyBase + ".username.displayName";
		public static final String password$description = keyBase + ".password.description";
		public static final String password$displayName = keyBase + ".password.displayName";
		public static final String forwardCredentials$description = keyBase + ".forwardCredentials.description";
		public static final String forwardCredentials$displayName = keyBase + ".forwardCredentials.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@Override
	public String getHTMLWarning() {
		return "{" + MessageKeys.htmlWarning + "}";
	}
	
	@STProperty(description = "{" + MessageKeys.serverURL$description + "}", displayName = "{" + MessageKeys.serverURL$displayName+ "}")
	@Required
	public URL serverURL;

	@STProperty(description = "{" + MessageKeys.username$description + "}", displayName = "{" + MessageKeys.username$displayName+ "}")
	public String username;

	@STProperty(description = "{" + MessageKeys.password$description + "}", displayName = "{" + MessageKeys.password$displayName+ "}")
	public String password;
	
	@STProperty(description = "{" + MessageKeys.forwardCredentials$description + "}", displayName = "{" + MessageKeys.forwardCredentials$displayName+ "}")
	public boolean forwardCredentials;
}
