package it.uniroma2.art.semanticturkey.extension.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import it.uniroma2.art.semanticturkey.extension.IdentifiableComponent;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 */
public interface SettingsManager extends IdentifiableComponent {

	default Collection<Scope> getSettingsScopes() {
		List<Scope> scopes = new ArrayList<>(Scope.values().length);
		if (this instanceof SystemSettingsManager<?>) {
			scopes.add(Scope.SYSTEM);
		}
		if (this instanceof ProjectSettingsManager<?>) {
			scopes.add(Scope.PROJECT);
		}
		if (this instanceof UserSettingsManager<?>) {
			scopes.add(Scope.USER);
		}
		if (this instanceof PUSettingsManager<?>) {
			scopes.add(Scope.PROJECT_USER);
		}
		return scopes;
	}

	default Settings getSettings(Project project, STUser user, Scope scope) throws STPropertyAccessException {
		return this.getSettings(project, user, scope, false);
	}

	default Settings getSettings(Project project, STUser user, Scope scope, boolean explicit)
			throws STPropertyAccessException {
		switch (scope) {
		case SYSTEM:
			return ((SystemSettingsManager<?>) this).getSystemSettings(explicit);
		case PROJECT:
			return ((ProjectSettingsManager<?>) this).getProjectSettings(project, explicit);
		case USER:
			return ((UserSettingsManager<?>) this).getUserSettings(user, explicit);
		case PROJECT_USER:
			return ((PUSettingsManager<?>) this).getProjectSettings(project, user, explicit);
		default:
			throw new IllegalArgumentException("Unrecognized scope: " + scope); // it should not happen
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	default void storeSettings(Project project, STUser user, Scope scope, Settings settings)
			throws STPropertyUpdateException {
		switch (scope) {
		case SYSTEM:
			((SystemSettingsManager) this).storeSystemSettings(settings);
			break;
		case PROJECT:
			((ProjectSettingsManager) this).storeProjectSettings(project, settings);
			break;
		case USER:
			((UserSettingsManager) this).storeUserSettings(user, settings);
			break;
		case PROJECT_USER:
			((PUSettingsManager) this).storeProjectSettings(project, user, settings);
			break;
		default:
			throw new IllegalArgumentException("Unrecognized scope: " + scope); // it should not happen
		}
	}
}
