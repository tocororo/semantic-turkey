package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.SettingsManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Collection;

/**
 * This class provides services for handling settings.
 */
@STService
public class Settings extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Settings.class);

	@Autowired
	private ExtensionPointManager exptManager;

	@STServiceOperation
	public Collection<SettingsManager> getSettingManagers() {
		return exptManager.getSettingsManagers();
	}

	/**
	 * Returns the settings scopes supported by a component
	 * 
	 * @param componentID
	 * @return
	 * @throws NoSuchSettingsManager
	 */
	@STServiceOperation
	public Collection<Scope> getSettingsScopes(String componentID) throws NoSuchSettingsManager {
		return exptManager.getSettingsScopes(componentID);
	}

	/**
	 * Returns the settings stored in a given scope for a component
	 * 
	 * @param componentID
	 * @return
	 * @throws NoSuchSettingsManager
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	public it.uniroma2.art.semanticturkey.extension.settings.Settings getSettings(String componentID,
			Scope scope) throws NoSuchSettingsManager, STPropertyAccessException {
		Project project = (scope == Scope.SYSTEM) ? null : getProject();
		return exptManager.getSettings(project, UsersManager.getLoggedUser(), componentID, scope);
	}

	/**
	 * Returns the project settings of a specific project.
	 * Useful for administration purposes (e.g. Admin that want to manage project settings for a project
	 * different from ctx_project).
	 * This service can still be used by PM providing as projectName the same ctx_project.
	 *
	 * @param componentID
	 * @param projectName
	 * @return
	 * @throws NoSuchSettingsManager
	 * @throws STPropertyAccessException
	 * @throws ProjectAccessException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 */
	@PreAuthorize("@auth.isAuthorizedInProject('pm(project)', 'U', #projectName)")
	@STServiceOperation
	public it.uniroma2.art.semanticturkey.extension.settings.Settings getProjectSettings(String componentID,
			String projectName) throws NoSuchSettingsManager, STPropertyAccessException, ProjectAccessException,
			ProjectInexistentException, InvalidProjectNameException {
		return exptManager.getSettings(ProjectManager.getProjectDescription(projectName), null, componentID, Scope.PROJECT);
	}

	/**
	 * Returns the PU settings of a specific project-user pair.
	 * Useful for administration purposes (e.g. Admin or PMs that want to manage user settings for different
	 * users/projects and not for themself in the ctx_project).
	 * Users that wants to get their PUSettings can still use {@link #getSettings} with the proper scope.
	 *
	 * @param componentID
	 * @param projectName
	 * @param userIri
	 * @return
	 * @throws NoSuchSettingsManager
	 * @throws STPropertyAccessException
	 * @throws ProjectAccessException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 * @throws UserException
	 */
	@PreAuthorize("@auth.isAuthorizedInProject('pm(project)', 'U', #projectName)")
	@STServiceOperation
	public it.uniroma2.art.semanticturkey.extension.settings.Settings getPUSettingsOfUser(String componentID,
			String projectName, IRI userIri) throws NoSuchSettingsManager, STPropertyAccessException, ProjectAccessException,
			ProjectInexistentException, InvalidProjectNameException, UserException {
		Project project = ProjectManager.getProjectDescription(projectName);
		STUser user = UsersManager.getUser(userIri);
		return exptManager.getSettings(project, user, componentID, Scope.PROJECT_USER);
	}

	/**
	 * Stores the settings in a given scope for a component
	 * 
	 * @param componentID
	 * @param scope
	 * @param settings
	 * @throws NoSuchSettingsManager
	 * @throws STPropertyAccessException
	 * @throws IllegalStateException
	 * @throws STPropertyUpdateException
	 * @throws WrongPropertiesException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void storeSettings(String componentID, Scope scope, ObjectNode settings)
			throws NoSuchSettingsManager, STPropertyAccessException, IllegalStateException,
			STPropertyUpdateException, WrongPropertiesException {
		Project project = (scope == Scope.SYSTEM) ? null : getProject();
		exptManager.storeSettings(componentID, project, UsersManager.getLoggedUser(), scope, settings);
	}

}