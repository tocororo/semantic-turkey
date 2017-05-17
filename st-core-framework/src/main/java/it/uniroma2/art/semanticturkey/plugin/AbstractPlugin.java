package it.uniroma2.art.semanticturkey.plugin;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;

/**
 * Abstract base-class of {@link Plugin} implementations.
 * 
 */
public class AbstractPlugin<Q extends STProperties, R extends STProperties, T extends PluginFactory<?, Q, R>>
		implements Plugin {
	private T factory;

	public AbstractPlugin(T factory) {
		this.factory = factory;
	}

	protected Q getExtensionPointProjectSettings(Project<?> project) throws STPropertyAccessException {
		return factory.getExtensonPointProjectSettings(project);
	}

	protected R getClassLevelProjectSettings(Project<?> project) throws STPropertyAccessException {
		return factory.getProjectSettings(project);
	}

}
