package it.uniroma2.art.semanticturkey.extension.settings;

import it.uniroma2.art.semanticturkey.extension.settings.impl.SettingsSupport;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.settings.events.SettingsUpdated;
import it.uniroma2.art.semanticturkey.user.UsersGroup;
import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <T>
 */
public interface PGSettingsManager<T extends Settings> extends SettingsManager
{
    default T getProjectSettings(Project project, UsersGroup group) throws STPropertyAccessException {
        return STPropertiesManager.getPGSettings(
                ReflectionUtilities.getInterfaceArgumentTypeAsClass(getClass(), PGSettingsManager.class, 0),
                project, group, getId(), false);
    }

    default T getProjectSettings(Project project, UsersGroup group, boolean explicit)
            throws STPropertyAccessException {
        return STPropertiesManager.getPGSettings(
                ReflectionUtilities.getInterfaceArgumentTypeAsClass(getClass(), PGSettingsManager.class, 0),
                project, group, getId(), explicit);
    }


    default void storeProjectSettings(Project project, UsersGroup group, T settings)
            throws STPropertyUpdateException {
        STPropertiesManager.setPGSettings(settings, project, group, getId(), true);
        SettingsSupport.getEventPublisher().publishEvent(new SettingsUpdated(this, project, null, group, Scope.PROJECT_GROUP, settings));
    }

}
