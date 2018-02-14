package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * This class provides services for handling settings.
 */
@STService
public class Settings extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Settings.class);

	@Autowired
	private ExtensionPointManager exptManager;

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
		return exptManager.getSettings(getProject(), UsersManager.getLoggedUser(), componentID, scope);
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
	public void storeSettings(String componentID, Scope scope, Map<String, Object> settings)
			throws NoSuchSettingsManager, STPropertyAccessException, IllegalStateException,
			STPropertyUpdateException, WrongPropertiesException {
		exptManager.storeSettings(componentID, getProject(), UsersManager.getLoggedUser(), scope, settings);
	}

}