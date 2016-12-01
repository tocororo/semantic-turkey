package it.uniroma2.art.semanticturkey.security;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.user.AccessContolUtils;
import it.uniroma2.art.semanticturkey.user.RoleCreationException;
import it.uniroma2.art.semanticturkey.user.RolesRepoHelper;
import it.uniroma2.art.semanticturkey.user.STRole;
import it.uniroma2.art.semanticturkey.user.UserCapabilitiesEnum;
import it.uniroma2.art.semanticturkey.user.UserCreationException;

@Component("rolesMgr")
public class RolesManager {
	
	private Collection<STRole> roleList;
	
	public RolesManager() {
		roleList = new ArrayList<>();
	}
	
	/**
	 * Loads all the roles into the repository
	 * @throws IOException 
	 * @throws RepositoryException 
	 * @throws RDFParseException 
	 * @throws UserCreationException
	 */
	public void loadRoles() throws RDFParseException, RepositoryException, IOException {
		RolesRepoHelper repoHelper = new RolesRepoHelper();
		File rolesFile = AccessContolUtils.getRolesDefinitionFile();
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
	public void createRole(STRole role) throws RoleCreationException, IOException {
		if (searchRole(role.getName()) != null) {
			throw new RoleCreationException("Role with name " + role.getName() + " already exists");
		} else {
			roleList.add(role);
			createOrUpdateRolesDefinitionFile();
		}
	}
	
	/**
	 * Returns a list of all the defined roles
	 * @return
	 */
	public Collection<STRole> listRoles() {
		return roleList;
	}
	
	/**
	 * Searches and returns the role with the given name. Null if no role with that id exists
	 * @param name 
	 * @return
	 */
	public STRole searchRole(String roleName) {
		STRole role = null;
		for (STRole r : roleList) {
			if (r.getName().equals(roleName)) {
				role = r;
			}
		}
		return role;
	}
	
	/**
	 * Adds a capability to the given role and returns this one updated
	 * @param role
	 * @param capability
	 * @return
	 * @throws IOException 
	 */
	public STRole addCapability(STRole role, UserCapabilitiesEnum capability) throws IOException {
		role = searchRole(role.getName());
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
	public STRole removeCapability(STRole role, UserCapabilitiesEnum capability) throws IOException {
		role = searchRole(role.getName());
		role.removeCapability(capability);
		createOrUpdateRolesDefinitionFile();
		return role;
	}
	
	/**
	 * Delete the given role. 
	 * @param user
	 * @throws IOException 
	 */
	public void deleteRole(STRole role) throws IOException {
		roleList.remove(searchRole(role.getName()));
		//save role definition file
		createOrUpdateRolesDefinitionFile();
	}
	
	/**
	 * Creates the users/roles.ttl file and serializes the roles definition.
	 * If the file is already created, simply update the info in it.
	 * @param user
	 * @throws IOException 
	 */
	private void createOrUpdateRolesDefinitionFile() throws IOException {
		RolesRepoHelper repoHelper = new RolesRepoHelper();
		for (STRole r : roleList) {
			repoHelper.insertRole(r);
		}
		repoHelper.saveRoleCapabilityFile(AccessContolUtils.getRolesDefinitionFile());
	}
	
}
