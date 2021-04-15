package it.uniroma2.art.semanticturkey.settings.core;

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
		public static final String ssl$description = keyBase + ".ssl.description";
		public static final String ssl$displayName = keyBase + ".ssl.displayName";
		public static final String starttls$description = keyBase + ".starttls.description";
		public static final String starttls$displayName = keyBase + ".starttls.displayName";
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

	@STProperty(description = "{" + MessageKeys.ssl$description + "}", displayName = "{"
			+ MessageKeys.ssl$displayName + "}")
	public SSLSmtpMailSettings ssl;

	@STProperty(description = "{" + MessageKeys.starttls$description + "}", displayName = "{"
			+ MessageKeys.starttls$displayName + "}")
	public STARTTLSSmtpMailSettings starttls;

}
