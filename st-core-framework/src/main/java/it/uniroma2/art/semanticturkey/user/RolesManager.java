package it.uniroma2.art.semanticturkey.user;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;

import it.uniroma2.art.semanticturkey.resources.Resources;

public class RolesManager {
	
	private static final String ROLES_DEFINITION_FILE_NAME = "roles.ttl";
	
	private static Collection<STRole> roleList = new ArrayList<>();
	
	/**
	 * Loads all the roles into the repository
	 * Protected since the load should be done just once by AccessControlManager during its initialization
	 * @throws IOException 
	 * @throws RepositoryException 
	 * @throws RDFParseException 
	 * @throws UserCreationException
	 */
	public static void loadRoles() throws RDFParseException, RepositoryException, IOException {
		RolesRepoHelper repoHelper = new RolesRepoHelper();
		File rolesFile = getRolesDefinitionFile();
		repoHelper.loadRolesDefinition(rolesFile);
		roleList = repoHelper.listRoles();
		repoHelper.shutDownRepository();
	}
	
	/**
	 * Creates a role
	 * @param role 
	 * @throws RoleCreationException 
	 * @throws IOException 
	 */
	public static void createRole(STRole role) throws RoleCreationException {
		if (searchRole(role.getName()) != null) {
			throw new RoleCreationException("Role with name " + role.getName() + " already exists");
		} else {
			try {
				roleList.add(role);
				createOrUpdateRolesDefinitionFile();
			} catch (IOException e) {
				throw new RoleCreationException(e);
			}
		}
	}
	
	/**
	 * Returns a list of all the defined roles
	 * @return
	 */
	public static Collection<STRole> listRoles() {
		return roleList;
	}
	
	/**
	 * Searches and returns the role with the given name. Null if no role with that id exists
	 * @param name 
	 * @return
	 */
	public static STRole searchRole(String roleName) {
		STRole role = null;
		for (STRole r : roleList) {
			if (r.getName().equals(roleName)) {
				role = r;
			}
		}
		return role;
	}
	
	/**
	 * Adds a capability to the given role and returns this one updated.
	 * @param role
	 * @param capability
	 * @return
	 * @throws IOException 
	 */
	public static STRole addCapability(STRole role, UserCapabilitiesEnum capability) throws IOException {
		role.addCapability(capability);
		createOrUpdateRolesDefinitionFile();
		return role;
	}
	
	/**
	 * Removes a capability from the given role and returns this one updated
	 * @param role
	 * @param capability
	 * @return
	 * @throws IOException 
	 */
	public static STRole removeCapability(STRole role, UserCapabilitiesEnum capability) throws IOException {
		role.removeCapability(capability);
		createOrUpdateRolesDefinitionFile();
		return role;
	}
	
	/**
	 * Delete the given role. 
	 * @param user
	 * @throws IOException 
	 */
	public static void deleteRole(String roleName) throws IOException {
		roleList.remove(searchRole(roleName));
		//save role definition file
		createOrUpdateRolesDefinitionFile();
	}
	
	/**
	 * Creates the users/roles.ttl file and serializes the roles definition.
	 * If the file is already created, simply update the info in it.
	 * @param user
	 * @throws IOException 
	 */
	private static void createOrUpdateRolesDefinitionFile() throws IOException {
		RolesRepoHelper repoHelper = new RolesRepoHelper();
		for (STRole r : roleList) {
			repoHelper.insertRole(r);
		}
		repoHelper.saveRoleCapabilityFile(getRolesDefinitionFile());
	}
	
	/**
	 * Returns the roles.ttl file under <STData>/users/
	 * @return
	 */
	private static File getRolesDefinitionFile() {
		File usersFolder = Resources.getUsersDir();
		if (!usersFolder.exists()) {
			usersFolder.mkdir();
		}
		return new File(usersFolder	+ File.separator + ROLES_DEFINITION_FILE_NAME);
	}
	
}