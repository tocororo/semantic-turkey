package it.uniroma2.art.semanticturkey.user;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;

import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.resources.Resources;

public class ProjectGroupBindingsManager {
	
	/**
	 * When a project is deleted, deletes all the project-group bindings folders related to the given project
	 * @param projectName
	 * @throws IOException 
	 */
	public static void deletePGBindingsOfProject(String projectName) throws IOException {
		//delete folder about the project's bindings
		FileUtils.deleteDirectory(getProjBindingsFolder(projectName));
	}
	
	/**
	 * When a group is deleted, deletes all the project-group bindings folders related to the given group
	 * @param group
	 * @throws IOException 
	 */
	public static void deletePGBindingsOfGroup(UsersGroup group) throws IOException {
		//delete folders about the group's bindings
		for (File groupBindingFolder : getGroupBindingsFolders(group)) {
			FileUtils.deleteDirectory(groupBindingFolder);
		}
	}
	
	/**
	 * Returns (and create if it doesn't exist) a folder for the given project-group binding.
	 * @param project
	 * @param group
	 * @return
	 */
	public static File getPGBindingFolder(AbstractProject project, UsersGroup group) {
		File bindingFolder = new File(Resources.getProjectGroupBindingsDir() + File.separator + project.getName() 
			+ File.separator + UsersGroup.encodeGroupIri(group.getIri()));
		if (!bindingFolder.exists()) {
			bindingFolder.mkdirs();
		}
		return bindingFolder;
	}
	
	/**
	 * Returns the folder about the given project under <STData>/pg_bindings/
	 * @param project
	 * @return
	 */
	public static File getProjBindingsFolder(AbstractProject project) {
		return getProjBindingsFolder(project.getName());
	}
	
	/**
	 * Returns the folder about the given project under <STData>/pg_bindings/
	 * @param projectName
	 * @return
	 */
	public static File getProjBindingsFolder(String projectName) {
		return new File(Resources.getProjectGroupBindingsDir() + File.separator + projectName);
	}
	
	/**
	 * Returns all the projects folder under <STData>/pg_bindings/
	 * @return
	 */
	private static Collection<File> getAllProjBindingsFolders() {
		Collection<File> projBindingsFolders = new ArrayList<>();
		File pgBindingFolder = Resources.getProjectGroupBindingsDir();
		//get all subfolder of "pg_binding" folder (one subfolder for each project)		
		String[] projDirectories = pgBindingFolder.list(new FilenameFilter() {
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		//get all subfolder of "pu_binding/<projectName>" folder (one subfolder for each group)
		for (String prDir : projDirectories) {
			projBindingsFolders.add(new File(pgBindingFolder, prDir));
		}
		return projBindingsFolders;
	}
	
	/**
	 * Returns the user folders under <STData>/pu_bindings/<projectName>/ for the given project-user pair
	 * @param projectName
	 * @param userEmail
	 * @return
	 */
	public static File getPUBindingsFolder(AbstractProject project, STUser user) {
		return new File(getProjBindingsFolder(project) + File.separator + STUser.encodeUserIri(user.getIRI()));
	}
	
	/**
	 * Returns the group folders under all the <STData>/pg_bindings/<projName>/ folders
	 * @param
	 * @return
	 */
	public static Collection<File> getGroupBindingsFolders(UsersGroup group) {
		Collection<File> groupBindingsFolders = new ArrayList<>();
		Collection<File> projBindFolders = getAllProjBindingsFolders();
		//get all subfolder of "pg_binding/<projectName>" folder (one subfolder for each group)
		for (File projFolder : projBindFolders) {
			groupBindingsFolders.add(new File(projFolder, UsersGroup.encodeGroupIri(group.getIri())));
		}
		return groupBindingsFolders;
	}
	
	
}
