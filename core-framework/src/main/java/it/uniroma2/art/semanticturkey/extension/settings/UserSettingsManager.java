package it.uniroma2.art.semanticturkey.extension.settings;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <T>
 */
public interface UserSettingsManager<T extends Settings> extends SettingsManager {

	default T getUserSettings(STUser user) throws STPropertyAccessException {
		return getUserSettings(user, false);
	}

	default T getUserSettings(STUser user, boolean explicit) throws STPropertyAccessException {
		return STPropertiesManager.getUserSettings(
				ReflectionUtilities.getInterfaceArgumentTypeAsClass(getClass(), UserSettingsManager.class, 0),
				user, getId(), explicit);
	}

	default void storeUserSettings(STUser user, T settings) throws STPropertyUpdateException {
		STPropertiesManager.setUserSettings(settings, user, getId(), true);
	}

	default void storeUserSettingsDefault(T settings) throws STPropertyUpdateException {
		STPropertiesManager.setUserSettingsDefault(settings, getId(), true);
	}


}
