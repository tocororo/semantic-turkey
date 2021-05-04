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
import it.uniroma2.art.semanticturkey.resources.Resources;
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
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UsersGroup;
import it.uniroma2.art.semanticturkey.user.UsersGroupsManager;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.File;
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
        STUser user = UsersManager.getLoggedUser();
        UsersGroup group = (scope == Scope.PROJECT_GROUP) ? ProjectUserBindingsManager.getUserGroup(user, project) : null;
        return exptManager.getSettings(project, user, group, componentID, scope);
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
        Project project = (defaultScope == Scope.PROJECT_USER || defaultScope == Scope.PROJECT_GROUP) ? getProject() : null;
        STUser user = UsersManager.getLoggedUser();
        UsersGroup group = (defaultScope == Scope.PROJECT_GROUP) ? ProjectUserBindingsManager.getUserGroup(user, project) : null;
        return exptManager.getSettingsDefault(project, user, group, componentID, scope, defaultScope);
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
        STUser user = UsersManager.getLoggedUser();
        UsersGroup group = (scope == Scope.PROJECT_GROUP) ? ProjectUserBindingsManager.getUserGroup(user, project) : null;
        exptManager.storeSettings(componentID, project, user, group, scope, settings);
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
        STUser user = UsersManager.getLoggedUser();
        UsersGroup group = (scope == Scope.PROJECT_GROUP) ? ProjectUserBindingsManager.getUserGroup(user, project) : null;
        exptManager.storeSetting(componentID, project, user, group, scope, propertyName, propertyValue);
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
        Project project = (defaultScope == Scope.SYSTEM || defaultScope == Scope.USER) ? null : getProject();
        STUser user = UsersManager.getLoggedUser();
        UsersGroup group = (scope == Scope.PROJECT_GROUP) ? ProjectUserBindingsManager.getUserGroup(user, project) : null;
        exptManager.storeSettingsDefault(componentID, project, user, group, scope, defaultScope, settings);
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
        Project project = (defaultScope == Scope.SYSTEM || defaultScope == Scope.USER) ? null : getProject();
        STUser user = UsersManager.getLoggedUser();
        UsersGroup group = (defaultScope == Scope.PROJECT_GROUP) ? ProjectUserBindingsManager.getUserGroup(user, project) : null;
        exptManager.storeSettingDefault(componentID, project, user, group, scope, defaultScope, propertyName, propertyValue);
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
            String componentID, Scope scope, String projectName, @Optional IRI userIri, @Optional IRI groupIri)
            throws NoSuchSettingsManager, STPropertyAccessException, ProjectAccessException,
            ProjectInexistentException, InvalidProjectNameException, UserException {

        //the service has been implemented just for project/project-user/project-group administration,
        //so none of the other scope is admitted
        if (scope != Scope.PROJECT_USER && scope != Scope.PROJECT && scope != Scope.PROJECT_GROUP) {
            throw new IllegalArgumentException("Invalid scope for this service");
        }
        Project project = ProjectManager.getProjectDescription(projectName);
        STUser user = (userIri != null) ? UsersManager.getUser(userIri) : UsersManager.getLoggedUser();
        UsersGroup group = (groupIri != null) ? UsersGroupsManager.getGroupByIRI(groupIri) : null;

        return exptManager.getSettings(project, user, group, componentID, scope);
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
            String projectName, @Optional IRI userIri, @Optional IRI groupIri,
            String propertyName, @JsonSerialized JsonNode propertyValue)
            throws NoSuchSettingsManager, STPropertyAccessException, ProjectAccessException,
            ProjectInexistentException, InvalidProjectNameException, UserException,
            STPropertyUpdateException, PropertyNotFoundException, WrongPropertiesException, IOException {

        //the service has been implemented just for project/project-user administration,
        //so none of the other scope is admitted
        if (scope != Scope.PROJECT_USER && scope != Scope.PROJECT && scope != Scope.PROJECT_GROUP) {
            throw new IllegalArgumentException("Invalid scope for this service");
        }
        Project project = ProjectManager.getProjectDescription(projectName);
        STUser user = (userIri != null) ? UsersManager.getUser(userIri) : null;
        UsersGroup group = (groupIri != null) ? UsersGroupsManager.getGroupByIRI(groupIri) : null;

        exptManager.storeSetting(componentID, project, user, group, scope, propertyName, propertyValue);
    }

    /**
     * Returns the default PU Settings at user level for the given user.
     * Useful for administration purposes (e.g. Admin that want to manage the default PUSettings for different users).
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
    @PreAuthorize("@auth.isAdmin()") //only admin can manage other users
    @STServiceOperation
    public it.uniroma2.art.semanticturkey.extension.settings.Settings getPUSettingsUserDefault(String componentID,
            IRI userIri) throws NoSuchSettingsManager, STPropertyAccessException, UserException {
        STUser user = UsersManager.getUser(userIri);
        return exptManager.getSettingsDefault(null, user, null, componentID, Scope.PROJECT_USER, Scope.USER);
    }

    /**
     * Stores the default PU Settings at user level for the given user.
     * Useful for administration purposes (e.g. Admin that want to manage the default PUSettings for different users).
     * Users that want to store their default can still use {@link #getSettingsDefault}.
     * @param componentID
     * @param userIri
     * @param propertyName
     * @param propertyValue
     * @return
     * @throws NoSuchSettingsManager
     * @throws STPropertyAccessException
     * @throws UserException
     * @throws STPropertyUpdateException
     * @throws PropertyNotFoundException
     * @throws WrongPropertiesException
     * @throws IOException
     */
    @PreAuthorize("@auth.isAdmin()") //only admin can manage other users
    @STServiceOperation(method = RequestMethod.POST)
    public void storePUSettingUserDefault(String componentID,
            IRI userIri, String propertyName, @JsonSerialized JsonNode propertyValue)
            throws NoSuchSettingsManager, STPropertyAccessException, UserException, STPropertyUpdateException,
            PropertyNotFoundException, WrongPropertiesException, IOException {
        STUser user = UsersManager.getUser(userIri);
        exptManager.storeSettingDefault(componentID, null, user, null, Scope.PROJECT_USER, Scope.USER, propertyName, propertyValue);
    }


    /***
     * Returns the default PU Settings at project level for the given project.
     * Useful for administration purposes (e.g. Admin that want to manage the default PUSettings for different projects).
     *
     * User that wants to manage project where he is PM, can still use {@link #getSettingsDefault}
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
    @PreAuthorize("@auth.isAdmin()") //only admin can manage all projects (even closed one)
    @STServiceOperation
    public it.uniroma2.art.semanticturkey.extension.settings.Settings getPUSettingsProjectDefault(String componentID,
            String projectName) throws NoSuchSettingsManager, STPropertyAccessException,  ProjectAccessException, ProjectInexistentException, InvalidProjectNameException {
        Project project = ProjectManager.getProject(projectName, true);
        return exptManager.getSettingsDefault(project, null, null, componentID, Scope.PROJECT_USER, Scope.PROJECT);
    }

    /**
     * Stores the default PU Settings at project level for the given project.
     * Useful for administration purposes (e.g. Admin that want to manage the default PUSettings for different projects).
     *
     * User that wants to manage project where he is PM, can still use {@link #storeSettingsDefault}
     *
     * @param componentID
     * @param projectName
     * @param propertyName
     * @param propertyValue
     * @throws NoSuchSettingsManager
     * @throws STPropertyAccessException
     * @throws STPropertyUpdateException
     * @throws PropertyNotFoundException
     * @throws WrongPropertiesException
     * @throws IOException
     * @throws ProjectAccessException
     * @throws ProjectInexistentException
     * @throws InvalidProjectNameException
     */
    @PreAuthorize("@auth.isAdmin()") //only admin can manage all projects (even closed one)
    @STServiceOperation(method = RequestMethod.POST)
    public void storePUSettingProjectDefault(String componentID, String projectName,
            String propertyName, @JsonSerialized JsonNode propertyValue)
            throws NoSuchSettingsManager, STPropertyAccessException, STPropertyUpdateException,
            PropertyNotFoundException, WrongPropertiesException, IOException, ProjectAccessException,
            ProjectInexistentException, InvalidProjectNameException {
        Project project = ProjectManager.getProject(projectName, true);
        exptManager.storeSettingDefault(componentID, project, null, null, Scope.PROJECT_USER, Scope.PROJECT, propertyName, propertyValue);
    }



    /**
     * Returns settings required at initialization of client application
     * @return
     * @throws STPropertyAccessException
     */
    @STServiceOperation
    public StartupSettings getStartupSettings() throws STPropertyAccessException {
        CoreProjectSettings defaultProjSettings = STPropertiesManager.getProjectSettingsDefault(CoreProjectSettings.class, SemanticTurkeyCoreSettingsManager.class.getName());
        CoreSystemSettings coreSysSettings = STPropertiesManager.getSystemSettings(CoreSystemSettings.class, SemanticTurkeyCoreSettingsManager.class.getName());
        StartupSettings startupSettings = new StartupSettings();
        startupSettings.experimentalFeaturesEnabled = coreSysSettings.experimentalFeaturesEnabled;
        startupSettings.homeContent = coreSysSettings.homeContent;
        startupSettings.languages = defaultProjSettings.languages;
        startupSettings.showFlags = coreSysSettings.showFlags;
        //privacyStatementAvailable is not a configurable settings, it needs to be evaluated on demand
        File psFile = new File(Resources.getDocsDir(), "privacy_statement.pdf");
        boolean privacyStatementAvailable = psFile.isFile();
        startupSettings.privacyStatementAvailable = privacyStatementAvailable;
        return startupSettings;
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