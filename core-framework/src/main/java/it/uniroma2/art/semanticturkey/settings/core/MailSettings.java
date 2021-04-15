package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class MailSettings implements STProperties {
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.MailSettings";

		public static final String shortName = keyBase + ".shortName";

		public static final String admin$description = keyBase + ".admin.description";
		public static final String admin$displayName = keyBase + ".admin.displayName";
		public static final String smtp$description = keyBase + ".smtp.description";
		public static final String smtp$displayName = keyBase + ".smtp.displayName";
		public static final String from$description = keyBase + ".from.description";
		public static final String from$displayName = keyBase + ".from.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}
	
	@STProperty(description = "{" + MessageKeys.admin$description + "}", displayName = "{"
			+ MessageKeys.admin$displayName + "}")
	public AdminMailSettings admin;

	@STProperty(description = "{" + MessageKeys.smtp$description + "}", displayName = "{"
			+ MessageKeys.smtp$displayName + "}")
	public SmtpMailSettings smtp;

	@STProperty(description = "{" + MessageKeys.from$description + "}", displayName = "{"
			+ MessageKeys.from$displayName + "}")
	public FromMailSettings from;
}
