package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefinedResources;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersGroup;
import it.uniroma2.art.semanticturkey.user.UsersGroupsManager;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.json.JSONException;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@STService
public class PreferencesSettings extends STServiceAdapter {
	
	/**
	 * Currently the UI of VB3 allows only a to manage project preferences (so preferences of a user in a project).
	 * So the following services just get/set the project preference.
	 * when there will be a richer UI, this services should get a parameter that specify the level of the preference:
	 * - project preference (done already in this method)
	 * - project preference - project default
	 * - project preference - user default
	 * - project preference - system default
	 * 
	 * At the moment, I disabled the getter of the single preferences since I preferred to provide a single service
	 * to return all the preferences.
	 * This choice depends on how I will implements the UI to manage the different preferences/settings (user/project/system)
	 */
	
	/**
	 * Sets the active scheme preference (in order to retrieve it on the future access) for the current project.
	 * The scheme is optional, if not provided it means that the concept tree is working in no scheme mode
	 * @param schemes
	 * @throws IllegalStateException
	 * @throws STPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void setActiveSchemes(@Optional @LocallyDefinedResources List<IRI> schemes) throws IllegalStateException, STPropertyUpdateException {
		if (schemes != null && !schemes.isEmpty()) {
			String value = "";
			for (IRI s : schemes) {
				value += s.stringValue() + ",";
			}
			value = value.substring(0, value.length()-1);
			STPropertiesManager.setPUSetting(STPropertiesManager.PREF_ACTIVE_SCHEMES, value, getProject(),
					UsersManager.getLoggedUser());
		} else { // no scheme mode
			STPropertiesManager.setPUSetting(STPropertiesManager.PREF_ACTIVE_SCHEMES, null, getProject(),
					UsersManager.getLoggedUser());
		}
	}
	
	/**
	 * @param projectName get this as parameter and not from getProject() since this method is useful also when
	 * exploring external project (not the working one)
	 * @return
	 * @throws IllegalStateException
	 * @throws STPropertyAccessException
	 * @throws ProjectAccessException
	 */
	@STServiceOperation
	public Collection<AnnotatedValue<IRI>> getActiveSchemes(String projectName) throws IllegalStateException, STPropertyAccessException,
			ProjectAccessException {
		Collection<AnnotatedValue<IRI>> schemes = new ArrayList<>();
		Project project = ProjectManager.getProject(projectName);
		if (project == null) {
			throw new ProjectAccessException("Cannot retrieve preferences of project " + projectName 
					+ ". It could be closed or not existing.");
		}
		String value = STPropertiesManager.getPUSetting(STPropertiesManager.PREF_ACTIVE_SCHEMES,
				ProjectManager.getProject(projectName), UsersManager.getLoggedUser());
		if (value != null) {
			String[] splitted = value.split(",");
			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			for (String s : splitted) {
				schemes.add(new AnnotatedValue<IRI>(vf.createIRI(s)));
			}
		}
		return schemes;
	}
	
	/**
	 * Returns the specified project preferences
	 * @param properties
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 */
	@STServiceOperation
	public JsonNode getPUSettings(List<String> properties, @Optional String projectName, @Optional String pluginID)
			throws STPropertyAccessException, InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode respNode = jsonFactory.objectNode();
		Project project;
		if (projectName != null) {
			project = ProjectManager.getProjectDescription(projectName);
		} else {
			project = getProject();
		}
		for (String prop: properties) {
			String value = STPropertiesManager.getPUSetting(prop, project, UsersManager.getLoggedUser(), pluginID);
			respNode.set(prop, jsonFactory.textNode(value));
		}
		return respNode;
	}
	
	/**
	 * Sets a project preference
	 * @param property
	 * @param value
	 * @param pluginID
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws STPropertyUpdateException
	 * @throws JSONException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void setPUSetting(String property, @Optional String value, @Optional String pluginID)
			throws STPropertyUpdateException {
		STPropertiesManager.setPUSetting(property, value, getProject(), UsersManager.getLoggedUser(), pluginID);
	}
	
	/**
	 * Returns the specified project preferences for the given group.
	 * @param properties
	 * @param projectName if not provided, returns the setting for the currently open project
	 * @param groupIri
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 */
	@STServiceOperation
	public JsonNode getPGSettings(List<String> properties, @Optional String projectName, IRI groupIri, @Optional String pluginID)
			throws STPropertyAccessException, InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		Project project;
		if (projectName != null) {
			project = ProjectManager.getProjectDescription(projectName);
		} else {
			project = getProject();
		}
		UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIri);
		if (group == null) {
			throw new IllegalArgumentException("Group not found");
		}
		
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode respNode = jsonFactory.objectNode();
		for (String prop: properties) {
			String value = STPropertiesManager.getPGSetting(prop, project, group, pluginID);
			respNode.set(prop, jsonFactory.textNode(value));
		}
		return respNode;
	}
	
	/**
	 * 
	 * @param property
	 * @param value
	 * @param pluginID
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws STPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void setPGSetting(String property, @Optional String value, @Optional String pluginID, @Optional String projectName, IRI groupIri)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, STPropertyUpdateException {
		Project project;
		if (projectName != null) {
			project = ProjectManager.getProjectDescription(projectName);
		} else {
			project = getProject();
		}
		UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIri);
		if (group == null) {
			throw new IllegalArgumentException("Group not found");
		}
		STPropertiesManager.setPGSetting(property, value, project, group, pluginID);
	}
	
	/**
	 * Returns the specified project settings
	 * @param properties
	 * @param projectName
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 */
	@STServiceOperation
	public JsonNode getProjectSettings(List<String> properties, @Optional String projectName, @Optional String pluginID)
			throws STPropertyAccessException, InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode respNode = jsonFactory.objectNode();
		Project project = (projectName != null) ? ProjectManager.getProjectDescription(projectName) : getProject();
		for (String prop: properties) {
			String value;
			if (pluginID == null) {
				value = STPropertiesManager.getProjectSetting(prop, project);
			} else {
				value = STPropertiesManager.getProjectSetting(prop, project, pluginID);
			}
			respNode.set(prop, jsonFactory.textNode(value));
		}
		return respNode;
	}
	
	/**
	 * Update the value of a project setting.
	 * @param property
	 * @param value if null remove the property
	 * @param projectName
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws STPropertyUpdateException
	 * @throws JSONException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project,_)', 'U')")
	public void setProjectSetting(String property, @Optional String value, @Optional String projectName, @Optional String pluginID)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, STPropertyUpdateException {
		Project project = (projectName != null) ? ProjectManager.getProjectDescription(projectName) : getProject();
		STPropertiesManager.setProjectSetting(property, value, project, pluginID);
	}

	@STServiceOperation
	public JsonNode getPUSettingsProjectDefault(List<String> properties, @Optional String projectName, @Optional String pluginID)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, STPropertyAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode respNode = jsonFactory.objectNode();
		Project project = (projectName != null) ? ProjectManager.getProjectDescription(projectName) : getProject();
		for (String prop: properties) {
			String value;
			if (pluginID == null) {
				value = STPropertiesManager.getPUSettingProjectDefault(prop, project);
			} else {
				value = STPropertiesManager.getPUSettingProjectDefault(prop, project, pluginID);
			}
			respNode.set(prop, jsonFactory.textNode(value));
		}
		return respNode;
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()") //admin required since it is possible to set the default even for other projects
	public void setPUSettingProjectDefault(String property, @Optional String value, @Optional String projectName, @Optional String pluginID)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, STPropertyUpdateException {
		Project project = (projectName != null) ? ProjectManager.getProjectDescription(projectName) : getProject();
		STPropertiesManager.setPUSettingProjectDefault(property, value, project, pluginID);
	}

	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('um(user)', 'R') || @auth.isLoggedUser(#email)")
	public JsonNode getPUSettingsUserDefault(List<String> properties, String email, @Optional String pluginID)
			throws STPropertyAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode respNode = jsonFactory.objectNode();
		STUser user = null;
		if (email != null) {
			user = UsersManager.getUserByEmail(email);
			if (user == null) {
				throw new IllegalArgumentException("User with email " + email + " doesn't exist");
			}
		}
		for (String prop: properties) {
			String value;
			if (pluginID == null) {
				value = STPropertiesManager.getPUSettingUserDefault(prop, user);
			} else {
				value = STPropertiesManager.getPUSettingUserDefault(prop, user, pluginID);
			}
			respNode.set(prop, jsonFactory.textNode(value));
		}
		return respNode;
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
	public void setPUSettingUserDefault(String property, String email, @Optional String value, @Optional String pluginID)
			throws STPropertyUpdateException {
		STUser user = null;
		if (email != null) {
			user = UsersManager.getUserByEmail(email);
			if (user == null) {
				throw new IllegalArgumentException("User with email " + email + " doesn't exist");
			}
		}
		if (pluginID == null) {
			STPropertiesManager.setPUSettingUserDefault(property, value, user);
		} else {
			STPropertiesManager.setPUSettingUserDefault(property, value, user, pluginID);
		}
	}
	
	/**
	 * Gets the default value of the given project settings
	 * @param properties
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	public JsonNode getDefaultProjectSettings(List<String> properties, @Optional String pluginID) throws STPropertyAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode respNode = jsonFactory.objectNode();
		for (String prop: properties) {
			String value;
			if (pluginID == null) {
				value = STPropertiesManager.getProjectSettingDefault(prop);
			} else {
				value = STPropertiesManager.getProjectSettingDefault(prop, pluginID);
			}
			respNode.set(prop, jsonFactory.textNode(value));
		}
		return respNode;
	}

	/**
	 * Returns some settings needed at the system startup (languages and experimental_features_enabled).
	 * This information could be retrieved calling {@link #getDefaultProjectSettings(List, String)} 
	 * passing the above property names and null plugin.
	 * Anyway, since this information should be available without be logged, this method is excluded from the 
	 * intercepted url
	 * @return
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	public JsonNode getStartupSystemSettings() throws STPropertyAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode respNode = jsonFactory.objectNode();
		
		TextNode langsNode = jsonFactory.textNode(
				STPropertiesManager.getProjectSettingDefault(STPropertiesManager.SETTING_PROJ_LANGUAGES));
		respNode.set(STPropertiesManager.SETTING_PROJ_LANGUAGES, langsNode);
		
		String expFeatValue = STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_EXP_FEATURES_ENABLED);
		boolean expFeatBool = "true".equals(expFeatValue);
		respNode.set(STPropertiesManager.SETTING_EXP_FEATURES_ENABLED, jsonFactory.booleanNode(expFeatBool));
		
		String showFlagValue = STPropertiesManager.getPUSettingSystemDefault(STPropertiesManager.PREF_SHOW_FLAGS);
		boolean showFlagBool = "true".equals(showFlagValue);
		respNode.set(STPropertiesManager.PREF_SHOW_FLAGS, jsonFactory.booleanNode(showFlagBool));
		
		File psFile = new File(Resources.getDocsDir(), "privacy_statement.pdf");
		boolean privacyStatementAvailable = psFile.isFile();
		respNode.set("privacy_statement_available", jsonFactory.booleanNode(privacyStatementAvailable));

		String homeContentValue = STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_HOME_CONTENT);
		respNode.set(STPropertiesManager.SETTING_HOME_CONTENT, jsonFactory.textNode(homeContentValue));
		
		return respNode;
	}
	
	/**
	 * 
	 * @param properties
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAdmin()")
	public JsonNode getSystemSettings(List<String> properties, @Optional String pluginID) throws STPropertyAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode respNode = jsonFactory.objectNode();
		for (String prop: properties) {
			String value;
			if (pluginID == null) {
				value = STPropertiesManager.getSystemSetting(prop);
			} else {
				value = STPropertiesManager.getSystemSetting(prop, pluginID);
			}
			respNode.set(prop, jsonFactory.textNode(value));
		}
		return respNode;
	}
	
	/**
	 * 
	 * @param property
	 * @param value if not provided, remove the previous value
	 * @throws STPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void setSystemSetting(String property, @Optional String value) throws STPropertyUpdateException {
		STPropertiesManager.setSystemSetting(property, value);
	}
	
	
}
