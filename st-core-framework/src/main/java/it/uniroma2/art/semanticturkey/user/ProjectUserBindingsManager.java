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

public class ProjectUserBindingsManager {
	
	private static final String PU_BINDING_DETAILS_FILE_NAME = "binding.ttl";
	
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
		Collection<File> bindingsFolders = getAllPUBindingFiles();
		for (File f : bindingsFolders) {
			repoHelper.loadBindingDetails(f);
		}
		puBindingList = repoHelper.listPUBindings();
		repoHelper.shutDownRepository();
		
		//For debug
//		System.out.println("Project-User Bindings");
//		for (ProjectUserBinding pub : puBindingList) {
//			System.out.println(pub.getProjectName() + "-" + pub.getUserEmail());
//			System.out.println("\troles: " + String.join(", ", pub.getRolesName()));
//		}
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
	public static void createPUBinding(ProjectUserBinding puBinding) throws PUBindingException {
		puBindingList.add(puBinding);
		createOrUpdatePUBindingFolder(puBinding);
	}
	
	/**
	 * Returns the ProjectUserBinding that binds the given user and project. Null if there is no binding
	 * @param user
	 * @param projectName
	 * @return
	 */
	public static ProjectUserBinding getPUBinding(STUser user, AbstractProject project) {
		ProjectUserBinding puBinding = null;
		for (ProjectUserBinding pub : puBindingList) {
			if (pub.getUser().getEmail().equals(user.getEmail()) && pub.getProject().getName().equals(project.getName())) {
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
	public static Collection<ProjectUserBinding> listPUBindingsOfProject(AbstractProject project) {
		Collection<ProjectUserBinding> pubList = new ArrayList<>();
		for (ProjectUserBinding pub : puBindingList) {
			if (pub.getProject().getName().equals(project.getName())) {
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
	public static boolean existsPUBindingsOfProject(AbstractProject project) {
		return !getProjBindingsFolder(project).exists();
	}
	
	/**
	 * Creates all the project-user bindings folders related to the given project.
	 * Useful when a project is created/imported
	 * and the existing users
	 * @param projectName
	 * @throws IOException 
	 */
	public static void createPUBindingsOfProject(AbstractProject project) throws PUBindingException {
		Collection<STUser> users = UsersManager.listUsers();
		//for each user creates the binding with the given project
		for (STUser u : users) {
			createPUBinding(new ProjectUserBinding(project, u));
		}
	}
	
	/**
	 * When a project is deleted, deletes all the project-user bindings folders related to the given project
	 * @param projectName
	 * @throws IOException 
	 */
	public static void deletePUBindingsOfProject(AbstractProject project) throws IOException {
		Iterator<ProjectUserBinding> itPUB = puBindingList.iterator();
		while (itPUB.hasNext()) {
			if (itPUB.next().getProject().getName().equals(project.getName())) {
				itPUB.remove();
			}
		}
		//delete folder about the project's bindings
		FileUtils.deleteDirectory(getProjBindingsFolder(project));
	}
	
	/**
	 * Creates all the project-user bindings folders related to the given user.
	 * Useful when a user is created/imported
	 * and the existing projects
	 * @param projectName
	 * @throws ProjectAccessException 
	 * @throws IOException 
	 */
	public static void createPUBindingsOfUser(STUser user) throws ProjectAccessException, PUBindingException {
		Collection<AbstractProject> projects = ProjectManager.listProjects();
		//for each project creates the binding with the given user
		for (AbstractProject abstrProj : projects) {
			if (abstrProj instanceof Project<?>) {
				createPUBinding(new ProjectUserBinding(abstrProj, user));
			}
		}
	}
	
	/**
	 * When a user is deleted, deletes all the project-user bindings folders related to the given user
	 * @param userEmail
	 * @throws IOException 
	 */
	public static void deletePUBindingsOfUser(STUser user) throws IOException {
		Iterator<ProjectUserBinding> itPUB = puBindingList.iterator();
		while (itPUB.hasNext()) {
			if (itPUB.next().getUser().getEmail().equals(user.getEmail())) {
				itPUB.remove();
			}
		}
		//delete folders about the user's bindings
		for (File userBindingFolder : getUserBindingsFolders(user)) {
			FileUtils.deleteDirectory(userBindingFolder);
		}
	}
	
	/**
	 * Adds roles to the binding between the given project-user pair
	 * @param userEmail
	 * @param projectName
	 * @param roles
	 * @throws PUBindingException
	 */
	public static void addRolesToPUBinding(STUser user, AbstractProject project, Collection<Role> roles) throws PUBindingException {
		for (ProjectUserBinding pub : puBindingList) {
			if (pub.getUser().getEmail().equals(user.getEmail()) && pub.getProject().getName().equals(project.getName())) {
				for (Role r : roles) {
					pub.addRole(r);
				}
				createOrUpdatePUBindingFolder(pub);
				return;
			}
		}
	}
	
	/**
	 * Adds roles to the binding between the given project-user pair
	 * @param userEmail
	 * @param projectName
	 * @param roles
	 * @throws PUBindingException
	 */
	public static void addRoleToPUBinding(STUser user, AbstractProject project, Role role) throws PUBindingException {
		for (ProjectUserBinding pub : puBindingList) {
			if (pub.getUser().getEmail().equals(user.getEmail()) && pub.getProject().getName().equals(project.getName())) {
				pub.addRole(role);
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
	 * @throws PUBindingException
	 */
	public static void removeRoleFromPUBinding(STUser user, AbstractProject project, Role role) throws PUBindingException {
		for (ProjectUserBinding pub : puBindingList) {
			if (pub.getUser().getEmail().equals(user.getEmail()) && pub.getProject().getName().equals(project.getName())) {
				Collection<Role> roles = pub.getRoles();
				roles.remove(role);
				pub.setRoles(roles);
				createOrUpdatePUBindingFolder(pub);
				return;
			}
		}
	}
	
	/**
	 * Removes a role from all the bindings of the given project (useful in case a role is deleted)
	 * @param role
	 * @throws PUBindingException 
	 */
	public static void removeRoleFromPUBindings(AbstractProject project, Role role) throws PUBindingException {
		for (ProjectUserBinding pub : listPUBindingsOfProject(project)) {
			Collection<Role> roles = pub.getRoles();
			roles.remove(role);
			pub.setRoles(roles);
			createOrUpdatePUBindingFolder(pub);
		}
	}
	
	/**
	 * Creates a folder for the given project-user bidning and serializes the details about it in a file.
	 * If the folder is already created, simply update the info in the details file.
	 * @throws PUBindingException 
	 */
	private static void createOrUpdatePUBindingFolder(ProjectUserBinding puBinding) throws PUBindingException {
		try {
			ProjectUserBindingsRepoHelper tempPUBindingsRepoHelper = new ProjectUserBindingsRepoHelper();
			tempPUBindingsRepoHelper.insertBinding(puBinding);
			tempPUBindingsRepoHelper.saveBindingDetailsFile(getPUBindingDetailsFile(puBinding));
			tempPUBindingsRepoHelper.shutDownRepository();
		} catch (IOException e) {
			throw new PUBindingException(e);
		}
	}
	
	/**
	 * Returns the folder about the given project under <STData>/pu_bindings/
	 * @param projectName
	 * @return
	 */
	public static File getProjBindingsFolder(AbstractProject project) {
		return new File(Resources.getProjectUserBindingsDir() + File.separator + project.getName());
	}
	
	/**
	 * Returns all the projects folder under <STData>/pu_bindings/
	 * @return
	 */
	private static Collection<File> getAllProjBindingsFolders() {
		Collection<File> projBindingsFolders = new ArrayList<>();
		File puBindingFolder = Resources.getProjectUserBindingsDir();
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
	
	/**
	 * Returns the user folders under <STData>/pu_bindings/<projectName>/ for the given project-user pair
	 * @param projectName
	 * @param userEmail
	 * @return
	 */
	public static File getPUBindingsFolder(AbstractProject project, STUser user) {
		return new File(getProjBindingsFolder(project) + File.separator + STUser.encodeUserEmail(user.getEmail()));
	}
	
	/**
	 * Returns the user folders under all the <STData>/pu_bindings/<projName>/ folders
	 * @param userEmail
	 * @return
	 */
	public static Collection<File> getUserBindingsFolders(STUser user) {
		Collection<File> userBindingsFolders = new ArrayList<>();
		Collection<File> projBindFolders = getAllProjBindingsFolders();
		//get all subfolder of "pu_binding/<projectName>" folder (one subfolder for each user)
		for (File projFolder : projBindFolders) {
			userBindingsFolders.add(new File(projFolder, STUser.encodeUserEmail(user.getEmail())));
		}
		return userBindingsFolders;
	}
	
	/**
	 * Returns the binding.tts file of the given puBinding
	 * @param puBinding
	 * @return
	 */
	private static File getPUBindingDetailsFile(ProjectUserBinding puBinding) {
		File bindingFolder = new File(Resources.getProjectUserBindingsDir() + File.separator + puBinding.getProject().getName() 
			+ File.separator + STUser.encodeUserEmail(puBinding.getUser().getEmail()));
		if (!bindingFolder.exists()) {
			bindingFolder.mkdirs();
		}
		return new File(bindingFolder + File.separator + PU_BINDING_DETAILS_FILE_NAME);
	}
	
	/**
	 * Returns all the binding.ttl files for every project-user bindings
	 * @return
	 */
	private static Collection<File> getAllPUBindingFiles() {
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
	
}
