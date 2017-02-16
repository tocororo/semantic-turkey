package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import it.uniroma2.art.semanticturkey.properties.PropertyLevel;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.STUser;

@STService
public class Preferences extends STServiceAdapter {
	
	@STServiceOperation
	public Collection<String> getLanguages(PropertyLevel level) throws STPropertyAccessException {
		Collection<String> languages = new ArrayList<>();
		String value = getPropertyValue(STPropertiesManager.PROP_LANGUAGES, level);
		if (value != null) {
			value.replaceAll(" ", ""); //remove all spaces
			String[] splitted = value.split(",");
			for (int i = 0; i < splitted.length; i++) {
				languages.add(splitted[i]);
			}
		}
		return languages;
	}
	
	@STServiceOperation
	public void setLanguages(Collection<String> languages, PropertyLevel level) throws STPropertyUpdateException, STPropertyAccessException {
		String value = "";
		if (languages.size() == 1) {
			value = languages.iterator().next();
		} else {
			value = String.join(",", languages); 
		}
		setPropertyValue(STPropertiesManager.PROP_LANGUAGES, value, level);
	}
	
	
	//utility methods
	
	/**
	 * Sets the value to the given property at the given level
	 * @param property
	 * @param value
	 * @param level
	 * @throws STPropertyUpdateException
	 * @throws STPropertyAccessException 
	 */
	private void setPropertyValue(String property, String value, PropertyLevel level) throws STPropertyUpdateException, STPropertyAccessException {
		if (level == PropertyLevel.USER) {
			STPropertiesManager.setUserProperty(getLoggedUser(), property, value);
		} else if (level == PropertyLevel.PROJECT) {
			STPropertiesManager.setProjectProperty(getProject().getName(), property, value);
		} else { //system
			STPropertiesManager.setSystemProperty(property, value);
		}
	}
	
	/**
	 * Gets the property value at the given level. If the property is not available at that level, looks for
	 * the superior level.
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
	 * @param property
	 * @return
	 * @throws STPropertyAccessException 
	 */
	private String getUserPropertyValue(String property) throws STPropertyAccessException {
		String value;
		value = STPropertiesManager.getUserProperty(getLoggedUser(), property);
		if (value == null) {
			value = getProjectPropertyValue(property);
		}
		return value;
	}
	
	/**
	 * Returns the property value at project level, if not available, returns the property value at system level
	 * @param property
	 * @return
	 * @throws STPropertyAccessException 
	 */
	private String getProjectPropertyValue(String property) throws STPropertyAccessException {
		String value;
		value = STPropertiesManager.getProjectProperty(getProject().getName(), property);
		if (value == null) {
			value = getSystemPropertyValue(property);
		}
		return value;
	}
	
	private String getSystemPropertyValue(String property) throws STPropertyAccessException {
		return STPropertiesManager.getSystemProperty(property);
	}
	
	/**
	 * This method will never returns null since if the user is not logged, the services are intercepted by the 
	 * security filter 
	 * @return
	 */
	private STUser getLoggedUser() {
		STUser loggedUser = null;
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (!(auth instanceof AnonymousAuthenticationToken)) {//if there's a user authenticated
			loggedUser = (STUser) auth.getPrincipal();
		}
		return loggedUser;
	}
	
}
