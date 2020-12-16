package it.uniroma2.art.semanticturkey.settings.notification;

import java.util.TimeZone;

import com.cronutils.model.CronType;
import com.cronutils.validation.Cron;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class NotificationSystemSettings implements Settings {

	public static class CronDefinition implements STProperties {

		public static class MessageKeys {
			public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.notification.NotificationSystemSettings.CronDefinition";
			
			public static final String shortName = keyBase + ".shortName";
			public static final String expression$description = keyBase + ".expression.description";
			public static final String expression$displayName = keyBase + ".expression.displayName";
			public static final String zone$description = keyBase + ".zone.description";
			public static final String zone$displayName = keyBase + ".zone.displayName";
		}
		
		@Override
		public String getShortName() {
			return "{" + MessageKeys.shortName + "}";
		}

		@STProperty(description = "{" + MessageKeys.expression$description +"}", displayName = "{" + MessageKeys.expression$displayName +"}")
		@Required
		@Cron(type = CronType.SPRING)
		public String expression;

		@STProperty(description = "{" + MessageKeys.zone$description +"}", displayName = "{" + MessageKeys.zone$displayName +"}")
		public TimeZone zone;

	}
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.notification.NotificationSystemSettings";
		
		public static final String shortName = keyBase + ".shortName";
		public static final String notificationDigestSchedule$description = keyBase + ".notificationDigestSchedule.description";
		public static final String notificationDigestSchedule$displayName = keyBase + ".notificationDigestSchedule.displayName";
	}


	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.notificationDigestSchedule$description +"}", displayName = "{" + MessageKeys.notificationDigestSchedule$displayName +"}")
	public CronDefinition notificationDigestSchedule;
}
