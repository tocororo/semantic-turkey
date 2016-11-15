package it.uniroma2.art.semanticturkey.security;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserPermissionsEnum;

/**
 * http://stackoverflow.com/a/14904130/5805661
 * @author Tiziano
 */
@Component("auth")
public class STAuthorizationEvaluator {

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
		Collection<? extends GrantedAuthority> authorities = loggedUser.getAuthorities();
		Iterator<? extends GrantedAuthority> itAuth = authorities.iterator();
		
		boolean authorized = false;
		
		System.out.println("User: " + loggedUser.getEmail());
		System.out.println("Permissions:");
		while (itAuth.hasNext()) {
			String a = itAuth.next().getAuthority();
			if (a.equals(UserPermissionsEnum.CAPABILITY_ADMIN.name())) {
				authorized = true;
			}
			System.out.println("\t" + a);
		}
		System.out.println("Role base access control:"
				+ "\n\tSubject: " + topicSubject
				+ "\n\tScope: " + topicScope
				+ "\n\tAccess-privilege: " + accessPrivilege);
		
		return authorized;
	}
	
}
