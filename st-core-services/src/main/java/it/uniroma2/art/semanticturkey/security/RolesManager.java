package it.uniroma2.art.semanticturkey.security;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.user.AccessContolUtils;
import it.uniroma2.art.semanticturkey.user.RoleCreationException;
import it.uniroma2.art.semanticturkey.user.RolesRepoHelper;
import it.uniroma2.art.semanticturkey.user.STRole;
import it.uniroma2.art.semanticturkey.user.UserCapabilitiesEnum;
import it.uniroma2.art.semanticturkey.user.UserCreationException;

@Component("rolesMgr")
@DependsOn("acRepoHolder")
public class RolesManager {
	
	private RolesRepoHelper rolesRepoHelper;
	
	@Autowired
	public RolesManager(AccessControlRepositoryHolder acRepoHolder) {
		rolesRepoHelper = new RolesRepoHelper(acRepoHolder.getRepository());
	}
	
	/**
	 * Loads all the roles into the repository
	 * @throws IOException 
	 * @throws RepositoryException 
	 * @throws RDFParseException 
	 * @throws UserCreationException
	 */
	public void loadRoles() throws RDFParseException, RepositoryException, IOException {
		File rolesFile = AccessContolUtils.getRolesDefinitionFile();
		rolesRepoHelper.loadRolesDefinition(rolesFile);
	}
	
	/**
	 * Creates a role
	 * @param role 
	 * @throws RoleCreationException 
	 * @throws IOException 
	 */
	public void createRole(STRole role) throws RoleCreationException, IOException {
		if (rolesRepoHelper.searchRole(role.getName()) != null) {
			throw new RoleCreationException("Role with name " + role.getName() + " already exists");
		} else {
			rolesRepoHelper.insertRole(role);
			createOrUpdateRolesDefinitionFile();
		}
	}
	
	/**
	 * Returns a list of all the defined roles
	 * @return
	 */
	public Collection<STRole> listRoles() {
		return rolesRepoHelper.listRoles();
	}
	
	/**
	 * Searches and returns the role with the given name. Null if no role with that id exists
	 * @param name 
	 * @return
	 */
	public STRole searchRole(String roleName) {
		return this.rolesRepoHelper.searchRole(roleName);
	}
	
	/**
	 * Adds a capability to the given role and returns this one updated
	 * @param role
	 * @param capability
	 * @return
	 * @throws IOException 
	 */
	public STRole addCapability(STRole role, UserCapabilitiesEnum capability) throws IOException {
		rolesRepoHelper.addCapability(role, capability);
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
		rolesRepoHelper.removeCapability(role, capability);
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
		//remove role from repository
		rolesRepoHelper.deleteRole(role);
		//save role definition file
		createOrUpdateRolesDefinitionFile();
	}
	
	public static Collection<UserCapabilitiesEnum> getCapabilitiesForRoles(Collection<STRole> roles) {
		Collection<UserCapabilitiesEnum> capabilities = new ArrayList<>();
		for (STRole r : roles) {
			for (UserCapabilitiesEnum p : r.getCapabilities()) {
				if (!capabilities.contains(p)) {
					capabilities.add(p);
				}
			}
		}
		return capabilities;
	}
	
	/**
	 * Creates the users/roles.ttl file and serializes the roles definition.
	 * If the file is already created, simply update the info in it.
	 * @param user
	 * @throws IOException 
	 */
	private void createOrUpdateRolesDefinitionFile() throws IOException {
		rolesRepoHelper.saveRoleCapabilityFile(AccessContolUtils.getRolesDefinitionFile());
	}
	
}
