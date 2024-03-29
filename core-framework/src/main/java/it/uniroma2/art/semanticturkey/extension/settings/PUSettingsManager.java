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

	default T getProjectSettings(Project project, STUser user, boolean explicit)
			throws STPropertyAccessException {
		return STPropertiesManager.getPUSettings(
				ReflectionUtilities.getInterfaceArgumentTypeAsClass(getClass(), PUSettingsManager.class, 0),
				project, user, getId(), explicit);
	}

	default T getPUSettingsProjectDefault(Project project) throws STPropertyAccessException {
		return STPropertiesManager.getPUSettingsProjectDefault(
				ReflectionUtilities.getInterfaceArgumentTypeAsClass(getClass(), PUSettingsManager.class, 0),
				project, getId());
	}

	default T getPUSettingsUserDefault(STUser user) throws STPropertyAccessException {
		return STPropertiesManager.getPUSettingsUserDefault(
				ReflectionUtilities.getInterfaceArgumentTypeAsClass(getClass(), PUSettingsManager.class, 0),
				user, getId());
	}

	@JsonIgnore
	default T getPUSettingsSystemDefault() throws STPropertyAccessException {
		return STPropertiesManager.getPUSettingsSystemDefault(
				ReflectionUtilities.getInterfaceArgumentTypeAsClass(getClass(), PUSettingsManager.class, 0), getId());
	}

	default void storeProjectSettings(Project project, STUser user, T settings)
			throws STPropertyUpdateException {
		STPropertiesManager.setPUSettings(settings, project, user, getId(), true);
		SettingsSupport.getEventPublisher().publishEvent(new SettingsUpdated(this, project, user, null, Scope.PROJECT_USER, settings));
	}

	default void storePUSettingsUserDefault(STUser user, T settings) throws STPropertyUpdateException {
		STPropertiesManager.setPUSettingsUserDefault(settings, user, getId(), true);
		SettingsSupport.getEventPublisher().publishEvent(new SettingsDefaultsUpdated(this, null, user, null, Scope.PROJECT_USER, Scope.USER, settings));
	}

	default void storePUSettingsProjectDefault(Project project, T settings) throws STPropertyUpdateException {
		STPropertiesManager.setPUSettingsProjectDefault(settings, project, getId(), true);
		SettingsSupport.getEventPublisher().publishEvent(new SettingsDefaultsUpdated(this, project, null, null, Scope.PROJECT_USER, Scope.PROJECT, settings));
	}

	default void storePUSettingsSystemDefault(T settings) throws STPropertyUpdateException {
		STPropertiesManager.setPUSettingsSystemDefault(settings, getId(), true);
		SettingsSupport.getEventPublisher().publishEvent(new SettingsDefaultsUpdated(this, null, null, null, Scope.PROJECT_USER, Scope.SYSTEM, settings));
	}

}
