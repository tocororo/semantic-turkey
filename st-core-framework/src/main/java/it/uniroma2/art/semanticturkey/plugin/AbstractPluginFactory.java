package it.uniroma2.art.semanticturkey.plugin;

import java.util.Map;
import java.util.function.Supplier;

import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;

/**
 * Abstract base class of concrete {@link PluginFactory} implementations.
 * 
 * @param <T>
 */
public abstract class AbstractPluginFactory<T extends PluginConfiguration, Q extends STProperties, R extends STProperties>
		implements PluginFactory<T, Q, R> {

	private String extensionPointId;

	public AbstractPluginFactory(String extensionPointId) {
		this.extensionPointId = extensionPointId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.uniroma2.art.semanticturkey.plugin.PluginFactory#getID()
	 */
	@Override
	public String getID() {
		return this.getClass().getName();
	}

	protected abstract R buildProjectSettingsInternal();

	protected abstract Q buildExtensionPointProjectSettingsInternal();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.uniroma2.art.semanticturkey.plugin.PluginFactory#getProjectSettings(it.uniroma2.art.semanticturkey.
	 * project.Project)
	 */
	@Override
	public R getProjectSettings(Project project) throws STPropertyAccessException {
		R projectSettings = buildProjectSettingsInternal();
		STPropertiesManager.getProjectSettings(projectSettings, project, getID());
		return projectSettings;
	};

	@Override
	public Q getExtensonPointProjectSettings(Project project) throws STPropertyAccessException {
		Q projectSettings = buildExtensionPointProjectSettingsInternal();
		STPropertiesManager.getProjectSettings(projectSettings, project, extensionPointId);
		return projectSettings;
	}

	@Override
	public void storeProjectSettings(Project project, Map<String, Object> settings)
			throws STPropertyUpdateException, STPropertyAccessException {
		storeProjectSettingInternal(project, settings, this::buildProjectSettingsInternal, getID());
	}

	@Override
	public void storeProjectSettings(Project project, STProperties settings)
			throws STPropertyUpdateException {
		storeProjectSettingInternal(project, settings, getID());
	}

	@Override
	public void storeExtensonPointProjectSettings(Project project, Map<String, Object> settings)
			throws STPropertyUpdateException, STPropertyAccessException {
		storeProjectSettingInternal(project, settings, this::buildExtensionPointProjectSettingsInternal,
				extensionPointId);
	}

	@Override
	public void storeExtensonPointProjectSettings(Project project, STProperties settings)
			throws STPropertyUpdateException {
		storeProjectSettingInternal(project, settings, extensionPointId);
	}

	public void storeProjectSettingInternal(Project project, STProperties settings, String pluginId)
			throws STPropertyUpdateException {
		STPropertiesManager.setProjectSettings(settings, project, getID());
	}

	public void storeProjectSettingInternal(Project project, Map<String, Object> settings,
			Supplier<? extends STProperties> propertiesSupplier, String pluginId)
			throws STPropertyUpdateException, STPropertyAccessException {
		try {
			STProperties settingsObject = propertiesSupplier.get();

			for (Map.Entry<String, Object> entry : settings.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				settingsObject.setPropertyValue(key, value);
			}

			STPropertiesManager.setProjectSettings(settingsObject, project, pluginId);
		} catch (WrongPropertiesException e) {
			throw new STPropertyUpdateException(e);
		}
	}
}
