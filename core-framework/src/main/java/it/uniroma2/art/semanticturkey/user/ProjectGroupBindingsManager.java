package it.uniroma2.art.semanticturkey.user;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;

import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.resources.Resources;

public class ProjectGroupBindingsManager {
	
	private static final String PG_BINDING_DETAILS_FILE_NAME = "binding.ttl";
	
	private static Collection<ProjectGroupBinding> pgBindingList;
	
	/**
	 * Loads all the bindings into the repository
	 * Protected since the load should be done just once by AccessControlManager during its initialization
	 * @throws IOException 
	 * @throws RepositoryException 
	 * @throws RDFParseException 
	 * @throws ProjectAccessException 
	 */
	public static void loadPGBindings() throws RDFParseException, RepositoryException, IOException, ProjectAccessException {
		pgBindingList = new ArrayList<>();
		Collection<AbstractProject> projects = ProjectManager.listProjects();
		Collection<UsersGroup> groups = UsersGroupsManager.listGroups();
		
		ProjectGroupBindingsRepoHelper repoHelper = new ProjectGroupBindingsRepoHelper();
		
		/*
		 * Iterate all over the project-group pair. If the PGB details file exists, load it into the repo,
		 * otherwise create the PGBinding and add it to the PUBlist.
		 * At the end of the iteration, add to the PGBList also all the PGB loaded into the repo.
		 */
		for (AbstractProject absProj : projects) {
			if (absProj instanceof Project) {
				Project project = (Project) absProj;
				for (UsersGroup group : groups) {
					File pgbFile = getPGBindingDetailsFile(project, group);
					if (pgbFile.exists()) { //if exists add it to the repo
						repoHelper.loadBindingDetails(pgbFile);
					} else { //otherwise create the "empty" PUB and add it to the list
						pgBindingList.add(new ProjectGroupBinding(project, group));
					}
				}
			}
		}
		//add to the list all the PGB loaded into the repo
		pgBindingList.addAll(repoHelper.listPGBindings());
		repoHelper.shutDownRepository();
		
		//For debug
//		System.out.println("Project-Group Bindings");
//		for (ProjectGroupBinding pgb : pgBindingList) {
//			System.out.println(pgb);
//		}
	}
	
	/**
	 * Returns all the project-group bindings
	 * @return
	 */
	public static Collection<ProjectGroupBinding> listPGBindings() {
		return pgBindingList;
	}
	
	/**
	 * Returns the ProjectGroupBinding that binds the given group and project
	 * @param group
	 * @param project
	 * @return
	 */
	public static ProjectGroupBinding getPGBinding(UsersGroup group, AbstractProject project) {
		ProjectGroupBinding pgBinding = null;
		for (ProjectGroupBinding pgb : pgBindingList) {
			if (pgb.getGroup().getIRI().equals(group.getIRI()) && pgb.getProject().getName().equals(project.getName())) {
				pgBinding = pgb;
			}
		}
		if (pgBinding == null) {
			/*
			 * if the binding doesn't exist initializes it, just in the list, not in the filesystem, it will be
			 * eventually stored on the filesystem in case it changes
			 */
			pgBinding = new ProjectGroupBinding(project, group);
		}
		return pgBinding;
	}
	
	/**
	 * Returns the ProjectGroupBindings of the given project
	 * @param project
	 * @return
	 */
	public static Collection<ProjectGroupBinding> listPGBindingsOfProject(AbstractProject project) {
		Collection<ProjectGroupBinding> pgbList = new ArrayList<>();
		for (ProjectGroupBinding pub : pgBindingList) {
			if (pub.getProject().getName().equals(project.getName())) {
				pgbList.add(pub);
			}
		}
		return pgbList;
	}
	
	/**
	 * Checks if there is the folder of project-group bindings for the given project
	 * @param project
	 * @return
	 */
	public static boolean existsPGBindingsOfProject(AbstractProject project) {
		return getProjBindingsFolder(project).exists();
	}
	
	/**
	 * Creates all the project-group bindings folders related to the given project.
	 * Useful when a project is created/imported
	 * @param project
	 */
	public static void createPGBindingsOfProject(AbstractProject project) {
		Collection<UsersGroup> groups = UsersGroupsManager.listGroups();
		//for each group creates the binding with the given project
		for (UsersGroup g : groups) {
			pgBindingList.add(new ProjectGroupBinding(project, g));
		}
	}
	
	/**
	 * When a project is deleted, deletes all the project-group bindings folders related to the given project
	 * @param projectName
	 * @throws IOException 
	 */
	public static void deletePGBindingsOfProject(String projectName) throws IOException {
		pgBindingList.removeIf(projectGroupBinding -> projectGroupBinding.getProject().getName().equals(projectName));
		//delete folder about the project's bindings
		FileUtils.deleteDirectory(getProjBindingsFolder(projectName));
	}
	
	/**
	 * Creates all the project-group bindings folders related to the given group.
	 * Useful when a group is created/imported
	 * @throws ProjectAccessException 
	 * @throws IOException 
	 */
	public static void createPGBindingsOfGroup(UsersGroup group) throws ProjectAccessException {
		Collection<AbstractProject> projects = ProjectManager.listProjects();
		//for each project creates the binding with the given group
		for (AbstractProject abstrProj : projects) {
			if (abstrProj instanceof Project) {
				pgBindingList.add(new ProjectGroupBinding(abstrProj, group));
			}
		}
	}
	
	/**
	 * When a group is deleted, deletes all the project-group bindings folders related to the given group
	 * @param group
	 * @throws IOException 
	 */
	public static void deletePGBindingsOfGroup(UsersGroup group) throws IOException {
		pgBindingList.removeIf(projectGroupBinding -> projectGroupBinding.getGroup().getIRI().equals(group.getIRI()));
		//delete folders about the group's bindings
		for (File userBindingFolder : getGroupBindingsFolders(group)) {
			FileUtils.deleteDirectory(userBindingFolder);
		}
	}
	
	/* =================================
	 * Schemes
	 * ================================= */
	
	/**
	 * Adds schemes to the binding between the given project-group pair
	 * @param group
	 * @param project
	 * @param schemes
	 * @throws ProjectBindingException
	 */
	public static void addSchemesToPGBinding(UsersGroup group, AbstractProject project, Collection<IRI> schemes) throws ProjectBindingException {
		ProjectGroupBinding pgb = getPGBinding(group, project);
		for (IRI s : schemes) {
			pgb.addScheme(s);
		}
		createOrUpdatePGBindingFolder(pgb);
	}
	
	/**
	 * Adds role to the binding between the given project-group pair
	 * @param group
	 * @param project
	 * @param scheme
	 * @throws ProjectBindingException
	 */
	public static void addSchemeToPGBinding(UsersGroup group, AbstractProject project, IRI scheme) throws ProjectBindingException {
		ProjectGroupBinding pgb = getPGBinding(group, project);
		pgb.addScheme(scheme);
		createOrUpdatePGBindingFolder(pgb);
	}
	
	/**
	 * Removes a scheme from the binding between the given project-group pair
	 * @param group
	 * @param project
	 * @param scheme
	 * @throws ProjectBindingException
	 */
	public static void removeSchemeFromPGBinding(UsersGroup group, AbstractProject project, IRI scheme) throws ProjectBindingException {
		ProjectGroupBinding pgb = getPGBinding(group, project);
		pgb.removeScheme(scheme);
		createOrUpdatePGBindingFolder(pgb);
	}
	
	/**
	 * Remove all the schemes assigned to a group in a project
	 * @param group
	 * @param project
	 * @throws ProjectBindingException
	 */
	public static void removeAllSchemesFromPGBinding(UsersGroup group, AbstractProject project) throws ProjectBindingException {
		ProjectGroupBinding pgb = getPGBinding(group, project);
		pgb.setSchemes(new ArrayList<IRI>());
		createOrUpdatePGBindingFolder(pgb);
	}
	
	/**
	 * Removes a scheme from all the bindings of the given project (useful in case a group is deleted)
	 * @param project
	 * @param scheme
	 * @throws ProjectBindingException 
	 */
	public static void removeSchemeFromPGBindings(AbstractProject project, IRI scheme) throws ProjectBindingException {
		for (ProjectGroupBinding pgb : listPGBindingsOfProject(project)) {
			Collection<IRI> schemes = pgb.getOwnedSchemes();
			schemes.remove(scheme);
			pgb.setSchemes(schemes);
			createOrUpdatePGBindingFolder(pgb);
		}
	}
	
	/**
	 * Returns true if user belongs to a group that has ownership on the given schemes. This method evaluates the 
	 * ownership of the scheme in OR or in AND according to the {@code or} parameter. 
	 * @param user
	 * @param project
	 * @param schemes
	 * @param or if {@code true} the method returns true if one of the schemes is owned by the user's group,
	 * 	if {@code false} returns true if all the schemes are owned by the user's group
	 * @return
	 * @throws STPropertyAccessException
	 */
	public static boolean hasUserOwnershipOfSchemes(STUser user, Project project, List<IRI> schemes, boolean or) {
		if (user.isAdmin()) {
			return true;
		}
		ProjectUserBinding puBinding = ProjectUserBindingsManager.getPUBinding(user, project);
		UsersGroup group = puBinding.getGroup();
		if (group == null) {
			return true;
		} else {
			if (!puBinding.isSubjectToGroupLimitations()) {
				return true; //user belongs to a group but is not subject to its limitations
			}
		}
		Collection<IRI> ownedSchems = getPGBinding(group, project).getOwnedSchemes();
		if (ownedSchems.isEmpty()) { //no schemes owned by the group
			return false;
		}
		if (or) { //OR mode, check if at least one scheme is in the ownedSchemes
			for (IRI s : schemes) {
				if (ownedSchems.contains(s)) {
					return true;
				}
			}
		} else { //AND mode, check if all the schemes are in the ownedSchemes
			for (IRI s : schemes) {
				if (!ownedSchems.contains(s)) {
					return false; //found one scheme not owned
				}
			}
			return true; //if this code is reached, every scheme
		}
		return false;
	}
	
	/* =================================
	 * Utils
	 * ================================= */
	
	/**
	 * Creates a folder for the given project-group binding and serializes the details about it in a file.
	 * If the folder is already created, simply update the info in the details file.
	 * @throws ProjectBindingException 
	 */
	private static void createOrUpdatePGBindingFolder(ProjectGroupBinding pgBinding) throws ProjectBindingException {
		try {
			ProjectGroupBindingsRepoHelper tempPGBindingsRepoHelper = new ProjectGroupBindingsRepoHelper();
			tempPGBindingsRepoHelper.insertBinding(pgBinding);
			tempPGBindingsRepoHelper.saveBindingDetailsFile(getPGBindingDetailsFile(pgBinding.getProject(), pgBinding.getGroup()));
			tempPGBindingsRepoHelper.shutDownRepository();
		} catch (IOException e) {
			throw new ProjectBindingException(e);
		}
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
	 * Returns the user folders under <STData>/pg_bindings/<projectName>/ for the given project-group pair
	 * @param project
	 * @param group
	 * @return
	 */
	public static File getPGBindingsFolder(AbstractProject project, UsersGroup group) {
		return new File(getProjBindingsFolder(project) + File.separator + UsersGroup.encodeGroupIri(group.getIRI()));
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
			groupBindingsFolders.add(new File(projFolder, UsersGroup.encodeGroupIri(group.getIRI())));
		}
		return groupBindingsFolders;
	}
	
	/**
	 * Returns the binding.tts file of the given puBinding
	 * @param project
	 * @param group
	 * @return
	 */
	private static File getPGBindingDetailsFile(AbstractProject project, UsersGroup group) {
		File bindingFolder = new File(Resources.getProjectGroupBindingsDir() + File.separator + 
				project.getName() + File.separator + 
				UsersGroup.encodeGroupIri(group.getIRI()));
		if (!bindingFolder.exists()) {
			bindingFolder.mkdirs();
		}
		return new File(bindingFolder + File.separator + PG_BINDING_DETAILS_FILE_NAME);
	}
	
	/**
	 * Returns (and create if it doesn't exist) a folder for the given project-group binding.
	 * @param project
	 * @param group
	 * @return
	 */
	public static File getPGBindingFolder(AbstractProject project, UsersGroup group) {
		File bindingFolder = new File(Resources.getProjectGroupBindingsDir() + File.separator + project.getName() 
			+ File.separator + UsersGroup.encodeGroupIri(group.getIRI()));
		if (!bindingFolder.exists()) {
			bindingFolder.mkdirs();
		}
		return bindingFolder;
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
	 * @param project
	 * @param user
	 * @return
	 */
	public static File getPUBindingsFolder(AbstractProject project, STUser user) {
		return new File(getProjBindingsFolder(project) + File.separator + STUser.encodeUserIri(user.getIRI()));
	}
	
}
