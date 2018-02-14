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
public interface PUSettingsManager<T extends Settings> extends SettingsManager {

	default T getProjectSettings(Project project, STUser user) throws STPropertyAccessException {
		try {
			@SuppressWarnings("unchecked")
			T settings = (T) ReflectionUtilities
					.getInterfaceArgumentTypeAsClass(getClass(), PUSettingsManager.class, 0).newInstance();
			STPropertiesManager.getProjectPreferences(settings, project, user, getId());
			return settings;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new STPropertyAccessException(e);
		}
	}

	default void storeProjectSettings(Project project, STUser user, T settings)
			throws STPropertyUpdateException {
		STPropertiesManager.setProjectPreferences(settings, project, user, getId(), true);
	}

}
