package it.uniroma2.art.semanticturkey.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.PostConstruct;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.user.AccessContolUtils;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.RoleCreationException;
import it.uniroma2.art.semanticturkey.user.STRole;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserCapabilitiesEnum;
import it.uniroma2.art.semanticturkey.user.UserCreationException;
import it.uniroma2.art.semanticturkey.user.UserRolesEnum;
import it.uniroma2.art.semanticturkey.user.UserStatus;

@Component
@DependsOn({"usersMgr", "rolesMgr", "puBindingMgr"})
public class AccessControlManager {
	
	/*
	 * TODO here there are two managers autowired. Both of them work with repositories,
	 * so serialize in an RDF format.
	 * If we want to provide an alternative serialization (e.g. property file), then we can
	 * create UsersManager/RolesManager interfaces (that expose the method to interact with properties)
	 * and transform this autowired components in implementations of the above interfaces.
	 * In this scenario, we will autowire the interfaces and specify which implementation use in an
	 * XML configuration file.
	 */
	
	@Autowired
	private UsersManager usersMgr;
	
	@Autowired
	private RolesManager rolesMgr;
	
	@Autowired
	private ProjectUserBindingManager puBindingMgr;
	
	public AccessControlManager() {}
	
	@PostConstruct
	public void init() throws UserCreationException, RDFParseException, RepositoryException, 
		IOException, ProjectAccessException, RoleCreationException {
		
		//Check if users files structure is already initialized
		if (!AccessContolUtils.getUsersFolder().exists()) {
			//not initialized (probably first ST launch) => initialize it
			initializeUserFileStructure();
		}
		//init users and roles manager so they load users and roles from ST data
		usersMgr.loadUsers();
		rolesMgr.loadRoles();
		
		if (!AccessContolUtils.getPUBindingsFolder().exists()) {
			initializePUBindingFileStructure();
		}
		//init project-user binding manager so it loads bindings from ST data
		puBindingMgr.loadPUBindings();
	}
	
	/**
	 * Given a project-user pair, retrieves the roles that the user has for the project, then retrieves the
	 * capabilities granted by that roles.
	 * @param user
	 * @param projectName
	 * @return
	 */
	public Collection<UserCapabilitiesEnum> getCapabilities(STUser user, String projectName) {
		Collection<UserCapabilitiesEnum> capabilities = new ArrayList<>();
		ProjectUserBinding puBinding = puBindingMgr.getPUBinding(user, projectName);
		Collection<String> rolesName = puBinding.getRolesName();
		for (String r : rolesName) {
			STRole role = rolesMgr.searchRole(r);
			for (UserCapabilitiesEnum p : role.getCapabilities()) {
				capabilities.add(p);
			}
		}
		return capabilities;
	}
	
	//Utility methods to manage files
	
	/**
	 * Initializes a folders structure with a users/ folder containing
	 * - roles.ttl: file that defines two default roles: ADMIN and USER 
	 * - a folder for a default admin user containing its user details file.
	 * @throws UserCreationException
	 * @throws ProjectAccessException 
	 * @throws RoleCreationException 
	 * @throws IOException 
	 */
	private void initializeUserFileStructure() throws UserCreationException, ProjectAccessException, RoleCreationException, IOException {
		AccessContolUtils.getUsersFolder().mkdir();
		
		// create default admin and user roles
		STRole roleAdmin = new STRole(UserRolesEnum.ROLE_ADMIN.name());
		roleAdmin.addCapability(UserCapabilitiesEnum.CAPABILITY_ADMIN);
		roleAdmin.addCapability(UserCapabilitiesEnum.CAPABILITY_1);
		roleAdmin.addCapability(UserCapabilitiesEnum.CAPABILITY_2);
		roleAdmin.addCapability(UserCapabilitiesEnum.CAPABILITY_3);
		rolesMgr.createRole(roleAdmin);
		STRole roleUser = new STRole(UserRolesEnum.ROLE_USER.name());
		roleUser.addCapability(UserCapabilitiesEnum.CAPABILITY_USER);
		roleUser.addCapability(UserCapabilitiesEnum.CAPABILITY_4);
		roleUser.addCapability(UserCapabilitiesEnum.CAPABILITY_5);
		rolesMgr.createRole(roleUser);

		// create and register admin user
		STUser admin = new STUser("admin@vocbench.com", "admin", "Admin", "Admin");
		admin.setStatus(UserStatus.ENABLED);
		usersMgr.registerUser(admin);
	}
	
	/**
	 * Initializes a folders structure with a pu_binding folder containing 
	 * - a folder per project
	 * 		- which in turn contains a folder for user
	 * 			- which in turn contains a property file that describe relations between project and user
	 * @throws ProjectAccessException
	 * @throws IOException 
	 */
	private void initializePUBindingFileStructure() throws ProjectAccessException, IOException {
		// create project-user bindings
		AccessContolUtils.getPUBindingsFolder().mkdir();

		for (AbstractProject abstrProj : ProjectManager.listProjects()) {
			if (abstrProj instanceof Project<?>) {
				String projName = abstrProj.getName();
				for (STUser user : usersMgr.listUsers()) {
					ProjectUserBinding puBinding = new ProjectUserBinding(projName, user.getEmail()); 
					puBinding.addRole(UserRolesEnum.ROLE_USER.name()); //TODO add user role as default???
					puBindingMgr.createPUBinding(puBinding);
				}
			}
		}
	}
	
	
}
