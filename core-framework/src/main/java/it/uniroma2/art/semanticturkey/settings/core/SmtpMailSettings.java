package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.email.EmailSender;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class SmtpMailSettings implements STProperties {
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.SmtpMailSettings";

		public static final String shortName = keyBase + ".shortName";
		public static final String host$description = keyBase + ".host.description";
		public static final String host$displayName = keyBase + ".host.displayName";
		public static final String port$description = keyBase + ".port.description";
		public static final String port$displayName = keyBase + ".port.displayName";
		public static final String auth$description = keyBase + ".auth.description";
		public static final String auth$displayName = keyBase + ".auth.displayName";
		public static final String sslEnabled$description = keyBase + ".sslEnabled.description";
		public static final String sslEnabled$displayName = keyBase + ".sslEnabled.displayName";
		public static final String starttlsEnabled$description = keyBase + ".starttlsEnabled.description";
		public static final String starttlsEnabled$displayName = keyBase + ".starttlsEnabled.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.host$description + "}", displayName = "{"
			+ MessageKeys.host$displayName + "}")
	public String host;

	@STProperty(description = "{" + MessageKeys.port$description + "}", displayName = "{"
			+ MessageKeys.port$displayName + "}")
	public Integer port;

	@STProperty(description = "{" + MessageKeys.auth$description + "}", displayName = "{"
			+ MessageKeys.auth$displayName + "}")
	public boolean auth = false;

	@STProperty(description = "{" + MessageKeys.sslEnabled$description + "}", displayName = "{"
			+ MessageKeys.sslEnabled$displayName + "}")
	public Boolean sslEnabled;

	@STProperty(description = "{" + MessageKeys.starttlsEnabled$description + "}", displayName = "{"
			+ MessageKeys.starttlsEnabled$displayName + "}")
	public Boolean starttlsEnabled;


}
