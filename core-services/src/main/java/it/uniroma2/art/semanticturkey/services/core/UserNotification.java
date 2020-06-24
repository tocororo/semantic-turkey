package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.user.notification.UserNotificationAPI;
import it.uniroma2.art.semanticturkey.user.notification.UserNotificationAPI.Action;
import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@STService
public class UserNotification extends STServiceAdapter {

    protected static Logger logger = LoggerFactory.getLogger(UserNotification.class);

    /* Notifications on single resource */

    /**
     * Enables notifications on a resource
     * @param resource
     * @throws IOException
     */
    @STServiceOperation(method = RequestMethod.POST)
    public void startWatching(Resource resource) throws IOException {
        UserNotificationAPI.getInstance().addToUser(UsersManager.getLoggedUser(), getProject(), resource);
    }

    /**
     * Disables notifications on a resource
     * @param resource
     * @throws IOException
     */
    @STServiceOperation(method = RequestMethod.POST)
    public void stopWatching(Resource resource) throws IOException {
        UserNotificationAPI.getInstance().removeProjResFromUser(UsersManager.getLoggedUser(), getProject(), resource);
    }

    /**
     * Returns all the resources for which the current user has enabled notifications
     * @return
     * @throws IOException
     */
    @STServiceOperation
    public List<Resource> listWatching() throws IOException {
        return UserNotificationAPI.getInstance().listResourcesFromUserInProject(getProject(), UsersManager.getLoggedUser());
    }

    /**
     * Returns true if the user has enabled notifications on the given resource, false otherwise
     * @param resource
     * @return
     * @throws IOException
     */
    @STServiceOperation
    public Boolean isWatching(Resource resource) throws IOException {
        List<Resource> watchedResources = UserNotificationAPI.getInstance().listResourcesFromUserInProject(getProject(), UsersManager.getLoggedUser());
        return watchedResources.stream().anyMatch(r -> r.equals(resource));
    }

    /* Notifications on roles */

    @STServiceOperation
    public Map<RDFResourceRole, List<Action>> getNotificationPreferences() throws IOException {
        return UserNotificationAPI.getInstance().getRoleActionsNotificationPreferences(getProject(), UsersManager.getLoggedUser());
    }

    /**
     * Updates the notification status on the role-action pairs
     * @param preferences for each role lists the actions for which notifications are enabled
     */
    @STServiceOperation(method = RequestMethod.POST)
    public void storeNotificationPreferences(@JsonSerialized Map<RDFResourceRole, List<Action>> preferences) throws IOException {
        STUser user = UsersManager.getLoggedUser();
        Project project = getProject();
        UserNotificationAPI notificationApi = UserNotificationAPI.getInstance();

        //TODO the following will be replaced when the notification API will provide a method to store the preference with one-shot
        //for each role-action pair, if the notifications are enabled, add the notifications, otherwise remove them
        for (RDFResourceRole role : preferences.keySet()) {
            List<Action> actions = preferences.get(role);
            System.out.println("role " + role);
            System.out.println("actions " + actions);
            if (actions.contains(Action.creation)) {
                notificationApi.addToUser(user, project, role, Action.creation);
            } else {
                notificationApi.removeProjRoleActionFromUser(user, project, role, Action.creation);
            }
            if (actions.contains(Action.deletion)) {
                notificationApi.addToUser(user, project, role, Action.deletion);
            } else {
                notificationApi.removeProjRoleActionFromUser(user, project, role, Action.deletion);
            }
            if (actions.contains(Action.update)) {
                notificationApi.addToUser(user, project, role, Action.update);
            } else {
                notificationApi.removeProjRoleActionFromUser(user, project, role, Action.update);
            }
        }
    }

    /**
     * Enables or disables the notification on a role-action pair
     * @param role
     * @param action
     * @param status
     */
    @STServiceOperation(method = RequestMethod.POST)
    public void updateNotificationPreferences(RDFResourceRole role, Action action, boolean status) throws IOException {
        if (!UserNotificationAPI.availableRoles.contains(role)) return;

        STUser user = UsersManager.getLoggedUser();
        Project project = getProject();
        UserNotificationAPI notificationApi = UserNotificationAPI.getInstance();
        if (status) {
            notificationApi.addToUser(user, project, role, action);
        } else {
            notificationApi.removeProjRoleActionFromUser(user, project, role, action);
        }
    }



//    @STServiceOperation(method = RequestMethod.GET)
//    public List<String> searchResourceFromUser(String userId) throws IOException {
//        return UserNotificationAPI.getInstance().searchResourceFromUser(userId);
//    }
//
//    @STServiceOperation(method = RequestMethod.GET)
//    public List<String> searchProjRoleActionFromUser(String userId) throws IOException {
//        return UserNotificationAPI.getInstance().searchProjRoleActionFromUser(userId);
//    }
//
//    @STServiceOperation(method = RequestMethod.GET)
//    public List<String> searchUserFromProjRes(String proj, IRI res) throws IOException {
//        return UserNotificationAPI.getInstance().searchUserFromProjRes(proj, res);
//    }
//
//    @STServiceOperation(method = RequestMethod.GET)
//    public List<String> searchUserFromProjRoleAction(String proj, RDFResourceRole role, UserNotificationAPI.Action action) throws IOException {
//        return UserNotificationAPI.getInstance().searchUserFromProjRoleAction(proj, role, action);
//    }
//
//    @STServiceOperation(method = RequestMethod.POST)
//    public String addToUserWithRes(String user, String projId, IRI res) throws IOException {
//        return String.valueOf(UserNotificationAPI.getInstance().addToUser(user, projId, res));
//    }
//
//    @STServiceOperation(method = RequestMethod.POST)
//    public String addToUserWithRole(String user, String projId, RDFResourceRole role, UserNotificationAPI.Action action) throws IOException {
//        return String.valueOf(UserNotificationAPI.getInstance().addToUser(user, projId, role, action));
//    }
//
//    @STServiceOperation(method = RequestMethod.POST)
//    public String removeUser(String user) throws IOException {
//        return String.valueOf(UserNotificationAPI.getInstance().removeUser(user));
//    }
//
//    @STServiceOperation(method = RequestMethod.POST)
//    public String removeProjResFromUser(String user, String projId, IRI res) throws IOException {
//        return String.valueOf(UserNotificationAPI.getInstance().removeProjResFromUser(user, projId, res));
//    }
//
//    @STServiceOperation(method = RequestMethod.POST)
//    public String removeProjRoleActionFromUser(String user, String projId, RDFResourceRole role, UserNotificationAPI.Action action) throws IOException {
//        return String.valueOf(UserNotificationAPI.getInstance().removeProjRoleActionFromUser(user, projId, role, action));
//    }

}
