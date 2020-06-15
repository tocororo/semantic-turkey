package it.uniroma2.art.semanticturkey.settings.notification;

import it.uniroma2.art.semanticturkey.extension.settings.SystemSettingsManager;

/**
 * A storage for notification-related system settings.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class NotificationSystemSettingsManager implements SystemSettingsManager<NotificationSystemSettings> {

	@Override
	public String getId() {
		return NotificationSystemSettingsManager.class.getName();
	}

}
