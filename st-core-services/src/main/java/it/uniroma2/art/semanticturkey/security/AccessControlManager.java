package it.uniroma2.art.semanticturkey.security;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.user.PUBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.RoleCreationException;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UsersManager;

@Component
public class AccessControlManager {
	
	@PostConstruct
	public void init() throws UserException, RDFParseException, RepositoryException,
		IOException, ProjectAccessException, RoleCreationException, PUBindingException, RBACException {
		
		//init users manager so it loads users and roles from ST data
		UsersManager.loadUsers();
		
		//init roles and load the RBACProcessor at system level
		//(processor at project level are loaded once the project is accessed)
		RBACManager.initRoles();
		RBACManager.loadRBACProcessor(null);
		
		//init project-user binding manager so it loads bindings from ST data
		//Note: this should be initialized strictly after UsersManager since ProjectUserBingingManager uses it
		ProjectUserBindingsManager.loadPUBindings();
	}
	
}
