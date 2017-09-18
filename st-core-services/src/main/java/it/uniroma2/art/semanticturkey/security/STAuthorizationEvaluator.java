package it.uniroma2.art.semanticturkey.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.Language;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesUtils;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.rbac.HaltedEngineException;
import it.uniroma2.art.semanticturkey.rbac.HarmingGoalException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.rbac.RBACProcessor;
import it.uniroma2.art.semanticturkey.rbac.TheoryNotFoundException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * http://stackoverflow.com/a/14904130/5805661
 * 
 * @author Tiziano
 */
@Component("auth")
public class STAuthorizationEvaluator {
	
	private static Logger logger = LoggerFactory.getLogger(STAuthorizationEvaluator.class);

	@Autowired
	private STServiceContext stServiceContext;
	
	/**
	 * Allows request only to system administrator
	 * To use like follow:
	 * <code>
	 * @PreAuthorize("@auth.isAdmin())
	 * </code>
	 * @return
	 */
	public boolean isAdmin() {
		return UsersManager.getLoggedUser().isAdmin();
	}

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
	 * @throws STPropertyAccessException 
	 * @throws JSONException 
	 */
	public boolean isAuthorized(String prologCapability, String crudv) throws InvalidTheoryException,
		MalformedGoalException, TheoryNotFoundException, HaltedEngineException, HarmingGoalException, STPropertyAccessException, JSONException {
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
	 * 		currently the only handled key is 'lang'
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
	 * @throws STPropertyAccessException 
	 * @throws JSONException 
	 */
	public boolean isAuthorized(String prologCapability, String userResponsibility, String crudv)
			throws InvalidTheoryException, TheoryNotFoundException, MalformedGoalException, HaltedEngineException, HarmingGoalException, STPropertyAccessException, JSONException {
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
		
		STUser loggedUser = UsersManager.getLoggedUser();
		Collection<Role> userRoles = getRoles(loggedUser);
		Project targetForRBAC = getTargetForRBAC();
		AccessLevel requestedAccessLevel = computeRequestedAccessLevel(crudv);
		LockLevel requestedLockLevel = LockLevel.NO;
		boolean aclSatisfied = checkACL(requestedAccessLevel, requestedLockLevel);
		
		logger.debug("Role base access control:");
		logger.debug("\tprolog goal = " + prologGoal);
		logger.debug("\tuser responsibility map = " + userRespMap);
		logger.debug("\tproject consumer = " + stServiceContext.getProjectConsumer().getName());
		logger.debug("\taccessed project = " + (targetForRBAC != null ? targetForRBAC.getName() : "SYSTEM"));
		logger.debug("\trequested access level = " + requestedAccessLevel);
		logger.debug("\taclCheck = " + aclSatisfied);
		logger.debug("\tlogged User = " + loggedUser.getEmail());
		logger.debug("\t\troles = " + userRoles);

		if (!aclSatisfied) {
			return false;
		}
		
		boolean authorized = false;
		
		authorized = false;
		if (loggedUser.isAdmin()) { //admin is authorized for every operation
			authorized = true;
		} else {
			for (Role role: userRoles) {
//				try {
//					System.out.println("capabilities: " + RBACManager.getRoleCapabilities(targetForRBAC, role.getName()));
//				} catch (RBACException e) {
//					System.out.println(e);
//				}
				RBACProcessor rbac = RBACManager.getRBACProcessor(targetForRBAC, role.getName());
				if (rbac.authorizes(prologGoal)) {
					authorized = true;
					break;
				} else {
					logger.debug("Goal not authorized: " + prologGoal);
				}
			}
			//check on the user responsibilities
			//at the moment the only check is to the lang capability
			String lang = (String) userRespMap.get("lang");
			if (lang != null) {
				Collection<String> assignedLangs = ProjectUserBindingsManager.getPUBinding(
						loggedUser, targetForRBAC).getLanguages();
				
				Collection<Language> projectLangs = STPropertiesUtils.parseLanguages(
						STPropertiesManager.getProjectSetting(STPropertiesManager.SETTING_PROJ_LANGUAGES, targetForRBAC));
				Collection<String> projLangTags = new ArrayList<>();
				for (Language l : projectLangs) {
					projLangTags.add(l.getTag());
				}
				
				//if the lang capability is not in project languages or is not assigned to the user, do not authorize
				if (!assignedLangs.contains(lang) || !projLangTags.contains(lang)) {
					logger.debug("language proficiency '" + lang + "' not authorized");
					authorized = false;
				}
			}
		}
//		System.out.println("prolog goal satisfied? " + authorized);
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
	private Collection<Role> getRoles(STUser user) {
		Collection<Role> roles = new ArrayList<>();
		AbstractProject project = getTargetForRBAC();
		if (project != null) {
			ProjectUserBinding puBinding = ProjectUserBindingsManager.getPUBinding(user, project);
			//puBinding could be null if user tries to access a 2nd project from the project in which is logged
			if (puBinding != null) {
				roles.addAll(puBinding.getRoles());
			}
		}
		return roles;
	}

	private Project getTargetForRBAC() {
		if (stServiceContext.hasContextParameter("project")) {
			Project project = stServiceContext.getProject();
			ProjectConsumer consumer = stServiceContext.getProjectConsumer();
			if (consumer.equals(ProjectConsumer.SYSTEM)) {
				return project;
			} else {
				return (Project) consumer;
			}
		} else {
			return null;
		}
	}

	private boolean checkACL(AccessLevel requestedAccessLevel, LockLevel requestedLockLevel) {
		ProjectConsumer consumer = stServiceContext.getProjectConsumer();
		if (ProjectConsumer.SYSTEM.equals(consumer)) {
			return true;
		}
		Project project = stServiceContext.getProject();
		if (consumer.equals(project)) {
			return true;
		} else {
			return ProjectManager.checkAccessibility(consumer, project, requestedAccessLevel, requestedLockLevel)
					.isAffirmative();
		}
	}
	
	/**
	 * To use at support of isAuthorized like
	 * @PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#individual)+ ')', 'R')")
	 * where individual is a method parameter name 
	 * @param resource
	 * @return
	 */
	public RDFResourceRole typeof(Resource resource) {
		Repository repo = stServiceContext.getProject().getRepository();
		RepositoryConnection repoConn = RDF4JRepositoryUtils.getConnection(repo);
		try {
			QueryBuilder qb;
			StringBuilder sb = new StringBuilder();
			sb.append(
					// @formatter:off
					" SELECT ?resource WHERE {							\n" +
					" BIND (?temp as ?resource)							\n" +
					" } 												\n" +
					" GROUP BY ?resource								\n"
					// @formatter:on
			);
			qb = new QueryBuilder(stServiceContext, sb.toString());
			qb.setBinding("temp", resource);
			qb.processRole();
			Collection<AnnotatedValue<Resource>> res = qb.runQuery();
			Value role = res.iterator().next().getAttributes().get("role");
			RDFResourceRole roleEnum = RDFResourceRole.valueOf(role.stringValue());
			return roleEnum;
		} finally {
			RDF4JRepositoryUtils.releaseConnection(repoConn, repo);
		}
	}
	
	/**
	 * To use at support of isAuthorized like 
	 * @PreAuthorize("@auth.isAuthorized('rdf(concept)', '{lang: ''' +@auth.langof(#label)+ '''}', 'C')")
	 * the three ''' are required because '' represents the double quotes surrounding the map value, the third '
	 * closes (or open) the string to evaluate in isAuthorized()
	 * where literal is a method parameter name of type Literal
	 * @param literal
	 * @return
	 */
	public String langof(Literal literal) {
		return literal.getLanguage().orElse(null);
	}
	/**
	 * Same of {@link #langof(Literal)} to use with xLabel
	 * @param xLabel
	 * @return
	 */
	public String langof(Resource xLabel) {
		String lang = null;
		Repository repo = stServiceContext.getProject().getRepository();
		RepositoryConnection repoConn = RDF4JRepositoryUtils.getConnection(repo);
		try {
			String query = 
					"SELECT ?lang WHERE {															\n" +
					" 	?xlabel " + NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM) + " ?lf .	\n" +
					"	BIND(lang(?lf) as ?lang)													\n" +
					"} 																				\n";
			TupleQuery tq = repoConn.prepareTupleQuery(query);
			tq.setBinding("xlabel", xLabel);
			TupleQueryResult result = tq.evaluate();
			if (result.hasNext()) {
				lang = result.next().getValue("lang").stringValue();
			}
			return lang;
		} finally {
			RDF4JRepositoryUtils.releaseConnection(repoConn, repo);
		}
	}
	
	/**
	 * Check if the user that is performing the request has the given email.
	 * Useful to the Preauthorize annotation in the service to edit user that are
	 * permitted to those user who have the required capability or to the same user that is
	 * the subject of the changes.
	 * @param email
	 * @return
	 */
	public boolean isLoggedUser(String email) {
		return (UsersManager.getLoggedUser().getEmail().equals(email));
	}
	
}
