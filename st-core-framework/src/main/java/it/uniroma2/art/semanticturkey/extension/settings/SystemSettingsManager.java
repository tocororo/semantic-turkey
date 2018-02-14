package it.uniroma2.art.semanticturkey.extension.settings;

import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;

/**
 * 
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <T>
 */
public interface SystemSettingsManager<T extends Settings> extends SettingsManager {

	default T getSystemSettings() throws STPropertyAccessException {
		try {
			@SuppressWarnings("unchecked")
			T settings = (T) ReflectionUtilities
					.getInterfaceArgumentTypeAsClass(getClass(), SystemSettingsManager.class, 0)
					.newInstance();
			STPropertiesManager.getSystemSettings(settings, getId());
			return settings;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new STPropertyAccessException(e);
		}
	}

	default void storeSystemSettings(T settings) throws STPropertyUpdateException {
		STPropertiesManager.setSystemSettings(settings, getId(), true);
	}

}
