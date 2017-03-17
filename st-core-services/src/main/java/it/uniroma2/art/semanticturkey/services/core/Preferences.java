package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.Collection;

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
	 * Get the languages preference.
	 * Currently the UI of VB3 allows only a to manage project preferences (so preferences of a user in a project).
	 * So this method just get the project preference.
	 * TODO when there will be a richer UI, this method should get a parameter that specify the level of the preference:
	 * - project preference (done already in this method)
	 * - project preference - project default
	 * - project preference - user default
	 * - project preference - system default 
	 * @return
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	public Collection<String> getLanguages() throws STPropertyAccessException {
		//Future properties manager use
		Collection<String> languages = new ArrayList<>();
		String value = STPropertiesManager.getProjectPreference(STPropertiesManager.PROP_LANGUAGES, getProject(),
				UsersManager.getLoggedUser(), RenderingEngine.class.getName());
		if (value != null) {
			value.replaceAll(" ", ""); // remove all spaces
			String[] splitted = value.split(",");
			for (int i = 0; i < splitted.length; i++) {
				languages.add(splitted[i]);
			}
		}
		return languages;
	}

	/**
	 * Sets the languages preference
	 * Currently the UI of VB3 allows only a to manage project preferences (so preferences of a user in a project).
	 * So this method just set the project preference.
	 * TODO when there will be a richer UI, this method should get a parameter that specify the level of the preference:
	 * - project preference (done already in this method)
	 * - project preference - project default
	 * - project preference - user default
	 * - project preference - system default
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

}
