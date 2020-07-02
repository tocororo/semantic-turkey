package it.uniroma2.art.semanticturkey.notification;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.email.EmailSender;
import it.uniroma2.art.semanticturkey.event.annotation.TransactionalEventListener;
import it.uniroma2.art.semanticturkey.event.annotation.TransactionalEventListener.Phase;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.services.events.ResourceCreated;
import it.uniroma2.art.semanticturkey.services.events.ResourceDeleted;
import it.uniroma2.art.semanticturkey.services.events.ResourceEvent;
import it.uniroma2.art.semanticturkey.services.events.ResourceModified;
import it.uniroma2.art.semanticturkey.settings.notification.NotificationSystemSettings;
import it.uniroma2.art.semanticturkey.settings.notification.NotificationSystemSettings.CronDefinition;
import it.uniroma2.art.semanticturkey.settings.notification.NotificationSystemSettingsManager;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.user.notification.NotificationPreferencesAPI;
import it.uniroma2.art.semanticturkey.user.notification.NotificationPreferencesAPI.Action;
import it.uniroma2.art.semanticturkey.user.notification.NotificationPreferencesMode;
import it.uniroma2.art.semanticturkey.user.notification.UserNotificationsAPI;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.support.CronTrigger;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ResourceChangeNotificationManager {

	private static final Logger logger = LoggerFactory.getLogger(ResourceChangeNotificationManager.class);

	@Autowired
	private NotificationSystemSettingsManager systemSettingsManager;

	@Autowired
	private TaskScheduler taskScheduler;
	private ScheduledFuture<?> currentFuture = null;

	private AtomicBoolean notificationDigestSending = new AtomicBoolean(false);

	private static final String NOTIFICATION_STATUS_PREF = "notifications_status";

	private class NotificationDigestRunnable implements Runnable {

		@Override
		public void run() {
			boolean updated = notificationDigestSending.compareAndSet(false, true);
			if (!updated)
				return; // another task is running, skip this one

			try {
				ResourceChangeNotificationManager.this.scheduledNotifications();
			} finally {
				notificationDigestSending.set(false);
			}
		}

	}

	@PostConstruct
	public void init() throws STPropertyAccessException {
		scheduleNotificationDigest();
	}

	@TransactionalEventListener(phase = Phase.afterCommit)
	@Async
	public void onCreation(ResourceCreated event) throws IOException, STPropertyAccessException {
		logger.debug("Send notifications about creation of " + event.getResource());
		triggerEventNotification(event);
	}

	@TransactionalEventListener(phase = Phase.afterCommit)
	@Async
	public void onUpdate(ResourceModified event) throws IOException, STPropertyAccessException {
		logger.debug("Send notifications about modification of " + event.getResource());
		triggerEventNotification(event);
	}

	@TransactionalEventListener(phase = Phase.afterCommit)
	@Async
	public void onDeletion(ResourceDeleted event) throws IOException, STPropertyAccessException {
		logger.debug("Send notifications about deletion of " + event.getResource());
		triggerEventNotification(event);
	}

	public void scheduledNotifications() {
		System.out.println("Scheduled notifications");
	}

	public synchronized void setNotificationDigestSchedule(NotificationSystemSettings.CronDefinition schedule)
			throws STPropertyUpdateException {
		NotificationSystemSettings settings = new NotificationSystemSettings();
		settings.notificationDigestSchedule = new NotificationSystemSettings.CronDefinition();
		settings.notificationDigestSchedule.expression = schedule.expression;
		settings.notificationDigestSchedule.zone = schedule.zone;

		systemSettingsManager.storeSystemSettings(settings);
		
		scheduleNotificationDigest(settings);
	}

	public synchronized void disableNotificationDigest() throws STPropertyUpdateException {
		NotificationSystemSettings settings = new NotificationSystemSettings();
		settings.notificationDigestSchedule = null;

		systemSettingsManager.storeSystemSettings(settings);
		
		scheduleNotificationDigest(settings);
	}

	protected synchronized void scheduleNotificationDigest() throws STPropertyAccessException {
		NotificationSystemSettings settings = systemSettingsManager.getSystemSettings();
		scheduleNotificationDigest(settings);
	}

	protected synchronized void scheduleNotificationDigest(NotificationSystemSettings settings) {
		if (currentFuture != null) {
			currentFuture.cancel(true);
			currentFuture = null;
		}

		CronDefinition cron = settings.notificationDigestSchedule;

		if (cron != null) {
			currentFuture = taskScheduler.schedule(new NotificationDigestRunnable(), buildTrigger(cron));
		}
	}

	protected CronTrigger buildTrigger(CronDefinition cron) {
		if (cron.zone != null) {
			return new CronTrigger(cron.expression, cron.zone);
		} else {
			return new CronTrigger(cron.expression);
		}
	}

	/**
	 * Execute an action (send email or store the notification) according the preference
	 * set by the users interested to the event.
	 *
	 * @param event
	 * @throws IOException
	 * @throws STPropertyAccessException
	 */
	private void triggerEventNotification(ResourceEvent event) throws IOException, STPropertyAccessException {
		Project project = event.getProject();
		Resource resource = event.getResource();
		RDFResourceRole role = event.getRole();
		Action action = (event instanceof ResourceCreated) ? Action.creation : (
				(event instanceof  ResourceModified) ? Action.update : Action.deletion);

		UserNotificationsAPI notificationAPI = UserNotificationsAPI.getInstance();
		//retrieves user interested to the event
		Set<STUser> users = listInterestedUsers(event, action);
		for (STUser user: users) {
			/* according the user notification preference:
			- store the notification (for in-app consulting or for daily email digest)
			- send an email immediately
			 */
			String notificationPrefValue = STPropertiesManager.getPUSetting(NOTIFICATION_STATUS_PREF, project, user);
			if (notificationPrefValue != null && EnumUtils.isValidEnum(NotificationPreferencesMode.class, notificationPrefValue)) {
				NotificationPreferencesMode mode = NotificationPreferencesMode.valueOf(notificationPrefValue);
				if (mode == NotificationPreferencesMode.in_app_only || mode == NotificationPreferencesMode.email_daily_digest) {
					notificationAPI.storeNotification(user, project, resource, role, action); //Store
				} else if (mode == NotificationPreferencesMode.email_instant) { //Send email and don't store
					try {
						String content = "Event notification:" +
								"<ul>" +
								"<li><b>Project:</b> <i>" + project.getName() + "</i></li>" +
								"<li><b>Resource:</b> <i>" + StringEscapeUtils.escapeHtml4(NTriplesUtil.toNTriplesString(resource)) + "</i></li>" +
								"<li><b>Role:</b> <i>" + role + "</i></li>" +
								"<li><b>Action:</b> <i>" + action + "</i></li>" +
								"</ul>" +
								"<br><br>" +
								"<i style=\"font-size: 11px;\">" +
								"This email has been automatically generated since you requested to be notified by VocBench " +
								"following certain events. Please do not reply." +
								"</i>";
						EmailSender.sendMail(user.getEmail(), "VocBench notification", content);
					} catch (MessagingException e) {
						//catch exception preventing error response in case email is not configured correctly
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Returns a Set of {@link STUser} interested to an event
	 * @param event
	 * @param action
	 * @returnel
	 * @throws IOException
	 */
	private Set<STUser> listInterestedUsers(ResourceEvent event, Action action) throws IOException {
		Project project = event.getProject();
		Resource resource = event.getResource();
		RDFResourceRole role = event.getRole();

		NotificationPreferencesAPI notificationPrefsApi = NotificationPreferencesAPI.getInstance();
		/* here I use a Set in order to prevent duplicated users in case an event covers multiple scenarios
		that a user is watching. E.g. User is watching a concept C and enabled notification for
		event=update on role=concept. If concept C is updated, both the above situations would be notified */
		Set<String> userIRIs = new HashSet<>();
		//users watching resource
		userIRIs.addAll(notificationPrefsApi.searchUserFromProjRes(project, resource));
		//users listening for role-action pair
		userIRIs.addAll(notificationPrefsApi.searchUserFromProjRoleAction(project, role, action));

		Set<STUser> users = new HashSet<>();
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		for (String iri: userIRIs) {
			try {
				//prevent to notify the author itself
//				if (!iri.equals(event.getAuthor().getIRI().stringValue())) {
					users.add(UsersManager.getUser(vf.createIRI(iri)));
//				}
			} catch (UserException e) {
				//do nothing, simply ignore user not found
			}
		}
		return users;
	}
}
