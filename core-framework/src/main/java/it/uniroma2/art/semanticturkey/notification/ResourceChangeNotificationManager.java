package it.uniroma2.art.semanticturkey.notification;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.email.EmailSender;
import it.uniroma2.art.semanticturkey.event.annotation.TransactionalEventListener;
import it.uniroma2.art.semanticturkey.event.annotation.TransactionalEventListener.Phase;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
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
import it.uniroma2.art.semanticturkey.user.notification.NotificationMode;
import it.uniroma2.art.semanticturkey.user.notification.NotificationPreferencesAPI;
import it.uniroma2.art.semanticturkey.user.notification.NotificationPreferencesAPI.Action;
import it.uniroma2.art.semanticturkey.user.notification.NotificationRecord;
import it.uniroma2.art.semanticturkey.user.notification.UserNotificationsAPI;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.support.CronTrigger;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
			} catch (STPropertyAccessException | ProjectAccessException | IOException | MessagingException | InterruptedException e) {
				e.printStackTrace();
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
	public void onCreation(ResourceCreated event) throws IOException, STPropertyAccessException, InterruptedException {
		logger.debug("Send notifications about creation of " + event.getResource());
		triggerEventNotification(event);
	}

	@TransactionalEventListener(phase = Phase.afterCommit)
	@Async
	public void onUpdate(ResourceModified event) throws IOException, STPropertyAccessException, InterruptedException {
		logger.debug("Send notifications about modification of " + event.getResource());
		triggerEventNotification(event);
	}

	@TransactionalEventListener(phase = Phase.afterCommit)
	@Async
	public void onDeletion(ResourceDeleted event) throws IOException, STPropertyAccessException, InterruptedException {
		logger.debug("Send notifications about deletion of " + event.getResource());
		triggerEventNotification(event);
	}

	public void scheduledNotifications() throws STPropertyAccessException, ProjectAccessException, IOException, MessagingException, InterruptedException {
		//for each user, collects the list of project for which he has set daily digest as notification mode preference
		Map<STUser, List<Project>> userProjMap = new HashMap<>();
		for (AbstractProject absProj : ProjectManager.listProjects()) {
			if (absProj instanceof Project) {
				Project proj = (Project) absProj;
				for (STUser user : UsersManager.listUsers()) {
					if (getUserNotificationMode(proj, user) == NotificationMode.email_daily_digest) {
						userProjMap.computeIfAbsent(user, u -> new ArrayList<>()).add(proj);
					}
				}
			}
		}
		dailyDigest(userProjMap);
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
	 * Send a mail notifications report for each user
	 * @param userProjMap
	 */
	private void dailyDigest(Map<STUser, List<Project>> userProjMap) throws IOException, STPropertyAccessException, MessagingException, InterruptedException {
		UserNotificationsAPI notificationApi = UserNotificationsAPI.getInstance();
		SimpleDateFormat timestampInputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		SimpleDateFormat timestampOutputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

		for (STUser user : userProjMap.keySet()) {
			String content = "";
			//write mail content for each notification per project
			for (Project project: userProjMap.get(user)) {
				content += "<b>Project:</b> <i>" + project.getName() + "</i><br>";
				List<NotificationRecord> notifications = notificationApi.retrieveNotifications(user, project);
				if (notifications.isEmpty()) {
					content += "<div style=\"margin: 1em\"><i>No notifications</i></div>";
				} else {
					for (NotificationRecord notification : notifications) {
						//try to re-format the timestamp
						String formattedTimestamp = notification.getTimestamp(); //by default, formatted as retrieved
						try {
							Date d = timestampInputFormat.parse(notification.getTimestamp());
							formattedTimestamp = timestampOutputFormat.format(d);
						} catch (ParseException e) {
						}
						//write content about single notification
						content += "<ul>" +
								"<li><b>Resource:</b> <i>" + StringEscapeUtils.escapeHtml4(notification.getResource()) + "</i></li>" +
								"<li><b>Role:</b> <i>" + notification.getRole() + "</i></li>" +
								"<li><b>Action:</b> <i>" + notification.getAction() + "</i></li>" +
								"<li><b>Time:</b> <i>" + formattedTimestamp + "</i></li>" +
								"</ul>";
					}
				}
				content += "<br>";
				//report compiled, notifications can be cleared
				notificationApi.clearNotifications(user, project);
			}
			content = appendDoNotReplyContent(content);
			EmailSender.sendMail(user.getEmail(), "VocBench daily digest notifications", content);
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
	private void triggerEventNotification(ResourceEvent event) throws IOException, STPropertyAccessException, InterruptedException {
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
			NotificationMode notificationMode = getUserNotificationMode(project, user);
			if (notificationMode == NotificationMode.in_app_only || notificationMode == NotificationMode.email_daily_digest) {
				notificationAPI.storeNotification(user, project, resource, role, action); //Store
			} else if (notificationMode == NotificationMode.email_instant) { //Send email and don't store
				try {
					String content = "Event notification:" +
							"<ul>" +
							"<li><b>Project:</b> <i>" + project.getName() + "</i></li>" +
							"<li><b>Resource:</b> <i>" + StringEscapeUtils.escapeHtml4(NTriplesUtil.toNTriplesString(resource)) + "</i></li>" +
							"<li><b>Role:</b> <i>" + role + "</i></li>" +
							"<li><b>Action:</b> <i>" + action + "</i></li>" +
							"</ul>";
					content = appendDoNotReplyContent(content);
					EmailSender.sendMail(user.getEmail(), "VocBench notification", content);
				} catch (MessagingException e) {
					//catch exception preventing error response in case email is not configured correctly
					e.printStackTrace();
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
				if (!iri.equals(event.getAuthor().getIRI().stringValue())) {
					users.add(UsersManager.getUser(vf.createIRI(iri)));
				}
			} catch (UserException e) {
				//do nothing, simply ignore user not found
			}
		}
		return users;
	}

	/**
	 * Returns the notification mode chosen by a user in a project
	 * @param project
	 * @param user
	 * @return
	 * @throws STPropertyAccessException
	 */
	private NotificationMode getUserNotificationMode(Project project, STUser user) throws STPropertyAccessException {
		NotificationMode mode = NotificationMode.no_notifications; //default
		String notificationPrefValue = STPropertiesManager.getPUSetting(NOTIFICATION_STATUS_PREF, project, user);
		if (notificationPrefValue != null && EnumUtils.isValidEnum(NotificationMode.class, notificationPrefValue)) {
			mode = NotificationMode.valueOf(notificationPrefValue);
		}
		return mode;
	}


	private String appendDoNotReplyContent(String emailContent) {
		emailContent += "<br><br>" +
			"<i style=\"font-size: 11px;\">" +
			"This email has been automatically generated since you requested to be notified by VocBench " +
			"following certain events. Please do not reply." +
			"</i>";
		return emailContent;
	}
}
