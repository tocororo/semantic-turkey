package it.uniroma2.art.semanticturkey.properties;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JBNodeDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JIRIDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JLiteralDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JResourceDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JValueDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JValueSerializer;
import it.uniroma2.art.semanticturkey.properties.yaml.STPropertiesPersistenceDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.STPropertiesPersistenceSerializer;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersGroup;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class STPropertiesManager {

    private static final String USER_SETTINGS_FILE_NAME = "settings.props";
    private static final String PU_SETTINGS_FILE_NAME = "settings.props";
    private static final String PG_SETTINGS_FILE_NAME = "settings.props";
    private static final String PROJECT_SETTINGS_FILE_NAME = "settings.props";
    private static final String SYSTEM_SETTINGS_FILE_NAME = "settings.props";

    private static final String USER_SETTINGS_DEFAULTS_FILE_NAME = "user-settings-defaults.props";

    private static final String PU_SETTINGS_USER_DEFAULTS_FILE_NAME = "pu-settings-defaults.props";
    private static final String PU_SETTINGS_PROJECT_DEFAULTS_FILE_NAME = "pu-settings-defaults.props";
    private static final String PU_SETTINGS_SYSTEM_DEFAULTS_FILE_NAME = "pu-settings-defaults.props";

    private static final String PROJECT_SETTINGS_DEFAULTS_FILE_NAME = "project-settings-defaults.props";

    public static final String SETTINGS_TYPE_PROPERTY = "@type";

    /*
     * Getter/Setter <STData>/pu_binding/<projectname>/<username>/plugins/<plugin>/settings.props
     */

    /**
     * Returns the pu_settings about a plugin. If the setting has no value for the
     * user, it looks for the value in the following order:
     * <ul>
     * <li>the value for the group of the user (if any)</li>
     * <li>the default value at project level</li>
     * <li>the default value at user level</li>
     * <li>the default value at system level.</li>
     * </ul>
     *
     * @param valueType
     * @param project
     * @param user
     * @param pluginID
     * @throws STPropertyAccessException
     */
    public static <T extends STProperties> T getPUSettings(Class<T> valueType, Project project, STUser user,
                                                           String pluginID) throws STPropertyAccessException {
        return getPUSettings(valueType, project, user, pluginID, false);
    }

    public static <T extends STProperties> T getPUSettings(Class<T> valueType, Project project, STUser user,
                                                           String pluginID, boolean explicit) throws STPropertyAccessException {
        File propFile = getPUSettingsFile(project, user, pluginID);

        if (explicit) {
            return loadSettings(valueType, propFile);
        } else {
            List<File> propFiles = new ArrayList<>(5);

            File systemDefaultPropFile = getPUSettingsSystemDefaultsFile(pluginID);
            File userDefaultPropFile = getPUSettingsUserDefaultsFile(user, pluginID);
            File projectDefaultPropFile = getPUSettingsProjectDefaultsFile(project, pluginID);

            // adds the "scopes" from the lowest to the highest priority: system, user, project, project-group, pu (explicit)

            propFiles.add(systemDefaultPropFile);
            propFiles.add(userDefaultPropFile);
            propFiles.add(projectDefaultPropFile);

            UsersGroup group = ProjectUserBindingsManager.getUserGroup(user, project);
            if (group != null) {
                File pgFile = getPGSettingsFile(project, group, pluginID);
                propFiles.add(pgFile);
            }

            propFiles.add(propFile);


            return loadSettings(valueType, propFiles.toArray(new File[0]));
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
            STPropertiesChecker preferencesChecker = STPropertiesChecker
                    .getModelConfigurationChecker(preferences).allowIncomplete(allowIncompletePropValueSet);
            if (!preferencesChecker.isValid()) {
                throw new InvalidSettingsUpdateException(preferencesChecker.getErrorMessage());
            }
            File propFile = getPUSettingsFile(project, user, pluginID);
            storeSTPropertiesInYAML(preferences, propFile, false);
        } catch (IOException e) {
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
     * Getter/Setter <STData>/pg_binding/<projectname>/<group>/plugins/<plugin>/settings.props
     */

    /**
     * Returns the value of a pg_setting about the given project-group-plugin.
     *
     * @param valueType
     * @param project
     * @param group
     * @param pluginID
     * @throws STPropertyAccessException
     */
    public static <T extends STProperties> T getPGSettings(Class<T> valueType, Project project, UsersGroup group,
                                                           String pluginID) throws STPropertyAccessException {
        return getPGSettings(valueType, project, group, pluginID, false);
    }

    public static <T extends STProperties> T getPGSettings(Class<T> valueType, Project project, UsersGroup group,
                                                           String pluginID, boolean explicit) throws STPropertyAccessException {
        File propFile = getPGSettingsFile(project, group, pluginID);
        return loadSettings(valueType, propFile);

    }

    /**
     * Sets the values of pg_setting related to the given project-group-plugin
     *
     * @param preferences
     * @param project
     * @param group
     * @param pluginID
     * @param allowIncompletePropValueSet
     */
    public static void setPGSettings(STProperties preferences, Project project, UsersGroup group,
                                     String pluginID, boolean allowIncompletePropValueSet) throws STPropertyUpdateException {
        try {
            STPropertiesChecker preferencesChecker = STPropertiesChecker
                .getModelConfigurationChecker(preferences).allowIncomplete(allowIncompletePropValueSet);
            if (!preferencesChecker.isValid()) {
                throw new InvalidSettingsUpdateException(preferencesChecker.getErrorMessage());
            }

            File propFile = getPGSettingsFile(project, group, pluginID);
            storeSTPropertiesInYAML(preferences, propFile, false);
        } catch (IOException e) {
            throw new STPropertyUpdateException(e);
        }
    }

    /**
     * Convenience overload of {@link #setPGSettings(STProperties, Project, UsersGroup, String, boolean)} that
     * disallows the storage of incomplete settings (i.e. missing values for required property).
     *
     * @param settings
     * @param project
     * @param group
     * @param pluginID
     * @throws STPropertyUpdateException
     */
    public static void setPGSettings(STProperties settings, Project project, UsersGroup group,
                                     String pluginID) throws STPropertyUpdateException {
        setPGSettings(settings, project, group, pluginID, false);
    }

    /*
     * Getter/Setter <STData>/projects/<projectname>/plugins/<plugin>/pu-settings-defaults.props
     */

    /**
     * Returns the value of a default pu_settings at project level
     *
     * @param valueType
     * @param pluginID
     * @return
     * @throws STPropertyAccessException
     */
    public static <T extends STProperties> T getPUSettingsProjectDefault(Class<T> valueType, Project project, String pluginID) throws STPropertyAccessException {
        File defaultPropFile = getPUSettingsProjectDefaultsFile(project, pluginID);

        return loadSettings(valueType, defaultPropFile);
    }

    /**
     * Returns the value of a default pu_settings at user level
     *
     * @param valueType
     * @param pluginID
     * @return
     * @throws STPropertyAccessException
     */
    public static <T extends STProperties> T getPUSettingsUserDefault(Class<T> valueType, STUser user, String pluginID) throws STPropertyAccessException {
        File defaultPropFile = getPUSettingsUserDefaultsFile(user, pluginID);

        return loadSettings(valueType, defaultPropFile);
    }

    /**
     * Returns the value of a default pu_settings at system level
     *
     * @param valueType
     * @param pluginID
     * @return
     * @throws STPropertyAccessException
     */
    public static <T extends STProperties> T getPUSettingsSystemDefault(Class<T> valueType, String pluginID) throws STPropertyAccessException {
        File defaultPropFile = getPUSettingsSystemDefaultsFile(pluginID);

        return loadSettings(valueType, defaultPropFile);
    }

    /**
     * Sets the value of a default pu-setting at project level.
     *
     * @param settings
     * @param project
     * @param pluginID
     * @param allowIncompletePropValueSet
     */
    public static void setPUSettingsProjectDefault(STProperties settings, Project project, String pluginID,
                                                   boolean allowIncompletePropValueSet) throws STPropertyUpdateException {
        try {
            STPropertiesChecker settingsChecker = STPropertiesChecker
                    .getModelConfigurationChecker(settings).allowIncomplete(allowIncompletePropValueSet);
            if (!settingsChecker.isValid()) {
                throw new InvalidSettingsUpdateException(settingsChecker.getErrorMessage());
            }
            File settingsFile = getPUSettingsProjectDefaultsFile(project, pluginID);
            storeSTPropertiesInYAML(settings, settingsFile, false);
        } catch (IOException e) {
            throw new STPropertyUpdateException(e);
        }
    }

    /*
     * Getter/Setter <STData>/users/<username>/plugins/<plugin>/pu_settings-defaults.props
     */

    /**
     * Sets the value of a default pu-setting at user level.
     *
     * @param settings
     * @param user
     * @param pluginID
     * @param allowIncompletePropValueSet
     */
    public static void setPUSettingsUserDefault(STProperties settings, STUser user, String pluginID,
                                                boolean allowIncompletePropValueSet) throws STPropertyUpdateException {
        try {
            STPropertiesChecker settingsChecker = STPropertiesChecker
                    .getModelConfigurationChecker(settings).allowIncomplete(allowIncompletePropValueSet);
            if (!settingsChecker.isValid()) {
                throw new InvalidSettingsUpdateException(settingsChecker.getErrorMessage());
            }
            File settingsFile = getPUSettingsUserDefaultsFile(user, pluginID);
            storeSTPropertiesInYAML(settings, settingsFile, false);
        } catch (IOException e) {
            throw new STPropertyUpdateException(e);
        }
    }

    /*
     * Getter/Setter <STData>/system/plugins/<plugin>/project-preference-defaults.props
     */

    /**
     * Sets the value of a default pu-setting at system level.
     *
     * @param settings
     * @param pluginID
     * @param allowIncompletePropValueSet
     */
    public static void setPUSettingsSystemDefault(STProperties settings, String pluginID,
                                                  boolean allowIncompletePropValueSet) throws STPropertyUpdateException {
        try {
            STPropertiesChecker settingsChecker = STPropertiesChecker
                    .getModelConfigurationChecker(settings).allowIncomplete(allowIncompletePropValueSet);
            if (!settingsChecker.isValid()) {
                throw new InvalidSettingsUpdateException(settingsChecker.getErrorMessage());
            }
            File settingsFile = getPUSettingsSystemDefaultsFile(pluginID);
            storeSTPropertiesInYAML(settings, settingsFile, false);
        } catch (IOException e) {
            throw new STPropertyUpdateException(e);
        }
    }

    /*
     * Getter/Setter <STData>/users/<username>/plugins/<plugin>/settings.props
     */

    public static <T extends STProperties> T getUserSettings(Class<T> valueType, STUser user, String pluginID)
            throws STPropertyAccessException {
        return getUserSettings(valueType, user, pluginID, false);
    }

    public static <T extends STProperties> T getUserSettings(Class<T> valueType, STUser user, String pluginID,
                                                             boolean explicit) throws STPropertyAccessException {
        File propFile = getUserSettingsFile(user, pluginID);
        File defaultPropFile = getUserSettingsDefaultsFile(pluginID);

        if (explicit) {
            return loadSettings(valueType, propFile);
        } else {
            return loadSettings(valueType, defaultPropFile, propFile);
        }
    }

    public static void setUserSettings(STProperties preferences, STUser user, String pluginID)
            throws STPropertyUpdateException {
        setUserSettings(preferences, user, pluginID, false);
    }

    public static ObjectMapper createObjectMapper() {
        return createObjectMapper(null);
    }

    public static ObjectMapper createObjectMapper(ExtensionPointManager exptManager) {
        YAMLFactory fact = new YAMLFactory();
        fact.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        fact.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);

        SimpleModule stPropsModule = new SimpleModule();
        stPropsModule.addDeserializer(Value.class, new RDF4JValueDeserializer());
        stPropsModule.addDeserializer(Resource.class, new RDF4JResourceDeserializer());
        stPropsModule.addDeserializer(BNode.class, new RDF4JBNodeDeserializer());
        stPropsModule.addDeserializer(IRI.class, new RDF4JIRIDeserializer());
        stPropsModule.addDeserializer(Literal.class, new RDF4JLiteralDeserializer());
        stPropsModule.addSerializer(new RDF4JValueSerializer());
        stPropsModule.addSerializer(new STPropertiesPersistenceSerializer());
        // see: https://stackoverflow.com/a/18405958
        stPropsModule.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                                                          BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                // create an STProperties deserializer targeting a given type
                if (STProperties.class.isAssignableFrom(beanDesc.getBeanClass())) {
                    return new STPropertiesPersistenceDeserializer(beanDesc.getBeanClass(), exptManager);
                }

                return deserializer;
            }
        });
        // ensures that the order of the items in a set is preserved
        stPropsModule.addAbstractTypeMapping(Set.class, LinkedHashSet.class);
        ObjectMapper mapper = new ObjectMapper(fact);
        mapper.registerModule(stPropsModule);
        return mapper;
    }

    public static ObjectNode storeSTPropertiesToObjectNode(STProperties properties, boolean storeObjType) {
        ObjectMapper mapper = createObjectMapper();
        return storeSTPropertiesToObjectNode(mapper, properties, storeObjType);
    }

    public static ObjectNode storeSTPropertiesToObjectNode(ObjectMapper mapper, STProperties properties,
                                                           boolean storeObjType) {
        ObjectNode objectNode = mapper.valueToTree(properties);

        if (storeObjType) {
            ObjectNode newObjectNode = JsonNodeFactory.instance.objectNode();
            newObjectNode.put(SETTINGS_TYPE_PROPERTY, properties.getClass().getName());
            newObjectNode.setAll(objectNode);

            objectNode = newObjectNode;
        }

        return objectNode;
    }

    public static void storeSTPropertiesInYAML(STProperties properties, File propertiesFile,
                                               boolean storeObjType) throws IOException {
        ObjectMapper mapper = createObjectMapper();
        ObjectNode objectNode = storeSTPropertiesToObjectNode(mapper, properties, storeObjType);

        storeObjectNodeInYAML(objectNode, propertiesFile);
    }

    public static void storeObjectNodeInYAML(ObjectNode objectNode, File propertiesFile) throws IOException {
        if (!propertiesFile.getParentFile().exists()) { // if path doesn't exist, first create it
            propertiesFile.getParentFile().mkdirs();
        }

        ObjectMapper mapper = createObjectMapper();
        mapper.writeValue(propertiesFile, objectNode);
    }

    public static <T extends STProperties> T loadSTPropertiesFromYAMLFiles(Class<T> valueType,
                                                                           boolean loadObjType, File... propFiles) throws STPropertyAccessException {
        return loadSTPropertiesFromYAMLFiles(valueType, loadObjType, null, propFiles);
    }

    public static <T extends STProperties> T loadSTPropertiesFromYAMLFiles(Class<T> valueType,
                                                                           boolean loadObjType, ExtensionPointManager exptManager, File... propFiles)
            throws STPropertyAccessException {
        try {
            ObjectMapper objectMapper = createObjectMapper(exptManager);
            ObjectReader objReader = objectMapper.reader();

            List<ObjectNode> objs = new ArrayList<>(propFiles.length + 1);

            for (File propFile : propFiles) {
                if (!propFile.exists())
                    continue;


                try (Reader reader = new InputStreamReader(new FileInputStream(propFile),
                        StandardCharsets.UTF_8)) {
                    JsonNode jsonNode = objReader.readTree(reader);
                    if (jsonNode != null) {
                        if (!(jsonNode instanceof ObjectNode))
                            throw new STPropertyAccessException(
                                    "YAML file not containing an object node: " + propFile);

                        objs.add((ObjectNode) jsonNode);
                    }
                } catch (JsonMappingException e) {
                    // Swallow exception due to empty property files
                    if (!(e.getPath().isEmpty() && e.getMessage().contains("end-of-input"))) {
                        throw e;
                    }
                }

            }

            if (objs.isEmpty()) {
                objs.add(objectMapper.createObjectNode()); // fallback empty object
            }
            return loadSTPropertiesFromObjectNodes(valueType, loadObjType, objectMapper, objs.toArray(new ObjectNode[0]));
        } catch (IOException e) {
            throw new STPropertyAccessException(e);
        }
    }

    public static <T extends STProperties> T loadSTPropertiesFromObjectNodes(Class<T> valueType,
                                                                             boolean loadObjType, ObjectNode... objs) throws STPropertyAccessException {
        return loadSTPropertiesFromObjectNodes(valueType, loadObjType, createObjectMapper(null), objs);
    }

    @SuppressWarnings("unchecked")
    public static <T extends STProperties> T loadSTPropertiesFromObjectNodes(Class<T> valueType,
                                                                             boolean loadObjType, ObjectMapper objectMapper, ObjectNode... objs) throws STPropertyAccessException {
        try {
            if (objs == null || objs.length < 1) {
                throw new IllegalArgumentException("Missing ObjectNode to deserialize into an STProperties");
            }

            T result = null;
            for (ObjectNode obj : objs) {
                if (!loadObjType && obj.hasNonNull(SETTINGS_TYPE_PROPERTY)) {
                    obj.remove(SETTINGS_TYPE_PROPERTY);
                }

                T aPropertyObj = objectMapper.readValue(objectMapper.treeAsTokens(obj), valueType);

                if (result == null) {
                    result = aPropertyObj;
                } else {
                    STProperties.deepMerge(result, aPropertyObj);
                }
            }

            return result;
        } catch (IOException e) {
            throw new STPropertyAccessException(e);
        }
    }

    public static void setUserSettings(STProperties preferences, STUser user, String pluginID,
                                       boolean allowIncompletePropValueSet) throws STPropertyUpdateException {
        try {
            STPropertiesChecker preferencesChecker = STPropertiesChecker
                    .getModelConfigurationChecker(preferences).allowIncomplete(allowIncompletePropValueSet);
            if (!preferencesChecker.isValid()) {
                throw new InvalidSettingsUpdateException(preferencesChecker.getErrorMessage());
            }
            File settingsFile = getUserSettingsFile(user, pluginID);
            storeSTPropertiesInYAML(preferences, settingsFile, false);
        } catch (IOException e) {
            throw new STPropertyUpdateException(e);
        }
    }

    /*
     * Getter/Setter <STData>/system/plugins/<plugin>/system-preference-defaults.props
     */

    /**
     * Returns the value of a default user settings at system level
     *
     * @param valueType
     * @param pluginID
     * @return
     * @throws STPropertyAccessException
     */
    public static <T extends STProperties> T getUserSettingsDefault(Class<T> valueType, String pluginID) throws STPropertyAccessException {
        File defaultPropFile = getUserSettingsDefaultsFile(pluginID);

        return loadSettings(valueType, defaultPropFile);
    }

    /**
     * Sets the value of a default user setting at system level.
     *
     * @param settings
     * @param pluginID
     * @param allowIncompletePropValueSet
     */
    public static void setUserSettingsDefault(STProperties settings, String pluginID,
                                              boolean allowIncompletePropValueSet) throws STPropertyUpdateException {
        try {
            STPropertiesChecker settingsChecker = STPropertiesChecker
                    .getModelConfigurationChecker(settings).allowIncomplete(allowIncompletePropValueSet);
            if (!settingsChecker.isValid()) {
                throw new InvalidSettingsUpdateException(settingsChecker.getErrorMessage());
            }
            File settingsFile = getUserSettingsDefaultsFile(pluginID);
            storeSTPropertiesInYAML(settings, settingsFile, false);
        } catch (IOException e) {
            throw new STPropertyUpdateException(e);
        }
    }


    /*
     * Getter/Setter <STData>/projects/<projectname>/plugins/<plugin>/settings.props
     */


    /**
     * Returns the project settings about a plugin. The returned settings are (in descending order of
     * priority):
     * <ul>
     * <li>the values stored in the project-settings for the plugin</li>
     * <li>the default values stored at the system level</li>
     * <li>the default value hard-wired in the provided {@link STProperties} object</li>
     * </ul>
     *
     * @param valueType
     * @param project
     * @param pluginID
     * @throws STPropertyAccessException
     */
    public static <T extends STProperties> T getProjectSettings(Class<T> valueType, Project project,
                                                                String pluginID) throws STPropertyAccessException {
        return getProjectSettings(valueType, project, pluginID, false);
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
     * @param valueType
     * @param project
     * @param pluginID
     * @param explicit
     * @throws STPropertyAccessException
     */
    public static <T extends STProperties> T getProjectSettings(Class<T> valueType, Project project,
                                                                String pluginID, boolean explicit) throws STPropertyAccessException {
        File defaultPropFile = getProjectSettingsDefaultsFile(pluginID);
        File propFile = getProjectSettingsFile(project, pluginID);

        if (explicit) {
            return loadSettings(valueType, propFile);
        } else {
            return loadSettings(valueType, defaultPropFile, propFile);
        }
    }

    /**
     * Returns the value of a default project settings at system level
     *
     * @param valueType
     * @param pluginID
     * @return
     * @throws STPropertyAccessException
     */
    public static <T extends STProperties> T getProjectSettingsDefault(Class<T> valueType, String pluginID) throws STPropertyAccessException {
        File defaultPropFile = getProjectSettingsDefaultsFile(pluginID);

        return loadSettings(valueType, defaultPropFile);
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
            STPropertiesChecker settingsChecker = STPropertiesChecker
                    .getModelConfigurationChecker(settings).allowIncomplete(allowIncompletePropValueSet);
            if (!settingsChecker.isValid()) {
                throw new InvalidSettingsUpdateException(settingsChecker.getErrorMessage());
            }
            File settingsFile = getProjectSettingsFile(project, pluginID);
            storeSTPropertiesInYAML(settings, settingsFile, false);
        } catch (IOException e) {
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
     * Sets the value of a default project setting at system level.
     *
     * @param settings
     * @param pluginID
     * @param allowIncompletePropValueSet
     */
    public static void setProjectSettingsDefault(STProperties settings, String pluginID,
                                                 boolean allowIncompletePropValueSet) throws STPropertyUpdateException {
        try {
            STPropertiesChecker settingsChecker = STPropertiesChecker
                    .getModelConfigurationChecker(settings).allowIncomplete(allowIncompletePropValueSet);
            if (!settingsChecker.isValid()) {
                throw new InvalidSettingsUpdateException(settingsChecker.getErrorMessage());
            }

            File settingsFile = getProjectSettingsDefaultsFile(pluginID);
            storeSTPropertiesInYAML(settings, settingsFile, false);
        } catch (IOException e) {
            throw new STPropertyUpdateException(e);
        }
    }

    /*
     * SYSTEM SETTING
     */

    /*
     * Getter/Setter <STData>/system/plugins/<plugin>/settings.props
     */

    private static <T extends STProperties> T loadSettings(Class<T> valueType, File... settingsFiles)
            throws STPropertyAccessException {
        Optional<Bundle> bundle = Optional.ofNullable(FrameworkUtil.getBundle(STPropertiesManager.class));
        Optional<BundleContext> bundleContext = bundle.map(Bundle::getBundleContext);
        Optional<ServiceReference> sr = bundleContext.map(bc -> bc.getServiceReference(ExtensionPointManager.class.getName()));

        @Nullable
        ExtensionPointManager exptManager;

        if (sr.isPresent()) {
            exptManager = (ExtensionPointManager) bundleContext.get().getService(sr.get());
        } else {
            exptManager = null;
        }


        try {
            return loadSTPropertiesFromYAMLFiles(valueType, false, exptManager, settingsFiles);
        } finally {
            if (sr.isPresent()) {
                bundleContext.ifPresent(bc -> bc.ungetService(sr.get()));
            }
        }
    }

    public static <T extends STProperties> T getSystemSettings(Class<T> valueType, String pluginID)
            throws STPropertyAccessException {
        return loadSettings(valueType, getSystemSettingsFile(pluginID));
    }

    public static void setSystemSettings(STProperties settings, String pluginID)
            throws STPropertyUpdateException {
        setSystemSettings(settings, pluginID, false);
    }

    public static void setSystemSettings(STProperties settings, String pluginID,
                                         boolean allowIncompletePropValueSet) throws STPropertyUpdateException {
        try {
            STPropertiesChecker settingsChecker = STPropertiesChecker
                    .getModelConfigurationChecker(settings).allowIncomplete(allowIncompletePropValueSet);
            if (!settingsChecker.isValid()) {
                throw new InvalidSettingsUpdateException(settingsChecker.getErrorMessage());
            }

            File settingsFile = getSystemSettingsFile(pluginID);
            storeSTPropertiesInYAML(settings, settingsFile, false);
        } catch (IOException e) {
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
     * <STData>/pg_bindings/<projName>/<group>/plugins/<plugin>/settings.props
     */

    /**
     * Returns the Properties file <STData>/system/plugins/<plugin>/user-settings-defaults.props
     *
     * @param pluginID
     * @return
     * @throws STPropertyAccessException
     */
    private static File getUserSettingsDefaultsFile(String pluginID) {
        return new File(
                getSystemPropertyFolder(pluginID) + File.separator + USER_SETTINGS_DEFAULTS_FILE_NAME);
    }

    /**
     * Returns the Properties file <STData>/system/plugins/<plugin>/pu-settings-defaults.props
     *
     * @param pluginID
     * @return
     * @throws STPropertyAccessException
     */
    public static File getPUSettingsSystemDefaultsFile(String pluginID) {
        return new File(
                getSystemPropertyFolder(pluginID) + File.separator + PU_SETTINGS_SYSTEM_DEFAULTS_FILE_NAME);
    }

    /**
     * Returns the Properties file <STData>/system/plugins/<plugin>/project-settings-defaults.props
     *
     * @param pluginID
     * @return
     * @throws STPropertyAccessException
     */
    public static File getProjectSettingsDefaultsFile(String pluginID) {
        return new File(
                getSystemPropertyFolder(pluginID) + File.separator + PROJECT_SETTINGS_DEFAULTS_FILE_NAME);
    }

    /**
     * Returns the Properties file <STData>/system/plugins/<plugin>/settings.props
     *
     * @param pluginID
     * @return
     * @throws STPropertyAccessException
     */
    public static File getSystemSettingsFile(String pluginID) {
        return new File(getSystemPropertyFolder(pluginID) + File.separator + SYSTEM_SETTINGS_FILE_NAME);
    }

    /**
     * Returns the Properties file <STData>/projects/<projName>/plugins/<plugin>/pu-settings-defaults.props
     *
     * @param project
     * @param pluginID
     * @return
     * @throws STPropertyAccessException
     */
    private static File getPUSettingsProjectDefaultsFile(Project project, String pluginID) {
        return new File(getProjectPropertyFolder(project, pluginID) + File.separator
                + PU_SETTINGS_PROJECT_DEFAULTS_FILE_NAME);
    }

    /**
     * Returns the Properties file <STData>/projects/<projName>/plugins/<plugin>/settings.props
     *
     * @param project
     * @param pluginID
     * @return
     * @throws STPropertyAccessException
     */
    private static File getProjectSettingsFile(Project project, String pluginID) {
        return new File(
                getProjectPropertyFolder(project, pluginID) + File.separator + PROJECT_SETTINGS_FILE_NAME);
    }

    /**
     * Returns the Properties file <STData>/users/<user>/plugins/<plugin>/pu-settings-defaults.props
     *
     * @param user
     * @param pluginID
     * @return
     * @throws STPropertyAccessException
     */
    private static File getPUSettingsUserDefaultsFile(STUser user, String pluginID) {
        return new File(
                getUserPropertyFolder(user, pluginID) + File.separator + PU_SETTINGS_USER_DEFAULTS_FILE_NAME);
    }

    /**
     * Returns the Properties file <STData>/users/<user>/plugins/<plugin>/settings.props
     *
     * @param user
     * @param pluginID
     * @return
     * @throws STPropertyAccessException
     */
    private static File getUserSettingsFile(STUser user, String pluginID) {
        return new File(getUserPropertyFolder(user, pluginID) + File.separator + USER_SETTINGS_FILE_NAME);
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
    private static File getPUSettingsFile(Project project, STUser user, String pluginID) {
        return new File(
                getPUBindingPropertyFolder(project, user, pluginID) + File.separator + PU_SETTINGS_FILE_NAME);
    }

    /**
     * Returns the Properties file <STData>/pg_bindings/<projName>/<group>/plugins/<plugin>/settings.props
     *
     * @param project
     * @param group
     * @param pluginID
     * @return
     * @throws STPropertyAccessException
     */
    private static File getPGSettingsFile(Project project, UsersGroup group, String pluginID) {
        return new File(getPGBindingPropertyFolder(project, group, pluginID) + File.separator
                + PG_SETTINGS_FILE_NAME);
    }

    /*
     * Methods to retrieve the following folders: <STData>/system/plugins/<plugin>/
     * <STData>/projects/<projectname>/plugins/<plugin>/ <STData>/users/<username>/plugins/<plugin>/
     * <STData>/pu_binding/<projectname>/<username>/plugins/<plugin>/
     * <STData>/pg_binding/<projectname>/<group>/plugins/<plugin>/
     */

    /**
     * Returns the folder <STData>/system/plugins/<plugin>/
     *
     * @param pluginID
     * @return
     */
    public static File getSystemPropertyFolder(String pluginID) {
        return new File(Resources.getSystemDir() + File.separator + "plugins" + File.separator + pluginID);
    }

    /**
     * Returns the folder <STData>/projects/<projectName>/plugins/<plugin>/
     *
     * @param project
     * @param pluginID
     * @return
     */
    public static File getProjectPropertyFolder(Project project, String pluginID) {
        return new File(Resources.getProjectsDir() + File.separator + project.getName() + File.separator
                + "plugins" + File.separator + pluginID);
    }

    /**
     * Returns the folder <STData>/users/<user>/plugins/<plugin>/
     *
     * @param user
     * @param pluginID
     * @return
     */
    public static File getUserPropertyFolder(STUser user, String pluginID) {
        return new File(Resources.getUsersDir() + File.separator + STUser.encodeUserIri(user.getIRI())
                + File.separator + "plugins" + File.separator + pluginID);
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
        return new File(Resources.getProjectUserBindingsDir() + File.separator + project.getName()
                + File.separator + STUser.encodeUserIri(user.getIRI()) + File.separator + "plugins"
                + File.separator + pluginID);
    }

    /**
     * Returns the folder <STData>/pu_bindings/<projectName>/<group>/plugins/<plugin>/
     *
     * @param project
     * @param group
     * @param pluginID
     * @return
     */
    public static File getPGBindingPropertyFolder(Project project, UsersGroup group, String pluginID) {
        return new File(Resources.getProjectGroupBindingsDir() + File.separator + project.getName()
                + File.separator + UsersGroup.encodeGroupIri(group.getIRI()) + File.separator + "plugins"
                + File.separator + pluginID);
    }

}
