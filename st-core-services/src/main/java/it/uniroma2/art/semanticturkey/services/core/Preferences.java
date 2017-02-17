package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.properties.PropertyLevel;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.UsersManager;

@STService
public class Preferences extends STServiceAdapter {

	@STServiceOperation
	public Collection<String> getLanguages(PropertyLevel level) throws STPropertyAccessException {
		Collection<String> languages = new ArrayList<>();
		String value = getPropertyValue(STPropertiesManager.PROP_LANGUAGES, level);
		if (value != null) {
			value.replaceAll(" ", ""); // remove all spaces
			String[] splitted = value.split(",");
			for (int i = 0; i < splitted.length; i++) {
				languages.add(splitted[i]);
			}
		}
		return languages;
	}

	@STServiceOperation
	public void setLanguages(Collection<String> languages, PropertyLevel level)
			throws STPropertyUpdateException, STPropertyAccessException {
		String value = "*";
		if (languages.size() == 1) {
			value = languages.iterator().next();
		} else {
			value = String.join(",", languages);
		}
		setPropertyValue(STPropertiesManager.PROP_LANGUAGES, value, level);
	}

	@STServiceOperation
	public String getResourceViewMode(PropertyLevel level)
			throws STPropertyAccessException, STPropertyUpdateException {
		String value = getPropertyValue(STPropertiesManager.PROP_RES_VIEW_MODE, level);
		if (value == null || (value != "splitted" && value != "tabbed")) { // if not set or not a valid value
			value = "tabbed"; // default
			setPropertyValue(STPropertiesManager.PROP_RES_VIEW_MODE, value, level);
		}
		return value;
	}

	@STServiceOperation
	public void setResourceViewMode(String resViewMode, PropertyLevel level)
			throws STPropertyUpdateException, STPropertyAccessException {
		if (resViewMode != "splitted" && resViewMode != "tabbed") {
			resViewMode = "tabbed"; // default
		}
		setPropertyValue(STPropertiesManager.PROP_RES_VIEW_MODE, resViewMode, level);
	}

	// utility methods

	/**
	 * Sets the value to the given property at the given level
	 * 
	 * @param property
	 * @param value
	 * @param level
	 * @throws STPropertyUpdateException
	 * @throws STPropertyAccessException
	 */
	private void setPropertyValue(String property, String value, PropertyLevel level)
			throws STPropertyUpdateException, STPropertyAccessException {
		if (level == PropertyLevel.USER) {
			STPropertiesManager.setUserProperty(UsersManager.getLoggedUser(), property, value);
		} else if (level == PropertyLevel.PROJECT) {
			STPropertiesManager.setProjectProperty(getProject().getName(), property, value);
		} else { // system
			STPropertiesManager.setSystemProperty(property, value);
		}
	}

	/**
	 * Gets the property value at the given level. If the property is not available at that level, looks for
	 * the superior level.
	 * 
	 * @param property
	 * @param level
	 * @return
	 * @throws STPropertyAccessException
	 */
	private String getPropertyValue(String property, PropertyLevel level) throws STPropertyAccessException {
		String value = null;
		if (level == PropertyLevel.USER) {
			value = getUserPropertyValue(property);
		} else if (level == PropertyLevel.PROJECT) {
			value = getProjectPropertyValue(property);
		} else if (level == PropertyLevel.SYSTEM) {
			value = getSystemPropertyValue(property);
		}
		return value;
	}

	/**
	 * Returns the property value at user level, if not available, returns the property value at project level
	 * 
	 * @param property
	 * @return
	 * @throws STPropertyAccessException
	 */
	private String getUserPropertyValue(String property) throws STPropertyAccessException {
		return STPropertiesManager.getUserPropertyWithFallback(UsersManager.getLoggedUser(), property,
				getProject().getName());
	}

	/**
	 * Returns the property value at project level, if not available, returns the property value at system
	 * level
	 * 
	 * @param property
	 * @return
	 * @throws STPropertyAccessException
	 */
	private String getProjectPropertyValue(String property) throws STPropertyAccessException {
		return STPropertiesManager.getProjectProperty(getProject().getName(), property, true);
	}

	private String getSystemPropertyValue(String property) throws STPropertyAccessException {
		return STPropertiesManager.getSystemProperty(property);
	}

}
