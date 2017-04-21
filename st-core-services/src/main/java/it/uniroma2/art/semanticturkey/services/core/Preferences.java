package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.UsersManager;

@STService
public class Preferences extends STServiceAdapter {
	
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
	@STServiceOperation
	public void setLanguages(Collection<String> languages)
			throws STPropertyUpdateException, STPropertyAccessException {
		String value = "*";
		if (languages.size() == 1) {
			value = languages.iterator().next();
		} else if (languages.size() > 1) {
			value = String.join(",", languages);
		}
		STPropertiesManager.setProjectPreference(STPropertiesManager.PROP_LANGUAGES, value, getProject(),	
				UsersManager.getLoggedUser(), RenderingEngine.class.getName());
	}
	
	/**
	 * Sets the show_flag preference. If the property is not set, sets true as default in the preference file and returns it.
	 * @return
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	public void setShowFlags(boolean show) throws STPropertyAccessException, STPropertyUpdateException {
		STPropertiesManager.setProjectPreference(STPropertiesManager.PROP_SHOW_FLAGS, show+"", getProject(),
			UsersManager.getLoggedUser());
	}
	
	/**
	 * Sets the show_instances_number preference. If the property is not set, sets true as default in the preference
	 * file and returns it.
	 * @return
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	public void setShowInstancesNumb(boolean show) throws STPropertyAccessException, STPropertyUpdateException {
		STPropertiesManager.setProjectPreference(STPropertiesManager.PROP_SHOW_INSTANCES_NUMBER, show+"", getProject(),
			UsersManager.getLoggedUser());
	}
	
	/**
	 * Set the active scheme preference (in order to retrieve it on the future access) for the current project.
	 * The scheme is optional, if not provided it means that the concept tree is working in no scheme mode
	 * @param scheme
	 * @throws IllegalStateException
	 * @throws STPropertyUpdateException
	 */
	@STServiceOperation
	public void setActiveScheme(@Optional @LocallyDefined IRI scheme) throws IllegalStateException, STPropertyUpdateException {
		if (scheme != null) {
			STPropertiesManager.setProjectPreference(STPropertiesManager.PROP_ACTIVE_SCHEME, scheme.stringValue(),
					getProject(), UsersManager.getLoggedUser());
		} else { // no scheme mode
			STPropertiesManager.setProjectPreference(STPropertiesManager.PROP_ACTIVE_SCHEME, null, getProject(),
					UsersManager.getLoggedUser());
		}
	}
	
	@STServiceOperation
	public AnnotatedValue<IRI> getActiveScheme(String projectName) throws IllegalStateException, STPropertyAccessException,
			ProjectAccessException {
		AnnotatedValue<IRI> scheme = null;
		Project<?> project = ProjectManager.getProject(projectName);
		if (project == null) {
			throw new ProjectAccessException("Cannot retrieve preferences of project " + projectName 
					+ ". It could be closed or not existing.");
		}
		String value = STPropertiesManager.getProjectPreference(STPropertiesManager.PROP_ACTIVE_SCHEME,
				ProjectManager.getProject(projectName), UsersManager.getLoggedUser());
		if (value != null) {
			scheme = new AnnotatedValue<IRI>(SimpleValueFactory.getInstance().createIRI(value));
		}
		return scheme;
	}
	
	
	
	@STServiceOperation
	public JsonNode getProjectPreferences() throws STPropertyAccessException, STPropertyUpdateException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode preferencesNode = jsonFactory.objectNode();
		
		//languages
		ArrayNode languagesArrayNode = jsonFactory.arrayNode();
		String value = STPropertiesManager.getProjectPreference(STPropertiesManager.PROP_LANGUAGES, getProject(),
				UsersManager.getLoggedUser(), RenderingEngine.class.getName());
		if (value != null) {
			value.replaceAll(" ", ""); // remove all spaces
			String[] splitted = value.split(",");
			for (int i = 0; i < splitted.length; i++) {
				languagesArrayNode.add(splitted[i]);
			}
		} else { //preference not set => set the default
			STPropertiesManager.setProjectPreference(STPropertiesManager.PROP_LANGUAGES, "*", getProject(),	
					UsersManager.getLoggedUser(), RenderingEngine.class.getName());
			languagesArrayNode.add("*");
		}
		preferencesNode.set(STPropertiesManager.PROP_LANGUAGES, languagesArrayNode);
		
		//show_flags
		value = STPropertiesManager.getProjectPreference(STPropertiesManager.PROP_SHOW_FLAGS, getProject(),
				UsersManager.getLoggedUser());
		boolean showFlags = true;
		if (value != null) {
			showFlags = Boolean.parseBoolean(value);
		} else { //property not set => set default
			STPropertiesManager.setProjectPreference(STPropertiesManager.PROP_SHOW_FLAGS, showFlags+"", getProject(),
					UsersManager.getLoggedUser());
		}
		preferencesNode.set(STPropertiesManager.PROP_SHOW_FLAGS, jsonFactory.booleanNode(showFlags));
		
		//show_instances_number
		value = STPropertiesManager.getProjectPreference(STPropertiesManager.PROP_SHOW_INSTANCES_NUMBER, getProject(),
				UsersManager.getLoggedUser());
		boolean showInst;
		try {
			//default: false in skos, true in owl
			showInst = getProject().getModelType().getName().contains("SKOS") ? false : true;
		} catch (ProjectInconsistentException e) {
			showInst = true;
		}
		if (value != null) {
			showInst = Boolean.parseBoolean(value);
		} else { //property not set => set default
			STPropertiesManager.setProjectPreference(STPropertiesManager.PROP_SHOW_INSTANCES_NUMBER, showInst+"",
					getProject(), UsersManager.getLoggedUser());
		}
		preferencesNode.set(STPropertiesManager.PROP_SHOW_INSTANCES_NUMBER, jsonFactory.booleanNode(showInst));
		
		//active_scheme
		value = STPropertiesManager.getProjectPreference(STPropertiesManager.PROP_ACTIVE_SCHEME, getProject(),
				UsersManager.getLoggedUser());
		preferencesNode.set(STPropertiesManager.PROP_ACTIVE_SCHEME, jsonFactory.textNode(value));
		
		return preferencesNode;
	}
	
}
