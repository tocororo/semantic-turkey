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
	
	private static Collection<ProjectGroupBinding> pgBindingList = new ArrayList<>();
	
	/**
	 * Loads all the bindings into the repository
	 * Protected since the load should be done just once by AccessControlManager during its initialization
	 * @throws IOException 
	 * @throws RepositoryException 
	 * @throws RDFParseException 
	 */
	public static void loadPGBindings() throws RDFParseException, RepositoryException, IOException {
		ProjectGroupBindingsRepoHelper repoHelper = new ProjectGroupBindingsRepoHelper();
		Collection<File> bindingsFolders = getAllPGBindingFiles();
		for (File f : bindingsFolders) {
			repoHelper.loadBindingDetails(f);
		}
		pgBindingList = repoHelper.listPGBindings();
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
	 * Adds a project-group binding 
	 * @param pgBinding
	 * @throws IOException 
	 */
	public static void createPGBinding(ProjectGroupBinding pgBinding) throws ProjectBindingException {
		pgBindingList.add(pgBinding);
		createOrUpdatePGBindingFolder(pgBinding);
	}
	
	/**
	 * Returns the ProjectGroupBinding that binds the given user and project. Null if there is no binding
	 * @param user
	 * @param projectName
	 * @return
	 */
	public static ProjectGroupBinding getPGBinding(UsersGroup group, AbstractProject project) {
		ProjectGroupBinding pgBinding = null;
		for (ProjectGroupBinding pgb : pgBindingList) {
			if (pgb.getGroup().getIRI().equals(group.getIRI()) && pgb.getProject().getName().equals(project.getName())) {
				pgBinding = pgb;
			}
		}
		return pgBinding;
	}
	
	/**
	 * Returns the ProjectGroupBindings of the given project
	 * @param projectName
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
	 * @param projectName
	 * @return
	 */
	public static boolean existsPGBindingsOfProject(AbstractProject project) {
		return getProjBindingsFolder(project).exists();
	}
	
	/**
	 * Creates all the project-group bindings folders related to the given project.
	 * Useful when a project is created/imported
	 * @param projectName
	 * @throws IOException 
	 */
	public static void createPGBindingsOfProject(AbstractProject project) throws ProjectBindingException {
		Collection<UsersGroup> groups = UsersGroupsManager.listGroups();
		//for each group creates the binding with the given project
		for (UsersGroup g : groups) {
			createPGBinding(new ProjectGroupBinding(project, g));
		}
	}
	
	/**
	 * When a project is deleted, deletes all the project-group bindings folders related to the given project
	 * @param projectName
	 * @throws IOException 
	 */
	public static void deletePGBindingsOfProject(String projectName) throws IOException {
		Iterator<ProjectGroupBinding> itPGB = pgBindingList.iterator();
		while (itPGB.hasNext()) {
			if (itPGB.next().getProject().getName().equals(projectName)) {
				itPGB.remove();
			}
		}
		//delete folder about the project's bindings
		FileUtils.deleteDirectory(getProjBindingsFolder(projectName));
	}
	
	/**
	 * Creates all the project-group bindings folders related to the given group.
	 * Useful when a group is created/imported
	 * @throws ProjectAccessException 
	 * @throws IOException 
	 */
	public static void createPGBindingsOfGroup(UsersGroup group) throws ProjectAccessException, ProjectBindingException {
		Collection<AbstractProject> projects = ProjectManager.listProjects();
		//for each project creates the binding with the given group
		for (AbstractProject abstrProj : projects) {
			if (abstrProj instanceof Project) {
				createPGBinding(new ProjectGroupBinding(abstrProj, group));
			}
		}
	}
	
	/**
	 * When a group is deleted, deletes all the project-group bindings folders related to the given group
	 * @param userEmail
	 * @throws IOException 
	 */
	public static void deletePGBindingsOfGroup(UsersGroup group) throws IOException {
		Iterator<ProjectGroupBinding> itPGB = pgBindingList.iterator();
		while (itPGB.hasNext()) {
			if (itPGB.next().getGroup().getIRI().equals(group.getIRI())) {
				itPGB.remove();
			}
		}
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
		for (ProjectGroupBinding pgb : pgBindingList) {
			if (pgb.getGroup().getIRI().equals(group.getIRI()) && pgb.getProject().getName().equals(project.getName())) {
				for (IRI s : schemes) {
					pgb.addScheme(s);
				}
				createOrUpdatePGBindingFolder(pgb);
				return;
			}
		}
	}
	
	/**
	 * Adds role to the binding between the given project-group pair
	 * @param group
	 * @param project
	 * @param schemes
	 * @throws ProjectBindingException
	 */
	public static void addSchemeToPGBinding(UsersGroup group, AbstractProject project, IRI scheme) throws ProjectBindingException {
		for (ProjectGroupBinding pgb : pgBindingList) {
			if (pgb.getGroup().getIRI().equals(group.getIRI()) && pgb.getProject().getName().equals(project.getName())) {
				pgb.addScheme(scheme);
				createOrUpdatePGBindingFolder(pgb);
				return;
			}
		}
	}
	
	/**
	 * Removes a scheme from the binding between the given project-group pair
	 * @param userEmail
	 * @param projectName
	 * @param role
	 * @throws ProjectBindingException
	 */
	public static void removeSchemeFromPGBinding(UsersGroup group, AbstractProject project, IRI scheme) throws ProjectBindingException {
		for (ProjectGroupBinding pgb : pgBindingList) {
			if (pgb.getGroup().getIRI().equals(group.getIRI()) && pgb.getProject().getName().equals(project.getName())) {
				pgb.removeScheme(scheme);
				createOrUpdatePGBindingFolder(pgb);
				return;
			}
		}
	}
	
	/**
	 * Remove all the schemes assigned to a group in a project
	 * @param user
	 * @param project
	 * @param role
	 * @throws ProjectBindingException
	 */
	public static void removeAllSchemesFromPGBinding(UsersGroup group, AbstractProject project) throws ProjectBindingException {
		for (ProjectGroupBinding pgb : pgBindingList) {
			if (pgb.getGroup().getIRI().equals(group.getIRI()) && pgb.getProject().getName().equals(project.getName())) {
				pgb.setSchemes(new ArrayList<IRI>());
				createOrUpdatePGBindingFolder(pgb);
				return;
			}
		}
	}
	
	/**
	 * Removes a scheme from all the bindings of the given project (useful in case a group is deleted)
	 * @param role
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
	public static boolean hasUserOwnershipOfSchemes(STUser user, Project project, List<IRI> schemes, boolean or) throws STPropertyAccessException {
		if (user.isAdmin()) {
			return true;
		}
		UsersGroup group = ProjectUserBindingsManager.getPUBinding(user, project).getGroup();
		if (group == null) {
			return true;
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
			tempPGBindingsRepoHelper.saveBindingDetailsFile(getPGBindingDetailsFile(pgBinding));
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
	 * @param projectName
	 * @param userEmail
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
	 * @param pgBinding
	 * @return
	 */
	private static File getPGBindingDetailsFile(ProjectGroupBinding pgBinding) {
		File bindingFolder = new File(Resources.getProjectGroupBindingsDir() + File.separator + pgBinding.getProject().getName() 
			+ File.separator + UsersGroup.encodeGroupIri(pgBinding.getGroup().getIRI()));
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
	 * @param projectName
	 * @param userEmail
	 * @return
	 */
	public static File getPUBindingsFolder(AbstractProject project, STUser user) {
		return new File(getProjBindingsFolder(project) + File.separator + STUser.encodeUserIri(user.getIRI()));
	}
	
	/**
	 * Returns all the binding.ttl files for every project-user bindings
	 * @return
	 */
	private static Collection<File> getAllPGBindingFiles() {
		Collection<File> pgBindingDetailsFolders = new ArrayList<>(); 
		Collection<File> projBindFolders = getAllProjBindingsFolders();
		//get all subfolder of "pg_binding/<projectName>" folder (one subfolder for each group)
		for (File projFolder : projBindFolders) {
			String[] groupDirectories = projFolder.list(new FilenameFilter() {
				public boolean accept(File current, String name) {
					return new File(current, name).isDirectory();
				}
			});
			for (String groupDir : groupDirectories) {
				pgBindingDetailsFolders.add(new File(projFolder + File.separator + groupDir 
						+ File.separator + PG_BINDING_DETAILS_FILE_NAME));
			}
		}
		return pgBindingDetailsFolders;
	}
	
	
}
