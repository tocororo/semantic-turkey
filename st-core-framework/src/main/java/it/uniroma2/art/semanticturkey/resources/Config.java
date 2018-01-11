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

package it.uniroma2.art.semanticturkey.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;

/**
 * Generic Configuration Properties for Semantic Turkey. These include:
 * <ul>
 * <li><code>adminStatus</code>: tells (<code>true/false</code>) if an "administrator" user profile is
 * activated (more detailed view of data)</li>
 * <li><code>version</code>: contains the version number of Semantic Turkey in a:
 * <em>major.minor.revision</em> format</li>
 * </ul>
 * 
 * @author Armando Stellato
 * 
 */
public class Config {

	private static Properties stProperties = null;
	private static File propFile = null;
	private static String adminStatusPropName = "adminStatus";
	private static String versionNumberPropName = "version";
	private static String stDataVersionNumberPropName = "stDataVersion";
	private static String singleProjectModePropName = "singleProjectMode";
	private static String dataDirPropName = "data.dir";
	
	private static void updatePropertyFile() {
		FileOutputStream os;
		try {
			os = new FileOutputStream(propFile);
			// properties.storeToXML(os, "local cache references for mirroring remote ontologies");
			stProperties.store(os, "Semantic Turkey Property File");
			os.close();
		} catch (FileNotFoundException e) {
			// TODO STARRED remove this and add throw exception, then update all references
			e.printStackTrace();
		} catch (IOException e) {
			// TODO STARRED remove this and add throw exception, then update all references
			e.printStackTrace();
		}
	}

	public static String getAnnotOntologyFileName() {
		return stProperties.getProperty("annotationOntologyFileName");
	}

	static File getOntologiesMirrorLocation() {
		String choice = stProperties.getProperty("ontologiesMirrorLocation");
		if (choice == null || choice.equals("default"))
			return Resources.getOntMirrorDirDefaultLocation();
		else
			return new File(choice);
	}

	public static void initialize(File propFile) throws FileNotFoundException, IOException {
		Config.propFile = propFile;
		stProperties = new Properties();
		stProperties.load(new FileInputStream(propFile));
	}

	/**
	 * @return the adminStatus
	 */
	public static boolean isAdminStatus() {
		String adminStatusString = stProperties.getProperty(adminStatusPropName);
		if (adminStatusString.equals("true"))
			return true;
		else
			return false;
	}

	/**
	 * @param adminStatus
	 *            the adminStatus to set
	 */
	public static void setAdminStatus(boolean adminStatus) {
		if (adminStatus)
			stProperties.setProperty(adminStatusPropName, "true");
		else
			stProperties.setProperty(adminStatusPropName, "false");
		updatePropertyFile();
	}

	public static void setVersionNumber(VersionNumber vn) {
		stProperties.setProperty(versionNumberPropName, vn.toString());
		updatePropertyFile();
	}

	/**
	 * @return
	 */
	public static VersionNumber getVersionNumber() {
		String versionCode = stProperties.getProperty(versionNumberPropName);
		return new VersionNumber(versionCode);
	}
	
	public static VersionNumber getSTDataVersionNumber() {
		String versionCode;
		try {
			versionCode = STPropertiesManager.getSystemSetting(stDataVersionNumberPropName);
		} catch (STPropertyAccessException e) {
			return new VersionNumber(0, 0, 0);
		}
		return new VersionNumber(versionCode);
	}
	
	public static void setSTDataVersionNumber(VersionNumber vn) {
		try {
			STPropertiesManager.setSystemSetting(stDataVersionNumberPropName, vn.toString());
		} catch (STPropertyUpdateException e) {
			e.printStackTrace();
		}
	}

	/**
	 * checks if Semantic Turkey is being run in single project mode (i.e. providing the notion of
	 * "current project", with dedicated APIs for retrieving it, where the current project is being reset each
	 * time a new project is being loaded).<br/>
	 * Note that there is not <code>set</code> method for this property as this is a specific system
	 * configuration which must be setup before running the system, and is never thought to be changed at
	 * runtime.
	 * 
	 * @return
	 */
	public static boolean isSingleProjectMode() {
		String singleProjectModeString = stProperties.getProperty(singleProjectModePropName, "false");
		if (singleProjectModeString.equals("true"))
			return true;
		else
			return false;
	}
	
	public static File getDataDir() {
		return new File(stProperties.getProperty(dataDirPropName));
	}

	public static void setDataDirProp(String dataDirPath) throws ConfigurationUpdateException {
		stProperties.setProperty(dataDirPropName, dataDirPath);
	}

}
