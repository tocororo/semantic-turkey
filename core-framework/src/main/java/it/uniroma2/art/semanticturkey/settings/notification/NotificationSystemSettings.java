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

		@Override
		public String getShortName() {
			return "Cron";
		}

		@STProperty(description = "A cron expression", displayName = "Expression")
		@Required
		@Cron(type=CronType.SPRING)
		public String expression;

		@STProperty(description = "A timezone id suitable for java.util.TimeZone.getTimeZone(String)", displayName = "Time zone")
		public TimeZone zone;

	}

	@Override
	public String getShortName() {
		return "Notification System Settings";
	}

	@STProperty(description = "A cron expression scheduling periodic notification digests", displayName = "Notification digest schedule")
	public CronDefinition notificationDigestSchedule;
}
