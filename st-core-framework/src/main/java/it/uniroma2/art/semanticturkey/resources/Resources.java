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

import it.uniroma2.art.semanticturkey.exceptions.STInitializationException;
import it.uniroma2.art.semanticturkey.utilities.Utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import org.hibernate.validator.util.privilegedactions.GetClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Armando Stellato
 * 
 */
public class Resources {

	private static final String _installConfigurationFilePathName = "install.cfg";
	//private static final String _sourceUserDirectoryRelPathName = "/components/data";
	private static final String _ontLibraryDirLocalName = "ontlibrary";
	private static final String _ontTempDirLocalName = "ont-temp";
	private static final String _ontMirrorDirDefaultLocationLocalName = "ontologiesMirror";
	private static final String _projectsDirName = "projects";
	
	/*new*/
	private static final String _karafEtcDir = "/etc";

	private static File extensionPath;

	//private static File sourceUserDirectory;
	private static File userDirectory;
	
	/*new*/
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

	protected static Logger logger = LoggerFactory.getLogger(Resources.class);

	public static void initializeUserResources(String extensionPathName)
			throws STInitializationException {

		logger.info("initializing resources:");

		setExtensionPath(extensionPathName);
		logger.info("extension path: " + getExtensionPath());

		OSGiPath = new File(getExtensionPath(), _OSGiDirName);
		logger.info("OSGi path: " + OSGiPath);
		
		/*new*/
		karafEtcDirectory = new File(getExtensionPath(), _karafEtcDir);
		/*new*/
		semTurkeyPropertyFile = new File(karafEtcDirectory, "it.uniroma2.art.semanticturkey.cfg");
		try {
			Config.initialize(semTurkeyPropertyFile);
		} catch (FileNotFoundException e2) {
			throw new STInitializationException(
					"Unable to initialize ST properties under /etc/custom.properties: "
							+ e2.getMessage());
		} catch (IOException e2) {
			throw new STInitializationException(
					"Unable to initialize ST properties under /etc/custom.properties: "
							+ e2.getMessage());
		}
		//sourceUserDirectory = new File(getExtensionPath(),
			//	_sourceUserDirectoryRelPathName);
	//	logger.info("user directory template: " + sourceUserDirectory);

//		InstallConfigFile installConfigFileManager;
//		try {
//			File installConfig = new File(getExtensionPath(),
//					_installConfigurationFilePathName);
//			installConfigFileManager = new InstallConfigFile(installConfig);
//		} catch (IOException e) {
//			throw new STInitializationException(
//					"problems during ST installation with install configuration file: "
//							+ e.getMessage());
//		}
		
		/*new*/
		File dataDir = Config.getDataDir();
		if (dataDir.isAbsolute())
			userDirectory = dataDir;
		else
			userDirectory = new File(getExtensionPath(), dataDir.getPath());
		logger.info("st data directory: " + getSemTurkeyDataDir());

		ontLibraryDir = new File(userDirectory, _ontLibraryDirLocalName);
		ontTempDir = new File(userDirectory, _ontTempDirLocalName);
		ontMirrorDirDefaultLocation = new File(userDirectory,
				_ontMirrorDirDefaultLocationLocalName);
		owlDefinitionFile = new File(ontLibraryDir, "owl.rdfs");
		annotOntologyFile = new File(ontLibraryDir, "annotation.owl");
		ontologiesMirrorFile = new File(userDirectory, "OntologiesMirror.xml");
		projectsDir = new File(userDirectory, _projectsDirName);

		if (!userDirectory.exists()) {
			try {
				// first Copy Of User Resources
				// Utilities.recursiveCopy(sourceUserDirectory, userDirectory);
				
				/*new*/
				createDataDirectoryFromScratch(userDirectory);
				
			} catch (IOException e) {
				throw new STInitializationException(
						"initial copy of Semantic Turkey resources failed during first install: "
								+ e.getMessage());
			} catch (URISyntaxException e) {
				throw new STInitializationException(
						"owl and rdfs initial resources not present in the classpath: "
								+ e.getMessage());
			}
		} else {
		//	try {
				//Config.initialize(semTurkeyPropertyFile);
			
			/*new (solo commentato ciò che ora non serve più -inizializzazione ora è fatta all'inizio -)*/
				UpdateRoutines.startUpdatesCheckAndRepair();
//			} catch (FileNotFoundException e) {
//				throw new STInitializationException(
//						"Semantic Turkey initilization failed: unable to find Semantic Turkey Configuration File: "
//								+ semTurkeyPropertyFile);
//			} catch (IOException e) {
//				throw new STInitializationException(
//						"Semantic Turkey initilization failed: "
//								+ e.getMessage());
//			}

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
	 * as from sturkey.properties property: ontologiesMirrorLocation if property
	 * is set to default, then its value is ontologiesMirror dir inside the
	 * ontDir directory (delegating to Config.getOntologiesMirrorLocation, which
	 * reads the property file)
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

	/**
	 * this method is used to get the path of a new temp file to be used for
	 * whatever reason (the file is stored in the default temp file directory of
	 * Semantic Turkey
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
			throw new IOException(
					"unable to create tempdir for building the exported project");
	}

	/* new */
	public static void createDataDirectoryFromScratch(File userDir) throws IOException, URISyntaxException, STInitializationException {
		if (userDir.mkdirs()) {
			String usrPath = userDir.getAbsolutePath();
			if (!new File(usrPath + "/ont-temp").mkdirs() ||
				!new File(usrPath + "/ontlibrary").mkdirs() ||
				!new File(usrPath + "/ontologiesMirror").mkdirs() ||
				!new File(usrPath + "/projects").mkdirs() ||
				!new File(usrPath + "/OntologiesMirror.xml").createNewFile())
				throw new STInitializationException("Unable to locate/create the correct files/folders"); 
			Utilities.copy(Resources.class.getClassLoader().getResourceAsStream("/it/uniroma2/art/semanticturkey/annotation.owl"), new File(usrPath + "/ontlibrary/annotation.owl"));
			Utilities.copy(Resources.class.getClassLoader().getResourceAsStream("/it/uniroma2/art/semanticturkey/owl.rdfs"), new File(usrPath + "/ontlibrary/owl.rdfs"));	
		} else 
			throw new STInitializationException("Unable to create the main data folder");
	}

	public static File getOSGiPath() {
		return OSGiPath;
	}

}
