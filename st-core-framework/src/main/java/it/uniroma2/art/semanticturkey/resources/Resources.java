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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.STInitializationException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.user.PUBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.user.RoleCreationException;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserCreationException;
import it.uniroma2.art.semanticturkey.user.UserStatus;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.Utilities;

/**
 * @author Armando Stellato
 * 
 */
public class Resources {

	private static final String _installConfigurationFilePathName = "install.cfg";
	// private static final String _sourceUserDirectoryRelPathName = "/components/data";
	private static final String _ontLibraryDirLocalName = "ontlibrary";
	private static final String _ontTempDirLocalName = "ont-temp";
	private static final String _ontMirrorDirDefaultLocationLocalName = "ontologiesMirror";
	private static final String _projectsDirName = "projects";
	private static final String _systemDirName = "system";
	private static final String _usersDirName = "users";
	private static final String _projectUserBindingsDirName = "pu_bindings";

	/* new */
	private static final String _karafEtcDir = "/etc";

	private static File extensionPath;

	// private static File sourceUserDirectory;
	private static File userDirectory;

	/* new */
	private static File karafEtcDirectory;

	private static String _OSGiDirName = "OSGi";
	private static File OSGiPath;

	private static File ontLibraryDir;
	private static File ontTempDir;
	private static File ontMirrorDirDefaultLocation;

	private static File semTurkeyPropertyFile;
	private static File annotOntologyFile;
	private static File owlDefinitionFile;
	private static File ontologiesMirrorFile;
	private static File projectsDir;
	private static File systemDir;
	private static File projectUserBindingsDir;
	private static File usersDir;

	protected static Logger logger = LoggerFactory.getLogger(Resources.class);

	public static void initializeUserResources(String extensionPathName) throws STInitializationException {

		logger.info("initializing resources:");

		setExtensionPath(extensionPathName);
		logger.info("extension path: " + getExtensionPath());

		OSGiPath = new File(getExtensionPath(), _OSGiDirName);
		logger.info("OSGi path: " + OSGiPath);

		// currently managed here with file system API. However, it is now an OSGi file, and should be
		// accessed through proper OSGi APIs
		karafEtcDirectory = new File(getExtensionPath(), _karafEtcDir);
		semTurkeyPropertyFile = new File(karafEtcDirectory, "it.uniroma2.art.semanticturkey.cfg");
		
		try {
			Config.initialize(semTurkeyPropertyFile);
		} catch (FileNotFoundException e2) {
			throw new STInitializationException(
					"Unable to initialize ST properties under /etc/custom.properties: " + e2.getMessage());
		} catch (IOException e2) {
			throw new STInitializationException(
					"Unable to initialize ST properties under /etc/custom.properties: " + e2.getMessage());
		}
		// sourceUserDirectory = new File(getExtensionPath(),
		// _sourceUserDirectoryRelPathName);
		// logger.info("user directory template: " + sourceUserDirectory);

		// InstallConfigFile installConfigFileManager;
		// try {
		// File installConfig = new File(getExtensionPath(),
		// _installConfigurationFilePathName);
		// installConfigFileManager = new InstallConfigFile(installConfig);
		// } catch (IOException e) {
		// throw new STInitializationException(
		// "problems during ST installation with install configuration file: "
		// + e.getMessage());
		// }

		/* new */
		File dataDir = Config.getDataDir();
		if (dataDir.isAbsolute())
			userDirectory = dataDir;
		else
			userDirectory = new File(getExtensionPath(), dataDir.getPath());
		logger.info("st data directory: " + getSemTurkeyDataDir());

		ontLibraryDir = new File(userDirectory, _ontLibraryDirLocalName);
		ontTempDir = new File(userDirectory, _ontTempDirLocalName);
		ontMirrorDirDefaultLocation = new File(userDirectory, _ontMirrorDirDefaultLocationLocalName);
		owlDefinitionFile = new File(ontLibraryDir, "owl.rdfs");
		annotOntologyFile = new File(ontLibraryDir, "annotation.owl");
		ontologiesMirrorFile = new File(userDirectory, "OntologiesMirror.xml");
		projectsDir = new File(userDirectory, _projectsDirName);
		systemDir = new File(userDirectory, _systemDirName);
		usersDir = new File(userDirectory, _usersDirName);
		projectUserBindingsDir = new File(userDirectory, _projectUserBindingsDirName);

		if (!userDirectory.exists()) {
			try {
				// first Copy Of User Resources
				// Utilities.recursiveCopy(sourceUserDirectory, userDirectory);

				/* new */
				createDataDirectoryFromScratch(userDirectory);

			} catch (IOException e) {
				throw new STInitializationException(
						"initial copy of Semantic Turkey resources failed during first install: "
								+ e.getMessage());
			} catch (URISyntaxException e) {
				throw new STInitializationException(
						"owl and rdfs initial resources not present in the classpath: " + e.getMessage());
			}
		} else {
			
			// TODO: the current UpdateRoutines are wrong, see comments in UpdateRoutines
			// UpdateRoutines.startUpdatesCheckAndRepair();
			
			//update routine: older ST didn't have "system", "users" and "pu_pindings" folders.
			//Here check them and eventually create the folders. TODO in the future versions this could be removed
			if (!systemDir.exists()) {
				systemDir.mkdirs();
			}
			if (!usersDir.exists()) {
				try {
					initializeUsersFileStructure();
				} catch (UserCreationException | ProjectAccessException | RoleCreationException e) {
					throw new STInitializationException(e);
				}
			}
			if (!projectUserBindingsDir.exists()) {
				try {
					initializePUBindingFileStructure();
				} catch (ProjectAccessException | PUBindingException e) {
					throw new STInitializationException(e);
				}
			}
			if (!CustomFormManager.getCustomFormsFolder(null).exists()) {
				try {
					initializeCustomFormFileStructure();
				} catch (IOException e) {
					throw new STInitializationException(e);
				}
			}
			if (!RBACManager.getRolesDir(null).exists()) {
				try {
					initializeRoles();
				} catch (IOException e) {
					throw new STInitializationException(e);
				}
			}
			
		}

		try {
			OntologiesMirror.setOntologiesMirrorRegistry(ontologiesMirrorFile);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public static File getSemTurkeyDataDir() {
		return userDirectory;
	}

	public static File getAnnotOntologyFile() {
		return annotOntologyFile;
	}

	public static File getOWLDefinitionFile() {
		return owlDefinitionFile;
	}

	/**
	 * @return the ontLibraryDir
	 */
	public static File getOntLibraryDir() {
		return ontLibraryDir;
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
	public static File getProjectUserBindingsDir() {
		return projectUserBindingsDir;
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
	public static void createDataDirectoryFromScratch(File userDir) throws IOException, URISyntaxException,
			STInitializationException {
		if (userDir.mkdirs()) {
			String usrPath = userDir.getAbsolutePath();
			if (!new File(usrPath, _ontTempDirLocalName).mkdirs()
					|| !new File(usrPath, _ontLibraryDirLocalName).mkdirs()
					|| !new File(usrPath, _ontMirrorDirDefaultLocationLocalName).mkdirs()
					|| !new File(usrPath, _projectsDirName).mkdirs()
					|| !new File(usrPath, _systemDirName).mkdirs()
					|| !new File(usrPath, _usersDirName).mkdirs()
					|| !new File(usrPath, _projectUserBindingsDirName).mkdirs()
					|| !new File(usrPath + "/OntologiesMirror.xml").createNewFile())
				throw new STInitializationException("Unable to locate/create the correct files/folders");
			Utilities.copy(
					Resources.class.getClassLoader().getResourceAsStream(
							"/it/uniroma2/art/semanticturkey/annotation.owl"), new File(usrPath
							+ "/ontlibrary/annotation.owl"));
			Utilities.copy(
					Resources.class.getClassLoader().getResourceAsStream(
							"/it/uniroma2/art/semanticturkey/owl.rdfs"), new File(usrPath
							+ "/ontlibrary/owl.rdfs"));
			
			initializeCustomFormFileStructure();
			initializeRoles();
						
			try {
				initializeUsersFileStructure();
				initializePUBindingFileStructure();
			} catch (UserCreationException | ProjectAccessException | RoleCreationException | PUBindingException e) {
				throw new STInitializationException(e);
			}
			
		} else
			throw new STInitializationException("Unable to create the main data folder");
	}

	public static File getOSGiPath() {
		return OSGiPath;
	}
	
	
	/**
	 * Initializes a folders structure with a users/ folder containing
	 * - roles.ttl: file that defines two default roles: ADMIN and USER 
	 * - a folder for a default admin user containing its user details file.
	 * @throws UserCreationException
	 * @throws ProjectAccessException 
	 * @throws RoleCreationException 
	 * @throws IOException 
	 */
	private static void initializeUsersFileStructure() throws UserCreationException, ProjectAccessException, RoleCreationException {
		usersDir.mkdir();
		// create and register admin user
		STUser admin = new STUser("admin@vocbench.com", "admin", "Admin", "Admin");
		admin.setStatus(UserStatus.ACTIVE);
		UsersManager.registerUser(admin);
	}
	
	/**
	 * Initializes a folders structure with a pu_binding folder containing 
	 * - a folder per project
	 * 		- which in turn contains a folder for user
	 * 			- which in turn contains a property file that describe relations between project and user
	 * @throws ProjectAccessException
	 * @throws IOException 
	 */
	private static void initializePUBindingFileStructure() throws ProjectAccessException, PUBindingException {
		// create project-user bindings
		projectUserBindingsDir.mkdir();

		for (AbstractProject abstrProj : ProjectManager.listProjects()) {
			if (abstrProj instanceof Project<?>) {
				for (STUser user : UsersManager.listUsers()) {
					ProjectUserBinding puBinding = new ProjectUserBinding(abstrProj, user);
					if (user.getEmail().equals("admin@vocbench.com")) {
						puBinding.addRole(RBACManager.DefaultRole.ADMINISTRATOR);
					}
					ProjectUserBindingsManager.createPUBinding(puBinding);
				}
			}
		}
	}
	
	/**
	 * Initializes a folders structure in <code>STData/system/customForms</code>:
	 * <ul>
	 * 	<li>customFormConfig.xml</li>
	 * 	<li>Forms/</li>
	 * 	<ul>
	 * 		<li>example.of.customform.xml</li>
	 * 	</ul>
	 * 	<li>FormCollections</li>
	 * 	<ul>
	 * 		<li>example.of.formcollection.xml</li>
	 * 	</ul> 
	 * </ul> 
	 * @throws IOException 
	 */
	private static void initializeCustomFormFileStructure() throws IOException {
		File customFormsFolder = CustomFormManager.getCustomFormsFolder(null);
		customFormsFolder.mkdir();
		File formCollFolder = CustomFormManager.getFormCollectionsFolder(null);
		formCollFolder.mkdir();
		File formsFolder = CustomFormManager.getFormsFolder(null);
		formsFolder.mkdir();
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
	}
	
	private static void initializeRoles() throws IOException {
		Role[] roles = {
				RBACManager.DefaultRole.ADMINISTRATOR, RBACManager.DefaultRole.LEXICOGRAPHER,
				RBACManager.DefaultRole.MAPPER, RBACManager.DefaultRole.PROJECTMANAGER
		};
		File rolesDir = RBACManager.getRolesDir(null);
		if (!rolesDir.exists()) {
			rolesDir.mkdirs();
		}
		for (Role r : roles) {
			Utilities.copy(Resources.class.getClassLoader()
					.getResourceAsStream("/it/uniroma2/art/semanticturkey/rbac/roles/role_" + r.getName() + ".pl"),
					new File(rolesDir, "role_" + r.getName() + ".pl")
			);
		}
	}

}
