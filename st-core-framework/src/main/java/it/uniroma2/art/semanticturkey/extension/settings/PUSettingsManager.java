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
		return STPropertiesManager.getPUSettings(
				ReflectionUtilities.getInterfaceArgumentTypeAsClass(getClass(), PUSettingsManager.class, 0),
				project, user, getId());
	}

	default void storeProjectSettings(Project project, STUser user, T settings)
			throws STPropertyUpdateException {
		STPropertiesManager.setPUSettings(settings, project, user, getId(), true);
	}

}
