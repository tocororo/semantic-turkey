package it.uniroma2.art.semanticturkey.plugin;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * Abstract base-class of {@link Plugin} implementations.
 * 
 */
public class AbstractPlugin<Q extends STProperties, R extends STProperties, S extends STProperties, T extends STProperties, F extends PluginFactory<?, Q, R, S, T>>
		implements Plugin {
	private F factory;

	public AbstractPlugin(F factory) {
		this.factory = factory;
	}

	protected Q getExtensionPointProjectSettings(Project project) throws STPropertyAccessException {
		return factory.getExtensonPointProjectSettings(project);
	}

	protected R getClassLevelProjectSettings(Project project) throws STPropertyAccessException {
		return factory.getProjectSettings(project);
	}

	protected void storeClassLevelProjectSettings(Project project, R settings)
			throws STPropertyUpdateException {
		factory.storeProjectSettings(project, settings);
	}

	protected S getExtensionPointProjectPreferences(Project project) throws STPropertyAccessException {
		return factory.getExtensonPointProjectPreferences(project);
	}

	protected T getClassLevelProjectPreferences(Project project, STUser user)
			throws STPropertyAccessException {
		return factory.getProjectPreferences(project, user);
	}

	protected void storeClassLevelProjectPreferences(Project project, STUser user, T preferences)
			throws STPropertyAccessException, STPropertyUpdateException {
		factory.storeProjectPreferences(project, user, preferences);
	}

}
