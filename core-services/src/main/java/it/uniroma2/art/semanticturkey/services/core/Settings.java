package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.SettingsManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.Language;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.settings.core.CoreProjectSettings;
import it.uniroma2.art.semanticturkey.settings.core.CoreSystemSettings;
import it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

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
     * Returns the default settings stored in a given scope for a component
     *
     * @param componentID
     * @param scope
     * @param defaultScope
     * @param projectName if scope is project, optionally
     * @return
     * @throws NoSuchSettingsManager
     * @throws STPropertyAccessException
     */
    @STServiceOperation
    public it.uniroma2.art.semanticturkey.extension.settings.Settings getSettingsDefault(String componentID, Scope scope,
            @Optional Scope defaultScope, @Optional String projectName) throws NoSuchSettingsManager,
            STPropertyAccessException {
        Project project = (scope == Scope.PROJECT_USER || scope == Scope.PROJECT_GROUP) ? getProject() : null;
        return exptManager.getSettingsDefault(project, UsersManager.getLoggedUser(), componentID, scope, defaultScope);
    }

    /**
     * Returns the default PU Settings at user  level for the given user.
     * Useful for administration purposes (e.g. Admin or PMs that want to manage the default PUSettings different users).
     * Users that want to get their default can still use {@link #getSettingsDefault}.
     *
     * @param componentID
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
    public it.uniroma2.art.semanticturkey.extension.settings.Settings getPUSettingsUserDefault(String componentID,
            IRI userIri) throws NoSuchSettingsManager, STPropertyAccessException, UserException {
        STUser user = UsersManager.getUser(userIri);
        return exptManager.getSettingsDefault(null, user, componentID, Scope.PROJECT_USER, Scope.USER);
    }

    @STServiceOperation
    public StartupSettings getStartupSettings() throws STPropertyAccessException {
        CoreProjectSettings defaultProjSettings = STPropertiesManager.getProjectSettingsDefault(CoreProjectSettings.class, SemanticTurkeyCoreSettingsManager.class.getName());
        CoreSystemSettings coreSysSettings = STPropertiesManager.getSystemSettings(CoreSystemSettings.class, SemanticTurkeyCoreSettingsManager.class.getName());
        StartupSettings startupSettings = new StartupSettings();
        startupSettings.experimentalFeaturesEnabled = coreSysSettings.experimentalFeaturesEnabled;
        startupSettings.homeContent = coreSysSettings.homeContent;
        startupSettings.languages = defaultProjSettings.languages;
        startupSettings.privacyStatementAvailable = coreSysSettings.privacyStatementAvailable;
        startupSettings.showFlags = coreSysSettings.showFlags;
        return startupSettings;
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

    /**
     * Set the value of a property in the settings in a given scope for a component
     *
     * @param componentID
     * @param scope
     * @param propertyName
     * @param propertyValue
     * @throws NoSuchSettingsManager
     * @throws STPropertyAccessException
     * @throws IllegalStateException
     * @throws STPropertyUpdateException
     * @throws WrongPropertiesException
     */
    @STServiceOperation(method = RequestMethod.POST)
    public void storeSetting(String componentID, Scope scope, String propertyName, @JsonSerialized JsonNode propertyValue)
            throws NoSuchSettingsManager, STPropertyAccessException, IllegalStateException,
            STPropertyUpdateException, WrongPropertiesException, PropertyNotFoundException, IOException {
        Project project = (scope == Scope.SYSTEM) ? null : getProject();
        exptManager.storeSetting(componentID, project, UsersManager.getLoggedUser(), scope, propertyName, propertyValue);
    }

    /**
     * Stores the default settings in a given scope for a component
     *
     * @param componentID
     * @param scope
     * @param defaultScope
     * @param settings
     * @throws NoSuchSettingsManager
     * @throws STPropertyAccessException
     * @throws IllegalStateException
     * @throws STPropertyUpdateException
     * @throws WrongPropertiesException
     */
    @STServiceOperation(method = RequestMethod.POST)
    public void storeSettingsDefault(String componentID, Scope scope, Scope defaultScope, ObjectNode settings)
            throws NoSuchSettingsManager, STPropertyAccessException, IllegalStateException,
            STPropertyUpdateException, WrongPropertiesException {
        Project project = (scope == Scope.SYSTEM) ? null : getProject();
        exptManager.storeSettingsDefault(componentID, project, UsersManager.getLoggedUser(), scope, defaultScope, settings);
    }

    /**
     * Stores the default settings property in a given scope for a component
     *
     * @param componentID
     * @param scope
     * @param defaultScope
     * @param propertyName
     * @param propertyValue
     * @throws NoSuchSettingsManager
     * @throws STPropertyAccessException
     * @throws IllegalStateException
     * @throws STPropertyUpdateException
     * @throws WrongPropertiesException
     */
    @STServiceOperation(method = RequestMethod.POST)
    public void storeSettingDefault(String componentID, Scope scope, Scope defaultScope, String propertyName, @JsonSerialized JsonNode propertyValue)
            throws NoSuchSettingsManager, STPropertyAccessException, IllegalStateException,
            STPropertyUpdateException, WrongPropertiesException, PropertyNotFoundException, IOException {
        Project project = (scope == Scope.SYSTEM) ? null : getProject();
        exptManager.storeSettingDefault(componentID, project, UsersManager.getLoggedUser(), scope, defaultScope, propertyName, propertyValue);
    }


    /**
     * Returns the PROJECT or PROJECT_USER settings of a specific project or project-user pair.
     * Useful for administration purposes
     * (e.g. Admin/PM that want to manage project settings for a project different from ctx_project,
     * or project_user settings for a project-user pair for a different user).
     *
     * @param componentID
     * @param scope
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
    @PreAuthorize("@auth.isAuthorizedInProject('pm(project)', 'R', #projectName)")
    @STServiceOperation
    public it.uniroma2.art.semanticturkey.extension.settings.Settings getSettingsForProjectAdministration(
            String componentID, Scope scope, String projectName, @Optional IRI userIri)
            throws NoSuchSettingsManager, STPropertyAccessException, ProjectAccessException,
            ProjectInexistentException, InvalidProjectNameException, UserException {

        //the service has been implemented just for project/project-user administration,
        //so none of the other scope is admitted
        if (scope != Scope.PROJECT_USER && scope != Scope.PROJECT) {
            throw new IllegalArgumentException("Invalid scope for this service");
        }
        Project project = ProjectManager.getProjectDescription(projectName);
        STUser user = (userIri != null) ? UsersManager.getUser(userIri) : null;

        return exptManager.getSettings(project, user, componentID, Scope.PROJECT);
    }

    /**
     * Stores the PROJECT or PROJECT_USER settings of a specific project or project-user pair.
     * Useful for administration purposes
     * (e.g. Admin/PM that want to manage project settings for a project different from ctx_project,
     * or project_user settings for a project-user pair for a different user).
     * @param componentID
     * @param scope
     * @param projectName
     * @param userIri
     * @param propertyName
     * @param propertyValue
     * @throws NoSuchSettingsManager
     * @throws STPropertyAccessException
     * @throws ProjectAccessException
     * @throws ProjectInexistentException
     * @throws InvalidProjectNameException
     * @throws UserException
     * @throws STPropertyUpdateException
     * @throws PropertyNotFoundException
     * @throws WrongPropertiesException
     * @throws IOException
     */
    @PreAuthorize("@auth.isAuthorizedInProject('pm(project)', 'U', #projectName)")
    @STServiceOperation(method = RequestMethod.POST)
    public void storeSettingForProjectAdministration(String componentID, Scope scope,
            String projectName, @Optional IRI userIri,
            String propertyName, @JsonSerialized JsonNode propertyValue)
            throws NoSuchSettingsManager, STPropertyAccessException, ProjectAccessException,
            ProjectInexistentException, InvalidProjectNameException, UserException,
            STPropertyUpdateException, PropertyNotFoundException, WrongPropertiesException, IOException {

        //the service has been implemented just for project/project-user administration,
        //so none of the other scope is admitted
        if (scope != Scope.PROJECT_USER && scope != Scope.PROJECT) {
            throw new IllegalArgumentException("Invalid scope for this service");
        }
        Project project = ProjectManager.getProjectDescription(projectName);
        STUser user = (userIri != null) ? UsersManager.getUser(userIri) : null;

        exptManager.storeSetting(componentID, project, user, scope, propertyName, propertyValue);
    }


    /**
     * Inner class useful just for { @link getStartupSettings } in order to return a Settings that is a mix
     * between a default project setting (languages) and a subset of CoreSystemSettings properties
     */
    public static class StartupSettings implements STProperties {
        @Override
        public String getShortName() {
            return "StartupSettings";
        }

        @STProperty(description = "")
        public Boolean experimentalFeaturesEnabled = false;

        @STProperty(description = "")
        public Boolean privacyStatementAvailable = false;

        @STProperty(description = "")
        public Boolean showFlags = true;

        @STProperty(description = "")
        public String homeContent;

        @STProperty(description = "")
        public List<Language> languages;
    }
}