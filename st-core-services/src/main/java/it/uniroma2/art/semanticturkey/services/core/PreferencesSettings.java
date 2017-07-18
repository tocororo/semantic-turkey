package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.constraints.LocallyDefinedResources;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.UsersManager;

@STService
public class PreferencesSettings extends STServiceAdapter {
	
	/**
	 * TODO
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
	 * Sets the languages preference
	 * @param languages
	 * @throws STPropertyUpdateException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void setLanguages(Collection<String> languages)
			throws STPropertyUpdateException, STPropertyAccessException {
		String value = "*";
		if (languages.size() == 1) {
			value = languages.iterator().next();
		} else if (languages.size() > 1) {
			value = String.join(",", languages);
		}
		STPropertiesManager.setProjectPreference(STPropertiesManager.PREF_LANGUAGES, value, getProject(),	
				UsersManager.getLoggedUser(), RenderingEngine.class.getName());
	}
	
	/**
	 * Sets the show_flag preference. If the property is not set, sets true as default in the preference file and returns it.
	 * @return
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void setShowFlags(boolean show) throws STPropertyAccessException, STPropertyUpdateException {
		STPropertiesManager.setProjectPreference(STPropertiesManager.PREF_SHOW_FLAGS, show+"", getProject(),
			UsersManager.getLoggedUser());
	}
	
	/**
	 * Sets the show_instances_number preference. If the property is not set, sets true as default in the preference
	 * file and returns it.
	 * @return
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void setShowInstancesNumb(boolean show) throws STPropertyAccessException, STPropertyUpdateException {
		STPropertiesManager.setProjectPreference(STPropertiesManager.PREF_SHOW_INSTANCES_NUMBER, show+"", getProject(),
			UsersManager.getLoggedUser());
	}
	
	/**
	 * Sets the active scheme preference (in order to retrieve it on the future access) for the current project.
	 * The scheme is optional, if not provided it means that the concept tree is working in no scheme mode
	 * @param scheme
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
			STPropertiesManager.setProjectPreference(STPropertiesManager.PREF_ACTIVE_SCHEMES, value, getProject(),
					UsersManager.getLoggedUser());
		} else { // no scheme mode
			STPropertiesManager.setProjectPreference(STPropertiesManager.PREF_ACTIVE_SCHEMES, null, getProject(),
					UsersManager.getLoggedUser());
		}
	}
	
	/**
	 * Changes the project theme
	 * @param themeId
	 * @throws STPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void setProjectTheme(int themeId) throws STPropertyUpdateException {
		STPropertiesManager.setProjectPreference(STPropertiesManager.PREF_PROJ_THEME, themeId+"", getProject(),
				UsersManager.getLoggedUser());
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
		String value = STPropertiesManager.getProjectPreference(STPropertiesManager.PREF_ACTIVE_SCHEMES,
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
	public JsonNode getProjectPreferences(List<String> properties, @Optional String pluginID)
			throws STPropertyAccessException, InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode respNode = jsonFactory.objectNode();
		for (String prop: properties) {
			String value;
			if (pluginID == null) {
				value = STPropertiesManager.getProjectPreference(prop, getProject(), UsersManager.getLoggedUser());
			} else {
				value = STPropertiesManager.getProjectPreference(prop, getProject(), UsersManager.getLoggedUser(), pluginID);
			}
			respNode.set(prop, jsonFactory.textNode(value));
		}
		return respNode;
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
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project,_)', 'U')")
	public void setProjectSetting(String property, @Optional String value, @Optional String projectName)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, STPropertyUpdateException {
		Project project = (projectName != null) ? ProjectManager.getProjectDescription(projectName) : getProject();
		STPropertiesManager.setProjectSetting(property, value, project);
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
	 * Returns the languages available in the system. This information could be retrieved calling
	 * {@link #getDefaultProjectSettings(List, String)} passing the 'languages' property and null plugin.
	 * Anyway, since this information should be available without be logged, this method is excluded from the 
	 * intercepted url
	 * @return
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	public JsonNode getSystemLanguages() throws STPropertyAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		return jsonFactory.textNode(STPropertiesManager.getProjectSettingDefault(STPropertiesManager.SETTING_PROJ_LANGUAGES));
	}
	
}
