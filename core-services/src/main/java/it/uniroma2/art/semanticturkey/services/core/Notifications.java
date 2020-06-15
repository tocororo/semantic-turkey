package it.uniroma2.art.semanticturkey.services.core;

import java.time.ZoneId;
import java.util.Collection;

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

	/**
	 * Schedules the sending of notifications digests. The provided <code>schedule</code> combines a cron
	 * expression (compliant with Spring flavor) and and optional time zone id. If schedule is omitted,
	 * notifications digests are disabled.
	 * 
	 * @see <a href=
	 *      "https://docs.spring.io/spring/docs/3.2.14.RELEASE/javadoc-api/org/springframework/scheduling/support/CronSequenceGenerator.html">org.springframework.scheduling.support.CronSequenceGenerator</a>
	 * @param schedule
	 * @throws STPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void scheduleNotificationDigest(
			@Optional @JsonSerialized @Valid NotificationSystemSettings.CronDefinition schedule)
			throws STPropertyUpdateException {
		if (schedule == null) {
			notificationManager.disableNotificationDigest();
		} else {
			notificationManager.setNotificationDigestSchedule(schedule);
		}
	}

	/**
	 * Returns the available time zone identifiers.
	 * 
	 * @return
	 */
	@STServiceOperation
	public Collection<String> getAvailableTimeZoneIds() {
		return ZoneId.getAvailableZoneIds();
	}
}
