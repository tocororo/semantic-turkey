package it.uniroma2.art.semanticturkey.security;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniroma2.art.semanticturkey.customform.CustomFormValue;
import it.uniroma2.art.semanticturkey.customform.SpecialValue;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.Language;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.rbac.HaltedEngineException;
import it.uniroma2.art.semanticturkey.rbac.HarmingGoalException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.rbac.RBACProcessor;
import it.uniroma2.art.semanticturkey.rbac.TheoryNotFoundException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager;
import it.uniroma2.art.semanticturkey.showvoc.ShowVocConstants;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * http://stackoverflow.com/a/14904130/5805661
 *
 * @author Tiziano
 */
public class STAuthorizationEvaluator {

	private static final Logger logger = LoggerFactory.getLogger(STAuthorizationEvaluator.class);

	@Autowired
	private STServiceContext stServiceContext;

	@Autowired
	private SemanticTurkeyCoreSettingsManager stCoreSettingsManager;

	/**
	 * Allows request only to system administrator To use like follow: <code>
	 * &#64;PreAuthorize("@auth.isAdmin())
	 * </code>
	 *
	 * @return
	 */
	public boolean isAdmin() {
		return UsersManager.getLoggedUser().isAdmin();
	}

	/**
	 * Returns true if the logged user is a SuperUser.
	 * Argument strict determines if the user needs to be only SuperUser (strict=true), or "at least" SuperUser,
	 * namely even Admin is ok (strict=false).
	 * @param strict
	 * @return
	 */
	public boolean isSuperUser(boolean strict) {
		return UsersManager.getLoggedUser().isSuperUser(strict);
	}

	/**
	 * Allows request only when the contextual project is public (i.e. {@link ShowVocConstants#SHOWVOC_VISITOR_EMAIL} has role
	 * {@link ShowVocConstants.ShowVocRole#PUBLIC}. To use like the following: <code>
	 * &#64;PreAuthorize("@auth.isCtxProjectPublic()")
	 * </code>
	 *
	 * @return
	 * @throws UserException
	 */
	public boolean isCtxProjectPublic() throws UserException {
		return ProjectUserBindingsManager
				.getPUBinding(UsersManager.getUser(ShowVocConstants.SHOWVOC_VISITOR_EMAIL), stServiceContext.getProject()).getRoles()
				.contains(ShowVocConstants.ShowVocRole.PUBLIC);
	}

	/**
	 * Allows request only when the given project is public (i.e.
	 * {@link ShowVocConstants#SHOWVOC_VISITOR_EMAIL} has role {@link ShowVocConstants.ShowVocRole#PUBLIC}. To use like
	 * the following: <code>
	 * &#64;PreAuthorize("@auth.isProjectPublic(#projectNameParam"))
	 * </code>
	 *
	 * @return
	 * @throws UserException
	 * @throws ProjectAccessException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 */
	public boolean isProjectPublic(String id) throws UserException, InvalidProjectNameException,
			ProjectInexistentException, ProjectAccessException {
		return ProjectUserBindingsManager
				.getPUBinding(UsersManager.getUser(ShowVocConstants.SHOWVOC_VISITOR_EMAIL),
						ProjectManager.getProjectDescription(id))
				.getRoles().contains(ShowVocConstants.ShowVocRole.PUBLIC);
	}

	/**
	 * This is useful for evaluating authorization for a project different from the one indicated in the context
	 * @param prologCapability
	 * @param crudv
	 * @param projectName
	 * @return
	 * @throws MalformedGoalException
	 * @throws HaltedEngineException
	 * @throws HarmingGoalException
	 * @throws STPropertyAccessException
	 * @throws JSONException
	 * @throws ProjectAccessException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 */
	public boolean isAuthorizedInProject(String prologCapability, String crudv, String projectName)
			throws MalformedGoalException, HaltedEngineException, HarmingGoalException,
			STPropertyAccessException, JSONException, ProjectAccessException, ProjectInexistentException,
			InvalidProjectNameException {
		return this.isAuthorized(prologCapability, "{}", crudv, projectName);
	}

	public boolean isSettingsActionAuthorized(Scope scope, String crud) throws ProjectAccessException,
			ProjectInexistentException, InvalidProjectNameException, HarmingGoalException, JSONException,
			HaltedEngineException, MalformedGoalException, STPropertyAccessException {
		if (scope.equals(Scope.SYSTEM)) {
			return isSuperUser(false);
		} else if (scope.equals(Scope.PROJECT)) {
			if (crud.equals("R")) { //only read
				return true; //PROJECT settings can be read by any user (e.g. project languages)
			} else {
				return isAuthorized("pm(project, _)", crud);
			}
		} else if (scope.equals(Scope.PROJECT_GROUP)) {
			if (crud.equals("R")) { //only read
				return true; //PROJECT_GROUP settings can be read by any user (e.g. group limitations)
			} else {
				return isAuthorized("pm(project, group)", crud);
			}
		} else { //scope: USER, PROJECT_USER
			return true; //it is enough that the user is logged
		}
	}

	/**
	 * Useful for authorizing file operation (read/create file)
	 * @param dir
	 * @param crud
	 * @return
	 * @throws ProjectAccessException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 * @throws HarmingGoalException
	 * @throws JSONException
	 * @throws HaltedEngineException
	 * @throws MalformedGoalException
	 * @throws STPropertyAccessException
	 */
	public boolean isFileActionAuthorized(String dir, String crud) throws ProjectAccessException,
			ProjectInexistentException,InvalidProjectNameException, HarmingGoalException, JSONException,
			HaltedEngineException, MalformedGoalException, STPropertyAccessException {
		Reference ref = parseReference(dir);
		Project project = ref.getProject().orElse(null);
		STUser user = ref.getUser().orElse(null);
		Scope scope;
		if (project == null && user == null) {
			scope = Scope.SYSTEM;
		} else if (project != null) {
			if (user != null) {
				scope = Scope.PROJECT_USER;
			} else {
				scope = Scope.PROJECT;
			}
		} else {
			scope = Scope.USER;
		}
		return isSettingsActionAuthorized(scope, crud);
	}

	/**
	 * To use like follow: <code>
	 * &#64;PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	 * </code> For complete documentation see {@link #isAuthorized(String, String, String)}
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
	public boolean isAuthorized(String prologCapability, String crudv) throws MalformedGoalException,
            HaltedEngineException, HarmingGoalException, STPropertyAccessException, JSONException,
            ProjectAccessException, ProjectInexistentException, InvalidProjectNameException {
		return this.isAuthorized(prologCapability, "{}", crudv);
	}

	public boolean isAuthorized(String prologCapability, String userResponsibility, String crudv)
            throws HarmingGoalException, JSONException, HaltedEngineException, MalformedGoalException,
            STPropertyAccessException, ProjectAccessException, ProjectInexistentException,
            InvalidProjectNameException {
		return this.isAuthorized(prologCapability, userResponsibility, crudv, null);
	}

	/**
	 *
	 * To use like follow: <code>
	 * &#64;PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', '{key1: ''value1'', key2: true}', 'R')")
	 * </code>
	 *
	 * @param prologCapability
	 *            Expressed in this way <code>&lt;area&gt;(&lt;subject&gt;, &lt;scope&gt;)</code>.
	 *
	 * @param userResponsibility
	 *            A String representing a JSON map serialization like
	 *            <code>{key1: "value1", key2: "value2"}</code> currently the only handled key is 'lang'
	 *
	 * @param crudv
	 *            Following the CRUD paradigma, it could be any of <code>C (create)</code>
	 *            <code>R (read)</code> <code>U (update)</code> <code>D (delete)</code>, plus
	 *            <code>V (validation)</code>.
	 *
	 * @param projectName
	 * 			  Name of the project where the capability will be evaluated. If null is provided, it will
	 * 			  be considered the context project
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
	public boolean isAuthorized(String prologCapability, String userResponsibility, String crudv, String projectName)
			throws MalformedGoalException, HaltedEngineException, HarmingGoalException,
			STPropertyAccessException, JSONException, ProjectAccessException, ProjectInexistentException,
            InvalidProjectNameException {
		String prologGoal = "auth(" + prologCapability + ", '" + crudv + "').";

		// parse userResponsibility
		Map<String, Object> userRespMap;
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
			mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
			userRespMap = mapper.readValue(userResponsibility, new TypeReference<Map<String, Object>>() {
			});
		} catch (IOException e) {
			throw new IllegalArgumentException(
					"Illegal authorization parameter 'userResponsabililty': " + userResponsibility);
		}

		STUser loggedUser = UsersManager.getLoggedUser();
		Collection<Role> userRoles = getRoles(loggedUser, projectName);
		Project targetForRBAC = getTargetForRBAC(projectName);
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

		boolean authorized;

		if (loggedUser.isAdmin()) { // admin is authorized for every operation
			authorized = true;
		} else {
			authorized = evaluatePrologGoal(prologGoal, userRoles, targetForRBAC);
			// check on the user responsibilities
			// at the moment the only check is to the lang capability
			ArrayList<String> langs = new ArrayList<>();

			Object langValue = userRespMap.get("lang");
			if (langValue instanceof String) {
				String langString = (String) langValue;
				if (langString != null && !langString.equals("null")) {
					langs.add(langString);
				}
			} else if (langValue instanceof ArrayList) {
				ArrayList<String> langList = (ArrayList<String>) langValue;
				for (String l : langList) {
					if (!l.equals("null")) {
						langs.add(l);
					}
				}
			}

			if (!langs.isEmpty()) {
				logger.debug("checking lang proficiencies on languages: " + langs);
				Collection<String> assignedLangs = ProjectUserBindingsManager
						.getPUBinding(loggedUser, targetForRBAC).getLanguages();

				Collection<Language> projectLangs = ObjectUtils.defaultIfNull(stCoreSettingsManager.getProjectSettings(targetForRBAC).languages, Collections.emptyList());
				Collection<String> projLangTags = new ArrayList<>();
				for (Language l : projectLangs) {
					projLangTags.add(l.getTag());
				}

				// in order to be authorized, all the languages must be among the project languages
				// and the languages assigned to the user
				for (String l : langs) {
					boolean isAssigned = assignedLangs.stream().anyMatch(l::equalsIgnoreCase);
					boolean isInProject = projLangTags.stream().anyMatch(l::equalsIgnoreCase);
					if (!isAssigned || !isInProject) {
						logger.debug("language proficiency '" + l + "' not authorized");
						authorized = false;
					}
				}

			}

			if (!AccessLevel.R.accepts(requestedAccessLevel)) { // if this is not a read-only operation
				Term termCapability = Term.createTerm(prologCapability);
				if (termCapability instanceof Struct) {
					if (Objects.equals(((Struct) termCapability).getName(), "rdf")) { // if the requested
																						// capability is about
																						// rdf
						if (!Objects.equals(stServiceContext.getWGraph().stringValue(),
								stServiceContext.getProject().getBaseURI())) {
							authorized &= evaluatePrologGoal("auth(rdf(graph), 'U').", userRoles,
									targetForRBAC);
						}

					}
				}
			}
		}
		// System.out.println("prolog goal satisfied? " + authorized);
		return authorized;
	}

	protected boolean evaluatePrologGoal(String prologGoal, Collection<Role> userRoles, Project targetForRBAC)
			throws MalformedGoalException, HaltedEngineException, HarmingGoalException {
		for (Role role : userRoles) {
			// try {
			// System.out.println("capabilities: " + RBACManager.getRoleCapabilities(targetForRBAC,
			// role.getName()));
			// } catch (RBACException e) {
			// System.out.println(e);
			// }
			RBACProcessor rbac = RBACManager.getRBACProcessor(targetForRBAC, role.getName());
			if (rbac.authorizes(prologGoal)) {
				return true;
			} else {
				logger.debug("Goal not authorized: " + prologGoal);
			}
		}
		return false;
	}

	/**
	 * Computes the requested <em>access level</em> to the <em>consumed</em> project based on the given
	 * <em>accessPrivilege</em>, expressed as a <em>crudv</em>. The requested access is <em>R</em> if there is
	 * no other privilege than <em>R</em>, otherwise it is <em>RW</em>.
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
	private Collection<Role> getRoles(STUser user, String projectName) throws ProjectAccessException, ProjectInexistentException, InvalidProjectNameException {
		Collection<Role> roles = new ArrayList<>();
		AbstractProject project = getTargetForRBAC(projectName);
		if (project != null) {
			ProjectUserBinding puBinding = ProjectUserBindingsManager.getPUBinding(user, project);
			// puBinding could be null if user tries to access a 2nd project from the project in which is
			// logged
			if (puBinding != null) {
				roles.addAll(puBinding.getRoles());
			}
		}
		return roles;
	}

	/**
	 * Returns the Project subject of the authorization checks.
	 * If a projectName is provided returns the related Project, otherwise returns the ctx_project (if any)
	 * @param projectName
	 * @return
	 * @throws ProjectAccessException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 */
	private Project getTargetForRBAC(String projectName) throws ProjectAccessException, ProjectInexistentException, InvalidProjectNameException {
		if (projectName != null) {
			return ProjectManager.getProject(projectName, true);
		} else {
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
			return ProjectManager
					.checkAccessibility(consumer, project, requestedAccessLevel, requestedLockLevel)
					.isAffirmative();
		}
	}

	/**
	 * To use at support of isAuthorized like @PreAuthorize("@auth.isAuthorized('rdf('
	 * +@auth.typeof(#individual)+ ')', 'R')") where individual is a method parameter name
	 *
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
			return RDFResourceRole.valueOf(role.stringValue());
		} finally {
			RDF4JRepositoryUtils.releaseConnection(repoConn, repo);
		}
	}

	/**
	 * To use at support of isAuthorized like @PreAuthorize("@auth.isAuthorized('rdf(concept)', '{lang: '''
	 * +@auth.langof(#label)+ '''}', 'C')") the three ''' are required because '' represents the double quotes
	 * surrounding the map value, the third ' closes (or open) the string to evaluate in isAuthorized() where
	 * literal is a method parameter name of type Literal
	 *
	 * @param literal
	 * @return
	 */
	public String langof(Literal literal) {
		return literal.getLanguage().orElse(null);
	}

	/**
	 * Same of {@link #langof(Literal)} to use with xLabel
	 *
	 * @param xLabel
	 * @return
	 */
	public String langof(Resource xLabel) {
		String lang = null;
		Repository repo = stServiceContext.getProject().getRepository();
		RepositoryConnection repoConn = RDF4JRepositoryUtils.getConnection(repo);
		try {
			String query = "SELECT ?lang WHERE {															\n"
					+ " 	?xlabel " + NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM) + " ?lf .	\n"
					+ "	BIND(lang(?lf) as ?lang)													\n"
					+ "} 																				\n";
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

	public String langof(SpecialValue value) {
		String lang = null;
		if (value.isCustomFormValue()) {
			CustomFormValue cfValue = value.getCustomFormValue();
			Map<String, Object> userRespMap = cfValue.getUserPromptMap();
			for (String key : userRespMap.keySet()) {
				if (key.equals("lang") || key.startsWith("lang_")) {
					lang = (String) userRespMap.get(key);
				}
			}
		} else { // value.isRdf4jValue()
			Value rdf4jValue = value.getRdf4jValue();
			if (rdf4jValue instanceof Literal) {
				lang = langof((Literal) rdf4jValue);
			}
		}
		return lang;
	}

	/**
	 * Check if the user that is performing the request has the given email. Useful to the Preauthorize
	 * annotation in those services that allow to edit user related staff. This check so is exploited in order
	 * to check that the user provided as parameter (which is the subject of the changes), is the logged one
	 *
	 * @param email
	 * @return
	 */
	public boolean isLoggedUser(String email) {
		return (UsersManager.getLoggedUser().getEmail().equalsIgnoreCase(email));
	}

	/**
	 * Copied from {@link it.uniroma2.art.semanticturkey.services.STServiceAdapter#parseReference(String)};
	 * @param relativeReference
	 * @return
	 */
	private Reference parseReference(String relativeReference) {
		int colonPos = relativeReference.indexOf(":");

		if (colonPos == -1)
			throw new IllegalArgumentException("Invalid reference: " + relativeReference);

		Scope scope = Scope.deserializeScope(relativeReference.substring(0, colonPos));
		String identifier = relativeReference.substring(colonPos + 1);

		switch (scope) {
			case SYSTEM:
				return new Reference(null, null, identifier);
			case PROJECT:
				return new Reference(stServiceContext.getProject(), null, identifier);
			case USER:
				return new Reference(null, UsersManager.getLoggedUser(), identifier);
			case PROJECT_USER:
				return new Reference(stServiceContext.getProject(), UsersManager.getLoggedUser(), identifier);
			default:
				throw new IllegalArgumentException("Unsupported scope: " + scope);
		}
	}

}
