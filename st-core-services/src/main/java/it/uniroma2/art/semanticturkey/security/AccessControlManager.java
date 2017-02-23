package it.uniroma2.art.semanticturkey.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.PostConstruct;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.user.PUBindingCreationException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.RoleCreationException;
import it.uniroma2.art.semanticturkey.user.RolesManager;
import it.uniroma2.art.semanticturkey.user.STRole;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserCapabilitiesEnum;
import it.uniroma2.art.semanticturkey.user.UserCreationException;
import it.uniroma2.art.semanticturkey.user.UsersManager;

@Component
public class AccessControlManager {
	
	@PostConstruct
	public void init() throws UserCreationException, RDFParseException, RepositoryException,
		IOException, ProjectAccessException, RoleCreationException, PUBindingCreationException {
		
		//init users and roles manager so they load users and roles from ST data
		UsersManager.loadUsers();
		RolesManager.loadRoles();
		//init project-user binding manager so it loads bindings from ST data
		//Note: this should be initialized strictly after UsersManager since ProjectUserBingingManager uses it
		ProjectUserBindingsManager.loadPUBindings();
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
		ProjectUserBinding puBinding = ProjectUserBindingsManager.getPUBinding(user, projectName);
		Collection<String> rolesName = puBinding.getRolesName();
		for (String r : rolesName) {
			STRole role = RolesManager.searchRole(r);
			for (UserCapabilitiesEnum p : role.getCapabilities()) {
				capabilities.add(p);
			}
		}
		return capabilities;
	}
	
}
