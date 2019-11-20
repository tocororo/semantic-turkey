package it.uniroma2.art.semanticturkey.user;

import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.resources.Resources;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class ProjectUserBindingsManager {
	
	private static final String PU_BINDING_DETAILS_FILE_NAME = "binding.ttl";
	
	private static Collection<ProjectUserBinding> puBindingList;
	
	/**
	 * Loads all the bindings into the repository
	 * Protected since the load should be done just once by AccessControlManager during its initialization
	 * @throws IOException 
	 * @throws RepositoryException 
	 * @throws RDFParseException 
	 * @throws ProjectAccessException 
	 */
	public static void loadPUBindings() throws RDFParseException, RepositoryException, IOException, ProjectAccessException {
		puBindingList = new ArrayList<>();
		Collection<AbstractProject> projects = ProjectManager.listProjects();
		Collection<STUser> users = UsersManager.listUsers();
		
		ProjectUserBindingsRepoHelper repoHelper = new ProjectUserBindingsRepoHelper();
		
		/*
		 * Iterate all over the project-user pair. If the PUB details file exists, load it into the repo,
		 * otherwise create the PUBinding and add it to the PUBlist.
		 * At the end of the iteration, add to the PUBList also all the PUB loaded into the repo.
		 */
		for (AbstractProject absProj : projects) {
			if (absProj instanceof Project) {
				Project project = (Project) absProj;
				for (STUser user : users) {
					File pubFile = getPUBindingDetailsFile(project, user);
					if (pubFile.exists()) { //if exists add it to the repo
						repoHelper.loadBindingDetails(pubFile);
					} else { //otherwise create the "empty" PUB and add it to the list
						puBindingList.add(new ProjectUserBinding(project, user));
					}
				}
			}
		}
		//add to the list all the PUB loaded into the repo
		puBindingList.addAll(repoHelper.listPUBindings());
		repoHelper.shutDownRepository();
		
		//For debug
//		System.out.println("Project-User Bindings");
//		for (ProjectUserBinding pub : puBindingList) {
//			System.out.println(pub);
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
	 * Returns the ProjectUserBinding that binds the given user and project
	 * @param user
	 * @param project
	 * @return
	 */
	public static ProjectUserBinding getPUBinding(STUser user, AbstractProject project) {
		ProjectUserBinding puBinding = null;
		for (ProjectUserBinding pub : puBindingList) {
			if (pub.getUser().getIRI().equals(user.getIRI()) && pub.getProject().getName().equals(project.getName())) {
				puBinding = pub;
			}
		}
		if (puBinding == null) {
			/*
			 * if the binding doesn't exist initializes it, just in the list, not in the filesystem, it will be
			 * eventually stored on the filesystem in case it changes
			 */
			puBinding = new ProjectUserBinding(project, user);
		}
		return puBinding;
	}
	
	/**
	 * Returns the ProjectUserBindings of the given project
	 * @param project
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
	 * @param project
	 * @return
	 */
	public static boolean existsPUBindingsOfProject(AbstractProject project) {
		return getProjBindingsFolder(project).exists();
	}
	
	/**
	 * Creates all the project-user bindings folders related to the given project.
	 * Useful when a project is created/imported
	 * and the existing users
	 * @param project
	 */
	public static void createPUBindingsOfProject(AbstractProject project) {
		Collection<STUser> users = UsersManager.listUsers();
		//for each user creates the binding with the given project
		for (STUser u : users) {
			puBindingList.add(new ProjectUserBinding(project, u));
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
			if (itPUB.next().getProject().getName().equals(projectName)) {
				itPUB.remove();
			}
		}
		//delete folder about the project's bindings
		FileUtils.deleteDirectory(getProjBindingsFolder(projectName));
	}
	
	/**
	 * Creates all the project-user bindings folders related to the given user.
	 * Useful when a user is created/imported
	 * and the existing projects
	 * @param user
	 * @throws ProjectAccessException 
	 * @throws IOException 
	 */
	public static void createPUBindingsOfUser(STUser user) throws ProjectAccessException {
		Collection<AbstractProject> projects = ProjectManager.listProjects();
		//for each project creates the binding with the given user
		for (AbstractProject abstrProj : projects) {
			if (abstrProj instanceof Project) {
				puBindingList.add(new ProjectUserBinding(abstrProj, user));
			}
		}
	}
	
	/**
	 * When a user is deleted, deletes all the project-user bindings folders related to the given user
	 * @param user
	 * @throws IOException 
	 */
	public static void deletePUBindingsOfUser(STUser user) throws IOException {
		Iterator<ProjectUserBinding> itPUB = puBindingList.iterator();
		while (itPUB.hasNext()) {
			if (itPUB.next().getUser().getIRI().equals(user.getIRI())) {
				itPUB.remove();
			}
		}
		//delete folders about the user's bindings
		for (File userBindingFolder : getUserBindingsFolders(user)) {
			FileUtils.deleteDirectory(userBindingFolder);
		}
	}
	
	/* =================================
	 * Roles
	 * ================================= */
	
	/**
	 * Adds roles to the binding between the given project-user pair
	 * @param user
	 * @param project
	 * @param roles
	 * @throws ProjectBindingException
	 */
	public static void addRolesToPUBinding(STUser user, AbstractProject project, Collection<Role> roles) throws ProjectBindingException {
		ProjectUserBinding pub = getPUBinding(user, project);
		for (Role r : roles) {
			pub.addRole(r);
		}
		createOrUpdatePUBindingFolder(pub);
	}
	
	/**
	 * Adds role to the binding between the given project-user pair
	 * @param user
	 * @param project
	 * @param role
	 * @throws ProjectBindingException
	 */
	public static void addRoleToPUBinding(STUser user, AbstractProject project, Role role) throws ProjectBindingException {
		ProjectUserBinding pub = getPUBinding(user, project);
		pub.addRole(role);
		createOrUpdatePUBindingFolder(pub);
	}
	
	/**
	 * Removes a role from the binding between the given project-user pair
	 * @param user
	 * @param project
	 * @param role
	 * @throws ProjectBindingException
	 */
	public static void removeRoleFromPUBinding(STUser user, AbstractProject project, Role role) throws ProjectBindingException {
		ProjectUserBinding pub = getPUBinding(user, project);
		Collection<Role> roles = pub.getRoles();
		roles.remove(role);
		pub.setRoles(roles);
		createOrUpdatePUBindingFolder(pub);
	}
	
	/**
	 * Remove all the roles assigned to a user in a project
	 * @param user
	 * @param project
	 * @throws ProjectBindingException
	 */
	public static void removeAllRoleFromPUBinding(STUser user, AbstractProject project) throws ProjectBindingException {
		ProjectUserBinding pub = getPUBinding(user, project);
		pub.setRoles(new ArrayList<Role>());
		createOrUpdatePUBindingFolder(pub);
	}
	
	/**
	 * Removes a role from all the bindings of the given project (useful in case a role is deleted)
	 * @param role
	 * @throws ProjectBindingException 
	 */
	public static void removeRoleFromPUBindings(AbstractProject project, Role role) throws ProjectBindingException {
		for (ProjectUserBinding pub : listPUBindingsOfProject(project)) {
			Collection<Role> roles = pub.getRoles();
			roles.remove(role);
			pub.setRoles(roles);
			createOrUpdatePUBindingFolder(pub);
		}
	}
	
	/**
	 * Returns true if user has right to access the project (if it has any role in the given project or if it is admin)
	 * @param user
	 * @param project
	 * @return
	 */
	public static boolean hasUserAccessToProject(STUser user, Project project) {
		if (user.isAdmin()) {
			return true;
		}
		return !getPUBinding(user, project).getRoles().isEmpty();
	}
	
	
	/* =================================
	 * Languages
	 * ================================= */
	
	/**
	 * Adds languages to the binding between the given project-user pair
	 * @param user
	 * @param project
	 * @param languages
	 * @throws ProjectBindingException
	 */
	public static void addLanguagesToPUBinding(STUser user, AbstractProject project, Collection<String> languages)
			throws ProjectBindingException {
		ProjectUserBinding pub = getPUBinding(user, project);
		for (String l : languages) {
			pub.addLanguage(l);
		}
		createOrUpdatePUBindingFolder(pub);
	}
	
	/**
	 * Removes a language from the binding between the given project-user pair
	 * @param user
	 * @param project
	 * @param languages
	 * @throws ProjectBindingException
	 */
	public static void updateLanguagesToPUBinding(STUser user, AbstractProject project, Collection<String> languages) throws ProjectBindingException {
		ProjectUserBinding pub = getPUBinding(user, project);
		pub.setLanguages(languages);
		createOrUpdatePUBindingFolder(pub);
	}
	
	/* =================================
	 * Groups
	 * ================================= */
	
	/**
	 * Assigns a group to the binding between the given project-user pair
	 * @param user
	 * @param project
	 * @param group
	 * @throws ProjectBindingException
	 */
	public static void assignGroupToPUBinding(STUser user, AbstractProject project, UsersGroup group) throws ProjectBindingException {
		ProjectUserBinding pub = getPUBinding(user, project);
		pub.assignGroup(group);
		createOrUpdatePUBindingFolder(pub);
	}
	
	/**
	 * Assigns a group to the binding between the given project-user pair
	 * @param user
	 * @param project
	 * @param group
	 * @throws ProjectBindingException
	 */
	public static void setGroupLimitationsToPUBinding(STUser user, AbstractProject project, UsersGroup group, boolean limitations) throws ProjectBindingException {
		ProjectUserBinding pub = getPUBinding(user, project);
		pub.setSubjectToGroupLimitations(limitations);
		createOrUpdatePUBindingFolder(pub);
	}
	
	/**
	 * Remove the group assigned to the user in the given project-user pair
	 * @param user
	 * @param project
	 * @throws ProjectBindingException
	 */
	public static void removeGroupFromPUBinding(STUser user, AbstractProject project) throws ProjectBindingException {
		ProjectUserBinding pub = getPUBinding(user, project);
		pub.removeGroup();
		createOrUpdatePUBindingFolder(pub);
	}
	
	/**
	 * Returns the UsersGroup which the given user belongs to
	 * @param user
	 * @param project
	 * @return
	 */
	public static UsersGroup getUserGroup(STUser user, Project project) {
		ProjectUserBinding pub = getPUBinding(user, project);
		if (pub != null) {
			return pub.getGroup();
		}
		return null;
	}
	
	/* =================================
	 * Utils
	 * ================================= */
	
	/**
	 * Creates a folder for the given project-user bidning and serializes the details about it in a file.
	 * If the folder is already created, simply update the info in the details file.
	 * @throws ProjectBindingException 
	 */
	private static void createOrUpdatePUBindingFolder(ProjectUserBinding puBinding) throws ProjectBindingException {
		try {
			ProjectUserBindingsRepoHelper tempPUBindingsRepoHelper = new ProjectUserBindingsRepoHelper();
			tempPUBindingsRepoHelper.insertBinding(puBinding);
			tempPUBindingsRepoHelper.saveBindingDetailsFile(getPUBindingDetailsFile(puBinding.getProject(), puBinding.getUser()));
			tempPUBindingsRepoHelper.shutDownRepository();
		} catch (IOException e) {
			throw new ProjectBindingException(e);
		}
	}
	
	/**
	 * Returns the folder about the given project under <STData>/pu_bindings/
	 * @param project
	 * @return
	 */
	public static File getProjBindingsFolder(AbstractProject project) {
		return getProjBindingsFolder(project.getName());
	}
	
	/**
	 * Returns the folder about the given project under <STData>/pu_bindings/
	 * @param projectName
	 * @return
	 */
	public static File getProjBindingsFolder(String projectName) {
		return new File(Resources.getProjectUserBindingsDir() + File.separator + projectName);
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
	 * @param project
	 * @param user
	 * @return
	 */
	public static File getPUBindingsFolder(AbstractProject project, STUser user) {
		return new File(getProjBindingsFolder(project) + File.separator + STUser.encodeUserIri(user.getIRI()));
	}
	
	/**
	 * Returns the user folders under all the <STData>/pu_bindings/<projName>/ folders
	 * @param user
	 * @return
	 */
	public static Collection<File> getUserBindingsFolders(STUser user) {
		Collection<File> userBindingsFolders = new ArrayList<>();
		Collection<File> projBindFolders = getAllProjBindingsFolders();
		//get all subfolder of "pu_binding/<projectName>" folder (one subfolder for each user)
		for (File projFolder : projBindFolders) {
			userBindingsFolders.add(new File(projFolder, STUser.encodeUserIri(user.getIRI())));
		}
		return userBindingsFolders;
	}
	
	/**
	 * Returns the binding.tts file of the given puBinding
	 * @param project
	 * @param user
	 * @return
	 */
	private static File getPUBindingDetailsFile(AbstractProject project, STUser user) {
		File bindingFolder = new File(Resources.getProjectUserBindingsDir() + File.separator + 
				project.getName() + File.separator + 
				STUser.encodeUserIri(user.getIRI()));
		if (!bindingFolder.exists()) {
			bindingFolder.mkdirs();
		}
		return new File(bindingFolder + File.separator + PU_BINDING_DETAILS_FILE_NAME);
	}

}
