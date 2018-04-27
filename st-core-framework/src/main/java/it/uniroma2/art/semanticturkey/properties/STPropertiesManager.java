package it.uniroma2.art.semanticturkey.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JBNodeDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JIRIDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JLiteralDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JResourceDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JValueDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JValueSerializer;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.user.STUser;

public class STPropertiesManager {

	// id of the "core plugin", used for preferences/properties that don't belong to any plugin
	public static final String CORE_PLUGIN_ID = "it.uniroma2.art.semanticturkey";

	private static final String USER_SETTINGS_FILE_NAME = "settings.props";
	private static final String PU_SETTINGS_FILE_NAME = "settings.props";
	private static final String PROJECT_SETTINGS_FILE_NAME = "settings.props";
	private static final String SYSTEM_SETTINGS_FILE_NAME = "settings.props";

	private static final String USER_SETTINGS_DEFAULTS_FILE_NAME = "user-settings-defaults.props";

	private static final String PU_SETTINGS_USER_DEFAULTS_FILE_NAME = "pu-settings-defaults.props";
	private static final String PU_SETTINGS_PROJECT_DEFAULTS_FILE_NAME = "pu-settings-defaults.props";
	private static final String PU_SETTINGS_SYSTEM_DEFAULTS_FILE_NAME = "pu-settings-defaults.props";

	private static final String PROJECT_SETTINGS_DEFAULTS_FILE_NAME = "project-settings-defaults.props";

	public static final String PREF_LANGUAGES = "languages";
	public static final String PREF_SHOW_FLAGS = "show_flags";
	public static final String PREF_SHOW_INSTANCES_NUMBER = "show_instances_number";
	public static final String PREF_ACTIVE_SCHEMES = "active_schemes";
	public static final String PREF_PROJ_THEME = "project_theme";

	public static final String SETTING_ADMIN_ADDRESS = "mail.admin.address";
	
	public static final String SETTING_MAIL_SMTP_AUTH = "mail.smtp.auth";
	public static final String SETTING_MAIL_SMTP_HOST= "mail.smtp.host";
	public static final String SETTING_MAIL_SMTP_PORT = "mail.smtp.port";
	public static final String SETTING_MAIL_FROM_ADDRESS = "mail.from.address";
	public static final String SETTING_MAIL_FROM_PASSWORD = "mail.from.password";
	public static final String SETTING_MAIL_FROM_ALIAS = "mail.from.alias";
	
	public static final String SETTING_PROJ_LANGUAGES = "languages";
	public static final String SETTING_EXP_FEATURES_ENABLED = "experimental_features_enabled";

	public static final String SETTINGS_TYPE_PROPERTY = "@type";

	/*
	 * Getter/Setter <STData>/pu_binding/<projectname>/<username>/plugins/<plugin>/settings.props
	 */

	/**
	 * Returns the value of a pu_setting for the given project-user pair. If the setting has no value for the
	 * user, it looks for the value in the following order:
	 * <ul>
	 * <li>the default value at project level</li>
	 * <li>the default value at user level</li>
	 * <li>the default value at system level.</li>
	 * </ul>
	 * Returns null if no value is defined at all
	 * 
	 * @param project
	 * @param user
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getPUSetting(String propName, Project project, STUser user)
			throws STPropertyAccessException {
		return getPUSetting(propName, project, user, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a pu_setting about the given project-user-plugin. If the setting has no value for
	 * the user, it looks for the value in the following order:
	 * <ul>
	 * <li>the default value at project level</li>
	 * <li>the default value at user level</li>
	 * <li>the default value at system level.</li>
	 * </ul>
	 * Returns null if no value is defined at all
	 * 
	 * @param pluginID
	 * @param project
	 * @param user
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getPUSetting(String propName, Project project, STUser user, String pluginID)
			throws STPropertyAccessException {
		String value;
		value = loadProperties(getPUSettingsFile(project, user, pluginID)).getProperty(propName);
		if (value == null) {
			value = getPUSettingProjectDefault(propName, project, pluginID);
			if (value == null) {
				value = getPUSettingUserDefault(propName, user, pluginID);
				if (value == null) {
					value = getPUSettingSystemDefault(propName, pluginID);
				}
			}
		}
		return value;
	}

	/**
	 * Returns the pu_settings about a plugin. See {@link #getPUSetting(String, Project, STUser, String)} for
	 * details about the lookup procedure.
	 * 
	 * @param projectPreferences
	 * @param project
	 * @param user
	 * @param pluginId
	 * @throws STPropertyAccessException
	 */
	public static <T extends STProperties> T getPUSettings(Class<T> valueType, Project project, STUser user,
			String pluginID) throws STPropertyAccessException {
		File defaultPropFile = getPUSettingsProjectDefaultsFile(project, pluginID);
		File propFile = getPUSettingsFile(project, user, pluginID);

		return loadSTPropertiesFromYAMLFiles(valueType, false, defaultPropFile, propFile);
	}

	/**
	 * Sets the value of a project pu_setting for the given project-user
	 * 
	 * @param project
	 * @param user
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setPUSetting(String propName, String propValue, Project project, STUser user)
			throws STPropertyUpdateException {
		setPUSetting(propName, propValue, project, user, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a pu_setting for the given project-user-plugin
	 * 
	 * @param pluginID
	 * @param project
	 * @param user
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setPUSetting(String propName, String propValue, Project project, STUser user,
			String pluginID) throws STPropertyUpdateException {
		try {
			File propFile = getPUSettingsFile(project, user, pluginID);
			Properties properties = loadProperties(propFile);
			setProperty(properties, propName, propValue);
			updatePropertyFile(properties, propFile);
		} catch (STPropertyAccessException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/**
	 * Sets the values of pu_setting related to the given project-user-plugin
	 * 
	 * @param preferences
	 * @param project
	 * @param user
	 * @param pluginID
	 * @param allowIncompletePropValueSet
	 */
	public static void setPUSettings(STProperties preferences, Project project, STUser user, String pluginID,
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
			File propFile = getPUSettingsFile(project, user, pluginID);
			storeSTPropertiesInYAML(preferences, propFile, false);
		} catch (STPropertyAccessException | IOException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/**
	 * Convenience overload of {@link #setPUSettings(STProperties, Project, STUser, String, boolean)} that
	 * disallows the storage of incomplete settings (i.e. missing values for required property).
	 * 
	 * @param settings
	 * @param project
	 * @param user
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setPUSettings(STProperties settings, Project project, STUser user, String pluginID)
			throws STPropertyUpdateException {
		setPUSettings(settings, project, user, pluginID, false);
	}

	/*
	 * Getter/Setter <STData>/projects/<projectname>/plugins/<plugin>/pu-settings-defaults.props
	 */

	/**
	 * Returns the value of a default pu_setting - project. Returns null if no value is defined
	 * 
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getPUSettingProjectDefault(String propName, Project project)
			throws STPropertyAccessException {
		return getPUSettingProjectDefault(propName, project, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a default pu_setting-project about a plugin. Returns null if no value is defined
	 * 
	 * @param pluginID
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getPUSettingProjectDefault(String propName, Project project, String pluginID)
			throws STPropertyAccessException {
		return loadProperties(getPUSettingsProjectDefaultsFile(project, pluginID)).getProperty(propName);
	}

	/**
	 * Sets the value of a default pu_setting for the given project
	 * 
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setPUSettingProjectDefault(String propName, String propValue, Project project)
			throws STPropertyUpdateException {
		getPUSettingProjectDefault(propName, propValue, project, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a default pu_setting for the given project-plugin
	 * 
	 * @param propName
	 * @param propValue
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void getPUSettingProjectDefault(String propName, String propValue, Project project,
			String pluginID) throws STPropertyUpdateException {
		try {
			File propFile = getPUSettingsProjectDefaultsFile(project, pluginID);
			Properties properties = loadProperties(propFile);
			setProperty(properties, propName, propValue);
			updatePropertyFile(properties, propFile);
		} catch (STPropertyAccessException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/*
	 * Getter/Setter <STData>/users/<username>/plugins/<plugin>/pu_settings-defaults.props
	 */
	/**
	 * Returns the value of a pu_setting default at user level.
	 * 
	 * @param user
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getPUSettingUserDefault(String propName, STUser user)
			throws STPropertyAccessException {
		return getPUSettingUserDefault(propName, user, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a pu_setting default at user level.
	 * 
	 * @param user
	 * @param pluginID
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getPUSettingUserDefault(String propName, STUser user, String pluginID)
			throws STPropertyAccessException {
		return loadProperties(getPUSettingsUserDefaultsFile(user, pluginID)).getProperty(propName);
	}

	/**
	 * Sets the value of a pu_setting default at user level.
	 * 
	 * @param user
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setPUSettingUserDefault(String propName, String propValue, STUser user)
			throws STPropertyUpdateException {
		setPUSettingUserDefault(propName, propValue, user, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a pu_setting default at user level.
	 * 
	 * @param user
	 * @param propName
	 * @param propValue
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setPUSettingUserDefault(String propName, String propValue, STUser user,
			String pluginID) throws STPropertyUpdateException {
		try {
			File propFile = getPUSettingsUserDefaultsFile(user, pluginID);
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
	 * Returns the value of a default pu_setting-system. Returns null if no value is defined
	 * 
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getPUSettingSystemDefault(String propName) throws STPropertyAccessException {
		return getPUSettingSystemDefault(propName, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a default default pu_setting-system about a plugin. Returns null if no value is
	 * defined
	 * 
	 * @param pluginID
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getPUSettingSystemDefault(String propName, String pluginID)
			throws STPropertyAccessException {
		return loadProperties(getPUSettingsSystemDefaultsFile(pluginID)).getProperty(propName);
	}

	/**
	 * Sets the value of a default pu_setting-system
	 * 
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setPUSettingSystemDefault(String propName, String propValue)
			throws STPropertyUpdateException {
		setPUSettingSystemDefault(propName, propValue, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a default pu_setting-system
	 * 
	 * @param propName
	 * @param propValue
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setPUSettingSystemDefault(String propName, String propValue, String pluginID)
			throws STPropertyUpdateException {
		try {
			File propFile = getPUSettingsSystemDefaultsFile(pluginID);
			Properties properties = loadProperties(propFile);
			setProperty(properties, propName, propValue);
			updatePropertyFile(properties, propFile);
		} catch (STPropertyAccessException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/*
	 * Getter/Setter <STData>/users/<username>/plugins/<plugin>/settings.props
	 */

	/**
	 * Returns the value of a user setting for the given user. If the preference has no value for the user, it
	 * returns the value at system level. Returns null if no value is defined at all
	 * 
	 * @param user
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getUserSetting(String propName, STUser user) throws STPropertyAccessException {
		return getUserSetting(propName, user, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a user setting about a plugin for the given user. If the preference has no value
	 * for the user, it returns the value at system level. Returns null if no value is defined at all
	 * 
	 * @param pluginID
	 * @param user
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getUserSetting(String propName, STUser user, String pluginID)
			throws STPropertyAccessException {
		// SP: system-preference: user -> default(sp,system)
		String value;
		value = loadProperties(getUserSettingsFile(user, pluginID)).getProperty(propName);
		if (value == null) {
			value = getUserSettingDefault(propName, pluginID);
		}
		return value;
	}

	public static <T extends STProperties> T getUserSettings(Class<T> valueType, STUser user, String pluginID)
			throws STPropertyAccessException {
		File propFile = getUserSettingsFile(user, pluginID);
		File defaultPropFile = getUserSettingsDefaultsFile(pluginID);
		return loadSTPropertiesFromYAMLFiles(valueType, false, defaultPropFile, propFile);
	}

	/**
	 * Sets the value of a user setting for the given user
	 * 
	 * @param user
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setUserSetting(String propName, String propValue, STUser user)
			throws STPropertyUpdateException {
		setUserSetting(propName, propValue, user, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a user setting for the given user
	 * 
	 * @param user
	 * @param propName
	 * @param propValue
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setUserSetting(String propName, String propValue, STUser user, String pluginID)
			throws STPropertyUpdateException {
		try {
			File propFile = getUserSettingsFile(user, pluginID);
			Properties properties = loadProperties(propFile);
			setProperty(properties, propName, propValue);
			updatePropertyFile(properties, propFile);
		} catch (STPropertyAccessException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	public static void setUserSettings(STProperties preferences, STUser user, String pluginID)
			throws STPropertyUpdateException {
		setUserSettings(preferences, user, pluginID, false);
	}

	// package-private
	static ObjectMapper createObjectMapper() {
		YAMLFactory fact = new YAMLFactory();
		fact.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
		fact.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);

		SimpleModule stPropsModule = new SimpleModule();
		stPropsModule.setMixInAnnotation(STProperties.class,
				it.uniroma2.art.semanticturkey.properties.yaml.STPropertiesPersistenceMixin.class);
		stPropsModule.addDeserializer(Value.class, new RDF4JValueDeserializer());
		stPropsModule.addDeserializer(Resource.class, new RDF4JResourceDeserializer());
		stPropsModule.addDeserializer(BNode.class, new RDF4JBNodeDeserializer());
		stPropsModule.addDeserializer(IRI.class, new RDF4JIRIDeserializer());
		stPropsModule.addDeserializer(Literal.class, new RDF4JLiteralDeserializer());
		stPropsModule.addSerializer(new RDF4JValueSerializer());
		ObjectMapper mapper = new ObjectMapper(fact);
		mapper.registerModule(stPropsModule);
		return mapper;
	}

	public static void storeSTPropertiesInYAML(STProperties properties, File propertiesFile,
			boolean storeObjType) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = createObjectMapper();
		mapper.writeValue(propertiesFile, properties);
	}

	public static <T extends STProperties> T loadSTPropertiesFromYAMLFiles(Class<T> valueType,
			boolean loadObjType, File... propFiles) throws STPropertyAccessException {
		try {
			ObjectMapper objectMapper = createObjectMapper();
			ObjectReader objReader = objectMapper.reader();

			ObjectNode obj = objectMapper.createObjectNode();

			for (int i = 0; i < propFiles.length; i++) {
				File propFile = propFiles[i];
				if (!propFile.exists())
					continue;

				try (Reader reader = new InputStreamReader(new FileInputStream(propFile),
						StandardCharsets.UTF_8)) {
					JsonNode jsonNode = objReader.readTree(reader);
					if (jsonNode != null) {
						if (!(jsonNode instanceof ObjectNode))
							throw new STPropertyAccessException(
									"YAML file not cotaining an object node: " + propFile);
						obj.setAll((ObjectNode) jsonNode);
					}
				} catch (JsonMappingException e) {
					// Swallow exception due to empty property files
					if (!(e.getPath().isEmpty() && e.getMessage().contains("end-of-input"))) {
						throw e;
					}
				}

			}

			return loadSTPropertiesFromObjectNode(valueType, loadObjType, obj, objectMapper);
		} catch (IOException e) {
			throw new STPropertyAccessException(e);
		}
	}

	public static <T extends STProperties> T loadSTPropertiesFromObjectNode(Class<T> valueType,
			boolean loadObjType, ObjectNode obj) throws STPropertyAccessException {
		return loadSTPropertiesFromObjectNode(valueType, loadObjType, obj, createObjectMapper());
	}

	@SuppressWarnings("unchecked")
	public static <T extends STProperties> T loadSTPropertiesFromObjectNode(Class<T> valueType,
			boolean loadObjType, ObjectNode obj, ObjectMapper objectMapper) throws STPropertyAccessException {
		try {
			Class<?> effectveValueType = valueType;

			if (loadObjType && obj.hasNonNull(SETTINGS_TYPE_PROPERTY)) {
				String specificClassName = obj.get(SETTINGS_TYPE_PROPERTY).asText();
				Class<?> specificClass = valueType.getClassLoader().loadClass(specificClassName);
				if (!valueType.isAssignableFrom(specificClass)) {
					throw new STPropertyAccessException("Specific type \"" + specificClassName
							+ "\" is not assignable to generic type \"" + valueType.getName() + "\"");
				}
				effectveValueType = specificClass;
			}
			
			STProperties properties = (STProperties) effectveValueType.newInstance();

			for (String prop : properties.getProperties()) {
				Type propType = properties.getPropertyType(prop);
				JavaType jacksonPropType = objectMapper.getTypeFactory().constructType(propType);

				if (obj.hasNonNull(prop)) {
					Object propValue = objectMapper.readValue(objectMapper.treeAsTokens(obj.get(prop)),
							jacksonPropType);
					properties.setPropertyValue(prop, propValue);
				}
			}

			return (T) properties;
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| PropertyNotFoundException | IOException | WrongPropertiesException e) {
			throw new STPropertyAccessException(e);
		}
	}

	public static void setUserSettings(STProperties preferences, STUser user, String pluginID,
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
			File settingsFile = getUserSettingsFile(user, pluginID);
			storeSTPropertiesInYAML(preferences, settingsFile, false);
		} catch (STPropertyAccessException | IOException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/*
	 * Getter/Setter <STData>/system/plugins/<plugin>/system-preference-defaults.props
	 */

	/**
	 * Returns the value of a default user setting. Returns null if no value is defined
	 * 
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getUserSettingDefault(String propName) throws STPropertyAccessException {
		return getUserSettingDefault(propName, CORE_PLUGIN_ID);
	}

	/**
	 * Returns the value of a default user setting about a plugin. Returns null if no value is defined
	 * 
	 * @param pluginID
	 * @param propName
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static String getUserSettingDefault(String propName, String pluginID)
			throws STPropertyAccessException {
		return loadProperties(getUserSettingsDefaultsFile(pluginID)).getProperty(propName);
	}

	/**
	 * Sets the value of a default user setting
	 * 
	 * @param propName
	 * @param propValue
	 * @throws STPropertyUpdateException
	 */
	public static void setUserSettingDefault(String propName, String propValue)
			throws STPropertyUpdateException {
		setUserSettingDefault(propName, propValue, CORE_PLUGIN_ID);
	}

	/**
	 * Sets the value of a default user setting
	 * 
	 * @param propName
	 * @param propValue
	 * @param pluginID
	 * @throws STPropertyUpdateException
	 */
	public static void setUserSettingDefault(String propName, String propValue, String pluginID)
			throws STPropertyUpdateException {
		try {
			File propFile = getUserSettingsDefaultsFile(pluginID);
			Properties properties = loadProperties(propFile);
			setProperty(properties, propName, propValue);
			updatePropertyFile(properties, propFile);
		} catch (STPropertyAccessException e) {
			throw new STPropertyUpdateException(e);
		}
	}

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
		File defaultPropFile = getProjectSettingsDefaultsFile(pluginID);
		File propFile = getProjectSettingsFile(project, pluginID);

		return loadSTPropertiesFromYAMLFiles(valueType, false, defaultPropFile, propFile);
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
			storeSTPropertiesInYAML(settings, settingsFile, false);
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
		return loadProperties(getProjectSettingsDefaultsFile(pluginID)).getProperty(propName);
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
			File propFile = getProjectSettingsDefaultsFile(pluginID);
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
		return loadSTPropertiesFromYAMLFiles(valueType, false, getSystemSettingsFile(pluginID));
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
			storeSTPropertiesInYAML(settings, settingsFile, false);
		} catch (STPropertyAccessException | IOException e) {
			throw new STPropertyUpdateException(e);
		}
	}

	/*
	 * Methods to retrieve the following Properties files User Settings Defaults:
	 * <STData>/system/plugins/<plugin>/user-settings-defaults.props PU SettingsSystem Defaults:
	 * <STData>/system/plugins/<plugin>/pu-settings-defaults.props Project Settings Defaults:
	 * <STData>/system/plugins/<plugin>/project-settings-defaults.props System Settings:
	 * <STData>/system/plugins/<plugin>/settings.props PU Settings Project Defaults:
	 * <STData>/projects/<projName>/plugins/<plugin>/pu-settings-defaults.props Project Settings:
	 * <STData>/projects/<projName>/plugins/<plugin>/settings.props PU Settings User Defaults:
	 * <STData>/users/<user>/plugins/<plugin>/pu-settings-defaults.props User Settings:
	 * <STData>/users/<user>/plugins/<plugin>/settings.props PU Settings:
	 * <STData>/pu_bindings/<projName>/<user>/plugins/<plugin>/settings.props
	 */

	/**
	 * Returns the Properties file <STData>/system/plugins/<plugin>/user-settings-defaults.props
	 * 
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	private static File getUserSettingsDefaultsFile(String pluginID) throws STPropertyAccessException {
		try {
			File propFile = new File(
					getSystemPropertyFolder(pluginID) + File.separator + USER_SETTINGS_DEFAULTS_FILE_NAME);
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
	 * Returns the Properties file <STData>/system/plugins/<plugin>/pu-settings-defaults.props * @param
	 * pluginID
	 * 
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static File getPUSettingsSystemDefaultsFile(String pluginID) throws STPropertyAccessException {
		try {
			File propFile = new File(getSystemPropertyFolder(pluginID) + File.separator
					+ PU_SETTINGS_SYSTEM_DEFAULTS_FILE_NAME);
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
	public static File getProjectSettingsDefaultsFile(String pluginID) throws STPropertyAccessException {
		try {
			File propFile = new File(
					getSystemPropertyFolder(pluginID) + File.separator + PROJECT_SETTINGS_DEFAULTS_FILE_NAME);
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
	 * Returns the Properties file <STData>/projects/<projName>/plugins/<plugin>/pu-settings-defaults.props
	 * 
	 * @param project
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	private static File getPUSettingsProjectDefaultsFile(Project project, String pluginID)
			throws STPropertyAccessException {
		try {
			File propFile = new File(getProjectPropertyFolder(project, pluginID) + File.separator
					+ PU_SETTINGS_PROJECT_DEFAULTS_FILE_NAME);
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
	 * Returns the Properties file <STData>/users/<user>/plugins/<plugin>/pu-settings-defaults.props
	 * 
	 * @param user
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	private static File getPUSettingsUserDefaultsFile(STUser user, String pluginID)
			throws STPropertyAccessException {
		try {
			File propFile = new File(getUserPropertyFolder(user, pluginID) + File.separator
					+ PU_SETTINGS_USER_DEFAULTS_FILE_NAME);
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
	 * Returns the Properties file <STData>/users/<user>/plugins/<plugin>/settings.props
	 * 
	 * @param user
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	private static File getUserSettingsFile(STUser user, String pluginID) throws STPropertyAccessException {
		try {
			File propFile = new File(
					getUserPropertyFolder(user, pluginID) + File.separator + USER_SETTINGS_FILE_NAME);
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
	 * Returns the Properties file <STData>/pu_bindings/<projName>/<user>/plugins/<plugin>/settings.props
	 * 
	 * @param project
	 * @param user
	 * @param pluginID
	 * @return
	 * @throws STPropertyAccessException
	 */
	private static File getPUSettingsFile(Project project, STUser user, String pluginID)
			throws STPropertyAccessException {
		try {
			File propFile = new File(getPUBindingPropertyFolder(project, user, pluginID) + File.separator
					+ PU_SETTINGS_FILE_NAME);
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
	 * Returns the folder <STData>/users/<user>/plugins/<plugin>/
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
	 * Returns the folder <STData>/pu_bindings/<projectName>/<user>/plugins/<plugin>/
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
