package it.uniroma2.art.semanticturkey.extension.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.uniroma2.art.semanticturkey.extension.IdentifiableComponent;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersGroup;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 */
@JsonIgnoreProperties(allowGetters = true)
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
		if (this instanceof PGSettingsManager<?>) {
			scopes.add(Scope.PROJECT_GROUP);
		}
		return scopes;
	}

	default Settings getSettings(Project project, STUser user, UsersGroup group, Scope scope) throws STPropertyAccessException {
		return this.getSettings(project, user, group, scope, false);
	}

	default Settings getSettings(Project project, STUser user, UsersGroup group, Scope scope, boolean explicit)
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
		case PROJECT_GROUP:
			return ((PGSettingsManager<?>) this).getProjectSettings(project, group, explicit);
		default:
			throw new IllegalArgumentException("Unrecognized scope: " + scope); // it should not happen
		}
	}

	default Settings getSettingsDefault(Project project, STUser user, UsersGroup group, Scope scope, Scope defaultScope)
			throws STPropertyAccessException {
		switch (scope) {
			case PROJECT:
				return ((ProjectSettingsManager<?>) this).getProjectSettingsDefault();
			case USER:
				return ((UserSettingsManager<?>) this).getUserSettingsDefault();
			case PROJECT_USER:
				switch (defaultScope) {
					case PROJECT:
						return ((PUSettingsManager<?>) this).getPUSettingsProjectDefault(project);
					case USER:
						return ((PUSettingsManager<?>) this).getPUSettingsUserDefault(user);
					case SYSTEM:
						return ((PUSettingsManager<?>) this).getPUSettingsSystemDefault();
					default:
						throw new IllegalArgumentException("Unrecognized scope for PU settings default: " + defaultScope); // it should not happen
				}
			default:
				throw new IllegalArgumentException("Unrecognized scope: " + scope); // it should not happen
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	default void storeSettings(Project project, STUser user, UsersGroup group, Scope scope, Settings settings)
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
		case PROJECT_GROUP:
				((PGSettingsManager) this).storeProjectSettings(project, group, settings);
			break;
		default:
			throw new IllegalArgumentException("Unrecognized scope: " + scope); // it should not happen
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	default void storeSettingsDefault(Project project, STUser user, UsersGroup group, Scope scope, Scope defaultScope, Settings settings)
			throws STPropertyUpdateException {
		switch (scope) {
			case PROJECT:
				((ProjectSettingsManager) this).storeProjectSettingsDefault(settings);
				break;
			case USER:
				((UserSettingsManager) this).storeUserSettingsDefault(settings);
				break;
			case PROJECT_USER:
				switch (defaultScope) {
					case PROJECT:
						((PUSettingsManager) this).storePUSettingsProjectDefault(project, settings);
						break;
					case USER:
						((PUSettingsManager) this).storePUSettingsUserDefault(user, settings);
						break;
					case SYSTEM:
						((PUSettingsManager) this).storePUSettingsSystemDefault(settings);
						break;
					default:
						throw new IllegalArgumentException("Unrecognized scope for PU settings default: " + defaultScope); // it should not happen
				}
				break;
			default:
				throw new IllegalArgumentException("Unrecognized scope: " + scope); // it should not happen
		}
	}

}
