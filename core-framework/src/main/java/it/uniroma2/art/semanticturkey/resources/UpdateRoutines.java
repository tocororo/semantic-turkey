/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http//www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is Semantic Turkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2010.
 * All Rights Reserved.
 *
 * Semantic Turkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about Semantic Turkey can be obtained at 
 * http://semanticturkey.uniroma2.it
 *
 */

package it.uniroma2.art.semanticturkey.resources;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang3.Functions;
import org.apache.commons.lang3.Functions.FailableConsumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLUtil;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import it.uniroma2.art.semanticturkey.SemanticTurkey;
import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.config.customservice.CustomServiceDefinitionStore;
import it.uniroma2.art.semanticturkey.config.invokablereporter.InvokableReporterStore;
import it.uniroma2.art.semanticturkey.config.resourcemetadata.ResourceMetadataAssociationStore;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.rbac.RBACManager.DefaultRole;
import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.utilities.Utilities;

import static java.util.stream.Collectors.toList;

/**
 * This class contains various integrity checks which are launched when Semantic Turkey is being started and
 * the SemanticTurkeyData directory is found in the user directory. Since previous versions of Semantic Turkey
 * may use a different folder structure for SemanticTurkeyData, this set of routines is in charge of aligning
 * a potential older version of the directory with the one which is being used.
 * 
 * <bold>note:</bold> this class needs to be updated to the changes after the mover to karaf.<br/>
 * In order to check if there were needs of updates on the data, the version was once retrieved from the
 * SemanticTurkeyData, and this way it made sense, as it was telling which version of ST last touched the data
 * folder. If the version was older than the version of the software being run, a check for updates to be made
 * was performed, and then the version in the data was updated with the one in the software.<br/>
 * Instead, now the semantic turkey version is retrieved from a file (it.uniroma2.art.semanticturkey.cfg )
 * which is contained in the server.<br/>
 * For the future, we should make the terminology more clear, and do the following:
 * <ul>
 * <li>suppress the version number hard coded in {@link SemanticTurkey} and replace its check with the check
 * from the cfg file in karaf</li>
 * <li>restore a property file in SemanticTurkeyData, containing the version of the latest ST which edited the
 * data folder</li>
 * </ul>
 * 
 * 
 * @author Armando Stellato
 * 
 */
public class UpdateRoutines {

	protected static Logger logger = LoggerFactory.getLogger(UpdateRoutines.class);

	public static void startUpdatesCheckAndRepair() throws IOException, ProjectAccessException {
		VersionNumber stVersionNumber = Config.getVersionNumber();
		VersionNumber stDataVersionNumber = Config.getSTDataVersionNumber();
		logger.debug("version number of installed Semantic Turkey is: " + stVersionNumber);
		logger.debug("version number of Semantic Turkey currently saved in data folder is: "
				+ stDataVersionNumber);

		if (stVersionNumber.compareTo(stDataVersionNumber) > 0) {
			if (stDataVersionNumber.compareTo(new VersionNumber(3, 0, 0)) < 0) {
				alignFromPreviousTo3();
			}
			if (stDataVersionNumber.compareTo(new VersionNumber(4, 0, 0)) < 0) {
				alignFrom3To4();
			}
			if (stDataVersionNumber.compareTo(new VersionNumber(5, 0, 0)) < 0) {
				alignFrom4To5();
			}
			if (stDataVersionNumber.compareTo(new VersionNumber(6, 0, 0)) < 0) {
				alignFrom5To6();
			}
			if (stDataVersionNumber.compareTo(new VersionNumber(7, 0, 0)) < 0) {
				alignFrom6To7();
			}
			if (stDataVersionNumber.compareTo(new VersionNumber(8, 0, 0)) < 0) {
				alignFrom7To8();
			}
			if (stDataVersionNumber.compareTo(new VersionNumber(8, 0, 1)) < 0) {
				alignFrom8To801();
			}
			if (stDataVersionNumber.compareTo(new VersionNumber(9, 0, 0)) < 0) {
				alignFrom801To90();
			}

			if (stDataVersionNumber.compareTo(new VersionNumber(9, 0, 1)) < 0) {
				alignFrom90To10();
			}


			Config.setSTDataVersionNumber(stVersionNumber);
		}

	}

	private static void alignFromPreviousTo3() throws IOException {
		logger.debug("Version 3.0.0 added capabilities to some roles");
		// In doubt, update all roles
		Role[] roles = { DefaultRole.LEXICOGRAPHER, DefaultRole.MAPPER, DefaultRole.ONTOLOGIST,
				DefaultRole.PROJECTMANAGER, DefaultRole.RDF_GEEK, DefaultRole.THESAURUS_EDITOR,
				DefaultRole.VALIDATOR };
		updateRoles(roles);

		logger.debug("Version 3.0.0 added new properties to the default project preferences");
		updatePUSettingsSystemDefaults(STPropertiesManager.CORE_PLUGIN_ID);
		updatePUSettingsSystemDefaults("it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine");
	}

	private static void alignFrom3To4() throws IOException {
		logger.debug("Version 4.0.0 renamed some settings files");
		// users\<username>\plugins\<plugin>\system-preferences.props -> settings.props
		// users\<username>\plugins\<plugin>\project-preference-defaults.props -> pu-settings-defaults.props
		List<File> userDirectories = listSubFolders(Resources.getUsersDir());
		for (File userDirectory : userDirectories) {
			File pluginsFolder = new File(userDirectory, "plugins");
			if (pluginsFolder.exists()) {
				List<File> pluginsFolders = listSubFolders(pluginsFolder);
				for (File pluginFolder : pluginsFolders) {
					renameFile(pluginFolder, "system-preferences.props", "settings.props");
					renameFile(pluginFolder, "project-preferences-defaults.props",
							"pu-settings-defaults.props");
				}
			}
		}

		// system\plugins\<plugin>\system-preferences-defaults.props -> user-settings-defaults.props
		// system\plugins\<plugin>\project-preferences-defaults.props -> pu-settings-defaults.cfg
		List<File> sysPluginFolders = listSubFolders(new File(Resources.getSystemDir(), "plugins"));
		for (File pluginFolder : sysPluginFolders) {
			renameFile(pluginFolder, "system-preferences-defaults", "user-settings-defaults.props");
			renameFile(pluginFolder, "project-preferences-defaults.props", "pu-settings-defaults.props");
		}

		// projects\<projectname>\plugins\<plugin>\preferences-defaults.props -> pu-settings-defaults.props
		List<File> projDirectories = listSubFolders(Resources.getProjectsDir());
		for (File projDirectory : projDirectories) {
			File pluginsFolder = new File(projDirectory, "plugins");
			if (pluginsFolder.exists()) {
				List<File> pluginsFolders = listSubFolders(pluginsFolder);
				for (File pluginFolder : pluginsFolders) {
					renameFile(pluginFolder, "preferences-defaults.props", "pu-settings-defaults.props");
				}
			}
		}

		// pu_binding\<projectname>\<username>\plugins\<plugin>\preferences.props -> settings.props
		List<File> puProjectDirectories = listSubFolders(Resources.getProjectUserBindingsDir());
		for (File puProjDir : puProjectDirectories) {
			List<File> puUserDirectories = listSubFolders(puProjDir);
			for (File puUserDir : puUserDirectories) {
				File pluginsFolder = new File(puUserDir, "plugins");
				if (pluginsFolder.exists()) {
					List<File> pluginsFolders = listSubFolders(pluginsFolder);
					for (File pluginFolder : pluginsFolders) {
						renameFile(pluginFolder, "preferences.props", "settings.props");
					}
				}
			}
		}

		logger.debug("Version 4.0.0 added lurker role and added a capability to projectmanager");
		Role[] roles = { DefaultRole.LURKER, DefaultRole.PROJECTMANAGER, };
		updateRoles(roles);

		logger.debug("Version 4.0.0 added groups and pg_bindings folders");
		Resources.initializeGroups();
	}

	private static void alignFrom4To5() throws IOException {
		logger.debug("Version 5.0.0 added a capability to some roles");
		Role[] roles = { DefaultRole.LEXICOGRAPHER, DefaultRole.MAPPER, DefaultRole.ONTOLOGIST,
				DefaultRole.PROJECTMANAGER, DefaultRole.RDF_GEEK, DefaultRole.THESAURUS_EDITOR };
		updateRoles(roles);

		logger.debug("Version 5.0.0 removed a property from the default project preferences");
		updatePUSettingsSystemDefaults(STPropertiesManager.CORE_PLUGIN_ID);
		updatePUSettingsSystemDefaults("it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine");
	}

	private static void alignFrom5To6() throws IOException {
		logger.debug("Version 6.0.0 added a docs folder under system/");
		Resources.getDocsDir().mkdirs();

		logger.debug("Version 6.0.0 changed a property from the default project preferences");
		updatePUSettingsSystemDefaults(STPropertiesManager.CORE_PLUGIN_ID);
		updatePUSettingsSystemDefaults("it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine");
	}

	private static void alignFrom6To7() throws IOException {
		logger.debug("Version 7.0.0 updated the class_tree_filter in the pu-settings-defaults");
		Resources.getDocsDir().mkdirs();
		updatePUSettingsSystemDefaults(STPropertiesManager.CORE_PLUGIN_ID);
	}

	private static void alignFrom7To8()
			throws UnsupportedRDFormatException, IOException, ProjectAccessException {
		logger.debug("Version 8.0 changed the namespace " + "associated with the metadata registry");
		File catalogFile = new File(Config.getDataDir(), "metadataRegistry/catalog.ttl");
		if (catalogFile.exists()) {
			replaceNamespaceDefinition(
					new SimpleNamespace("mdreg", "http://semanticturkey.uniroma2.it/ns/mdreg#"),
					new SimpleNamespace("mdr", "http://semanticturkey.uniroma2.it/ns/mdr#"), catalogFile);
		}

		logger.debug(
				"Version 8.0 added capabilities about customService, invokableReporter and resourceMetadata");
		Role[] roles = { DefaultRole.LEXICOGRAPHER, DefaultRole.LURKER, DefaultRole.MAPPER,
				DefaultRole.ONTOLOGIST, DefaultRole.PROJECTMANAGER, DefaultRole.RDF_GEEK,
				DefaultRole.THESAURUS_EDITOR, DefaultRole.VALIDATOR };
		updateRoles(roles);

		logger.debug("Version 8.0 added predefined custom services and invokable reporters");
		updateCustomServices("it.uniroma2.art.semanticturkey.customservice.OntoLexLemonReport",
				"it.uniroma2.art.semanticturkey.customservice.OWLReport",
				"it.uniroma2.art.semanticturkey.customservice.SKOSReport");
		updateInvokableReporters("it.uniroma2.art.semanticturkey.invokablereporter.OntoLexLemonReport",
				"it.uniroma2.art.semanticturkey.invokablereporter.OWLReport",
				"it.uniroma2.art.semanticturkey.invokablereporter.SKOSReport");

		// set the default <undetermined, DublinCore metadata> association if project has
		// updateForRoles: resource, and DC properties as creation and modification properties
		logger.debug("Version 8.0 replaced updateForRoles with the resource metadata mechanism");

		Collection<AbstractProject> projects = ProjectManager.listProjects();
		for (AbstractProject absProj : ProjectManager.listProjects()) {
			if (absProj instanceof Project) {
				Project proj = (Project) absProj;
				Set<RDFResourceRole> updateForRoles = proj.getUpdateForRoles();
				String modificationDateProp = proj.getProperty(Project.MODIFICATION_DATE_PROP_DEPRECATED);
				String creationDateProp = proj.getProperty(Project.CREATION_DATE_PROP_DEPRECATED);
				if (updateForRoles.size() == 1 && updateForRoles.contains(RDFResourceRole.undetermined)
						&& modificationDateProp != null
						&& modificationDateProp.equals("http://purl.org/dc/terms/modified")
						&& creationDateProp != null
						&& creationDateProp.equals("http://purl.org/dc/terms/created")) {
					File configFolder = STPropertiesManager.getProjectPropertyFolder(proj,
							ResourceMetadataAssociationStore.class.getName());
					FileUtils.forceMkdir(configFolder);
					String configName = "default dc association.cfg";
					try (InputStream is = UpdateRoutines.class.getResourceAsStream(
							"/it/uniroma2/art/semanticturkey/config/resourcemetadata/" + configName)) {
						FileUtils.copyInputStreamToFile(is, new File(configFolder, configName));
					}
				}
			}
		}
	}

	private static void alignFrom8To801() throws IOException {
		logger.debug("Version 8.0.1 updated the languages in the project-settings-defaults");
		updateProjectSettingsDefaults();
	}

	private static void alignFrom801To90() throws FileNotFoundException, IOException {
		logger.debug(
				"Version 9.0 updated the URIGenerator and the RenderingEngine to the new style (including persistence of properties as YAML). Upgrade deferred on the fist attempt to obtain the description of the project, because we need the extension point manager");

		updateStoredPropertiesForComponentRenaming(
				"it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine",
				"it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngine");

	}

	private static void alignFrom90To10() throws IOException {
		logger.debug(
				"Version 9.0.1 updated Semantic Turkey core settings to the new style (including persistence of properties as YAML)");

		ObjectMapper om = STPropertiesManager.createObjectMapper();

		// upgrade system settings
		File oldCoreSystemSettingsFile = STPropertiesManager.getSystemSettingsFile("it.uniroma2.art.semanticturkey");
		File newCoreSystemSettingsFile = STPropertiesManager.getSystemSettingsFile(SemanticTurkeyCoreSettingsManager.class.getName());
		alignFrom90to10_upgradeCoreSystemSettings(om, oldCoreSystemSettingsFile, newCoreSystemSettingsFile);

		// upgrade pu settings system defaults
		File oldCorePUSettingsSystemDefaultsFile = STPropertiesManager.getPUSettingsSystemDefaultsFile("it.uniroma2.art.semanticturkey");
		File newCorePUSettingsSystemDefaultsFile = STPropertiesManager.getPUSettingsSystemDefaultsFile(SemanticTurkeyCoreSettingsManager.class.getName());

		alignFrom90to10_upgradeCorePUSettings(om, oldCorePUSettingsSystemDefaultsFile, newCorePUSettingsSystemDefaultsFile);

		for (File projectDir : Resources.getProjectsDir()
				.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
			// upgrade project settings
			File oldCoreProjectSettingsFile = FileUtils.getFile(projectDir, "plugins", "it.uniroma2.art.semanticturkey", "settings.props");
			File newCoreProjectSettingsFile = FileUtils.getFile(projectDir, "plugins", SemanticTurkeyCoreSettingsManager.class.getName(), "settings.props");
			alignFrom90to10_upgradeCoreProjectSettings(om, oldCoreProjectSettingsFile, newCoreProjectSettingsFile);

			// upgrade pu settings project default
			File oldCorePUSettingsProjectDefaultsFile = FileUtils.getFile(projectDir, "plugins", "it.uniroma2.art.semanticturkey", "pu-settings-defaults.props");
			File newCorePUSettingsProjectDefaultsFile = FileUtils.getFile(projectDir, "plugins", SemanticTurkeyCoreSettingsManager.class.getName(), "pu-settings-defaults.props");

			alignFrom90to10_upgradeCorePUSettings(om, oldCorePUSettingsProjectDefaultsFile, newCorePUSettingsProjectDefaultsFile);
		}

		// upgrade project settings system defaults
		File oldCoreProjectSettingsDefaultFile = STPropertiesManager.getProjectSettingsDefaultsFile("it.uniroma2.art.semanticturkey");
		File newCoreProjectSettingsDefaultFile = STPropertiesManager.getProjectSettingsDefaultsFile(SemanticTurkeyCoreSettingsManager.class.getName());
		alignFrom90to10_upgradeCoreProjectSettings(om, oldCoreProjectSettingsDefaultFile, newCoreProjectSettingsDefaultFile);

		for (File userDir : Resources.getUsersDir()
				.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
			// upgrade pu settings user defaults
			File oldCorePUSettingsUserDefaultsFile = FileUtils.getFile(userDir, "plugins", "it.uniroma2.art.semanticturkey", "pu-settings-defaults.props");
			File newCorePUSettingsUserDefaultsFile = FileUtils.getFile(userDir, "plugins", SemanticTurkeyCoreSettingsManager.class.getName(), "pu-settings-defaults.props");
			alignFrom90to10_upgradeCorePUSettings(om, oldCorePUSettingsUserDefaultsFile, newCorePUSettingsUserDefaultsFile);
		}


		for (File projectBindingsDir : Resources.getProjectUserBindingsDir()
				.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
			for (File puBindingsDir : projectBindingsDir
					.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
				// upgrade pu settings
				File oldCorePUSettingsFile = FileUtils.getFile(puBindingsDir, "plugins", "it.uniroma2.art.semanticturkey", "settings.props");
				File newCorePUSettingsFile = FileUtils.getFile(puBindingsDir, "plugins", SemanticTurkeyCoreSettingsManager.class.getName(), "settings.props");

				alignFrom90to10_upgradeCorePUSettings(om, oldCorePUSettingsFile, newCorePUSettingsFile);
			}
		}

		for (File projectBindingsDir : Resources.getProjectGroupBindingsDir()
				.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
			for (File pgBindingsDir : projectBindingsDir
					.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
				// upgrade pg settings
				File oldCorePUSettingsFile = FileUtils.getFile(pgBindingsDir, "plugins", "it.uniroma2.art.semanticturkey", "settings.props");
				File newCorePUSettingsFile = FileUtils.getFile(pgBindingsDir, "plugins", SemanticTurkeyCoreSettingsManager.class.getName(), "settings.props");

				alignFrom90to10_upgradeCorePUSettings(om, oldCorePUSettingsFile, newCorePUSettingsFile); // core pg settings are handled the same as pu settings
			}
		}

	}

	private static void alignFrom90to10_upgradeCorePUSettings(ObjectMapper om, File oldCorePUSettingsFile, File newCorePUSettingsFile) throws IOException {
		if (oldCorePUSettingsFile.isFile()) {
			Properties properties = new Properties();
			try (FileInputStream fis = new FileInputStream(oldCorePUSettingsFile)) {
				properties.load(fis);
			}

			ValueFactory vf = SimpleValueFactory.getInstance();

			ObjectNode newCorePUSettingsNode = JsonNodeFactory.instance.objectNode();
			convertPropertiesSettingToYAML(properties, "editing_language", newCorePUSettingsNode, "editingLanguage", String::valueOf);
			convertPropertiesSettingToYAML(properties, "filter_value_languages", newCorePUSettingsNode, "filterValueLanguages", v -> om.readTree(new StringReader(v)));
			convertPropertiesSettingToYAML(properties, "active_schemes", newCorePUSettingsNode, "activeSchemes", v -> Arrays.stream(v.split(",")).filter(StringUtils::isNotBlank).map(vf::createIRI).collect(toList()));
			convertPropertiesSettingToYAML(properties, "active_lexicon", newCorePUSettingsNode, "activeLexicon", vf::createIRI);
			convertPropertiesSettingToYAML(properties, "show_flags", newCorePUSettingsNode, "showFlags", Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "project_theme", newCorePUSettingsNode, "projectTheme", String::valueOf);
			convertPropertiesSettingToYAML(properties, "class_tree_root", newCorePUSettingsNode, Arrays.asList("classTree", "rootClass"), vf::createIRI);
			convertPropertiesSettingToYAML(properties, "class_tree_filter", newCorePUSettingsNode, Arrays.asList("classTree", "filter"), v -> om.readTree(new StringReader(v)));
			convertPropertiesSettingToYAML(properties, "show_instances_number", newCorePUSettingsNode, Arrays.asList("classTree", "showInstancesNumber"), Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "instance_list_visualization", newCorePUSettingsNode, Arrays.asList("instanceList", "visualization"), String::valueOf);
			convertPropertiesSettingToYAML(properties, "instance_list_safe_to_go_limit", newCorePUSettingsNode, Arrays.asList("instanceList", "safeToGoLimit"), Integer::valueOf);
			convertPropertiesSettingToYAML(properties, "concept_tree_base_broader_prop", newCorePUSettingsNode, Arrays.asList("conceptTree", "baseBroaderProp"), vf::createIRI);
			convertPropertiesSettingToYAML(properties, "concept_tree_broader_props", newCorePUSettingsNode, Arrays.asList("conceptTree", "broaderProps"), v -> Arrays.stream(v.split(",")).filter(StringUtils::isNotBlank).map(vf::createIRI).collect(toList()));
			convertPropertiesSettingToYAML(properties, "concept_tree_narrower_props", newCorePUSettingsNode, Arrays.asList("conceptTree", "narrowerProps"), v -> Arrays.stream(v.split(",")).filter(StringUtils::isNotBlank).map(vf::createIRI).collect(toList()));
			convertPropertiesSettingToYAML(properties, "concept_tree_include_subprops", newCorePUSettingsNode, Arrays.asList("conceptTree", "includeSubProps"), Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "concept_tree_sync_inverse", newCorePUSettingsNode, Arrays.asList("conceptTree", "syncInverse"), Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "concept_tree_visualization", newCorePUSettingsNode, Arrays.asList("conceptTree", "visualization"), String::valueOf);
			convertPropertiesSettingToYAML(properties, "concept_tree_multischeme_mode", newCorePUSettingsNode, Arrays.asList("conceptTree", "multischemeMode"), String::valueOf);
			convertPropertiesSettingToYAML(properties, "concept_tree_safe_to_go_limit", newCorePUSettingsNode, Arrays.asList("conceptTree", "safeToGoLimit"), Integer::valueOf);
			convertPropertiesSettingToYAML(properties, "lex_entry_list_visualization", newCorePUSettingsNode, Arrays.asList("lexEntryList", "visualization"), String::valueOf);
			convertPropertiesSettingToYAML(properties, "lex_entry_list_index_lenght", newCorePUSettingsNode, Arrays.asList("lexEntryList", "indexLength"), Integer::valueOf);
			convertPropertiesSettingToYAML(properties, "lex_entry_list_safe_to_go_limit", newCorePUSettingsNode, Arrays.asList("lexEntryList", "safeToGoLimit"), Integer::valueOf);
			convertPropertiesSettingToYAML(properties, "res_view_default_concept_type", newCorePUSettingsNode, Arrays.asList("resourceView", "defaultConceptType"), String::valueOf);
			convertPropertiesSettingToYAML(properties, "res_view_default_lexentry_type", newCorePUSettingsNode, Arrays.asList("resourceView", "defaultLexEntryType"), String::valueOf);
			convertPropertiesSettingToYAML(properties, "resViewPartitionFilter", newCorePUSettingsNode, Arrays.asList("resourceView", "resViewPartitionFilter"), v -> om.readTree(new StringReader(v)));
			convertPropertiesSettingToYAML(properties, "graph_partition_filter", newCorePUSettingsNode, "graphViewPartitionFilter", v -> om.readTree(new StringReader(v)));
			convertPropertiesSettingToYAML(properties, "hide_literal_graph_nodes", newCorePUSettingsNode, "hideLiteralGraphNodes", Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "search_restrict_lang", newCorePUSettingsNode, Arrays.asList("searchSettings", "restrictLang"), Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "search_languages", newCorePUSettingsNode, Arrays.asList("searchSettings", "languages"), v -> om.readTree(new StringReader(v)));
			convertPropertiesSettingToYAML(properties, "search_include_locales", newCorePUSettingsNode, Arrays.asList("searchSettings", "includeLocales"), Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "search_use_autocomplete", newCorePUSettingsNode, Arrays.asList("searchSettings", "useAutocompletion"), Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "notifications_status", newCorePUSettingsNode, Arrays.asList("searchSettings", "notificationsStatus"), String::valueOf);

			if (newCorePUSettingsNode.fields().hasNext()) {
				STPropertiesManager.storeObjectNodeInYAML(newCorePUSettingsNode, newCorePUSettingsFile);
			}

		}

	}

	private static void alignFrom90to10_upgradeCoreProjectSettings(ObjectMapper om, File oldCoreProjectSettingsFile, File newCoreProjectSettingsFile) throws IOException {
		if (oldCoreProjectSettingsFile.isFile()) {
			Properties properties = new Properties();
			try (FileInputStream fis = new FileInputStream(oldCoreProjectSettingsFile)) {
				properties.load(fis);
			}

			ObjectNode newCoreProjectSettingsNode = JsonNodeFactory.instance.objectNode();
			convertPropertiesSettingToYAML(properties, "languages", newCoreProjectSettingsNode, "languages", v -> om.readTree(new StringReader(v)));
			convertPropertiesSettingToYAML(properties, "label_clash_mode", newCoreProjectSettingsNode, "labelClashMode", String::valueOf);

			if (newCoreProjectSettingsNode.fields().hasNext()) {
				STPropertiesManager.storeObjectNodeInYAML(newCoreProjectSettingsNode, newCoreProjectSettingsFile);
			}

		}
	}

	private static void alignFrom90to10_upgradeCoreSystemSettings(ObjectMapper om, File oldCoreSystemSettingsFile, File newCoreSystemSettingsFile) throws IOException {
		if (oldCoreSystemSettingsFile.isFile()) {
			Properties properties = new Properties();
			try (FileInputStream fis = new FileInputStream(oldCoreSystemSettingsFile)) {
				properties.load(fis);
			}

			ObjectNode newCoreSystemSettingsNode = JsonNodeFactory.instance.objectNode();

			convertPropertiesSettingToYAML(properties, "remote_configs", newCoreSystemSettingsNode, "remoteConfigs", v -> om.readTree(new StringReader(v)));
			convertPropertiesSettingToYAML(properties, "experimental_features_enabled", newCoreSystemSettingsNode, "experimentalFeaturesEnabled", Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "privacy_statement_available", newCoreSystemSettingsNode, "privacyStatementAvailable", Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "show_flags", newCoreSystemSettingsNode, "showFlags", Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "home_content", newCoreSystemSettingsNode, "homeContent", String::valueOf);
			convertPropertiesSettingToYAML(properties, "proj_creation_default_acl_set_universal_access", newCoreSystemSettingsNode, Arrays.asList("projectCreation", "aclUniversalAccessDefault"), Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "proj_creation_default_open_at_startup", newCoreSystemSettingsNode, Arrays.asList("projectCreation", "openAtStartUpDefault"), Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "preload.profiler.treshold_bytes", newCoreSystemSettingsNode, Arrays.asList("preload", "profiler", "threshold"), v -> v + " B");

			convertPropertiesSettingToYAML(properties, "stDataVersion", newCoreSystemSettingsNode, "stDataVersion", String::valueOf);
			convertPropertiesSettingToYAML(properties, "mail.admin.address", newCoreSystemSettingsNode, Arrays.asList("mail", "admin", "address"), v -> {
				v = v.trim();
				return v.startsWith("[")
						? new ObjectMapper().readValue(v, new TypeReference<Set<String>>() {
				})
						: new HashSet<>(Collections.singletonList(v));
			});
			convertPropertiesSettingToYAML(properties, "mail.smtp.host", newCoreSystemSettingsNode, Arrays.asList("mail", "smtp", "host"), String::valueOf);
			convertPropertiesSettingToYAML(properties, "mail.smtp.port", newCoreSystemSettingsNode, Arrays.asList("mail", "smtp", "port"), Integer::valueOf);
			convertPropertiesSettingToYAML(properties, "mail.smtp.auth", newCoreSystemSettingsNode, Arrays.asList("mail", "smtp", "auth"), Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "mail.smtp.ssl.enable", newCoreSystemSettingsNode, Arrays.asList("mail", "smtp", "ssl", "enable"), Boolean::valueOf);
			convertPropertiesSettingToYAML(properties, "mail.smtp.starttls.enable", newCoreSystemSettingsNode, Arrays.asList("mail", "smtp", "starttls", "enable"), Boolean::valueOf);

			convertPropertiesSettingToYAML(properties, "mail.from.address", newCoreSystemSettingsNode, Arrays.asList("mail", "from", "address"), String::valueOf);
			convertPropertiesSettingToYAML(properties, "mail.from.password", newCoreSystemSettingsNode, Arrays.asList("mail", "from", "password"), String::valueOf);
			convertPropertiesSettingToYAML(properties, "mail.from.alias", newCoreSystemSettingsNode, Arrays.asList("mail", "from", "alias"), String::valueOf);

			convertPropertiesSettingToYAML(properties, "pmki.vb_connection_config", newCoreSystemSettingsNode, Arrays.asList("pmki", "vbConnectionConfig"), v -> {
				JsonNode jsonNode = om.readTree(new StringReader(v));
				if (jsonNode instanceof ObjectNode) {
					ObjectNode jsonObject = (ObjectNode) jsonNode;
					// rename vbUrl to vbURL
					JsonNode vbURL = jsonObject.remove("vbUrl");
					if (vbURL != null) {
						jsonObject.set("vbURL", vbURL);
					}
				}
				return jsonNode;
			});

			if (newCoreSystemSettingsNode.fields().hasNext()) {
				STPropertiesManager.storeObjectNodeInYAML(newCoreSystemSettingsNode, newCoreSystemSettingsFile);
			}
		}
	}

	private static <T> void convertPropertiesSettingToYAML(Properties properties, String oldProp, ObjectNode settingsObjectNode, String yamlProp, Functions.FailableFunction<String, T, IOException> propValueCoverter) throws IOException {
		convertPropertiesSettingToYAML(properties, oldProp, settingsObjectNode, Collections.singletonList(yamlProp), propValueCoverter);
	}


	private static <T> void convertPropertiesSettingToYAML(Properties properties, String oldProp, ObjectNode settingsObjectNode, List<String> yamlPropPath, Functions.FailableFunction<String, T, IOException> propValueCoverter) throws IOException {
		String oldPropValue = properties.getProperty(oldProp);

		if (StringUtils.isNoneEmpty(oldPropValue)) {
			T yamlValue = propValueCoverter.apply(oldPropValue);

			ObjectNode ctxObj = settingsObjectNode;

			for (String yamlProp : Iterables.limit(yamlPropPath, yamlPropPath.size() - 1)) {
				JsonNode intermediateNode = ctxObj.get(yamlProp);
				if (intermediateNode instanceof ObjectNode) {
					ctxObj = (ObjectNode) intermediateNode;
				} else {
					ctxObj = ctxObj.putObject(yamlProp);
				}
			}

			String leaveYamlProp = Iterables.getLast(yamlPropPath);
			if (yamlValue instanceof JsonNode) {
				ctxObj.set(leaveYamlProp, (JsonNode) yamlValue);
			} else {
				ctxObj.putPOJO(leaveYamlProp, yamlValue);
			}
		}
	}

	public static synchronized void upgradeURIGeneratorAndRenderingEngine(ExtensionPointManager exptMgr,
			File projectDir) throws IOException {
		File projectFile = new File(projectDir, Project.INFOFILENAME);
		Properties projectProperties = new Properties();
		boolean projectEdited = false;
		try (FileInputStream is = new FileInputStream(projectFile)) {
			projectProperties.load(is);
		}

		String oldUriGenFactoryIDProp = Project.URI_GENERATOR_FACTORY_ID_PROP;
		String oldUriGenConfigTypeProp = Project.URI_GENERATOR_CONFIGURATION_TYPE_PROP;
		String oldUriGenConfigFilename = "urigen.config";
		String newUriGenConfigFilename = "urigen.cfg";
		Map<String, String> uriGenFactoryMapping = ImmutableMap.of(
				"it.uniroma2.art.semanticturkey.plugin.impls.urigen.NativeTemplateBasedURIGeneratorFactory",
				"it.uniroma2.art.semanticturkey.extension.impl.urigen.template.NativeTemplateBasedURIGenerator",
				"it.uniroma2.art.semanticturkey.plugin.impls.urigen.CODAURIGeneratorFactory",
				"it.uniroma2.art.semanticturkey.extension.impl.urigen.coda.CODAURIGenerator");
		Map<String, String> uriGenConfigMapping = ImmutableMap.of(
				"it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.NativeTemplateBasedURIGeneratorConfiguration",
				"it.uniroma2.art.semanticturkey.extension.impl.urigen.template.NativeTemplateBasedURIGeneratorConfiguration",
				"it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.CODATemplateBasedURIGeneratorConfiguration",
				"it.uniroma2.art.semanticturkey.extension.impl.urigen.coda.CODATemplateBasedURIGeneratorConfiguration",
				"it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.CODAAnyURIGeneratorConfiguration",
				"it.uniroma2.art.semanticturkey.extension.impl.urigen.coda.CODAAnyURIGeneratorConfiguration");

		projectEdited |= upgradeAndRenameProjectBoundComponent(exptMgr, projectDir, projectProperties,
				oldUriGenFactoryIDProp, oldUriGenConfigTypeProp, oldUriGenConfigFilename,
				newUriGenConfigFilename, uriGenFactoryMapping, uriGenConfigMapping);

		String oldRendEngFactoryIDProp = Project.RENDERING_ENGINE_FACTORY_ID_PROP;
		String oldRendEngConfigTypeProp = Project.RENDERING_ENGINE_CONFIGURATION_TYPE_PROP;
		String oldRendEngConfigFilename = "rendering.config";
		String newRendEngConfigFilename = "rendering.cfg";
		Map<String, String> rendEngFactoryMapping = ImmutableMap.of(
				"it.uniroma2.art.semanticturkey.plugin.impls.rendering.RDFSRenderingEngineFactory",
				"it.uniroma2.art.semanticturkey.extension.impl.rendering.rdfs.RDFSRenderingEngine",
				"it.uniroma2.art.semanticturkey.plugin.impls.rendering.SKOSRenderingEngineFactory",
				"it.uniroma2.art.semanticturkey.extension.impl.rendering.skos.SKOSRenderingEngine",
				"it.uniroma2.art.semanticturkey.plugin.impls.rendering.SKOSXLRenderingEngineFactory",
				"it.uniroma2.art.semanticturkey.extension.impl.rendering.skosxl.SKOSXLRenderingEngine",
				"it.uniroma2.art.semanticturkey.plugin.impls.rendering.OntoLexLemonRenderingEngineFactory",
				"it.uniroma2.art.semanticturkey.extension.impl.rendering.ontolexlemon.OntoLexLemonRenderingEngine");
		Map<String, String> rendEngConfigMapping = ImmutableMap.of(
				"it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.RDFSRenderingEngineConfiguration",
				"it.uniroma2.art.semanticturkey.extension.impl.rendering.rdfs.RDFSRenderingEngineConfiguration",
				"it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.SKOSRenderingEngineConfiguration",
				"it.uniroma2.art.semanticturkey.extension.impl.rendering.skos.SKOSRenderingEngineConfiguration",
				"it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.SKOSXLRenderingEngineConfiguration",
				"it.uniroma2.art.semanticturkey.extension.impl.rendering.skosxl.SKOSXLRenderingEngineConfiguration",
				"it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.OntoLexLemonRenderingEngineConfiguration",
				"it.uniroma2.art.semanticturkey.extension.impl.rendering.ontolexlemon.OntoLexLemonRenderingEngineConfiguration");

		projectEdited |= upgradeAndRenameProjectBoundComponent(exptMgr, projectDir, projectProperties,
				oldRendEngFactoryIDProp, oldRendEngConfigTypeProp, oldRendEngConfigFilename,
				newRendEngConfigFilename, rendEngFactoryMapping, rendEngConfigMapping);

		if (projectEdited) {
			projectProperties.store(new FileWriter(projectFile), "");
		}
	}

	private static boolean upgradeAndRenameProjectBoundComponent(ExtensionPointManager exptMgr,
			File projectDir, Properties projectProperties, String factoryIDProp, String configTypeProp,
			String oldConfigFilename, String newConfigFilename, Map<String, String> factoryMapping,
			Map<String, String> configMapping) throws IOException {
		boolean projectEdited = false;

		String factoryID = projectProperties.getProperty(factoryIDProp);

		String newFactoryID = factoryMapping.get(factoryID);
		if (newFactoryID != null) {
			factoryID = newFactoryID;
			projectProperties.setProperty(factoryIDProp, factoryID);
			projectEdited = true;
		}

		File oldConfigFile = new File(projectDir, oldConfigFilename);

		if (oldConfigFile.exists()) {
			File newConfigFile = new File(oldConfigFile.getParentFile(), newConfigFilename);
			if (!newConfigFile.exists()) { // if the new configuration file exists, do not repeat the
											// conversion
				Properties oldConfig = new Properties();
				try (FileInputStream is = new FileInputStream(oldConfigFile)) {
					oldConfig.load(is);
				}

				@Nullable
				String configType = projectProperties.getProperty(configTypeProp);

				if (configType != null) {
					projectProperties.remove(configTypeProp);
					projectEdited = true;
				}

				configType = configMapping.getOrDefault(configType, configType); // if not mapped, returns
																					// the original value

				ConfigurationManager<?> configMgr;
				ObjectNode configJson = JsonNodeFactory.instance.objectNode();
				ObjectMapper objectMapper = STPropertiesManager.createObjectMapper();

				try {
					configMgr = exptMgr.getConfigurationManager(factoryID);
					Class<?> configClass = configMgr.getClass().getClassLoader().loadClass(configType);
					if (!Configuration.class.isAssignableFrom(configClass)) {
						throw new IllegalArgumentException("Not a configuration class: " + configClass);
					}

					Configuration configObj = (Configuration) configClass.newInstance();
					for (String propName : configObj.getProperties()) {
						String propValue = (String) oldConfig.get(propName);
						JsonNode jsonPropValue;

						if (propValue == null)
							continue;

						Type propType = configObj.getPropertyType(propName);
						if (TypeUtils.isAssignable(propType, Map.class)
								|| TypeUtils.isAssignable(propType, Collection.class)
								|| TypeUtils.isArrayType(propType)
								|| TypeUtils.isAssignable(propType, STProperties.class)) {
							jsonPropValue = objectMapper.readTree(propValue);
						} else if (TypeUtils.isAssignable(propType, Number.class)) {
							try {
								jsonPropValue = objectMapper.getNodeFactory()
										.numberNode(Long.valueOf(propValue));
							} catch (NumberFormatException e) {
								try {
									jsonPropValue = objectMapper.getNodeFactory()
											.numberNode(Double.valueOf(propValue));
								} catch (NumberFormatException e2) {
									e2.addSuppressed(e);
									throw e2;
								}
							}
						} else if (TypeUtils.isAssignable(propType, Boolean.class)) {
							jsonPropValue = objectMapper.getNodeFactory()
									.booleanNode(Boolean.parseBoolean(propValue));
						} else {
							jsonPropValue = objectMapper.getNodeFactory().textNode(propValue);
						}

						configJson.set(propName, jsonPropValue);
					}

				} catch (NoSuchConfigurationManager | ClassNotFoundException | InstantiationException
						| IllegalAccessException | PropertyNotFoundException e) {
					throw new IOException(e);
				}

				if (configType != null) {
					configJson.put("@type", configType);
				}

				STPropertiesManager.storeObjectNodeInYAML(configJson, newConfigFile);
			}
			oldConfigFile.delete();
		}

		return projectEdited;
	}

	private static void applyToComponentFolders(String componentID,
			FailableConsumer<File, IOException> consumer) throws IOException {
		File componentDir;

		// apply to folders at scope system
		componentDir = STPropertiesManager.getSystemPropertyFolder(componentID);
		if (componentDir.exists()) {
			consumer.accept(componentDir);
		}

		// apply to folders at scope user
		for (File userDir : Resources.getUsersDir().listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
			componentDir = FileUtils.getFile(userDir, "plugins", componentID);
			if (componentDir.exists()) {
				consumer.accept(componentDir);
			}
		}

		// apply to folders at scope project
		for (File projectDir : Resources.getProjectsDir()
				.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
			componentDir = FileUtils.getFile(projectDir, "plugins", componentID);
			if (componentDir.exists()) {
				consumer.accept(componentDir);
			}
		}

		// apply to folders at pu-scope
		for (File projectBindingsDir : Resources.getProjectUserBindingsDir()
				.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
			for (File puBindingsDir : projectBindingsDir
					.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
				componentDir = FileUtils.getFile(puBindingsDir, "plugins", componentID);
				if (componentDir.exists()) {
					consumer.accept(componentDir);
				}
			}
		}

		// apply to folders at pg-scope
		for (File projectBindingsDir : Resources.getProjectGroupBindingsDir()
				.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
			for (File pgBindingsDir : projectBindingsDir
					.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
				componentDir = FileUtils.getFile(pgBindingsDir, "plugins", componentID);
				if (componentDir.exists()) {
					consumer.accept(componentDir);
				}
			}
		}
	}

	private static void updateStoredPropertiesForComponentRenaming(String oldName, String newName)
			throws IOException {
		applyToComponentFolders(oldName, d -> d.renameTo(new File(d.getParentFile(), newName)));
	}

	private static void updateCustomServices(String... serviceID) throws IOException {
		File configFolder = STPropertiesManager
				.getSystemPropertyFolder(CustomServiceDefinitionStore.class.getName());
		FileUtils.forceMkdir(configFolder);
		for (String configName : Arrays.stream(serviceID).map(s -> s + ".cfg").collect(toList())) {
			try (InputStream is = UpdateRoutines.class.getResourceAsStream(
					"/it/uniroma2/art/semanticturkey/config/customservice/" + configName)) {
				FileUtils.copyInputStreamToFile(is, new File(configFolder, configName));
			}
		}
	}

	private static void updateInvokableReporters(String... reporterID) throws IOException {
		File configFolder = STPropertiesManager
				.getSystemPropertyFolder(InvokableReporterStore.class.getName());
		FileUtils.forceMkdir(configFolder);
		for (String configName : Arrays.stream(reporterID).map(s -> s + ".cfg")
				.collect(toList())) {
			try (InputStream is = UpdateRoutines.class.getResourceAsStream(
					"/it/uniroma2/art/semanticturkey/config/invokablereporter/" + configName)) {
				FileUtils.copyInputStreamToFile(is, new File(configFolder, configName));
			}
		}
	}

	private static void replaceNamespaceDefinition(SimpleNamespace oldNS, SimpleNamespace newNS,
			File... files) throws UnsupportedRDFormatException, IOException {
		for (File oldFile : files) {
			RDFFormat fileFormat = Rio.getParserFormatForFileName(oldFile.getName())
					.orElseThrow(() -> new IOException("Unable to determine the RDF format from file name"));

			File newFile = new File(oldFile.getParent(), oldFile.getName() + ".tmp");
			try (InputStream is = new FileInputStream(oldFile);
					OutputStream os = new FileOutputStream(newFile)) {
				RDFWriter writer = Rio.createWriter(fileFormat, os);
				RDFParser parser = Rio.createParser(fileFormat);
				parser.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
				parser.setRDFHandler(new RDFHandler() {

					boolean nsAlreadyProcessed = false;

					@Override
					public void startRDF() throws RDFHandlerException {
						nsAlreadyProcessed = false;
						writer.startRDF();
					}

					@Override
					public void handleStatement(Statement st) throws RDFHandlerException {
						Resource subj = rewrite(st.getSubject());
						IRI pred = rewrite(st.getPredicate());
						Value obj = rewrite(st.getObject());

						writer.handleStatement(SimpleValueFactory.getInstance().createStatement(subj, pred,
								obj, st.getContext()));
					}

					@Override
					public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
						if (uri.equals(oldNS.getName())) {
							if (!nsAlreadyProcessed) {
								writer.handleNamespace(newNS.getPrefix(), newNS.getName());
								nsAlreadyProcessed = true;
							}
						} else {
							writer.handleNamespace(prefix, uri);
						}
					}

					@Override
					public void handleComment(String comment) throws RDFHandlerException {
						writer.handleComment(comment);
					}

					@Override
					public void endRDF() throws RDFHandlerException {
						writer.endRDF();
					}

					@SuppressWarnings("unchecked")
					protected <T extends Value> T rewrite(T value) {
						if (value instanceof IRI) {
							String ns = ((IRI) value).getNamespace();
							String ln = ((IRI) value).getLocalName();
							if (ns.equals(oldNS.getName())) {
								return (T) SimpleValueFactory.getInstance().createIRI(newNS.getName(), ln);
							} else {
								return (T) value;
							}
						} else {
							return value;
						}
					}
				});
				parser.parse(is, oldFile.getAbsolutePath());
			}
			Files.move(newFile.toPath(), oldFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void updateRoles(Role[] roles) throws IOException {
		File rolesDir = RBACManager.getRolesDir(null);
		for (Role r : roles) {
			Utilities.copy(
					Resources.class.getClassLoader().getResourceAsStream(
							"/it/uniroma2/art/semanticturkey/rbac/roles/role_" + r.getName() + ".pl"),
					new File(rolesDir, "role_" + r.getName() + ".pl"));
		}
	}

	private static void updatePUSettingsSystemDefaults(String pluginId) throws IOException {
		Utilities.copy(
				Resources.class.getClassLoader()
						.getResourceAsStream("/it/uniroma2/art/semanticturkey/properties/" + pluginId
								+ "/pu-settings-defaults.props"),
				STPropertiesManager.getPUSettingsSystemDefaultsFile(pluginId));
	}

	@SuppressWarnings("unused")
	private static void updateProjectSettingsDefaults() throws IOException {
		Utilities.copy(Resources.class.getClassLoader().getResourceAsStream(
				"/it/uniroma2/art/semanticturkey/properties/it.uniroma2.art.semanticturkey/project-settings-defaults.props"),
				STPropertiesManager.getProjectSettingsDefaultsFile(STPropertiesManager.CORE_PLUGIN_ID));
	}

	@SuppressWarnings("unused")
	private static void updateCustomFormStructure() throws IOException {
		File customFormsFolder = CustomFormManager.getCustomFormsFolder(null);
		File formCollFolder = CustomFormManager.getFormCollectionsFolder(null);
		File formsFolder = CustomFormManager.getFormsFolder(null);
		Utilities.copy(
				Resources.class.getClassLoader().getResourceAsStream(
						"/it/uniroma2/art/semanticturkey/customform/customFormConfig.xml"),
				new File(customFormsFolder, "customFormConfig.xml"));
		Utilities.copy(Resources.class.getClassLoader().getResourceAsStream(
				"/it/uniroma2/art/semanticturkey/customform/it.uniroma2.art.semanticturkey.customform.collection.note.xml"),
				new File(formCollFolder, "it.uniroma2.art.semanticturkey.customform.collection.note.xml"));
		Utilities.copy(Resources.class.getClassLoader().getResourceAsStream(
				"/it/uniroma2/art/semanticturkey/customform/it.uniroma2.art.semanticturkey.customform.form.reifiednote.xml"),
				new File(formsFolder, "it.uniroma2.art.semanticturkey.customform.form.reifiednote.xml"));
		Utilities.copy(Resources.class.getClassLoader().getResourceAsStream(
				"/it/uniroma2/art/semanticturkey/customform/it.uniroma2.art.semanticturkey.customform.form.generictemplate.xml"),
				new File(formsFolder, "it.uniroma2.art.semanticturkey.customform.form.generictemplate.xml"));
	}

	/**
	 * Lists the subfolders of a given folder
	 * 
	 * @param parentFolder
	 * @return
	 */
	private static List<File> listSubFolders(File parentFolder) {
		String[] subFoldersNames = parentFolder.list(new FilenameFilter() {
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		List<File> subFolders = new ArrayList<>();
		for (String subFolderName : subFoldersNames) {
			subFolders.add(new File(parentFolder, subFolderName));
		}
		return subFolders;
	}

	/**
	 * Renames a file from fromName to toName The rename is performed only if source file exists
	 * 
	 * @param parentFolder
	 * @param fromName
	 * @param toName
	 */
	private static void renameFile(File parentFolder, String fromName, String toName) {
		File fromFile = new File(parentFolder, fromName);
		File toFile = new File(parentFolder, toName);
		if (fromFile.exists()) {
			if (toFile.exists()) {
				toFile.delete();
			}
			fromFile.renameTo(toFile);
		}
	}

}
