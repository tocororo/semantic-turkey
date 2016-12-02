package it.uniroma2.art.semanticturkey.security;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.user.AccessContolUtils;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsRepoHelper;
import it.uniroma2.art.semanticturkey.user.STUser;

@Component("puBindingMgr")
@DependsOn("usersMgr")
public class ProjectUserBindingManager {
	
	@Autowired
	private UsersManager usersMgr;
	
	private Collection<ProjectUserBinding> puBindingList;
	
	public ProjectUserBindingManager() {
		puBindingList = new ArrayList<>();
	}
	
	/**
	 * Loads all the bindings into the repository
	 * @throws IOException 
	 * @throws RepositoryException 
	 * @throws RDFParseException 
	 */
	public void loadPUBindings() throws RDFParseException, RepositoryException, IOException {
		ProjectUserBindingsRepoHelper repoHelper = new ProjectUserBindingsRepoHelper();
		Collection<File> bindingsFolders = AccessContolUtils.getAllPUBindingFiles();
		for (File f : bindingsFolders) {
			repoHelper.loadBindingDetails(f);
		}
		puBindingList = repoHelper.listPUBindings();
		repoHelper.shutDownRepository();
	}
	
	/**
	 * Returns all the project-user bindings
	 * @return
	 */
	public Collection<ProjectUserBinding> listPUBindings() {
		return this.puBindingList;
	}
	
	/**
	 * Adds a project-user binding 
	 * @param puBinding
	 * @throws IOException 
	 */
	public void createPUBinding(ProjectUserBinding puBinding) throws IOException {
		puBindingList.add(puBinding);
		createOrUpdatePUBindingFolder(puBinding);
	}
	
	/**
	 * Returns the ProjectUserBinding that binds the given user and project. Null if there is no binding
	 * @param user
	 * @param projectName
	 * @return
	 */
	public ProjectUserBinding getPUBinding(STUser user, String projectName) {
		ProjectUserBinding puBinding = null;
		for (ProjectUserBinding pub : puBindingList) {
			if (pub.getUserEmail().equals(user.getEmail()) && pub.getProjectName().equals(projectName)) {
				puBinding = pub;
			}
		}
		return puBinding;
	}
	
	/**
	 * Returns the ProjectUserBindings of the given project
	 * @param projectName
	 * @return
	 */
	public Collection<ProjectUserBinding> listPUBindingsOfProject(String projectName) {
		Collection<ProjectUserBinding> pubList = new ArrayList<>();
		for (ProjectUserBinding pub : puBindingList) {
			if (pub.getProjectName().equals(projectName)) {
				pubList.add(pub);
			}
		}
		return pubList;
	}
	
	/**
	 * Checks if there is the folder of project-user bindings for the given project
	 * @param projectName
	 * @return
	 */
	public boolean existsPUBindingsOfProject(String projectName) {
		return !AccessContolUtils.getProjBindingsFolder(projectName).exists();
	}
	
	/**
	 * Creates all the project-user bindings folders related to the given project.
	 * Useful when a project is created/imported
	 * and the existing users
	 * @param projectName
	 * @throws IOException 
	 */
	public void createPUBindingsOfProject(String projectName) throws IOException {
		Collection<STUser> users = usersMgr.listUsers();
		//for each user creates the binding with the given project
		for (STUser u : users) {
			createPUBinding(new ProjectUserBinding(projectName, u.getEmail()));
		}
	}
	
	/**
	 * When a project is deleted, deletes all the project-user bindings folders related to the given project
	 * @param projectName
	 * @throws IOException 
	 */
	public void deletePUBindingsOfProject(String projectName) throws IOException {
		Iterator<ProjectUserBinding> itPUB = puBindingList.iterator();
		while (itPUB.hasNext()) {
			if (itPUB.next().getProjectName().equals(projectName)) {
				itPUB.remove();
			}
		}
		//delete folder about the project's bindings
		FileUtils.deleteDirectory(AccessContolUtils.getProjBindingsFolder(projectName));
	}
	
	/**
	 * Creates all the project-user bindings folders related to the given user.
	 * Useful when a user is created/imported
	 * and the existing projects
	 * @param projectName
	 * @throws ProjectAccessException 
	 * @throws IOException 
	 */
	public void createPUBindingsOfUser(String userEmail) throws ProjectAccessException, IOException {
		Collection<AbstractProject> projects = ProjectManager.listProjects();
		//for each project creates the binding with the given user
		for (AbstractProject abstrProj : projects) {
			createPUBinding(new ProjectUserBinding(abstrProj.getName(), userEmail));
		}
	}
	
	/**
	 * When a user is deleted, deletes all the project-user bindings folders related to the given user
	 * @param userEmail
	 * @throws IOException 
	 */
	public void deletePUBindingsOfUser(String userEmail) throws IOException {
		Iterator<ProjectUserBinding> itPUB = puBindingList.iterator();
		while (itPUB.hasNext()) {
			if (itPUB.next().getUserEmail().equals(userEmail)) {
				itPUB.remove();
			}
		}
		//delete folders about the user's bindings
		for (File userBindingFolder : AccessContolUtils.getUserBindingsFolders(userEmail)) {
			FileUtils.deleteDirectory(userBindingFolder);
		}
	}
	
	/**
	 * Creates a folder for the given project-user bidning and serializes the details about it in a file.
	 * If the folder is already created, simply update the info in the details file.
	 * @throws IOException 
	 */
	private void createOrUpdatePUBindingFolder(ProjectUserBinding puBinding) throws IOException {
		ProjectUserBindingsRepoHelper tempPUBindingsRepoHelper = new ProjectUserBindingsRepoHelper();
		tempPUBindingsRepoHelper.insertBinding(puBinding);
		tempPUBindingsRepoHelper.saveBindingDetailsFile(AccessContolUtils.getPUBindingDetailsFile(puBinding));
		tempPUBindingsRepoHelper.shutDownRepository();
	}
	
}
