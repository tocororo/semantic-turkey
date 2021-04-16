/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is SemanticTurkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2007.
 * All Rights Reserved.
 *
 * SemanticTurkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata (ART)
 * Current information about SemanticTurkey can be obtained at 
 * http://semanticturkey.uniroma2.it
 *
 */

/*
 * Contributor(s): Armando Stellato stellato@info.uniroma2.it
 */
package it.uniroma2.art.semanticturkey.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.config.customservice.CustomServiceDefinitionStore;
import it.uniroma2.art.semanticturkey.config.invokablereporter.InvokableReporterStore;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.exceptions.STInitializationException;
import it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngine;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.utilities.Utilities;

/**
 * @author Armando Stellato
 * 
 */
public class Resources {

	private static final String _ontTempDirLocalName = "ont-temp";
	private static final String _ontMirrorDirDefaultLocationLocalName = "ontologiesMirror";
	private static final String _ontMirrorFileName = "OntologiesMirror.properties";
	private static final String _projectsDirName = "projects";
	private static final String _systemDirName = "system";
	private static final String _usersDirName = "users";
	private static final String _groupsDirName = "groups";
	private static final String _projectUserBindingsDirName = "pu_bindings";
	private static final String _projectGroupBindingsDirName = "pg_bindings";
	private static final String _docsDirName = "docs";

	/* new */
	private static final String _karafEtcDir = "/etc";

	private static File extensionPath;

	private static File stDataDirectory;

	/* new */
	private static File karafEtcDirectory;

	private static String _OSGiDirName = "OSGi";
	private static File OSGiPath;

	private static File ontTempDir;
	private static File ontMirrorDirDefaultLocation;

	private static File semTurkeyPropertyFile;
	private static File ontologiesMirrorFile;
	private static File projectsDir;
	private static File systemDir;
	private static File projectUserBindingsDir;
	private static File projectGroupBindingsDir;
	private static File usersDir;
	private static File groupsDir;
	private static File docsDir;

	protected static Logger logger = LoggerFactory.getLogger(Resources.class);

	public static void initializeUserResources(String extensionPathName) throws STInitializationException {

		logger.debug("initializing resources:");

		setExtensionPath(extensionPathName);
		logger.debug("extension path: " + getExtensionPath());

		OSGiPath = new File(getExtensionPath(), _OSGiDirName);
		logger.debug("OSGi path: " + OSGiPath);

		// currently managed here with file system API. However, it is now an OSGi file, and should be
		// accessed through proper OSGi APIs
		karafEtcDirectory = new File(getExtensionPath(), _karafEtcDir);
		semTurkeyPropertyFile = new File(karafEtcDirectory, "it.uniroma2.art.semanticturkey.cfg");

		try {
			Config.initialize(semTurkeyPropertyFile);
		} catch (IOException e2) {
			throw new STInitializationException(
					"Unable to initialize ST properties under /etc/custom.properties: " + e2.getMessage());
		}

		/* new */
		initSemTurkeyDataDir();

		if (!stDataDirectory.exists()) { // stData doesn't exist => create from scratch
			try {
				createDataDirectoryFromScratch(stDataDirectory);
			} catch (IOException e) {
				throw new STInitializationException(
						"initial copy of Semantic Turkey resources failed during first install: "
								+ e.getMessage());
			}
		} else { // stData exists => check if need to be updated
			try {
				UpdateRoutines.startUpdatesCheckAndRepair();
			} catch (IOException | ProjectAccessException e) {
				throw new STInitializationException(e);
			}
		}

		try {
			OntologiesMirror.setOntologiesMirrorRegistry(ontologiesMirrorFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initializes the SemanticTurkeyData directory according the dataDir property set in
	 * it.uniroma2.art.semanticturkey.cfg Then initializes also all the subfolders in the ST data dir
	 */
	public static void initSemTurkeyDataDir() {
		File dataDir = Config.getDataDir();
		if (dataDir.isAbsolute())
			stDataDirectory = dataDir;
		else
			stDataDirectory = new File(getExtensionPath(), dataDir.getPath());
		logger.debug("st data directory: " + getSemTurkeyDataDir());

		ontTempDir = new File(stDataDirectory, _ontTempDirLocalName);
		ontMirrorDirDefaultLocation = new File(stDataDirectory, _ontMirrorDirDefaultLocationLocalName);
		ontologiesMirrorFile = new File(stDataDirectory, _ontMirrorFileName);
		projectsDir = new File(stDataDirectory, _projectsDirName);
		systemDir = new File(stDataDirectory, _systemDirName);
		usersDir = new File(stDataDirectory, _usersDirName);
		groupsDir = new File(stDataDirectory, _groupsDirName);
		projectUserBindingsDir = new File(stDataDirectory, _projectUserBindingsDirName);
		projectGroupBindingsDir = new File(stDataDirectory, _projectGroupBindingsDirName);
		docsDir = new File(systemDir, _docsDirName);
	}

	public static File getSemTurkeyDataDir() {
		return stDataDirectory;
	}

	/**
	 * as from sturkey.properties property: ontologiesMirrorLocation if property is set to default, then its
	 * value is ontologiesMirror dir inside the ontDir directory (delegating to
	 * Config.getOntologiesMirrorLocation, which reads the property file)
	 * 
	 * @return
	 */
	public static File getOntologiesMirrorDir() {
		return Config.getOntologiesMirrorLocation();
	}

	public static void setExtensionPath(String extensionPathName) {
		logger.debug("setting extension path to: " + extensionPathName);
		extensionPath = new File(extensionPathName);
	}

	public static File getExtensionPath() {
		return extensionPath;
	}

	public static String getXSLDirectoryPath() {
		return getExtensionPath() + "/components/lib/xsl/";
	}

	/**
	 * @return the ontTempDir
	 */
	public static File getOntTempDir() {
		return ontTempDir;
	}

	/**
	 * @return the ontMirrorDirDefaultLocation
	 */
	static File getOntMirrorDirDefaultLocation() {
		return ontMirrorDirDefaultLocation;
	}

	public static File getProjectsDir() {
		return projectsDir;
	}

	public static File getSystemDir() {
		return systemDir;
	}

	public static File getUsersDir() {
		return usersDir;
	}

	public static File getGroupsDir() {
		return groupsDir;
	}

	public static File getProjectUserBindingsDir() {
		return projectUserBindingsDir;
	}

	public static File getProjectGroupBindingsDir() {
		return projectGroupBindingsDir;
	}

	public static File getDocsDir() {
		return docsDir;
	}

	/**
	 * this method is used to get the path of a new temp file to be used for whatever reason (the file is
	 * stored in the default temp file directory of Semantic Turkey
	 * 
	 * @return the path to the temp file
	 * @throws IOException
	 */
	public static File createTempDir() throws IOException {
		UUID uuid;
		File tempDir;
		do {
			uuid = UUID.randomUUID();
			tempDir = new File(Resources.getOntTempDir(), uuid.toString());
		} while (tempDir.exists());
		if (tempDir.mkdir())
			return tempDir;
		else
			throw new IOException("unable to create tempdir for building the exported project");
	}

	/* new */
	public static void createDataDirectoryFromScratch(File userDir)
			throws IOException, STInitializationException {
		if (userDir.mkdirs()) {
			String usrPath = userDir.getAbsolutePath();
			if (!new File(usrPath, _ontTempDirLocalName).mkdirs()
					|| !new File(usrPath, _ontMirrorDirDefaultLocationLocalName).mkdirs()
					|| !new File(usrPath, _projectsDirName).mkdirs()
					|| !new File(usrPath, _systemDirName).mkdirs()
					|| !new File(usrPath, _usersDirName).mkdirs()
					|| !new File(usrPath, _groupsDirName).mkdirs()
					|| !new File(usrPath, _projectUserBindingsDirName).mkdirs()
					|| !new File(usrPath, _projectGroupBindingsDirName).mkdirs()
					|| !new File(usrPath, _systemDirName + File.separator + _docsDirName).mkdirs()
					|| !new File(usrPath, _ontMirrorFileName).createNewFile())
				throw new STInitializationException("Unable to locate/create the correct files/folders");
			initializeCustomFormFileStructure();
			initializeRoles();
			initializeSystemProperties();
			initializePredefinedCustomServices();
			initializePredefinedInvokableReporters();

			Config.setSTDataVersionNumber(Config.getVersionNumber());
		} else {
			throw new STInitializationException("Unable to create the main data folder");
		}
	}

	public static File getOSGiPath() {
		return OSGiPath;
	}

	/**
	 * Initializes a folders structure in <code>STData/system/customForms</code>:
	 * <ul>
	 * <li>customFormConfig.xml</li>
	 * <li>Forms/</li>
	 * <ul>
	 * <li>example.of.customform.xml</li>
	 * </ul>
	 * <li>FormCollections</li>
	 * <ul>
	 * <li>example.of.formcollection.xml</li>
	 * </ul>
	 * </ul>
	 * 
	 * @throws IOException
	 */
	private static void initializeCustomFormFileStructure() throws STInitializationException {
		try {
			File customFormsFolder = CustomFormManager.getCustomFormsFolder(null);
			customFormsFolder.mkdir();
			File formCollFolder = CustomFormManager.getFormCollectionsFolder(null);
			formCollFolder.mkdir();
			File formsFolder = CustomFormManager.getFormsFolder(null);
			formsFolder.mkdir();
			Utilities.copy(
					Resources.class.getResourceAsStream(
							"/it/uniroma2/art/semanticturkey/customform/customFormConfig.xml"),
					new File(customFormsFolder, "customFormConfig.xml"));
			Utilities.copy(Resources.class.getResourceAsStream(
					"/it/uniroma2/art/semanticturkey/customform/it.uniroma2.art.semanticturkey.customform.collection.note.xml"),
					new File(formCollFolder,
							"it.uniroma2.art.semanticturkey.customform.collection.note.xml"));
			Utilities.copy(Resources.class.getResourceAsStream(
					"/it/uniroma2/art/semanticturkey/customform/it.uniroma2.art.semanticturkey.customform.form.reifiednote.xml"),
					new File(formsFolder, "it.uniroma2.art.semanticturkey.customform.form.reifiednote.xml"));
			Utilities.copy(Resources.class.getResourceAsStream(
					"/it/uniroma2/art/semanticturkey/customform/it.uniroma2.art.semanticturkey.customform.form.generictemplate.xml"),
					new File(formsFolder,
							"it.uniroma2.art.semanticturkey.customform.form.generictemplate.xml"));
		} catch (IOException e) {
			throw new STInitializationException(e);
		}
	}

	private static void initializeRoles() throws STInitializationException {
		try {
			Role[] roles = { RBACManager.DefaultRole.LEXICOGRAPHER, RBACManager.DefaultRole.LURKER,
					RBACManager.DefaultRole.MAPPER, RBACManager.DefaultRole.ONTOLOGIST,
					RBACManager.DefaultRole.PROJECTMANAGER, RBACManager.DefaultRole.RDF_GEEK,
					RBACManager.DefaultRole.THESAURUS_EDITOR, RBACManager.DefaultRole.VALIDATOR };
			File rolesDir = RBACManager.getRolesDir(null);
			if (!rolesDir.exists()) {
				rolesDir.mkdirs();
			}
			for (Role r : roles) {
				Utilities.copy(
						Resources.class.getResourceAsStream(
								"/it/uniroma2/art/semanticturkey/rbac/roles/role_" + r.getName() + ".pl"),
						new File(rolesDir, "role_" + r.getName() + ".pl"));
			}
		} catch (IOException e) {
			throw new STInitializationException(e);
		}
	}

	/**
	 * Initializes groups folder: simply creates an empty folder named "groups"
	 */
	public static void initializeGroups() {
		groupsDir.mkdir();
	}

	private static void initializeSystemProperties() throws STInitializationException {
		try {
			// Old version based on properties files TODO: remove

			// system settings
			{
				File propFile = STPropertiesManager.getSystemSettingsFile(STPropertiesManager.CORE_PLUGIN_ID);
				if (!propFile.getParentFile().exists()) { // if path doesn't exist, first create it
					propFile.getParentFile().mkdirs();
				}
				Utilities.copy(Resources.class.getResourceAsStream(
						"/it/uniroma2/art/semanticturkey/properties/it.uniroma2.art.semanticturkey/settings.props"),
						propFile);
				// default project settings
				propFile = STPropertiesManager.getProjectSettingsDefaultsFile(STPropertiesManager.CORE_PLUGIN_ID);
				if (!propFile.getParentFile().exists()) { // if path doesn't exist, first create it
					propFile.getParentFile().mkdirs();
				}
				Utilities.copy(Resources.class.getResourceAsStream(
						"/it/uniroma2/art/semanticturkey/properties/it.uniroma2.art.semanticturkey/project-settings-defaults.props"),
						propFile);
				// pu_settings - system default
				// * core plugin
				propFile = STPropertiesManager
						.getPUSettingsSystemDefaultsFile(STPropertiesManager.CORE_PLUGIN_ID);
				if (!propFile.getParentFile().exists()) { // if path doesn't exist, first create it
					propFile.getParentFile().mkdirs();
				}
				Utilities.copy(Resources.class.getResourceAsStream(
						"/it/uniroma2/art/semanticturkey/properties/it.uniroma2.art.semanticturkey/pu-settings-defaults.props"),
						propFile);
				// * rendering engine
				propFile = STPropertiesManager.getPUSettingsSystemDefaultsFile(RenderingEngine.class.getName());
				if (!propFile.getParentFile().exists()) { // if path doesn't exist, first create it
					propFile.getParentFile().mkdirs();
				}
				Utilities.copy(Resources.class.getResourceAsStream(
						"/it/uniroma2/art/semanticturkey/properties/it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngine/pu-settings-defaults.props"),
						propFile);
			}

			// New version based on YAML files
			File propFile = STPropertiesManager.getSystemSettingsFile(SemanticTurkeyCoreSettingsManager.class.getName());
			if (!propFile.getParentFile().exists()) { // if path doesn't exist, first create it
				propFile.getParentFile().mkdirs();
			}
			Utilities.copy(Resources.class.getResourceAsStream(
					"/it/uniroma2/art/semanticturkey/properties/it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager/settings.props"),
					propFile);
			// default project settings
			propFile = STPropertiesManager.getProjectSettingsDefaultsFile(RenderingEngine.class.getName());
			if (!propFile.getParentFile().exists()) { // if path doesn't exist, first create it
				propFile.getParentFile().mkdirs();
			}
			Utilities.copy(Resources.class.getResourceAsStream(
					"/it/uniroma2/art/semanticturkey/properties/it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager/project-settings-defaults.props"),
					propFile);
			// pu_settings - system default
			// * core plugin
			propFile = STPropertiesManager
					.getPUSettingsSystemDefaultsFile(SemanticTurkeyCoreSettingsManager.class.getName());
			if (!propFile.getParentFile().exists()) { // if path doesn't exist, first create it
				propFile.getParentFile().mkdirs();
			}
			Utilities.copy(Resources.class.getResourceAsStream(
					"/it/uniroma2/art/semanticturkey/properties/it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager/pu-settings-defaults.props"),
					propFile);
			// * rendering engine
			propFile = STPropertiesManager.getPUSettingsSystemDefaultsFile(RenderingEngine.class.getName());
			if (!propFile.getParentFile().exists()) { // if path doesn't exist, first create it
				propFile.getParentFile().mkdirs();
			}
			Utilities.copy(Resources.class.getResourceAsStream(
					"/it/uniroma2/art/semanticturkey/properties/it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngine/pu-settings-defaults.props"),
					propFile);
		} catch (IOException e) {
			throw new STInitializationException(e);
		}
	}

	private static void initializePredefinedCustomServices() throws STInitializationException {
		try {
			File configFolder = STPropertiesManager
					.getSystemPropertyFolder(CustomServiceDefinitionStore.class.getName());
			FileUtils.forceMkdir(configFolder);
			for (String configName : Arrays.asList(
					"it.uniroma2.art.semanticturkey.customservice.OntoLexLemonReport.cfg",
					"it.uniroma2.art.semanticturkey.customservice.OWLReport.cfg",
					"it.uniroma2.art.semanticturkey.customservice.SKOSReport.cfg")) {
				try (InputStream is = Resources.class.getResourceAsStream(
						"/it/uniroma2/art/semanticturkey/config/customservice/" + configName)) {
					FileUtils.copyInputStreamToFile(is, new File(configFolder, configName));
				}
			}
		} catch (IOException e) {
			throw new STInitializationException(e);
		}
	}

	private static void initializePredefinedInvokableReporters() throws STInitializationException {
		try {
			File configFolder = STPropertiesManager
					.getSystemPropertyFolder(InvokableReporterStore.class.getName());
			FileUtils.forceMkdir(configFolder);
			for (String configName : Arrays.asList(
					"it.uniroma2.art.semanticturkey.invokablereporter.OntoLexLemonReport.cfg",
					"it.uniroma2.art.semanticturkey.invokablereporter.OWLReport.cfg",
					"it.uniroma2.art.semanticturkey.invokablereporter.SKOSReport.cfg")) {
				try (InputStream is = Resources.class.getResourceAsStream(
						"/it/uniroma2/art/semanticturkey/config/invokablereporter/" + configName)) {
					FileUtils.copyInputStreamToFile(is, new File(configFolder, configName));
				}
			}
		} catch (IOException e) {
			throw new STInitializationException(e);
		}
	}
}
