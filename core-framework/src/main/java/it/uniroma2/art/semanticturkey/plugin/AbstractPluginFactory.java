package it.uniroma2.art.semanticturkey.plugin;

import java.util.Map;
import java.util.function.Supplier;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * Abstract base class of concrete {@link PluginFactory} implementations.
 * 
 * @param <T>
 */
public abstract class AbstractPluginFactory<T extends STProperties, Q extends STProperties, R extends STProperties, P extends STProperties, S extends STProperties>
		implements PluginFactory<T, Q, R, P, S> {

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

	protected abstract S buildProjectPreferencesInternal();

	protected abstract P buildExtensionPointProjectPreferencesInternal();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.uniroma2.art.semanticturkey.plugin.PluginFactory#getProjectSettings(it.uniroma2.art.semanticturkey.
	 * project.Project)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public R getProjectSettings(Project project) throws STPropertyAccessException {
		R projectSettings = buildProjectSettingsInternal();
		return (R) STPropertiesManager.getProjectSettings(projectSettings.getClass(), project, getID());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Q getExtensonPointProjectSettings(Project project) throws STPropertyAccessException {
		Q projectSettings = buildExtensionPointProjectSettingsInternal();
		return (Q) STPropertiesManager.getProjectSettings(projectSettings.getClass(), project,
				extensionPointId);
	}

	@SuppressWarnings("unchecked")
	@Override
	public S getProjectPreferences(Project project, STUser user) throws STPropertyAccessException {
		S projectPreferences = buildProjectPreferencesInternal();
		return (S) STPropertiesManager.getPUSettings(projectPreferences.getClass(), project, user,
				getID());
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
	public void storeProjectPreferences(Project project, STUser user, STProperties settings)
			throws STPropertyUpdateException {
		storeProjectPreferencesInternal(project, user, settings, getID());
	}

	@Override
	public void storeProjectPreferences(Project project, STUser user, Map<String, Object> settings)
			throws STPropertyUpdateException, STPropertyAccessException {
		storeProjectPreferencesInternal(project, user, settings, this::buildProjectPreferencesInternal,
				getID());
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

	public void storeProjectPreferencesInternal(Project project, STUser user, STProperties settings,
			String pluginId) throws STPropertyUpdateException {
		STPropertiesManager.setPUSettings(settings, project, user, pluginId);
	}

	public void storeProjectPreferencesInternal(Project project, STUser user, Map<String, Object> preferences,
			Supplier<? extends STProperties> propertiesSupplier, String pluginId)
			throws STPropertyUpdateException, STPropertyAccessException {
		try {
			STProperties preferencesObject = propertiesSupplier.get();

			for (Map.Entry<String, Object> entry : preferences.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				preferencesObject.setPropertyValue(key, value);
			}

			STPropertiesManager.setPUSettings(preferencesObject, project, user, pluginId);
		} catch (WrongPropertiesException e) {
			throw new STPropertyUpdateException(e);
		}
	}

}
