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
		return getProjectSettings(project, false);
	}

	default T getProjectSettings(Project project, boolean explicit) throws STPropertyAccessException {
		return STPropertiesManager.getProjectSettings(ReflectionUtilities.getInterfaceArgumentTypeAsClass(
				getClass(), ProjectSettingsManager.class, 0), project, getId(), explicit);

	}

	default T getProjectSettingsDefault(Project project) throws STPropertyAccessException {
		return STPropertiesManager.getProjectSettingsDefault(ReflectionUtilities.getInterfaceArgumentTypeAsClass(
				getClass(), ProjectSettingsManager.class, 0), project, getId());
	}

	default void storeProjectSettings(Project project, T settings) throws STPropertyUpdateException {
		STPropertiesManager.setProjectSettings(settings, project, getId(), true);
	}

	default void storeProjectSettingsDefault(T settings) throws STPropertyUpdateException {
		STPropertiesManager.setProjectSettingsDefault(settings, getId(), true);
	}
}