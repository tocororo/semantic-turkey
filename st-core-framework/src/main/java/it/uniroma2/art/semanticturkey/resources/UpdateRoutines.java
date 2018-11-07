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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.SemanticTurkey;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.exceptions.STInitializationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.utilities.Utilities;

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

	public static void startUpdatesCheckAndRepair() throws IOException, STPropertyAccessException, STInitializationException {
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
			Config.setSTDataVersionNumber(stVersionNumber);
		}

	}
	
	private static void alignFromPreviousTo3() throws IOException, STPropertyAccessException {
		logger.debug("Version 3.0.0 added capabilities to some roles");
		//In doubt, update all roles
		Role[] roles = {
				RBACManager.DefaultRole.LEXICOGRAPHER, RBACManager.DefaultRole.MAPPER,
				RBACManager.DefaultRole.ONTOLOGIST, RBACManager.DefaultRole.PROJECTMANAGER, 
				RBACManager.DefaultRole.RDF_GEEK, RBACManager.DefaultRole.THESAURUS_EDITOR,
				RBACManager.DefaultRole.VALIDATOR
		};
		updateRoles(roles);

		logger.debug("Version 3.0.0 added new properties to the default project preferences");
		updatePUSettingsSystemDefaults();
	}
	
	private static void alignFrom3To4() throws IOException, STPropertyAccessException, STInitializationException {
		logger.debug("Version 4.0.0 renamed some settings files");
		//users\<username>\plugins\<plugin>\system-preferences.props -> settings.props
		//users\<username>\plugins\<plugin>\project-preference-defaults.props -> pu-settings-defaults.props
		List<File> userDirectories = listSubFolders(Resources.getUsersDir());
		for (File userDirectory : userDirectories) {
			File pluginsFolder = new File(userDirectory, "plugins");
			if (pluginsFolder.exists()) {
				List<File> pluginsFolders = listSubFolders(pluginsFolder);
				for (File pluginFolder : pluginsFolders) {
					renameFile(pluginFolder, "system-preferences.props", "settings.props");
					renameFile(pluginFolder, "project-preferences-defaults.props", "pu-settings-defaults.props");
				}
			}
		}
		
		//system\plugins\<plugin>\system-preferences-defaults.props -> user-settings-defaults.props
		//system\plugins\<plugin>\project-preferences-defaults.props -> pu-settings-defaults.cfg
		List<File> sysPluginFolders = listSubFolders(new File(Resources.getSystemDir(), "plugins"));
		for (File pluginFolder : sysPluginFolders) {
			renameFile(pluginFolder, "system-preferences-defaults", "user-settings-defaults.props");
			renameFile(pluginFolder, "project-preferences-defaults.props", "pu-settings-defaults.props");
		}
		
		//projects\<projectname>\plugins\<plugin>\preferences-defaults.props -> pu-settings-defaults.props
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
		
		//pu_binding\<projectname>\<username>\plugins\<plugin>\preferences.props -> settings.props
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
		Role[] roles = { RBACManager.DefaultRole.LURKER, RBACManager.DefaultRole.PROJECTMANAGER, };
		updateRoles(roles);
		
		logger.debug("Version 4.0.0 added groups and pg_bindings folders");
		Resources.initializeGroups();
	}
	
	private static void alignFrom4To5() throws IOException, STPropertyAccessException {
		logger.debug("Version 5.0.0 added a capability to some roles");
		Role[] roles = { RBACManager.DefaultRole.LEXICOGRAPHER, RBACManager.DefaultRole.MAPPER, RBACManager.DefaultRole.ONTOLOGIST,
				RBACManager.DefaultRole.PROJECTMANAGER, RBACManager.DefaultRole.RDF_GEEK, RBACManager.DefaultRole.THESAURUS_EDITOR };
		updateRoles(roles);
		
		logger.debug("Version 5.0.0 removed a property from the default project preferences");
		updatePUSettingsSystemDefaults();
	}
	
	public static void repairProject(String projectName) throws IOException, InvalidProjectNameException,
			ProjectInexistentException, ProjectInconsistentException {
//		// ADDING PROJECT_MODEL_TYPE WHICH WAS MISSING FROM PROJECT STRUCTURE IN 0.7.1
//		checkAndInitializeMissingProperty(projectName, Project.PROJECT_MODEL_TYPE, OWLModel.class.getName(),
//				"0.7.0");
//
//		// ADDING MODELCONFIG_ID WHICH WAS MISSING FROM PROJECT STRUCTURE IN 0.7.1
//		ProjectType type = ProjectManager.getProjectType(projectName);
//		String modelConfigID;
//		if (type.equals(ProjectType.saveToStore))
//			modelConfigID = "it.uniroma2.art.owlart.sesame2impl.models.conf.Sesame2NonPersistentInMemoryModelConfiguration";
//		else
//			modelConfigID = "it.uniroma2.art.owlart.sesame2impl.models.conf.Sesame2PersistentInMemoryModelConfiguration";
//		checkAndInitializeMissingProperty(projectName, Project.MODELCONFIG_ID, modelConfigID, "0.7.2");
//
//		File projDir = ProjectManager.getProjectDir(projectName);
//		File modelConfigFile = new File(projDir, Project.MODELCONFIG_FILENAME);
//		if (!modelConfigFile.exists()) {
//			try {
//				modelConfigFile.createNewFile();
//			} catch (IOException e) {
//				logger.error("UPDATING OLD PROJECT TO NEW FORMAT: unable to create "
//						+ Project.MODELCONFIG_FILENAME + " file in Project: " + projectName);
//			}
//		}
	}
	
	private static void updateRoles(Role[] roles) throws IOException {
		File rolesDir = RBACManager.getRolesDir(null);
		for (Role r : roles) {
			Utilities.copy(Resources.class.getClassLoader()
					.getResourceAsStream("/it/uniroma2/art/semanticturkey/rbac/roles/role_" + r.getName() + ".pl"),
					new File(rolesDir, "role_" + r.getName() + ".pl")
			);
		}
	}
	
	private static void updatePUSettingsSystemDefaults() throws IOException, STPropertyAccessException {
		Utilities.copy(Resources.class.getClassLoader().getResourceAsStream(
				"/it/uniroma2/art/semanticturkey/properties/it.uniroma2.art.semanticturkey/pu-settings-defaults.props"),
				STPropertiesManager.getPUSettingsSystemDefaultsFile(STPropertiesManager.CORE_PLUGIN_ID)
		);
		Utilities.copy(Resources.class.getClassLoader().getResourceAsStream(
				"/it/uniroma2/art/semanticturkey/properties/it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine/pu-settings-defaults.props"),
				STPropertiesManager.getPUSettingsSystemDefaultsFile(RenderingEngine.class.getName())
		);
	}
	
	@SuppressWarnings("unused")
	private static void updateProjectSettingsDefaults() throws IOException, STPropertyAccessException {
		Utilities.copy(Resources.class.getClassLoader().getResourceAsStream(
				"/it/uniroma2/art/semanticturkey/properties/it.uniroma2.art.semanticturkey/project-settings-defaults.props"),
				STPropertiesManager.getProjectSettingsDefaultsFile(STPropertiesManager.CORE_PLUGIN_ID)
		);
	}
	
	@SuppressWarnings("unused")
	private static void updateCustomFormStructure() throws IOException {
		File customFormsFolder = CustomFormManager.getCustomFormsFolder(null);
		File formCollFolder = CustomFormManager.getFormCollectionsFolder(null);
		File formsFolder = CustomFormManager.getFormsFolder(null);
		Utilities.copy(
				Resources.class.getClassLoader().getResourceAsStream(
						"/it/uniroma2/art/semanticturkey/customform/customFormConfig.xml"),
				new File(customFormsFolder, "customFormConfig.xml")
		);
		Utilities.copy(
				Resources.class.getClassLoader().getResourceAsStream(
						"/it/uniroma2/art/semanticturkey/customform/it.uniroma2.art.semanticturkey.customform.collection.note.xml"),
				new File(formCollFolder, "it.uniroma2.art.semanticturkey.customform.collection.note.xml")
		);
		Utilities.copy(
				Resources.class.getClassLoader().getResourceAsStream(
						"/it/uniroma2/art/semanticturkey/customform/it.uniroma2.art.semanticturkey.customform.form.reifiednote.xml"),
				new File(formsFolder, "it.uniroma2.art.semanticturkey.customform.form.reifiednote.xml")
		);
		Utilities.copy(
				Resources.class.getClassLoader().getResourceAsStream(
						"/it/uniroma2/art/semanticturkey/customform/it.uniroma2.art.semanticturkey.customform.form.generictemplate.xml"),
				new File(formsFolder, "it.uniroma2.art.semanticturkey.customform.form.generictemplate.xml")
		);
	}
	
	/**
	 * Lists the subfolders of a given folder
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
	 * Renames a file from fromName to toName
	 * The rename is performed only if source file exists
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
