package it.uniroma2.art.semanticturkey.extension.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.uniroma2.art.semanticturkey.extension.settings.impl.SettingsSupport;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.settings.events.SettingsDefaultsUpdated;
import it.uniroma2.art.semanticturkey.settings.events.SettingsUpdated;
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

	@JsonIgnore
	default T getUserSettingsDefault() throws STPropertyAccessException {
		return STPropertiesManager.getUserSettingsDefault(ReflectionUtilities.getInterfaceArgumentTypeAsClass(getClass(), UserSettingsManager.class, 0), getId());
	}


	default void storeUserSettings(STUser user, T settings) throws STPropertyUpdateException {
		STPropertiesManager.setUserSettings(settings, user, getId(), true);
		SettingsSupport.getEventPublisher().publishEvent(new SettingsUpdated(this, null, user, null, Scope.USER, settings));
	}

	default void storeUserSettingsDefault(T settings) throws STPropertyUpdateException {
		STPropertiesManager.setUserSettingsDefault(settings, getId(), true);
		SettingsSupport.getEventPublisher().publishEvent(new SettingsDefaultsUpdated(this, null, null, null, Scope.USER, Scope.SYSTEM, settings));
	}


}
