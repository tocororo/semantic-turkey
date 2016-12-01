package it.uniroma2.art.semanticturkey.security;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
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
@DependsOn({"acRepoHolder","usersMgr"})
public class ProjectUserBindingManager {
	
	private ProjectUserBindingsRepoHelper puRepoHelper;
	
	@Autowired
	private UsersManager usersMgr;
	
	@Autowired
	public ProjectUserBindingManager(AccessControlRepositoryHolder acRepoHolder) {
		puRepoHelper = new ProjectUserBindingsRepoHelper(acRepoHolder.getRepository());
	}
	
	/**
	 * Loads all the bindings into the repository
	 * @throws IOException 
	 * @throws RepositoryException 
	 * @throws RDFParseException 
	 */
	public void loadPUBindings() throws RDFParseException, RepositoryException, IOException {
		Collection<File> bindingsFolders = AccessContolUtils.getAllPUBindingFiles();
		for (File f : bindingsFolders) {
			puRepoHelper.loadBindingDetails(f);
		}
	}
	
	/**
	 * Adds a project-user binding 
	 * @param puBinding
	 * @throws IOException 
	 */
	public void createPUBinding(ProjectUserBinding puBinding) throws IOException {
		puRepoHelper.insertBinding(puBinding);
		createOrUpdatePUBindingFolder(puBinding);
	}
	
	/**
	 * Returns the ProjectUserBinding that binds the given user and project
	 * @param user
	 * @param projectName
	 * @return
	 */
	public ProjectUserBinding getPUBinding(STUser user, String projectName) {
		return puRepoHelper.getPUBinding(user.getEmail(), projectName);
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
		//remove bindings from repository
		puRepoHelper.deletePUBindingOfProject(projectName);
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
		//remove bindings from repository
		puRepoHelper.deletePUBindingOfUser(userEmail);
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
		// creates a temporary not persistent repository
		MemoryStore memStore = new MemoryStore();
		memStore.setPersist(false);
		SailRepository tempRepo = new SailRepository(memStore);
		tempRepo.initialize();

		ProjectUserBindingsRepoHelper tempPUBindingsRepoHelper = new ProjectUserBindingsRepoHelper(tempRepo);
		tempPUBindingsRepoHelper.insertBinding(puBinding);
		tempPUBindingsRepoHelper.saveBindingDetailsFile(AccessContolUtils.getPUBindingDetailsFile(puBinding));
		tempPUBindingsRepoHelper.shutDownRepository();
	}
	
}
