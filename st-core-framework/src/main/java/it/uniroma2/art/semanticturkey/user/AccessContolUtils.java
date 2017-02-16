package it.uniroma2.art.semanticturkey.user;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;


import it.uniroma2.art.semanticturkey.resources.Config;

public class AccessContolUtils {
	
	private static final String USERS_FOLDER_NAME = "users";
	private static final String USERS_DETAILS_FILE_NAME = "details.ttl";
	
	private static final String ROLES_DEFINITION_FILE_NAME = "roles.ttl";
	
	private static final String PU_BINDING_FOLDER_NAME = "pu_binding";
	private static final String PU_BINDING_DETAILS_FILE_NAME = "binding.ttl";
	
	//Users folders utility methods
	
	//TODO move under UserManager?
	
	public static File getUsersFolder(){
		return new File(Config.getDataDir() + File.separator + USERS_FOLDER_NAME);
	}
	
	public static File getUserFolder(String userEmail) {
		return new File(getUsersFolder() + File.separator + getUserFolderName(userEmail));
	}
	
	public static File getUserDetailsFile(String userEmail) {
		File userFolder = new File(getUsersFolder() + File.separator + getUserFolderName(userEmail));
		if (!userFolder.exists()) {
			userFolder.mkdir();
		}
		return new File(userFolder + File.separator + USERS_DETAILS_FILE_NAME);
	}
	
	public static Collection<File> getAllUserDetailsFiles() {
		Collection<File> userDetailsFolders = new ArrayList<>(); 
		Collection<File> userFolders = getAllUserFolders();
		for (File f : userFolders) {
			userDetailsFolders.add(new File(f + File.separator + USERS_DETAILS_FILE_NAME));
		}
		return userDetailsFolders;
	}
	
	public static Collection<File> getAllUserFolders() {
		Collection<File> userFolders = new ArrayList<>(); 
		File usersFolder = getUsersFolder();
		//get all subfolder of "users" folder (one subfolder for each user)		
		String[] userDirectories = usersFolder.list(new FilenameFilter() {
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		for (int i = 0; i < userDirectories.length; i++) {
			userFolders.add(new File(usersFolder + File.separator + userDirectories[i]));
		}
		return userFolders;
	}
	
	private static String getUserFolderName(String email) {
		String folderName = email.replace("@", "AT");
		folderName = folderName.replace(".", "_");
		return folderName;
	}
	
	//Roles file utility method
	//TODO move under RolesManager?
	
	public static File getRolesDefinitionFile() {
		File usersFolder = getUsersFolder();
		if (!usersFolder.exists()) {
			usersFolder.mkdir();
		}
		return new File(usersFolder	+ File.separator + ROLES_DEFINITION_FILE_NAME);
	}
	
	//Project-User bindings files utility methods
	//TODO move under ProjectUserBindingsManager?
	
	public static Collection<File> getAllPUBindingFiles() {
		Collection<File> puBindingDetailsFolders = new ArrayList<>(); 
		Collection<File> projBindFolders = getAllProjBindingsFolders();
		//get all subfolder of "pu_binding/<projectName>" folder (one subfolder for each user)
		for (File projFolder : projBindFolders) {
			String[] userDirectories = projFolder.list(new FilenameFilter() {
				public boolean accept(File current, String name) {
					return new File(current, name).isDirectory();
				}
			});
			for (String userDir : userDirectories) {
				puBindingDetailsFolders.add(new File(projFolder + File.separator + userDir 
						+ File.separator + PU_BINDING_DETAILS_FILE_NAME));
			}
		}
		return puBindingDetailsFolders;
	}

	public static File getProjBindingsFolder(String projectName) {
		return new File(Config.getDataDir() + File.separator + PU_BINDING_FOLDER_NAME + File.separator + projectName);
	}
	
	public static Collection<File> getUserBindingsFolders(String userEmail) {
		Collection<File> userBindingsFolders = new ArrayList<>();
		Collection<File> projBindFolders = getAllProjBindingsFolders();
		//get all subfolder of "pu_binding/<projectName>" folder (one subfolder for each user)
		for (File projFolder : projBindFolders) {
			userBindingsFolders.add(new File(projFolder, getUserFolderName(userEmail)));
		}
		return userBindingsFolders;
	}
	
	public static File getPUBindingsFolder(){
		return new File(Config.getDataDir() + File.separator + PU_BINDING_FOLDER_NAME);
	}
	
	public static File getPUBindingDetailsFile(ProjectUserBinding puBinding) {
		File bindingFolder = new File(getPUBindingsFolder() + File.separator + puBinding.getProjectName() 
			+ File.separator + getUserFolderName(puBinding.getUserEmail()));
		if (!bindingFolder.exists()) {
			bindingFolder.mkdirs();
		}
		return new File(bindingFolder + File.separator + PU_BINDING_DETAILS_FILE_NAME);
	}
	
	private static Collection<File> getAllProjBindingsFolders() {
		Collection<File> projBindingsFolders = new ArrayList<>();
		File puBindingFolder = getPUBindingsFolder();
		//get all subfolder of "pu_binding" folder (one subfolder for each project)		
		String[] projDirectories = puBindingFolder.list(new FilenameFilter() {
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		//get all subfolder of "pu_binding/<projectName>" folder (one subfolder for each user)
		for (String prDir : projDirectories) {
			projBindingsFolders.add(new File(puBindingFolder, prDir));
		}
		return projBindingsFolders;
	}
	
}
