package it.uniroma2.art.semanticturkey.user;

import java.io.File;
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

public class ProjectUserBindingsManager {
	
	private static Collection<ProjectUserBinding> puBindingList = new ArrayList<>();
	
	/**
	 * Loads all the bindings into the repository
	 * Protected since the load should be done just once by AccessControlManager during its initialization
	 * @throws IOException 
	 * @throws RepositoryException 
	 * @throws RDFParseException 
	 */
	public static void loadPUBindings() throws RDFParseException, RepositoryException, IOException {
		ProjectUserBindingsRepoHelper repoHelper = new ProjectUserBindingsRepoHelper();
		Collection<File> bindingsFolders = AccessContolUtils.getAllPUBindingFiles();
		for (File f : bindingsFolders) {
			repoHelper.loadBindingDetails(f);
		}
		puBindingList = repoHelper.listPUBindings();
		repoHelper.shutDownRepository();
		
		//For debug
		System.out.println("Project-User Bindings");
		for (ProjectUserBinding pub : puBindingList) {
			System.out.println(pub.getProjectName() + "-" + pub.getUserEmail());
			System.out.println("\troles: " + String.join(", ", pub.getRolesName()));
		}
	}
	
	/**
	 * Returns all the project-user bindings
	 * @return
	 */
	public static Collection<ProjectUserBinding> listPUBindings() {
		return puBindingList;
	}
	
	/**
	 * Adds a project-user binding 
	 * @param puBinding
	 * @throws IOException 
	 */
	public static void createPUBinding(ProjectUserBinding puBinding) throws IOException {
		puBindingList.add(puBinding);
		createOrUpdatePUBindingFolder(puBinding);
	}
	
	/**
	 * Returns the ProjectUserBinding that binds the given user and project. Null if there is no binding
	 * @param user
	 * @param projectName
	 * @return
	 */
	public static ProjectUserBinding getPUBinding(STUser user, String projectName) {
		System.out.println("getPub for " + user.getEmail() + " " + projectName);
		ProjectUserBinding puBinding = null;
		for (ProjectUserBinding pub : puBindingList) {
			System.out.println("pub.getUserEmail() " + pub.getUserEmail() + ", pub.getProjectName()" + pub.getProjectName());
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
	public static Collection<ProjectUserBinding> listPUBindingsOfProject(String projectName) {
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
	public static boolean existsPUBindingsOfProject(String projectName) {
		return !AccessContolUtils.getProjBindingsFolder(projectName).exists();
	}
	
	/**
	 * Creates all the project-user bindings folders related to the given project.
	 * Useful when a project is created/imported
	 * and the existing users
	 * @param projectName
	 * @throws IOException 
	 */
	public static void createPUBindingsOfProject(String projectName) throws IOException {
		Collection<STUser> users = UsersManager.listUsers();
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
	public static void deletePUBindingsOfProject(String projectName) throws IOException {
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
	public static void createPUBindingsOfUser(String userEmail) throws ProjectAccessException, IOException {
		Collection<AbstractProject> projects = ProjectManager.listProjects();
		//for each project creates the binding with the given user
		for (AbstractProject abstrProj : projects) {
			if (abstrProj instanceof Project<?>) {
				createPUBinding(new ProjectUserBinding(abstrProj.getName(), userEmail));
			}
		}
	}
	
	/**
	 * When a user is deleted, deletes all the project-user bindings folders related to the given user
	 * @param userEmail
	 * @throws IOException 
	 */
	public static void deletePUBindingsOfUser(String userEmail) throws IOException {
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
	 * Adds roles to the binding between the given project-user pair
	 * @param userEmail
	 * @param projectName
	 * @param roles
	 * @throws IOException
	 */
	public static void addRolesToPUBinding(String userEmail, String projectName, Collection<String> roles) throws IOException {
		for (ProjectUserBinding pub : puBindingList) {
			if (pub.getUserEmail().equals(userEmail) && pub.getProjectName().equals(projectName)) {
				for (String r : roles) {
					pub.addRole(r);
				}
				createOrUpdatePUBindingFolder(pub);
				return;
			}
		}
	}
	
	/**
	 * Removes a role from the binding between the given project-user pair
	 * @param userEmail
	 * @param projectName
	 * @param role
	 * @throws IOException
	 */
	public static void removeRoleFromPUBinding(String userEmail, String projectName, String role) throws IOException {
		for (ProjectUserBinding pub : puBindingList) {
			if (pub.getUserEmail().equals(userEmail) && pub.getProjectName().equals(projectName)) {
				Collection<String> roles = pub.getRolesName();
				roles.remove(role);
				pub.setRoles(roles);
				createOrUpdatePUBindingFolder(pub);
				return;
			}
		}
	}
	
	/**
	 * Removes a role from all the bindings (useful in case a role is deleted)
	 * @param role
	 * @throws IOException
	 */
	public static void removeRoleFromAllPUBindings(String role) throws IOException {
		for (ProjectUserBinding pub : puBindingList) {
			Collection<String> roles = pub.getRolesName();
			roles.remove(role);
			pub.setRoles(roles);
			createOrUpdatePUBindingFolder(pub);
		}
	}
	
	/**
	 * Creates a folder for the given project-user bidning and serializes the details about it in a file.
	 * If the folder is already created, simply update the info in the details file.
	 * @throws IOException 
	 */
	private static void createOrUpdatePUBindingFolder(ProjectUserBinding puBinding) throws IOException {
		ProjectUserBindingsRepoHelper tempPUBindingsRepoHelper = new ProjectUserBindingsRepoHelper();
		tempPUBindingsRepoHelper.insertBinding(puBinding);
		tempPUBindingsRepoHelper.saveBindingDetailsFile(AccessContolUtils.getPUBindingDetailsFile(puBinding));
		tempPUBindingsRepoHelper.shutDownRepository();
	}
	
}
