package it.uniroma2.art.semanticturkey.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserCapabilitiesEnum;

/**
 * http://stackoverflow.com/a/14904130/5805661
 * @author Tiziano
 */
@Component("auth")
public class STAuthorizationEvaluator {
	
	@Autowired
	private HttpServletRequest request;
	
	@Autowired
	AccessControlManager acMgr;


	/**
	 * 
	 * To use like follow:
	 * @PreAuthorize("@auth.isAuthorized('concept', 'lexicalization', 'update')")
	 * @param topicSubject can be any object in the domain of ST. It could be:
	 * <ul>
	 * 	<li>
	 * 		An <i>xRole</i> of RDF objects
	 * 		(<code>cls, individual, property, ontology, concept, conceptScheme, xLabel, skosCollection</code>)
	 * </li>
	 * 	<li>
	 * 		Some special classes (e.g. <code>triples, project, user, system</code>)
	 * 	</li>
	 * </ul>
	 * 
	 * @param topicScope might refer to specific actions or range of actions that are allowed on
	 * 	specific resource types. It could be:
	 * <ul>
	 * 	<li>
	 * 		partitions from the resource view:
	 * 		<code>types, schemes, broader, superclasses, superproperties, otherproperties etc...</code>
	 * 	</li>
	 * 	<li>
	 * 		<code>lexicalization</code> (any kind of triple or set of triples related to the 
	 * 		lexicalization of an object, in any possible lexicalization model).
	 * 	</li>
	 * 	<li>
	 * 		<code>axiom</code> editing of OWL axioms (or to be included in the possibility
	 * 	</li>
	 * </ul>
	 * 
	 * @param accessPrivilege following the CRUD paradigma, it could be
	 * 	any of <code>create</code> <code>read</code> <code>update</code> <code>delete</code>
	 * 
	 * @return
	 */
	public boolean isAuthorized(String topicSubject, String topicScope, String accessPrivilege) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		STUser loggedUser = (STUser) auth.getPrincipal();
		
		Collection<UserCapabilitiesEnum> capabilities = getCapabilities(loggedUser);

		System.out.println("User: " + loggedUser);
		System.out.println("Capabilities:");
		Iterator<UserCapabilitiesEnum> it = capabilities.iterator();
		while (it.hasNext()) {
			System.out.println("\t" + it.next().name());
		}
		
		System.out.println("Role base access control:"
				+ "\n\tSubject: " + topicSubject
				+ "\n\tScope: " + topicScope
				+ "\n\tAccess-privilege: " + accessPrivilege);
		
		boolean authorized = false;
		//TODO here should go the logic to determine which capability is required based on the
		//topicSubject, topicScope and accessPrivilege triple
		if (capabilities.contains(UserCapabilitiesEnum.CAPABILITY_ADMIN)) {
			authorized = true;
		}
		return authorized;
	}
	
	/**
	 * Retrieves the capabilities of the user for the current project. Returns an empty collection the project param
	 * is not present in the service request
	 * @param user
	 * @return
	 */
	private Collection<UserCapabilitiesEnum> getCapabilities(STUser user) {
		String projectParam = request.getParameter("ctx_project");
		if (projectParam != null) {
			return acMgr.getCapabilities(user, projectParam);
		} else {
			return Collections.emptyList();
		}
	}
	
}
