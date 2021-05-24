package it.uniroma2.art.semanticturkey.settings.events;

import it.uniroma2.art.semanticturkey.event.Event;
import it.uniroma2.art.semanticturkey.event.TransactionalEventListener;
import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.extension.settings.SettingsManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersGroup;
import org.eclipse.rdf4j.repository.Repository;

/**
 * A {@link Event} raised when a settings has been updated. Raised by the diverse {@code storeXXXSettings} in
 * {@link it.uniroma2.art.semanticturkey.extension.settings.SettingsManager} and its specializations, there is
 * no guarantee that the settings have been actually changed.
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class SettingsUpdated extends SettingsEvent {

    public SettingsUpdated(SettingsManager settingsManager, Project project, STUser user, UsersGroup group, Scope scope, Settings settings) {
        super(settingsManager, project, user, group, scope, settings);
    }

}
