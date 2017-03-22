package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
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
	 * Gets the languages preference.
	 * @return
	 * @throws STPropertyAccessException
	 * @throws STPropertyUpdateException 
	 * @throws IllegalStateException 
	 */
//	@STServiceOperation
//	public Collection<String> getLanguages() throws STPropertyAccessException, STPropertyUpdateException {
//		Collection<String> languages = new ArrayList<>();
//		String value = STPropertiesManager.getProjectPreference(STPropertiesManager.PROP_LANGUAGES, getProject(),
//				UsersManager.getLoggedUser(), RenderingEngine.class.getName());
//		if (value != null) {
//			value.replaceAll(" ", ""); // remove all spaces
//			String[] splitted = value.split(",");
//			for (int i = 0; i < splitted.length; i++) {
//				languages.add(splitted[i]);
//			}
//		} else {
//			STPropertiesManager.setProjectPreference(STPropertiesManager.PROP_LANGUAGES, "*", getProject(),	
//					UsersManager.getLoggedUser(), RenderingEngine.class.getName());
//		}
//		return languages;
//	}

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
	 * Gets the show_flag preference. If the property is not set, sets true as default in the preference file and returns it.
	 * @return
	 * @throws STPropertyAccessException
	 */
//	@STServiceOperation
//	public Boolean getShowFlags() throws STPropertyAccessException, STPropertyUpdateException {
//		String value = STPropertiesManager.getProjectPreference(STPropertiesManager.PROP_SHOW_FLAGS, getProject(),
//				UsersManager.getLoggedUser());
//		boolean show = true;
//		if (value != null) {
//			show = Boolean.parseBoolean(value);
//		} else { //property not set => set default
//			STPropertiesManager.setProjectPreference(STPropertiesManager.PROP_SHOW_FLAGS, show+"", getProject(),
//					UsersManager.getLoggedUser());
//		}
//		return show;
//	}
	
	/**
	 * Gets the show_flag preference. If the property is not set, sets true as default in the preference file and returns it.
	 * @return
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	public void setShowFlags(boolean show) throws STPropertyAccessException, STPropertyUpdateException {
		STPropertiesManager.setProjectPreference(STPropertiesManager.PROP_SHOW_FLAGS, show+"", getProject(),
			UsersManager.getLoggedUser());
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
		boolean show = true;
		if (value != null) {
			show = Boolean.parseBoolean(value);
		} else { //property not set => set default
			STPropertiesManager.setProjectPreference(STPropertiesManager.PROP_SHOW_FLAGS, show+"", getProject(),
					UsersManager.getLoggedUser());
		}
		preferencesNode.set(STPropertiesManager.PROP_SHOW_FLAGS, jsonFactory.booleanNode(show));
		
		return preferencesNode;
	}
	
}
