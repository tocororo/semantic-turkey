package it.uniroma2.art.semanticturkey.security;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.rbac.HaltedEngineException;
import it.uniroma2.art.semanticturkey.rbac.HarmingGoalException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.rbac.RBACProcessor;
import it.uniroma2.art.semanticturkey.rbac.TheoryNotFoundException;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * http://stackoverflow.com/a/14904130/5805661
 * 
 * @author Tiziano
 */
@Component("auth")
public class STAuthorizationEvaluator {

	@Autowired
	private STServiceContext stServiceContext;

	@Autowired
	AccessControlManager acMgr;
	
	/**
	 * To use like follow:
	 * <code>
	 * @PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	 * </code>
	 * For complete documentation see {@link #isAuthorized(String, String, String)} 
	 * 
	 * @param prologCapability
	 * @param crudv
	 * @return
	 * @throws HarmingGoalException 
	 * @throws HaltedEngineException 
	 * @throws TheoryNotFoundException 
	 * @throws MalformedGoalException 
	 * @throws InvalidTheoryException 
	 */
	public boolean isAuthorized(String prologCapability, String crudv) throws InvalidTheoryException,
		MalformedGoalException, TheoryNotFoundException, HaltedEngineException, HarmingGoalException {
		return this.isAuthorized(prologCapability, "{}", crudv);
	}

	/**
	 * 
	 * To use like follow:
	 * <code>
	 * @PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', '{key1: ''value1'', key2: true}', 'R')")
	 * </code> 
	 * 
	 * @param prologCapability
	 * 		Expressed in this way <code>&lt;area&gt;(&lt;subject&gt;, &lt;scope&gt;)</code>.
	 * 
	 * @param userResponsibility
	 * 		A String representing a JSON map serialization like <code>{key1: "value1", key2: "value2"}</code>
	 * 		TODO: define the available keys
	 * 
	 * @param crudv
	 * 		Following the CRUD paradigma, it could be any of <code>C (create)</code> <code>R (read)</code>
	 *		<code>U (update)</code> <code>D (delete)</code>, plus <code>V (validation)</code>.
	 * 
	 * @return
	 * @throws TheoryNotFoundException 
	 * @throws InvalidTheoryException 
	 * @throws HarmingGoalException 
	 * @throws HaltedEngineException 
	 * @throws MalformedGoalException 
	 */
	public boolean isAuthorized(String prologCapability, String userResponsibility, String crudv)
			throws InvalidTheoryException, TheoryNotFoundException, MalformedGoalException, HaltedEngineException, HarmingGoalException {
		String prologGoal = "auth(" + prologCapability + ", '" + crudv + "').";
		
		//parse userResponsibility
		Map<String, Object> userRespMap;
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
			mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
			userRespMap = mapper.readValue(userResponsibility, new TypeReference<Map<String, Object>>(){});
		} catch (IOException e) {
			throw new IllegalArgumentException(
					"Illegal authorization parameter 'userResponsabililty': " + userResponsibility);
		}
		
		System.out.println("Role base access control:");
		Project<?> targetForRBAC = getTargetForRBAC();

		System.out.println("\ttarget for RBAC = " + targetForRBAC.getName());
		System.out.println("\tprolog goal = " + prologGoal);
		System.out.println("\tuser responsibility map = " + userRespMap);
		
		AccessLevel requestedAccessLevel = computeRequestedAccessLevel(crudv);
		LockLevel requestedLockLevel = LockLevel.NO;
		boolean aclSatisfied = checkACL(requestedAccessLevel, requestedLockLevel);

		System.out.println("\tproject consumer = " + stServiceContext.getProjectConsumer().getName());
		System.out.println("\taccessed project = " + stServiceContext.getProject().getName());
		System.out.println("\trequested access level = " + requestedAccessLevel);
		System.out.println("\taclCheck = " + aclSatisfied);

		if (!aclSatisfied) {
			return false;
		}
		
		boolean authorized = false;
		
		// TODO here should go the logic to determine which capability is required based on the
		// topicSubject, topicScope and accessPrivilege triple
		
		STUser loggedUser = UsersManager.getLoggedUser();
		Collection<String> userRoles = getRoles(loggedUser);
		System.out.println("User roles: " + userRoles);//TODO gestire ruoli utenti e assegnazione capab
		boolean prologGoalSatisfied = false;
		for (String role: userRoles) {
			RBACProcessor rbac = RBACManager.getRBACProcessor(targetForRBAC, role);
			if (rbac.authorizes(prologGoal)) {
				prologGoalSatisfied = true;
				break;
			}
		}
		System.out.println("prolog goal satisfied? " + prologGoalSatisfied);
		
		authorized = true;

		return authorized;
	}

	/**
	 * Computes the requested <em>access level</em> to the <em>consumed</em> project based on the given
	 * <em>accessPrivilege</em>, expressed as a <em>crudv</em>. The requested access is <em>R</em> if there
	 * is no other privilege than <em>R</em>, otherwise it is <em>RW</em>.
	 * 
	 * @param crudv
	 * @return
	 */
	private AccessLevel computeRequestedAccessLevel(String crudv) {
		return crudv.chars().anyMatch(c -> c != 'R') ? AccessLevel.RW : AccessLevel.R;
	}

	/**
	 * Retrieves the capabilities of the user for the current project. Returns an empty collection the project
	 * param is not present in the service request
	 * 
	 * @param user
	 * @return
	 */
	private Collection<String> getRoles(STUser user) {
		String projectName = getTargetForRBAC().getName();
		if (projectName != null) {
			ProjectUserBinding puBinding = ProjectUserBindingsManager.getPUBinding(user, projectName);
			return puBinding.getRolesName();
		} else {
			return Collections.emptyList();
		}
	}

	private Project<?> getTargetForRBAC() {
		Project<?> project = stServiceContext.getProject();
		ProjectConsumer consumer = stServiceContext.getProjectConsumer();

		if (consumer.equals(ProjectConsumer.SYSTEM)) {
			return project;
		} else {
			return (Project<?>) consumer;
		}
	}

	private boolean checkACL(AccessLevel requestedAccessLevel, LockLevel requestedLockLevel) {
		ProjectConsumer consumer = stServiceContext.getProjectConsumer();
		Project<?> project = stServiceContext.getProject();

		if (ProjectConsumer.SYSTEM.equals(consumer)) {
			return true;
		}

		return ProjectManager.checkAccessibility(consumer, project, requestedAccessLevel, requestedLockLevel)
				.isAffirmative();
	}

}
