package it.uniroma2.art.semanticturkey.services.core;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.notification.ResourceChangeNotificationManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.settings.notification.NotificationSystemSettings;

/**
 * This class provides access to the capabilities of <a href="http://art.uniroma2.it/maple/">MAPLE</a>
 * (Mapping Architecture based on Linguistic Evidences).
 * 
 */
@STService
public class Notifications extends STServiceAdapter {

	@Autowired
	private ResourceChangeNotificationManager notificationManager;

	@STServiceOperation(method = RequestMethod.POST)
	public void scheduleNotificationDigest(
			@Optional @JsonSerialized @Valid NotificationSystemSettings.CronDefinition schedule)
			throws STPropertyUpdateException {
		if (schedule == null)  {
			notificationManager.disableNotificationDigest();
		} else {
			notificationManager.setNotificationDigestSchedule(schedule);
		}
	}
}
