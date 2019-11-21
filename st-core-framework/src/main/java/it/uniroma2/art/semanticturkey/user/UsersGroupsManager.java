package it.uniroma2.art.semanticturkey.user;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;

import it.uniroma2.art.semanticturkey.resources.Resources;

public class UsersGroupsManager {
	
	private static final String GROUP_DETAILS_FILE_NAME = "details.ttl";

	private static Collection<UsersGroup> groupList = new ArrayList<>();
	
	/**
	 * Loads all the groups into the repository
	 * @throws IOException 
	 * @throws RepositoryException 
	 * @throws RDFParseException 
	 */
	public static void loadGroups() throws RDFParseException, RepositoryException, IOException {
		UsersGroupsRepoHelper groupRepoHelper = new UsersGroupsRepoHelper();
		Collection<File> groupDetailsFolders = getAllGroupDetailsFiles();
		for (File f : groupDetailsFolders) {
			groupRepoHelper.loadGroupDetails(f);
		}
		groupList = groupRepoHelper.listGroups();
		groupRepoHelper.shutDownRepository();
	}
	
	/**
	 * add a group
	 * 
	 * @param group
	 * @throws UserException
	 * @throws IOException
	 */
	public static void createGroup(UsersGroup group) throws UsersGroupException {
		if (getGroupByShortName(group.getShortName()) != null) {
			throw new UsersGroupException("Name " + group.getShortName() + " already used by another group");
		}
		if (getGroupByIRI(group.getIRI()) != null) {
			throw new UsersGroupException("IRI " + group.getIRI().stringValue() + " already used by another group");
		}
		groupList.add(group);
		createOrUpdateGroupDetailsFolder(group); // serialize group detials
	}

	/**
	 * Returns a list of all the groups
	 * 
	 * @return
	 */
	public static Collection<UsersGroup> listGroups() {
		return groupList;
	}
	
	/**
	 * Returns the group with the given IRI. Null if there is no group with the given IRI.
	 * 
	 * @param iri
	 * @return
	 */
	public static UsersGroup getGroupByIRI(IRI iri) {
		UsersGroup group = null;
		for (UsersGroup g : groupList) {
			if (g.getIRI().equals(iri)) {
				group = g;
			}
		}
		return group;
	}
	
	/**
	 * Returns the group with the given name. Null if there is no group with the given name.
	 * 
	 * @param name
	 * @return
	 */
	public static UsersGroup getGroupByShortName(String name) {
		UsersGroup group = null;
		for (UsersGroup g : groupList) {
			if (g.getShortName().equals(name)) {
				group = g;
			}
		}
		return group;
	}

	/**
	 * Delete the user with the given email
	 * 
	 * @param group
	 * @throws IOException
	 */
	public static void deleteGroup(UsersGroup group) throws IOException {
		groupList.remove(group);
		// delete its folder from server data
		FileUtils.deleteDirectory(getGroupFolder(group));
		// delete the bindings
		ProjectGroupBindingsManager.deletePGBindingsOfGroup(group);
	}
	
	
	/**
	 * Updates the shortName of the given group and returns it updated
	 * 
	 * @param group
	 * @param shortName
	 * @return
	 * @throws IOException
	 */
	public static UsersGroup updateShortName(UsersGroup group, String shortName) throws UsersGroupException {
		group.setShortName(shortName);
		createOrUpdateGroupDetailsFolder(group);
		return group;
	}
	
	/**
	 * Updates the fullName of the given group and returns it updated
	 * 
	 * @param group
	 * @param fullName
	 * @return
	 * @throws IOException
	 */
	public static UsersGroup updateFullName(UsersGroup group, String fullName) throws UsersGroupException {
		group.setFullName(fullName);
		createOrUpdateGroupDetailsFolder(group);
		return group;
	}

	/**
	 * Updates the description of the given group and returns it updated
	 * 
	 * @param group
	 * @param description
	 * @return
	 * @throws IOException
	 */
	public static UsersGroup updateDescription(UsersGroup group, String description) throws UsersGroupException {
		group.setDescription(description);
		createOrUpdateGroupDetailsFolder(group);
		return group;
	}
	
	/**
	 * Updates the webPage of the given group and returns it updated
	 *
	 * @param group
	 * @param webPage
	 * @return
	 * @throws IOException
	 */
	public static UsersGroup updateWebPage(UsersGroup group, String webPage) throws UsersGroupException {
		group.setWebPage(webPage);
		createOrUpdateGroupDetailsFolder(group);
		return group;
	}
	
	/**
	 * Updates the logoUrl of the given group and returns it updated
	 * 
	 * @param group
	 * @param logoUrl
	 * @return
	 * @throws IOException
	 */
	public static UsersGroup updateLogoUrl(UsersGroup group, String logoUrl) throws UsersGroupException {
		group.setLogoUrl(logoUrl);
		createOrUpdateGroupDetailsFolder(group);
		return group;
	}
	
	/**
	 * Creates a folder for the given user and serializes the details about the user in a file. If the folder
	 * is already created, simply update the info in the user details file.
	 * 
	 * @param group
	 * @throws IOException
	 */
	private static void createOrUpdateGroupDetailsFolder(UsersGroup group) throws UsersGroupException {
		try {
			// creates a temporary not persistent repository
			UsersGroupsRepoHelper tempGroupRepoHelper = new UsersGroupsRepoHelper();
			tempGroupRepoHelper.insertGroup(group);
			tempGroupRepoHelper.saveGroupDetailsFile(getGroupDetailsFile(group));
			tempGroupRepoHelper.shutDownRepository();
		} catch (IOException e) {
			throw new UsersGroupException(e);
		}
	}
	
	/**
	 * Returns the group folder under <STData>/groups/ for the given group
	 * @param group
	 * @return
	 */
	public static File getGroupFolder(UsersGroup group) {
		return new File(Resources.getGroupsDir() + File.separator + UsersGroup.encodeGroupIri(group.getIRI()));
	}
	
	/**
	 * Returns all the user folders under <STData>/users/
	 * @return
	 */
	public static Collection<File> getAllGroupFolders() {
		Collection<File> groupFolders = new ArrayList<>(); 
		File groupsFolder = Resources.getGroupsDir();
		//get all subfolder of "groups" folder (one subfolder for each group)		
		String[] groupDirectories = groupsFolder.list(new FilenameFilter() {
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		for (String groupDirectory : groupDirectories) {
			groupFolders.add(new File(groupsFolder + File.separator + groupDirectory));
		}
		return groupFolders;
	}
	
	/**
	 * Returns the group details file for the given group
	 * @param group
	 * @return
	 */
	private static File getGroupDetailsFile(UsersGroup group) {
		File groupFolder = new File(Resources.getGroupsDir() + File.separator + UsersGroup.encodeGroupIri(group.getIRI()));
		if (!groupFolder.exists()) {
			groupFolder.mkdir();
		}
		return new File(groupFolder + File.separator + GROUP_DETAILS_FILE_NAME);
	}
	
	/**
	 * Returns the details files for all the groups
	 * @return
	 */
	private static Collection<File> getAllGroupDetailsFiles() {
		Collection<File> groupDetailsFolders = new ArrayList<>(); 
		Collection<File> groupsFolders = getAllGroupFolders();
		for (File f : groupsFolders) {
			groupDetailsFolders.add(new File(f + File.separator + GROUP_DETAILS_FILE_NAME));
		}
		return groupDetailsFolders;
	}

}
