package it.uniroma2.art.semanticturkey.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.resources.Config;
import it.uniroma2.art.semanticturkey.user.AccessContolUtils;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * Note: this is a class with static method to read and write user/project/system properties.
 * It uses UsersManager which loadUsers method load the users in a in-memory structure. This method is called
 * (so UsersManager is initialized) in PostConstructor of it.uniroma2.art.semanticturkey.user.AccessControlManager
 * that is a spring Component. So, UsersManager is initialized during the SemanticTukery startup.
 * If there is another Component which uses UsersManager (or RolesManager or ProjectUserBindingsManager,
 * initialized by AccessControlManager as well) in its PostConstructor, it is not guaranteed that it is initialized
 * 
 * @author Tiziano
 *
 */
public class STPropertiesManager {
	
	public static final String PROP_LANGUAGES = "languages";
	
	private static final String SYSTEM_PROP_FILE_NAME = "st_system.properties";
	private static final String PROJECT_PROP_FILE_NAME = "project.properties";
	private static final String USER_PROP_FILE_NAME = "user.properties";
	
	public static String getSystemProperty(String propName) throws STPropertyAccessException {
		return loadProperties(getSystemPropertyFile()).getProperty(propName);
	}
	
	public static void setSystemProperty(String propName, String value) throws STPropertyUpdateException, STPropertyAccessException {
		File propFile = getSystemPropertyFile();
		Properties properties = loadProperties(propFile);
		properties.setProperty(propName, value);
		updatePropertyFile(properties, propFile);
	}
	
	public static String getProjectProperty(String projectName, String propName) throws STPropertyAccessException {
		return loadProperties(getProjectPropertyFile(projectName)).getProperty(propName);
	}
	
	public static void setProjectProperty(String projectName, String propName, String value) throws STPropertyUpdateException, STPropertyAccessException {
		File propFile = getProjectPropertyFile(projectName);
		Properties properties = loadProperties(propFile);
		properties.setProperty(propName, value);
		updatePropertyFile(properties, propFile);
	}
	
	public static String getUserProperty(STUser user, String propName) throws STPropertyAccessException {
		return loadProperties(getUserPropertyFile(user)).getProperty(propName);
	}
	
	public static void setUserProperty(STUser user, String propName, String value)
			throws STPropertyUpdateException, STPropertyAccessException {
		File propFile = getUserPropertyFile(user);
		Properties properties = loadProperties(propFile);
		properties.setProperty(propName, value);
		updatePropertyFile(properties, propFile);
	}
	
	private static void updatePropertyFile(Properties properties, File propFile) throws STPropertyUpdateException {
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
	
	private static File getSystemPropertyFile() throws STPropertyAccessException {
		try {
			File sysPropFile = new File(Config.getDataDir() + File.separator + SYSTEM_PROP_FILE_NAME);
			if (!sysPropFile.exists()) { //if .properties file doesn't exist, create and initialize it 
				Properties properties = new Properties();
				updatePropertyFile(properties, sysPropFile);
			}
			return sysPropFile;
		} catch (STPropertyUpdateException e) {
			throw new STPropertyAccessException(e);
		}
	}
	
	private static File getProjectPropertyFile(String projectName) throws STPropertyAccessException {
		try {
			File projectDir = ProjectManager.getProjectDir(projectName);
			File projPropFile = new File(projectDir + File.separator + PROJECT_PROP_FILE_NAME);
			if (!projPropFile.exists()) { //if .properties file doesn't exist, create and initialize it
				Properties properties = new Properties();
				updatePropertyFile(properties, projPropFile);
			}
			return projPropFile;
		} catch (STPropertyUpdateException | ProjectInexistentException | InvalidProjectNameException e) {
			throw new STPropertyAccessException(e);
		}
	}
	
	private static File getUserPropertyFile(STUser user) throws STPropertyAccessException {
		try {
			File userPropFile = new File(
					AccessContolUtils.getUserFolder(user.getEmail()) + File.separator + USER_PROP_FILE_NAME);
			if (!userPropFile.exists()) { // if .properties file doesn't exist, create and initialize it
				Properties properties = new Properties();
				updatePropertyFile(properties, userPropFile);
			}
			return userPropFile;
		} catch (STPropertyUpdateException e) {
			throw new STPropertyAccessException(e);
		}
	}
	
}

