package it.uniroma2.art.semanticturkey.extension.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.uniroma2.art.semanticturkey.extension.settings.impl.SettingsSupport;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.settings.events.SettingsDefaultsUpdated;
import it.uniroma2.art.semanticturkey.settings.events.SettingsUpdated;
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

	@JsonIgnore
	default T getProjectSettingsDefault() throws STPropertyAccessException {
		return STPropertiesManager.getProjectSettingsDefault(ReflectionUtilities.getInterfaceArgumentTypeAsClass(
				getClass(), ProjectSettingsManager.class, 0), getId());
	}

	default void storeProjectSettings(Project project, T settings) throws STPropertyUpdateException {
		STPropertiesManager.setProjectSettings(settings, project, getId(), true);
		SettingsSupport.getEventPublisher().publishEvent(new SettingsUpdated(this, project, null, null, Scope.PROJECT, settings));
	}

	default void storeProjectSettingsDefault(T settings) throws STPropertyUpdateException {
		STPropertiesManager.setProjectSettingsDefault(settings, getId(), true);
		SettingsSupport.getEventPublisher().publishEvent(new SettingsDefaultsUpdated(this, null, null, null, Scope.PROJECT, Scope.SYSTEM, settings));
	}
}