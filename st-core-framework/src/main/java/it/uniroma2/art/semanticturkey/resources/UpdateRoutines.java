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
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.SemanticTurkey;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.ProjectManager;

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

	static void startUpdatesCheckAndRepair() throws ProjectAccessException {

		// this method has been deactivated, it was originally invoked in the last "else" branch of the
		// Resources.initializeUserResources(...) method

		// this "versionNumber" variable was originally in SemanticTurkey.java, not it should be superseded by
		// the one in the it.uniroma2.art.semanticturkey.cfg of Karaf, which is synced with the Maven pom
		// version and can be correctly parsed by the server
		// I leave it here just to leave this method still operative (though not being invoked anymore)
		VersionNumber versionNumber = new VersionNumber(0, 9, 0);

		// while this one, should be data folder...has to be taken from a dedicated prop file in the folder
		// I suggest to rename it as dataTouchVersion or dataTouchVersionNumber 
		VersionNumber currentVersionNumber = Config.getVersionNumber();

		logger.debug("version number of installed Semantic Turkey is: " + versionNumber);
		logger.debug("version number of Semantic Turkey currently saved in data folder is: "
				+ currentVersionNumber);

		if (versionNumber.compareTo(currentVersionNumber) > 0) {

			logger.debug("updating resources from version: " + currentVersionNumber + " to version: "
					+ versionNumber);

			if (currentVersionNumber.compareTo(new VersionNumber(0, 7, 0)) < 0)
				align_from_06x_to_07x();

			if (currentVersionNumber.compareTo(new VersionNumber(0, 7, 2)) < 0)
				align_from_071_to_072();

			Config.setVersionNumber(versionNumber);
		}
	}

	/**
	 * upgrade from version 0.6.x of SemanticTurkeyData
	 * 
	 * this limits to check that the Projects Folder exists and, in negative case, create it
	 * 
	 * @throws IOException
	 */
	private static void align_from_06x_to_07x() {
		logger.debug("versions prior to 0.7.x had no project folder, creating it");
		File projDir = Resources.getProjectsDir();
		if (!projDir.exists())
			projDir.mkdir();
	}

	/**
	 * upgrade from version 0.7.1 of SemanticTurkeyData to 0.7.2
	 * 
	 * this checks that if main-project exists, and, in case copies it as any other project in the projects
	 * directory
	 * @throws ProjectAccessException 
	 * 
	 * @throws IOException
	 */
	private static void align_from_071_to_072() throws ProjectAccessException {
		String mainProjectName = "project-main";
		File mainProjDir = new File(Resources.getSemTurkeyDataDir(), mainProjectName);

		if (mainProjDir.exists()) {
			logger.debug("version 0.7.1 had a main project folder, main project is no more present (any project can be a project loaded at startup now, and this feature is completely handled by the client)");
			try {
				String newMainProjectName = "wasMainProject";
				ProjectManager.cloneProjectToNewProject(mainProjectName, newMainProjectName);
				repairProject("wasMainProject");

				// Utilities.recursiveCopy(mainProjDir, new File(Resources.getProjectsDir(),
				// newMainProjectName));
				// ProjectManager.setProjectProperty(newMainProjectName, Project.PROJECT_NAME_PROP,
				// newMainProjectName);

			} catch (IOException e) {
				logger.error("unable to access property file for main project (which seems however to exist)");
				e.printStackTrace();
			} catch (InvalidProjectNameException e) {
				logger.error("UPDATING OLD PROJECT TO NEW FORMAT: strangely, the project name is invalid");
			} catch (ProjectInexistentException e) {
				logger.error("UPDATING OLD PROJECT TO NEW FORMAT: strangely, the main project does not exist, while it has been previously checked for existence");
			} catch (DuplicatedResourceException e) {
				logger.error("unable to copy main project to normal project wasMainProject cause a project with this name already exists");
			} catch (ProjectInconsistentException e) {
				logger.error("unable to repair old main project, because it is in an inconsistent state even for its original Semantic Turkey version");
			}
		}
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

	private static void checkAndInitializeMissingProperty(String projectName, String propertyName,
			String defaultValue, String stversion) throws IOException, InvalidProjectNameException,
			ProjectInexistentException {
		logger.debug("checking existence of a value for property: " + propertyName);
		String propertyValue = ProjectManager.getProjectProperty(projectName, propertyName);
		if (propertyValue == null) {
			logger.debug(projectName + " is present, though it shows no property: " + propertyName
					+ ". Since it is a project previous to version: " + stversion
					+ ", its configuration type is probably (and being set to): " + defaultValue);
			ProjectManager.setProjectProperty(projectName, propertyName, defaultValue);
		} else {
			logger.debug("property " + propertyName + " is already initialized to value: " + propertyValue);
		}
	}

}
