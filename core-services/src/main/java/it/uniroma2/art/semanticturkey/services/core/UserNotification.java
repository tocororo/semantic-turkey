package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.notification.UserNotificationAPI;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@STService
public class UserNotification extends STServiceAdapter {

    protected static Logger logger = LoggerFactory.getLogger(UserNotification.class);


    @STServiceOperation(method = RequestMethod.GET)
    public List<String> searchResourceFromUser(String userId) throws IOException {
        return UserNotificationAPI.getInstance().searchResourceFromUser(userId);
    }

    @STServiceOperation(method = RequestMethod.GET)
    public List<String> searchProjRoleActionFromUser(String userId) throws IOException {
        return UserNotificationAPI.getInstance().searchProjRoleActionFromUser(userId);
    }

    @STServiceOperation(method = RequestMethod.GET)
    public List<String> searchUserFromProjRes(String proj, IRI res) throws IOException {
        return UserNotificationAPI.getInstance().searchUserFromProjRes(proj, res);
    }

    @STServiceOperation(method = RequestMethod.GET)
    public List<String> searchUserFromProjRoleAction(String proj, UserNotificationAPI.Role role, UserNotificationAPI.Action action) throws IOException {
        return UserNotificationAPI.getInstance().searchUserFromProjRoleAction(proj, role, action);
    }

    @STServiceOperation(method = RequestMethod.POST)
    public String addToUserWithRes(String user, String projId, IRI res) throws IOException {
        return String.valueOf(UserNotificationAPI.getInstance().addToUser(user, projId, res));
    }

    @STServiceOperation(method = RequestMethod.POST)
    public String addToUserWithRole(String user, String projId, UserNotificationAPI.Role role, UserNotificationAPI.Action action) throws IOException {
        return String.valueOf(UserNotificationAPI.getInstance().addToUser(user, projId, role, action));
    }

    @STServiceOperation(method = RequestMethod.POST)
    public String removeUser(String user) throws IOException {
        return String.valueOf(UserNotificationAPI.getInstance().removeUser(user));
    }

    @STServiceOperation(method = RequestMethod.POST)
    public String removeProjResFromUser(String user, String projId, IRI res) throws IOException {
        return String.valueOf(UserNotificationAPI.getInstance().removeProjResFromUser(user, projId, res));
    }

    @STServiceOperation(method = RequestMethod.POST)
    public String removeProjRoleActionFromUser(String user, String projId, UserNotificationAPI.Role role, UserNotificationAPI.Action action) throws IOException {
        return String.valueOf(UserNotificationAPI.getInstance().removeProjRoleActionFromUser(user, projId, role, action));
    }

}
