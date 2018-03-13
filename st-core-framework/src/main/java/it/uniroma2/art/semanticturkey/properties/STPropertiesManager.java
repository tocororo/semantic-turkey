package it.uniroma2.art.semanticturkey.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.user.STUser;

public class STPropertiesManager {

	// id of the "core plugin", used for preferences/properties that don't belong to any plugin
	public static final String CORE_PLUGIN_ID = "it.uniroma2.art.semanticturkey";

	private static final String SYSTEM_PREFERENCES_DEFAULTS_FILE_NAME = "system-preferences-defaults.props";
	private static final String SYSTEM_PROJECT_PREFERENCES_DEFAULTS_FILE_NAME = "project-preferences-defaults.props";
	private static final String SYSTEM_PROJECT_SETTINGS_DEFAULTS_FILE_NAME = "project-settings-defaults.props";
	private static final String SYSTEM_SETTINGS_FILE_NAME = "settings.props";

	private static final String PROJECT_PREFERENCES_DEFAULTS_FILE_NAME = "preferences-defaults.props";
	private static final String PROJECT_SETTINGS_FILE_NAME = "settings.props";

	private static final String PU_BINDING_PREFERENCES_FILE_NAME = "preferences.props";

	private static final String USER_PROJECT_PREFERENCES_DEFAULTS_FILE_NAME = "project-preferences-defaults.props";
	private static final String USER_SYSTEM_PREFERENCES_FILE_NAME = "system-preferences.props";

	public static final String PREF_LANGUAGES = "languages";
	public static final String PREF_SHOW_FLAGS = "show_flags";
	public static final String PREF_SHOW_INSTANCES_NUMBER = "show_instances_number";
	public static final String PREF_ACTIVE_SCHEMES = "active_schemes";
	public static final String PREF_PROJ_THEME = "project_theme";

	public static final String SETTING_EMAIL_ADMIN_ADDRESS = "mail.admin.address";
	public static final String SETTING_EMAIL_FROM_ADDRESS = "mail.from.address";
	public static final String SETTING_EMAIL_FROM_PASSWORD = "mail.from.password";
	public static final String SETTING_EMAIL_FROM_ALIAS = "mail.from.alias";
	public static final String SETTING_EMAIL_FROM_HOST = "mail.from.host";
	public static final String SETTING_EMAIL_FROM_PORT = "mail.from.port";

	public static final String SETTING_PROJ_LANGUAGES = "languages";
	public static final String SETTING_EXP_FEATURES_ENABLED = "experimental_features_enabled";

	/*
	 * Methods to get/set properties to/from the following Properties files
	 * 
	 * PP: project-preference: user-project --> default(pp,project) --> default(pp,user) -->
	 * default(pp,system) getProjectPreference(prop, project, user, pluginID)
	 * pu_binding/<projectname>/<username>/plugins/<plugin>/preferences.props
	 * getProjectPreferenceDefault(prop, project, pluginID)
	 * projects/<projectname>/plugins/<plugin>/preference-defaults.props getProjectPreferenceDefault(prop,
	 * user, pluginID) users/<username>/plugins/<plugin>/project-preference-defaults.props
	 * getProjectPreferenceDefault(prop, pluginID) system/plugins/<plugin>/project-preference-defaults.props
	 * 
	 * SP: system-preference: user --> default(sp,system) getSystemPreference(prop, user, pluginID)
	 * users/<username>/plugins/<plugin>/system-preferences.props getSystemPreferenceDefault(prop, pluginID)
	 * system/plugins/<plugin>/system-preference-defaults.props
	 * 
	 * PS: project-settings: project --> default(ps, system) getProjectSetting(prop, project, pluginID)
	 * projects/<projectname>/plugins/<plugin>/settings.props getProjectSettingDefault(prop, pluginID)
	 * system/plugins/<plugin>/project-settings-defaults.props
	 * 
	 * SS: system-settings: system getSystemSetting(prop, pluginID) system/plugins/<plugin>/settings.props
	 */

	/*
	 * PROJECT PREFERENCE
	 */

	/*
	 * Getter/Setter <STData>/pu_binding/<projectname>/<username>/plugins/<plugin>/preferences.props
	 */

	/**
	 * Returns the value of a project preference for the given user. If the preference has no value for the
	 * user, it looks for the value in the following order: - the default value at project level - the default
	 * value at user level - the default value at system level Returns null if no value is defined at all
	 * 
	 * @param project
	 * @param user
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getProjectPreference(String propName, Project project, STUser user)
			throws STPropertyAccessException {
		return getProjectPreference(propName, project, user, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a project preference about a plugin for the given user. If the preference has no
	 * value for the user, it looks for the value in the following order: - the default value at project level
	 * - the default value at user level - the default value at system level Returns null if no value is
	 * defined at all
	 * 
	 * @param pluginID
	 * @param project
	 * @param user
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getProjectPreference(String propName, Project project, STUser user, String pluginID)
			throws STPropertyAccessException {
		// PP: project-preference: user-project -> default(pp,project) -> default(pp,user) ->
		// default(pp,system)
		String value;
		value = loadProperties(getPUBindingsPreferencesFile(project, user, pluginID)).getProperty(propName);
		if (value == null) {
			value = getProjectPreferenceDefault(propName, project, pluginID);
			if (value == null) {
				value = getProjectPreferenceDefault(propName, user, pluginID);
				if (value == null) {
					value = getProjectPreferenceDefault(propName, pluginID);
				}
			}
		}
		return value;
	}

	/**
	 * Returns the project preferences about a plugin. See
	 * {@link #getProjectPreference(String, Project, STUser, String)} for details about the lookup procedure.
	 * 
	 * @param projectPreferences
	 * @param project
	 * @param user
	 * @param pluginId
	 * @throws STPropertyAccessException
	 */
	public static <T extends STProperties> T getProjectPreferences(Class<T> valueType, Project project,
			STUser user, String pluginID) throws STPropertyAccessException {
		File defaultPropFile = getProjectPreferencesDefaultsFile(project, pluginID);
		File propFile = getPUBindingsPreferencesFile(project, user, pluginID);

		return loadSTPropertiesFromYAMLFiles(valueType, defaultPropFile, propFile);
	}

	/**
	 * Sets the value of a project preference for the given user
	 * 
	 * @param project
	 * @param user
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setProjectPreference(String propName, String propValue, Project project, STUser user)
			throws STPropertyUpdateException {
		setProjectPreference(propName, propValue, project, user, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a project preference for the given user
	 * 
	 * @param pluginID
	 * @param project
	 * @param user
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setProjectPreference(String propName, String propValue, Project project, STUser user,
			String pluginID) throws STPropertyUpdateException {
		try {
			File propFile = getPUBindingsPreferencesFile(project, user, pluginID);
			Properties properties = loadProperties(propFile);
			setProperty(properties, propName, propValue);
			updatePropertyFile(properties, propFile);
		} catch (STPropertyAccessException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/**
	 * Sets the values of project preferences related to a plugin for the given user.
	 * 
	 * @param preferences
	 * @param project
	 * @param user
	 * @param pluginID
	 * @param allowIncompletePropValueSet
	 */
	public static void setProjectPreferences(STProperties preferences, Project project, STUser user,
			String pluginID, boolean allowIncompletePropValueSet) throws STPropertyUpdateException {
		try {
			if (!allowIncompletePropValueSet) {
				STPropertiesChecker preferencesChecker = STPropertiesChecker
						.getModelConfigurationChecker(preferences);
				if (!preferencesChecker.isValid()) {
					throw new STPropertyUpdateException(
							"Preferences not valid: " + preferencesChecker.getErrorMessage());
				}
			}
			File propFile = getPUBindingsPreferencesFile(project, user, pluginID);
			storeSTPropertiesInYAML(preferences, propFile);
		} catch (STPropertyAccessException | IOException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/**
	 * Convenience overload of {@link #setProjectPreferences(STProperties, Project, STUser, String, boolean)}
	 * that disallows the storage of incomplete preferences (i.e. missing values for required property).
	 * 
	 * @param settings
	 * @param project
	 * @param user
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setProjectPreferences(STProperties settings, Project project, STUser user,
			String pluginID) throws STPropertyUpdateException {
		setProjectPreferences(settings, project, user, pluginID, false);
	}

	/*
	 * Getter/Setter <STData>/projects/<projectname>/plugins/<plugin>/preference-defaults.props
	 */

	/**
	 * Returns the value of a default project preference. Returns null if no value is defined
	 * 
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getProjectPreferenceDefault(String propName, Project project)
			throws STPropertyAccessException {
		return getProjectPreferenceDefault(propName, project, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a default project preference about a plugin. Returns null if no value is defined
	 * 
	 * @param pluginID
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getProjectPreferenceDefault(String propName, Project project, String pluginID)
			throws STPropertyAccessException {
		return loadProperties(getProjectPreferencesDefaultsFile(project, pluginID)).getProperty(propName);
	}

	/**
	 * Sets the value of a default project preference
	 * 
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setProjectPreferenceDefault(String propName, String propValue, Project project)
			throws STPropertyUpdateException {
		setProjectPreferenceDefault(propName, propValue, project, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a default project preference
	 * 
	 * @param propName
	 * @param propValue
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setProjectPreferenceDefault(String propName, String propValue, Project project,
			String pluginID) throws STPropertyUpdateException {
		try {
			File propFile = getProjectPreferencesDefaultsFile(project, pluginID);
			Properties properties = loadProperties(propFile);
			setProperty(properties, propName, propValue);
			updatePropertyFile(properties, propFile);
		} catch (STPropertyAccessException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/*
	 * Getter/Setter <STData>/users/<username>/plugins/<plugin>/project-preference-defaults.props
	 */
	/**
	 * Returns the value of a project setting at user level.
	 * 
	 * @param user
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getProjectPreferenceDefault(String propName, STUser user)
			throws STPropertyAccessException {
		return getProjectPreferenceDefault(propName, user, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a project setting at user level.
	 * 
	 * @param user
	 * @param pluginID
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getProjectPreferenceDefault(String propName, STUser user, String pluginID)
			throws STPropertyAccessException {
		return loadProperties(getUserProjectPreferencesDefaultsFile(user, pluginID)).getProperty(propName);
	}

	/**
	 * Sets the value of a project setting at user level.
	 * 
	 * @param user
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setProjectPreferenceDefault(String propName, String propValue, STUser user)
			throws STPropertyUpdateException {
		setProjectPreferenceDefault(propName, propValue, user, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a project setting at user level.
	 * 
	 * @param user
	 * @param propName
	 * @param propValue
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setProjectPreferenceDefault(String propName, String propValue, STUser user,
			String pluginID) throws STPropertyUpdateException {
		try {
			File propFile = getUserProjectPreferencesDefaultsFile(user, pluginID);
			Properties properties = loadProperties(propFile);
			setProperty(properties, propName, propValue);
			updatePropertyFile(properties, propFile);
		} catch (STPropertyAccessException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/*
	 * Getter/Setter <STData>/system/plugins/<plugin>/project-preference-defaults.props
	 */

	/**
	 * Returns the value of a default project preference at system level Returns null if no value is defined
	 * 
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getProjectPreferenceDefault(String propName) throws STPropertyAccessException {
		return getProjectPreferenceDefault(propName, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a default project preference about a plugin. Returns null if no value is defined
	 * 
	 * @param pluginID
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getProjectPreferenceDefault(String propName, String pluginID)
			throws STPropertyAccessException {
		return loadProperties(getSystemProjectPreferencesDefaultsFile(pluginID)).getProperty(propName);
	}

	/**
	 * Sets the value of a default project preference
	 * 
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setProjectPreferenceDefault(String propName, String propValue)
			throws STPropertyUpdateException {
		setProjectPreferenceDefault(propName, propValue, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a default project preference
	 * 
	 * @param propName
	 * @param propValue
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setProjectPreferenceDefault(String propName, String propValue, String pluginID)
			throws STPropertyUpdateException {
		try {
			File propFile = getSystemProjectPreferencesDefaultsFile(pluginID);
			Properties properties = loadProperties(propFile);
			setProperty(properties, propName, propValue);
			updatePropertyFile(properties, propFile);
		} catch (STPropertyAccessException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/*
	 * SYSTEM PREFERENCE
	 */

	/*
	 * Getter/Setter <STData>/users/<username>/plugins/<plugin>/system-preferences.props
	 */

	/**
	 * Returns the value of a system preference for the given user. If the preference has no value for the
	 * user, it returns the value at system level. Returns null if no value is defined at all
	 * 
	 * @param user
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getSystemPreference(String propName, STUser user) throws STPropertyAccessException {
		return getSystemPreference(propName, user, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a system preference about a plugin for the given user. If the preference has no
	 * value for the user, it returns the value at system level. Returns null if no value is defined at all
	 * 
	 * @param pluginID
	 * @param user
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getSystemPreference(String propName, STUser user, String pluginID)
			throws STPropertyAccessException {
		// SP: system-preference: user -> default(sp,system)
		String value;
		value = loadProperties(getUserSystemPreferencesFile(user, pluginID)).getProperty(propName);
		if (value == null) {
			value = getSystemPreferenceDefault(propName, pluginID);
		}
		return value;
	}

	public static <T extends STProperties> T getSystemPreferences(Class<T> valueType, STUser user,
			String pluginID) throws STPropertyAccessException {
		File propFile = getUserSystemPreferencesFile(user, pluginID);
		File defaultPropFile = getSystemPreferencesDefaultsFile(pluginID);
		return loadSTPropertiesFromYAMLFiles(valueType, defaultPropFile, propFile);
	}

	/**
	 * Sets the value of a system preference for the given user
	 * 
	 * @param user
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setSystemPreference(String propName, String propValue, STUser user)
			throws STPropertyUpdateException {
		setSystemPreference(propName, propValue, user, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a system preference for the given user
	 * 
	 * @param user
	 * @param propName
	 * @param propValue
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setSystemPreference(String propName, String propValue, STUser user, String pluginID)
			throws STPropertyUpdateException {
		try {
			File propFile = getUserSystemPreferencesFile(user, pluginID);
			Properties properties = loadProperties(propFile);
			setProperty(properties, propName, propValue);
			updatePropertyFile(properties, propFile);
		} catch (STPropertyAccessException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	public static void setSystemPreferences(STProperties preferences, STUser user, String pluginID)
			throws STPropertyUpdateException {
		setSystemPreferences(preferences, user, pluginID);
	}

	private static ObjectMapper createObjectMapper() {
		YAMLFactory fact = new YAMLFactory();
		fact.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
		fact.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);

		SimpleModule stPropsModule = new SimpleModule();
		stPropsModule.setMixInAnnotation(STProperties.class,
				it.uniroma2.art.semanticturkey.properties.yaml.STPropertiesPersistenceMixin.class);
		ObjectMapper mapper = new ObjectMapper(fact);
		mapper.registerModule(stPropsModule);
		return mapper;
	}

	public static void storeSTPropertiesInYAML(STProperties properties, File propertiesFile)
			throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = createObjectMapper();
		mapper.writeValue(propertiesFile, properties);
	}

	private static <T extends STProperties> T loadSTPropertiesFromYAMLFiles(Class<T> valueType,
			File... propFiles) throws STPropertyAccessException {
		try {
			T props = valueType.newInstance();

			ObjectMapper mapper = createObjectMapper();
			ObjectReader objReader = mapper.readerForUpdating(props);

			for (int i = 0; i < propFiles.length; i++) {
				File propFile = propFiles[i];
				if (!propFile.exists())
					continue;

				try (Reader reader = new InputStreamReader(new FileInputStream(propFile),
						StandardCharsets.UTF_8)) {
					objReader.readValue(reader);
				} catch (JsonMappingException e) {
					// Swallow exception due to empty property files
					if (!(e.getPath().isEmpty() && e.getMessage().contains("end-of-input"))) {
						throw e;
					}
				}

			}
			return props;
		} catch (IOException | InstantiationException | IllegalAccessException e) {
			throw new STPropertyAccessException(e);
		}
	}

	private static <T extends STProperties> T loadExistingSTPropertiesFromYAMLFile(Class<T> valueType,
			File propFile) throws STPropertyAccessException {
		try {
			if (!propFile.exists()) {
				throw new FileNotFoundException(propFile.getAbsolutePath());
			}

			ObjectMapper mapper = createObjectMapper();

			Class<?> deserialiationType;

			JsonNode tree = null;
			try (Reader reader = new InputStreamReader(new FileInputStream(propFile),
					StandardCharsets.UTF_8)) {
				tree = mapper.readTree(reader);
				JsonNode typeNode = tree.get("@type");
				if (typeNode != null) {
					String specificTypeName = typeNode.asText();
					Class<?> specificClass = valueType.getClassLoader().loadClass(specificTypeName);

					if (valueType.isAssignableFrom(specificClass)) {
						deserialiationType = specificClass;
					} else {
						throw new STPropertyAccessException(
								"Not an appropriate configuration type: " + specificTypeName);
					}
				} else {
					deserialiationType = valueType;
				}
			} catch (JsonMappingException e) {
				// Swallow exception due to empty property files
				if (!(e.getPath().isEmpty() && e.getMessage().contains("end-of-input"))) {
					throw e;
				}
				deserialiationType = valueType;
			}

			if (tree == null) {
				return (T) deserialiationType.newInstance();
			} else {
				((ObjectNode)tree).remove("@type");
			}
			
			return (T) mapper.treeToValue(tree, deserialiationType);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e) {
			throw new STPropertyAccessException(e);
		}

	}

	public static void setSystemPreferences(STProperties preferences, STUser user, String pluginID,
			boolean allowIncompletePropValueSet) throws STPropertyUpdateException {
		try {
			if (!allowIncompletePropValueSet) {
				STPropertiesChecker preferencesChecker = STPropertiesChecker
						.getModelConfigurationChecker(preferences);
				if (!preferencesChecker.isValid()) {
					throw new STPropertyUpdateException(
							"Preferences not valid: " + preferencesChecker.getErrorMessage());
				}
			}
			File settingsFile = getUserSystemPreferencesFile(user, pluginID);
			storeSTPropertiesInYAML(preferences, settingsFile);
		} catch (STPropertyAccessException | IOException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/*
	 * Getter/Setter <STData>/system/plugins/<plugin>/system-preference-defaults.props
	 */

	/**
	 * Returns the value of a default system preference. Returns null if no value is defined
	 * 
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getSystemPreferenceDefault(String propName) throws STPropertyAccessException {
		return getSystemPreferenceDefault(propName, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a default system preference about a plugin. Returns null if no value is defined
	 * 
	 * @param pluginID
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getSystemPreferenceDefault(String propName, String pluginID)
			throws STPropertyAccessException {
		return loadProperties(getSystemPreferencesDefaultsFile(pluginID)).getProperty(propName);
	}

	/**
	 * Sets the value of a default system preference
	 * 
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setSystemPreferenceDefault(String propName, String propValue)
			throws STPropertyUpdateException {
		setSystemPreferenceDefault(propName, propValue, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a default system preference
	 * 
	 * @param propName
	 * @param propValue
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setSystemPreferenceDefault(String propName, String propValue, String pluginID)
			throws STPropertyUpdateException {
		try {
			File propFile = getSystemPreferencesDefaultsFile(pluginID);
			Properties properties = loadProperties(propFile);
			setProperty(properties, propName, propValue);
			updatePropertyFile(properties, propFile);
		} catch (STPropertyAccessException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/*
	 * PROJECT SETTING
	 */

	/*
	 * Getter/Setter <STData>/projects/<projectname>/plugins/<plugin>/settings.props
	 */

	/**
	 * Returns the value of a project setting. If the setting has no value for the project, it returns the
	 * default value at system level. Returns null if no value is defined at all.
	 * 
	 * @param project
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getProjectSetting(String propName, Project project)
			throws STPropertyAccessException {
		return getProjectSetting(propName, project, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a project setting about a plugin. If the setting has no value for the project, it
	 * returns the default value at system level. Returns null if no value is defined at all.
	 * 
	 * @param pluginID
	 * @param project
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getProjectSetting(String propName, Project project, String pluginID)
			throws STPropertyAccessException {
		// PS: project-settings: project -> default(ps, system)
		String value;
		value = loadProperties(getProjectSettingsFile(project, pluginID)).getProperty(propName);
		if (value == null) {
			value = getProjectSettingDefault(propName, pluginID);
		}
		return value;
	}

	/**
	 * Returns the project settings about a plugin. The returned settings are (in descending order of
	 * priority):
	 * <ul>
	 * <li>the values stored in the project-settings for the plugin</li>
	 * <li>the default values stored at the system level</li>
	 * <li>the default value hard-wired in the provided {@link STProperties} object</li>
	 * </ul>
	 * 
	 * @param projectSettings
	 * @param project
	 * @param pluginId
	 * @throws STPropertyAccessException
	 */
	public static <T extends STProperties> T getProjectSettings(Class<T> valueType, Project project,
			String pluginID) throws STPropertyAccessException {
		File defaultPropFile = getSystemProjectSettingsDefaultsFile(pluginID);
		File propFile = getProjectSettingsFile(project, pluginID);

		return loadSTPropertiesFromYAMLFiles(valueType, defaultPropFile, propFile);
	}

	/**
	 * Sets the value of a project setting
	 * 
	 * @param project
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setProjectSetting(String propName, String propValue, Project project)
			throws STPropertyUpdateException {
		setProjectSetting(propName, propValue, project, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a project setting
	 * 
	 * @param project
	 * @param propName
	 * @param propValue
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setProjectSetting(String propName, String propValue, Project project, String pluginID)
			throws STPropertyUpdateException {
		try {
			File propFile = getProjectSettingsFile(project, pluginID);
			Properties properties = loadProperties(propFile);
			setProperty(properties, propName, propValue);
			updatePropertyFile(properties, propFile);
		} catch (STPropertyAccessException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/**
	 * Sets the values of project settings related to a plugin.
	 * 
	 * @param settings
	 * @param project
	 * @param pluginID
	 * @param allowIncompletePropValueSet
	 */
	public static void setProjectSettings(STProperties settings, Project project, String pluginID,
			boolean allowIncompletePropValueSet) throws STPropertyUpdateException {
		try {
			if (!allowIncompletePropValueSet) {
				STPropertiesChecker settingsChecker = STPropertiesChecker
						.getModelConfigurationChecker(settings);
				if (!settingsChecker.isValid()) {
					throw new STPropertyUpdateException(
							"Settings not valid: " + settingsChecker.getErrorMessage());
				}
			}
			File settingsFile = getProjectSettingsFile(project, pluginID);
			storeSTPropertiesInYAML(settings, settingsFile);
		} catch (STPropertyAccessException | IOException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/**
	 * Convenience overload of {@link #setProjectSettings(STProperties, Project, String, boolean)} that
	 * disallows the storage of incomplete settings (i.e. missing values for required property).
	 * 
	 * @param settings
	 * @param project
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setProjectSettings(STProperties settings, Project project, String pluginID)
			throws STPropertyUpdateException {
		setProjectSettings(settings, project, pluginID, false);
	}

	/*
	 * Getter/Setter <STData>/system/plugins/<plugin>/project-settings-defaults.props
	 */

	/**
	 * Returns the value of a default project setting at system level Returns null if no value is defined
	 * 
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getProjectSettingDefault(String propName) throws STPropertyAccessException {
		return getProjectSettingDefault(propName, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a default project setting about a plugin at system level Returns null if no value
	 * is defined
	 * 
	 * @param pluginID
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getProjectSettingDefault(String propName, String pluginID)
			throws STPropertyAccessException {
		return loadProperties(getSystemProjectSettingsDefaultsFile(pluginID)).getProperty(propName);
	}

	/**
	 * Sets the value of a default project setting at system level
	 * 
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setProjectSettingsDefault(String propName, String propValue)
			throws STPropertyUpdateException {
		setProjectSettingsDefault(propName, propValue, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a default project setting at system level
	 * 
	 * @param propName
	 * @param propValue
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setProjectSettingsDefault(String propName, String propValue, String pluginID)
			throws STPropertyUpdateException {
		try {
			File propFile = getSystemProjectSettingsDefaultsFile(pluginID);
			Properties properties = loadProperties(propFile);
			setProperty(properties, propName, propValue);
			updatePropertyFile(properties, propFile);
		} catch (STPropertyAccessException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/*
	 * SYSTEM SETTING
	 */

	/*
	 * Getter/Setter <STData>/system/plugins/<plugin>/settings.props
	 */

	/**
	 * Returns the value of a system setting. Returns null if no value is defined
	 * 
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getSystemSetting(String propName) throws STPropertyAccessException {
		return getSystemSetting(propName, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a system setting about a plugin. Returns null if no value is defined
	 * 
	 * @param pluginID
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getSystemSetting(String propName, String pluginID) throws STPropertyAccessException {
		// SS: system-settings: system
		return loadProperties(getSystemSettingsFile(pluginID)).getProperty(propName);
	}

	public static <T extends STProperties> T getSystemSettings(Class<T> valueType, String pluginID)
			throws STPropertyAccessException {
		return loadSTPropertiesFromYAMLFiles(valueType, getSystemSettingsFile(pluginID));
	}

	/**
	 * Sets the value of a system setting
	 * 
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setSystemSetting(String propName, String propValue) throws STPropertyUpdateException {
		setSystemSetting(propName, propValue, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a system setting
	 * 
	 * @param propName
	 * @param propValue
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setSystemSetting(String propName, String propValue, String pluginID)
			throws STPropertyUpdateException {
		try {
			File propFile = getSystemSettingsFile(pluginID);
			Properties properties = loadProperties(propFile);
			setProperty(properties, propName, propValue);
			updatePropertyFile(properties, propFile);
		} catch (STPropertyAccessException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	public static void setSystemSettings(STProperties settings, String pluginID)
			throws STPropertyUpdateException {
		setSystemSettings(settings, pluginID, false);
	}

	public static void setSystemSettings(STProperties settings, String pluginID,
			boolean allowIncompletePropValueSet) throws STPropertyUpdateException {
		try {
			if (!allowIncompletePropValueSet) {
				STPropertiesChecker settingsChecker = STPropertiesChecker
						.getModelConfigurationChecker(settings);
				if (!settingsChecker.isValid()) {
					throw new STPropertyUpdateException(
							"Settings not valid: " + settingsChecker.getErrorMessage());
				}
			}
			File settingsFile = getSystemSettingsFile(pluginID);
			storeSTPropertiesInYAML(settings, settingsFile);
		} catch (STPropertyAccessException | IOException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/*
	 * Methods to retrieve the following Properties files
	 * <STData>/system/plugins/<plugin>/system-preference-defaults.props
	 * <STData>/system/plugins/<plugin>/project-preference-defaults.props
	 * <STData>/system/plugins/<plugin>/project-settings-defaults.props
	 * <STData>/system/plugins/<plugin>/settings.props
	 * <STData>/projects/<projectname>/plugins/<plugin>/preference-defaults.props
	 * <STData>/projects/<projectname>/plugins/<plugin>/settings.props
	 * <STData>/users/<username>/plugins/<plugin>/project-preference-defaults.props
	 * <STData>/users/<username>/plugins/<plugin>/system-preferences.props
	 * <STData>/pu_binding/<projectname>/<username>/plugins/<plugin>/preferences.props
	 */

	/**
	 * Returns the Properties file <STData>/system/plugins/<plugin>/system-preferences-defaults.props
	 * 
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	private static File getSystemPreferencesDefaultsFile(String pluginID) throws STPropertyAccessException {
		try {
			File propFile = new File(getSystemPropertyFolder(pluginID) + File.separator
					+ SYSTEM_PREFERENCES_DEFAULTS_FILE_NAME);
			if (!propFile.exists()) { // if .props file doesn't exist, create and initialize it
				Properties properties = new Properties();
				updatePropertyFile(properties, propFile);
			}
			return propFile;
		} catch (STPropertyUpdateException e) {
			throw new STPropertyAccessException(e);
		}
	}

	/**
	 * Returns the Properties file <STData>/system/plugins/<plugin>/project-preferences-defaults.c.props
	 * * @param pluginID
	 * 
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static File getSystemProjectPreferencesDefaultsFile(String pluginID)
			throws STPropertyAccessException {
		try {
			File propFile = new File(getSystemPropertyFolder(pluginID) + File.separator
					+ SYSTEM_PROJECT_PREFERENCES_DEFAULTS_FILE_NAME);
			if (!propFile.exists()) { // if .props file doesn't exist, create and initialize it
				Properties properties = new Properties();
				updatePropertyFile(properties, propFile);
			}
			return propFile;
		} catch (STPropertyUpdateException e) {
			throw new STPropertyAccessException(e);
		}
	}

	/**
	 * Returns the Properties file <STData>/system/plugins/<plugin>/project-settings-defaults.props
	 * 
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static File getSystemProjectSettingsDefaultsFile(String pluginID)
			throws STPropertyAccessException {
		try {
			File propFile = new File(getSystemPropertyFolder(pluginID) + File.separator
					+ SYSTEM_PROJECT_SETTINGS_DEFAULTS_FILE_NAME);
			if (!propFile.exists()) { // if .props file doesn't exist, create and initialize it
				Properties properties = new Properties();
				updatePropertyFile(properties, propFile);
			}
			return propFile;
		} catch (STPropertyUpdateException e) {
			throw new STPropertyAccessException(e);
		}
	}

	/**
	 * Returns the Properties file <STData>/system/plugins/<plugin>/settings.props
	 * 
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static File getSystemSettingsFile(String pluginID) throws STPropertyAccessException {
		try {
			File propFile = new File(
					getSystemPropertyFolder(pluginID) + File.separator + SYSTEM_SETTINGS_FILE_NAME);
			if (!propFile.exists()) { // if .props file doesn't exist, create and initialize it
				Properties properties = new Properties();
				updatePropertyFile(properties, propFile);
			}
			return propFile;
		} catch (STPropertyUpdateException e) {
			throw new STPropertyAccessException(e);
		}
	}

	/**
	 * Returns the Properties file <STData>/projects/<projName>/plugins/<plugin>/preferences-defaults.props
	 * 
	 * @param project
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	private static File getProjectPreferencesDefaultsFile(Project project, String pluginID)
			throws STPropertyAccessException {
		try {
			File propFile = new File(getProjectPropertyFolder(project, pluginID) + File.separator
					+ PROJECT_PREFERENCES_DEFAULTS_FILE_NAME);
			if (!propFile.exists()) { // if .props file doesn't exist, create and initialize it
				Properties properties = new Properties();
				updatePropertyFile(properties, propFile);
			}
			return propFile;
		} catch (STPropertyUpdateException e) {
			throw new STPropertyAccessException(e);
		}
	}

	/**
	 * Returns the Properties file <STData>/projects/<projName>/plugins/<plugin>/settings.props
	 * 
	 * @param project
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	private static File getProjectSettingsFile(Project project, String pluginID)
			throws STPropertyAccessException {
		try {
			File propFile = new File(getProjectPropertyFolder(project, pluginID) + File.separator
					+ PROJECT_SETTINGS_FILE_NAME);
			if (!propFile.exists()) { // if .props file doesn't exist, create and initialize it
				Properties properties = new Properties();
				updatePropertyFile(properties, propFile);
			}
			return propFile;
		} catch (STPropertyUpdateException e) {
			throw new STPropertyAccessException(e);
		}
	}

	/**
	 * Returns the Properties file
	 * <STData>/users/<userEmail>/plugins/<plugin>/project-preferences-defaults.props
	 * 
	 * @param user
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	private static File getUserProjectPreferencesDefaultsFile(STUser user, String pluginID)
			throws STPropertyAccessException {
		try {
			File propFile = new File(getUserPropertyFolder(user, pluginID) + File.separator
					+ USER_PROJECT_PREFERENCES_DEFAULTS_FILE_NAME);
			if (!propFile.exists()) { // if .props file doesn't exist, create and initialize it
				Properties properties = new Properties();
				updatePropertyFile(properties, propFile);
			}
			return propFile;
		} catch (STPropertyUpdateException e) {
			throw new STPropertyAccessException(e);
		}
	}

	/**
	 * Returns the Properties file <STData>/users/<userEmail>/plugins/<plugin>/system-preferences.props
	 * 
	 * @param user
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	private static File getUserSystemPreferencesFile(STUser user, String pluginID)
			throws STPropertyAccessException {
		try {
			File propFile = new File(getUserPropertyFolder(user, pluginID) + File.separator
					+ USER_SYSTEM_PREFERENCES_FILE_NAME);
			if (!propFile.exists()) { // if .props file doesn't exist, create and initialize it
				Properties properties = new Properties();
				updatePropertyFile(properties, propFile);
			}
			return propFile;
		} catch (STPropertyUpdateException e) {
			throw new STPropertyAccessException(e);
		}
	}

	/**
	 * Returns the Properties file
	 * <STData>/pu_bindings/<projName>/<userEmail>/plugins/<plugin>/preferences.props
	 * 
	 * @param project
	 * @param user
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	private static File getPUBindingsPreferencesFile(Project project, STUser user, String pluginID)
			throws STPropertyAccessException {
		try {
			File propFile = new File(getPUBindingPropertyFolder(project, user, pluginID) + File.separator
					+ PU_BINDING_PREFERENCES_FILE_NAME);
			if (!propFile.exists()) { // if .props file doesn't exist, create and initialize it
				Properties properties = new Properties();
				updatePropertyFile(properties, propFile);
			}
			return propFile;
		} catch (STPropertyUpdateException e) {
			throw new STPropertyAccessException(e);
		}
	}

	/*
	 * Methods to retrieve the following folders: <STData>/system/plugins/<plugin>/
	 * <STData>/projects/<projectname>/plugins/<plugin>/ <STData>/users/<username>/plugins/<plugin>/
	 * <STData>/pu_binding/<projectname>/<username>/plugins/<plugin>/
	 */

	/**
	 * Returns the folder <STData>/system/plugins/<plugin>/
	 * 
	 * @param pluginID
	 * @return
	 */
	public static File getSystemPropertyFolder(String pluginID) {
		File prefFolder = new File(
				Resources.getSystemDir() + File.separator + "plugins" + File.separator + pluginID);
		if (!prefFolder.exists()) {
			prefFolder.mkdirs();
		}
		return prefFolder;
	}

	/**
	 * Returns the folder <STData>/projects/<projectName>/plugins/<plugin>/
	 * 
	 * @param project
	 * @param pluginID
	 * @return
	 */
	public static File getProjectPropertyFolder(Project project, String pluginID) {
		File prefFolder = new File(Resources.getProjectsDir() + File.separator + project.getName()
				+ File.separator + "plugins" + File.separator + pluginID);
		if (!prefFolder.exists()) {
			prefFolder.mkdirs();
		}
		return prefFolder;
	}

	/**
	 * Returns the folder <STData>/users/<userEmail>/plugins/<plugin>/
	 * 
	 * @param user
	 * @param pluginID
	 * @return
	 */
	public static File getUserPropertyFolder(STUser user, String pluginID) {
		File prefFolder = new File(
				Resources.getUsersDir() + File.separator + STUser.encodeUserIri(user.getIRI())
						+ File.separator + "plugins" + File.separator + pluginID);
		if (!prefFolder.exists()) {
			prefFolder.mkdirs();
		}
		return prefFolder;
	}

	/**
	 * Returns the folder <STData>/pu_bindings/<projectName>/<userEmail>/plugins/<plugin>/
	 * 
	 * @param project
	 * @param user
	 * @param pluginID
	 * @return
	 */
	public static File getPUBindingPropertyFolder(Project project, STUser user, String pluginID) {
		File prefFolder = new File(Resources.getProjectUserBindingsDir() + File.separator + project.getName()
				+ File.separator + STUser.encodeUserIri(user.getIRI()) + File.separator + "plugins"
				+ File.separator + pluginID);
		if (!prefFolder.exists()) {
			prefFolder.mkdirs();
		}
		return prefFolder;
	}

	/**
	 * Sets the value for a property. If the value is null, removes the property.
	 * 
	 * @param properties
	 * @param propName
	 * @param propValue
	 */
	private static void setProperty(Properties properties, String propName, String propValue) {
		if (propValue != null) {
			properties.setProperty(propName, propValue);
		} else {
			properties.remove(propName);
		}
	}

	private static void updatePropertyFile(Properties properties, File propFile)
			throws STPropertyUpdateException {
		FileOutputStream os = null;
		try {
			os = new FileOutputStream(propFile);
			properties.store(os, null);
			os.close();
		} catch (IOException e) {
			throw new STPropertyUpdateException(e);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static Properties loadProperties(File propertiesFile) throws STPropertyAccessException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(propertiesFile);
			Properties properties = new Properties();
			properties.load(fis);
			return properties;
		} catch (IOException e) {
			throw new STPropertyAccessException(e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
