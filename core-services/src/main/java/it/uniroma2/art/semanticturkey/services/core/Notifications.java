package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.user.notification.NotificationPreferencesAPI;
import it.uniroma2.art.semanticturkey.user.notification.NotificationPreferencesAPI.Action;
import it.uniroma2.art.semanticturkey.user.notification.NotificationRecord;
import it.uniroma2.art.semanticturkey.user.notification.UserNotificationsAPI;
import org.eclipse.rdf4j.model.Resource;
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
import org.springframework.security.access.prepost.PreAuthorize;

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
	@PreAuthorize("@auth.isAdmin()")
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


	/* Notifications on single resource */

	/**
	 * Enables notifications on a resource
	 * @param resource
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void startWatching(Resource resource) throws IOException {
		NotificationPreferencesAPI.getInstance().addToUser(UsersManager.getLoggedUser(), getProject(), resource);
	}

	/**
	 * Disables notifications on a resource
	 * @param resource
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void stopWatching(Resource resource) throws IOException {
		NotificationPreferencesAPI.getInstance().removeProjResFromUser(UsersManager.getLoggedUser(), getProject(), resource);
	}

	/**
	 * Returns all the resources for which the current user has enabled notifications
	 * @return
	 * @throws IOException
	 */
	@STServiceOperation
	public List<Resource> listWatching() throws IOException {
		return NotificationPreferencesAPI.getInstance().listResourcesFromUserInProject(getProject(), UsersManager.getLoggedUser());
	}

	/**
	 * Returns true if the user has enabled notifications on the given resource, false otherwise
	 * @param resource
	 * @return
	 * @throws IOException
	 */
	@STServiceOperation
	public Boolean isWatching(Resource resource) throws IOException {
		List<Resource> watchedResources = NotificationPreferencesAPI.getInstance().listResourcesFromUserInProject(getProject(), UsersManager.getLoggedUser());
		return watchedResources.stream().anyMatch(r -> r.equals(resource));
	}

	/* Notifications on roles */

	@STServiceOperation
	public Map<RDFResourceRole, List<Action>> getNotificationPreferences() throws IOException {
		return NotificationPreferencesAPI.getInstance().getRoleActionsNotificationPreferences(getProject(), UsersManager.getLoggedUser());
	}

	/**
	 * Updates the notification status on the role-action pairs
	 * @param preferences for each role lists the actions for which notifications are enabled
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void storeNotificationPreferences(@JsonSerialized Map<RDFResourceRole, List<Action>> preferences) throws IOException {
		STUser user = UsersManager.getLoggedUser();
		Project project = getProject();
		NotificationPreferencesAPI.getInstance().addToUser(user, project, preferences);
	}

	/**
	 * Enables or disables the notification on a role-action pair
	 * @param role
	 * @param action
	 * @param status
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void updateNotificationPreferences(RDFResourceRole role, Action action, boolean status) throws IOException {
		if (!NotificationPreferencesAPI.availableRoles.contains(role)) return;

		STUser user = UsersManager.getLoggedUser();
		Project project = getProject();
		NotificationPreferencesAPI notificationPrefsApi = NotificationPreferencesAPI.getInstance();
		if (status) {
			notificationPrefsApi.addToUser(user, project, role, action);
		} else {
			notificationPrefsApi.removeProjRoleActionFromUser(user, project, role, action);
		}
	}


	/* Actual Notifications */

	@STServiceOperation
	public JsonNode listNotifications() throws IOException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode notificationsArrayNode = jsonFactory.arrayNode();
		List<NotificationRecord> notifications = UserNotificationsAPI.getInstance()
				.retrieveNotifications(UsersManager.getLoggedUser(), getProject());
		for (NotificationRecord n: notifications) {
			//filter notifications for the current project
			ObjectNode notificationNode = jsonFactory.objectNode();
			notificationNode.put("resource", n.getResource());
			notificationNode.put("role", n.getRole().toString());
			notificationNode.put("action", n.getAction().toString());
			notificationNode.put("timestamp", n.getTimestamp());
			notificationsArrayNode.add(notificationNode);
		}
		return notificationsArrayNode;
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void clearNotifications() throws IOException {
		UserNotificationsAPI.getInstance().clearNotifications(UsersManager.getLoggedUser(), getProject());
	}
}
