package it.uniroma2.art.semanticturkey.extension.settings;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <T>
 */
public interface ProjectSettingsManager<T extends Settings> extends SettingsManager {

	default T getProjectSettings(Project project /* , String identifier */) throws STPropertyAccessException {
		try {
			@SuppressWarnings("unchecked")
			T settings = (T) ReflectionUtilities
					.getInterfaceArgumentTypeAsClass(getClass(), ProjectSettingsManager.class, 0)
					.newInstance();
			STPropertiesManager.getProjectSettings(settings, project, getId());
			return settings;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new STPropertyAccessException(e);
		}
	}

	default void storeProjectSettings(Project project, T settings) throws STPropertyUpdateException {
		STPropertiesManager.setProjectSettings(settings, project, getId(), true);
	}

}
