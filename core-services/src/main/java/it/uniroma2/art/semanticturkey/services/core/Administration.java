package it.uniroma2.art.semanticturkey.services.core;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.email.EmailApplicationContext;
import it.uniroma2.art.semanticturkey.email.EmailService;
import it.uniroma2.art.semanticturkey.email.EmailServiceFactory;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.DataSize;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.rbac.InvalidRoleFileException;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.rbac.RBACProcessor;
import it.uniroma2.art.semanticturkey.rbac.TheoryNotFoundException;
import it.uniroma2.art.semanticturkey.resources.Config;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.settings.core.CoreSystemSettings;
import it.uniroma2.art.semanticturkey.settings.core.PreloadProfilerSettings;
import it.uniroma2.art.semanticturkey.settings.core.PreloadSettings;
import it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager;
import it.uniroma2.art.semanticturkey.user.ProjectBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.user.RoleCreationException;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.json.JSONException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;

@STService
@Controller
public class Administration extends STServiceAdapter {

	/**
	 *
	 * @param email
	 * @throws STPropertyUpdateException
	 * @throws UserException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void setAdministrator(String email) throws STPropertyUpdateException, UserException, STPropertyAccessException {
		STUser user = UsersManager.getUser(email);
		UsersManager.addAdmin(user);
	}

	/**
	 *
	 * @param email
	 * @throws STPropertyUpdateException
	 * @throws UserException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void removeAdministrator(String email) throws STPropertyUpdateException, UserException, STPropertyAccessException {
		STUser user = UsersManager.getUser(email);
		UsersManager.removeAdmin(user);
	}

	/**
	 *
	 * @param email
	 * @throws STPropertyUpdateException
	 * @throws UserException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void setSuperUser(String email) throws STPropertyUpdateException, UserException, STPropertyAccessException {
		STUser user = UsersManager.getUser(email);
		UsersManager.addSuperUser(user);
	}

	/**
	 *
	 * @param email
	 * @throws STPropertyUpdateException
	 * @throws UserException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void removeSuperUser(String email) throws STPropertyUpdateException, UserException, STPropertyAccessException {
		STUser user = UsersManager.getUser(email);
		UsersManager.removeSuperUser(user);
	}

	/**
	 * Gets the data directory path
	 * @return
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAdmin()")
	public String getDataDir() {
		return Config.getDataDir().getPath();
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void setDataDir(String path) throws IOException {
		File oldDir = Resources.getSemTurkeyDataDir();
		Config.setDataDirProp(path);
		Resources.initSemTurkeyDataDir(); //update the data dir (and sub-dir) reference in memory
		File newDir = Resources.getSemTurkeyDataDir();
		Files.move(oldDir, newDir);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void setPreloadProfilerThreshold(@Optional DataSize threshold) throws STPropertyUpdateException, STPropertyAccessException {
		SemanticTurkeyCoreSettingsManager coreSettingsManager;
		try {
			coreSettingsManager = (SemanticTurkeyCoreSettingsManager)exptManager.getSettingsManager(SemanticTurkeyCoreSettingsManager.class.getName());
		} catch (NoSuchSettingsManager e) {
			throw new RuntimeException(e); // this should not happen
		}
		CoreSystemSettings explicitCoreSettings = (CoreSystemSettings) coreSettingsManager.getSettings(null, UsersManager.getLoggedUser(), null, Scope.SYSTEM, true);
		if (threshold != null && threshold.getValue()> 0) { // set threshold
			PreloadSettings preloadSettings = explicitCoreSettings.preload;
			if (preloadSettings == null) {
				preloadSettings = explicitCoreSettings.preload = new PreloadSettings();
			}

			PreloadProfilerSettings preloadProfilerSettings = preloadSettings.profiler;
			if (preloadProfilerSettings == null) {
				preloadProfilerSettings = preloadSettings.profiler = new PreloadProfilerSettings();
			}

			preloadProfilerSettings.threshold = threshold;
		} else { // reset threshold
			PreloadSettings preloadSettings = explicitCoreSettings.preload;
			if (preloadSettings != null) {
				PreloadProfilerSettings preloadProfilerSettings = preloadSettings.profiler;
				if (preloadProfilerSettings != null) {
					preloadProfilerSettings.threshold = null;
				}
			}
		}

		// stores the updated settings
		coreSettingsManager.storeSettings(null, UsersManager.getLoggedUser(), null, Scope.SYSTEM, explicitCoreSettings);
	}
	
	/**
	 * 
	 * @param mailTo
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAdmin()")
	public void testEmailConfig(String mailTo, @Optional EmailApplicationContext appCtx) throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		EmailService emailService = EmailServiceFactory.getService(appCtx);
		emailService.sendMailServiceConfigurationTest(mailTo);
	}
	
	
	//PROJECT-USER BINDING SERVICES
	
	/**
	 * @throws ProjectBindingException
	 * @throws JSONException 
	 * @throws ProjectAccessException 
	 * @throws ProjectInexistentException 
	 * @throws InvalidProjectNameException 
	 * @throws STPropertyAccessException 
	 */
	@STServiceOperation
	public JsonNode getProjectUserBinding(String projectName, String email) throws InvalidProjectNameException,
			ProjectInexistentException, ProjectAccessException, UserException {
		STUser user = UsersManager.getUser(email);
		Project project = ProjectManager.getProjectDescription(projectName);
		ProjectUserBinding puBinding = ProjectUserBindingsManager.getPUBinding(user, project);
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode bindingNode = jsonFactory.objectNode();
		bindingNode.set("userEmail", jsonFactory.textNode(puBinding.getUser().getEmail()));
		bindingNode.set("projectName", jsonFactory.textNode(puBinding.getProject().getName()));
		ArrayNode rolesArrayNode = jsonFactory.arrayNode();
		for (Role role: puBinding.getRoles()) {
			rolesArrayNode.add(role.getName());
		}
		bindingNode.set("roles", rolesArrayNode);
		ArrayNode languagesArrayNode = jsonFactory.arrayNode();
		
		Collection<String> boundLangs = puBinding.getLanguages();
		for (String lang: boundLangs) {
			languagesArrayNode.add(lang);
		}

		bindingNode.set("languages", languagesArrayNode);
		
		if (puBinding.getGroup() != null) {
			bindingNode.set("group", puBinding.getGroup().getAsJsonObject());
			bindingNode.set("groupLimitations", jsonFactory.booleanNode(puBinding.isSubjectToGroupLimitations()));
		} else {
			bindingNode.set("group", null);
		}
		
		return bindingNode;
	}
	
	/**
	 * Adds the given roles to the user.
	 * @throws ProjectBindingException 
	 * @throws ProjectAccessException 
	 * @throws ProjectInexistentException 
	 * @throws InvalidProjectNameException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorizedInProject('rbac(user, role)', 'C', #projectName)")
	public void addRolesToUser(String projectName, String email, String[] roles) throws ProjectBindingException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, UserException {
		STUser user = UsersManager.getUser(email);
		Project project = ProjectManager.getProjectDescription(projectName);
		Collection<Role> roleList = new ArrayList<>();
		for (String r : roles) {
			Role role = RBACManager.getRole(project, r);
			if (role == null) {
				throw ProjectBindingException.noRole(r);
			} else {
				roleList.add(role);
			}
		}
		ProjectUserBindingsManager.addRolesToPUBinding(user, project, roleList);
	}
	
	/**
	 * Removes roles, languages and groups from the user in the given project
	 * @throws ProjectBindingException 
	 * @throws ProjectAccessException 
	 * @throws ProjectInexistentException 
	 * @throws InvalidProjectNameException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorizedInProject('rbac(user, role)', 'D', #projectName)")
	public void removeUserFromProject(String projectName, String email) throws ProjectBindingException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, UserException {
		STUser user = UsersManager.getUser(email);
		Project project = ProjectManager.getProjectDescription(projectName);
		//removes all role from the binding
		ProjectUserBindingsManager.removeAllRoleFromPUBinding(user, project);
		ProjectUserBindingsManager.removeGroupFromPUBinding(user, project);
		ProjectUserBindingsManager.updateLanguagesToPUBinding(user, project, new ArrayList<>());
	}
	
	/**
	 * @throws ProjectBindingException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorizedInProject('rbac(user, role)', 'D', #projectName)")
	public void removeRoleFromUser(String projectName, String email, String role) throws ProjectBindingException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, UserException {
		STUser user = UsersManager.getUser(email);
		Project project = ProjectManager.getProjectDescription(projectName);
		Role aRole = RBACManager.getRole(project, role);
		if (aRole == null) {
			throw ProjectBindingException.noRole(role);
		}
		ProjectUserBindingsManager.removeRoleFromPUBinding(user, project, aRole);
	}
	
	/**
	 * @throws ProjectBindingException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorizedInProject('rbac(user, role)', 'U', #projectName)")
	public void updateLanguagesOfUserInProject(String projectName, String email, Collection<String> languages) throws ProjectBindingException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, UserException {
		STUser user = UsersManager.getUser(email);
		Project project = ProjectManager.getProjectDescription(projectName);
		ProjectUserBindingsManager.updateLanguagesToPUBinding(user, project, languages);
	}
	
	//ROLES AND CAPABILITIES SERVICES
	
	/**
	 * 
	 * @return
	 * @throws RBACException
	 * @throws ProjectAccessException 
	 * @throws ProjectInexistentException 
	 * @throws InvalidProjectNameException 
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('rbac(role)', 'R')")
	public JsonNode listRoles(@Optional String projectName) throws InvalidProjectNameException,
		ProjectInexistentException, ProjectAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode rolesArrayNode = jsonFactory.arrayNode();
		Collection<Role> roles;
		if (projectName != null) {
			Project project = ProjectManager.getProjectDescription(projectName);
			roles = RBACManager.getRoles(project);
		} else {
			roles = RBACManager.getRoles(null);
		}
		for (Role role : roles) {
			ObjectNode roleNode = jsonFactory.objectNode();
			roleNode.set("name", jsonFactory.textNode(role.getName()));
			roleNode.set("level", jsonFactory.textNode(role.getLevel().name()));
			rolesArrayNode.add(roleNode);
		}
		return rolesArrayNode;
	}
	
	/**
	 * 
	 * @param projectName
	 * @param role
	 * @return
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws RBACException
	 */
	@STServiceOperation
	public JsonNode listCapabilities(@Optional String projectName, String role) throws InvalidProjectNameException, 
			ProjectInexistentException, ProjectAccessException, RBACException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode capabilitiesArrayNode = jsonFactory.arrayNode();
		if (projectName != null) {
			Project project = ProjectManager.getProjectDescription(projectName);
			for (String c: RBACManager.getRoleCapabilities(project, role)) {
				capabilitiesArrayNode.add(c);
			}
		} else {
			for (String c: RBACManager.getRoleCapabilities(null, role)) {
				capabilitiesArrayNode.add(c);
			}
		}
		return capabilitiesArrayNode;
	}
	
	/**
	 * 
	 * @param roleName
	 * @return
	 * @throws RoleCreationException
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('rbac(role)', 'C')")
	public void createRole(String roleName) throws RoleCreationException {
		RBACManager.createRole(getProject(), roleName);
	}
	
	/**
	 * 
	 * @param roleName
	 * @return
	 * @throws ProjectBindingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('rbac(role)', 'D')")
	public void deleteRole(String roleName) throws ProjectBindingException {
		Project project = null;
		if (stServiceContext.hasContextParameter("project")) {
			project = getProject();
		}
		Role aRole = RBACManager.getRole(project, roleName);
		if (aRole == null) {
			throw ProjectBindingException.noRole(roleName);
		}
		RBACManager.deleteRole(getProject(), roleName);
		ProjectUserBindingsManager.removeRoleFromPUBindings(getProject(), aRole);
	}
	
	/**
	 * Exports the {@link CustomForm} with the given id
	 * @param oRes
	 * @param roleName
	 * @throws RBACException 
	 * @throws CustomFormException
	 * @throws IOException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('rbac(role)', 'R')")
	public void exportRole(HttpServletResponse oRes, String roleName) throws RBACException, IOException {
		Project project = null;
		if (stServiceContext.hasContextParameter("project")) {
			project = getProject();
		}
		if (RBACManager.getRBACProcessor(project, roleName) == null) {
			throw new RBACException(Administration.class.getName() + ".messages.unable_to_export_role", new Object[] {roleName});
		}
		File roleFile = RBACManager.getRoleFile(project, roleName);
		if (!roleFile.exists()) { //in case the role file was not found at project level)
			roleFile = RBACManager.getRoleFile(null, roleName);
		}
		File tempServerFile = File.createTempFile("roleExport", ".pl");
		try {
			FileUtils.copyFile(roleFile, tempServerFile);
			oRes.setHeader("Content-Disposition", "attachment; filename=" + roleFile.getName());
			oRes.setContentType("text/plain");
			oRes.setContentLength((int) tempServerFile.length());
			try (InputStream is = new FileInputStream(tempServerFile)) {
				IOUtils.copy(is, oRes.getOutputStream());
			}
			oRes.flushBuffer();
		} finally {
			tempServerFile.delete();
		}
	}
	
	/**
	 * Imports a new role in the current project
	 * @param newRoleName name of the new role that will be created
	 * @throws IOException 
	 * @throws RBACException
	 * @throws RoleCreationException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('rbac(role)', 'C')")
	public void importRole(MultipartFile inputFile, String newRoleName) 
			throws IOException, RBACException, RoleCreationException {
		if (RBACManager.getRBACProcessor(getProject(), newRoleName) != null) {
			throw new RBACException(Administration.class.getName() + ".messages.unable_to_import_role", new Object[] {newRoleName, getProject().getName()});
		}
		File tempServerFile = File.createTempFile("roleImport", ".pl");
		try {
			inputFile.transferTo(tempServerFile);
			try {
				RBACProcessor rbac = new RBACProcessor(tempServerFile);
				RBACManager.createRole(getProject(), newRoleName);
				Collection<String> capabilities = rbac.getCapabilitiesAsStringList();
				RBACManager.setCapabilities(getProject(), newRoleName, capabilities);
			} catch (InvalidTheoryException | TheoryNotFoundException | 
					MalformedGoalException | NoSolutionException | NoMoreSolutionException e) {
				throw new InvalidRoleFileException(e);
			}
		} finally {
			tempServerFile.delete();
		}
	}
	
	/**
	 * This service allows to create a role by cloning an existing one. Since a role can be created just at
	 * project level, this service should be called only when a project is open.
	 * @param sourceRoleName
	 * @param targetRoleName
	 * @throws RBACException
	 * @throws RoleCreationException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('rbac(role)', 'CR')")
	public void cloneRole(String sourceRoleName, String targetRoleName) throws RoleCreationException, RBACException {
		Project project = getProject();
		if (stServiceContext.hasContextParameter("project")) {
			project = getProject();
		}
		Role sourceRole = RBACManager.getRole(project, sourceRoleName);
		if (sourceRole == null) {
			throw RoleCreationException.noRole(sourceRoleName);
		}
		//doesn't check the existence of targetRoleName since it is already done by createRole()
		RBACManager.createRole(getProject(), targetRoleName);
		RBACManager.setCapabilities(getProject(), targetRoleName, RBACManager.getRoleCapabilities(project, sourceRoleName));
	}
	
	/**
	 * 
	 * @param role
	 * @param capability
	 * @return
	 * @throws RBACException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('rbac(role, capability)', 'C')")
	public void addCapabilityToRole(String role, String capability) throws RBACException {
		RBACManager.addCapability(getProject(), role, capability);
	}

	/**
	 * 
	 * @param role
	 * @param capability
	 * @return
	 * @throws RBACException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('rbac(role, capability)', 'D')")
	public void removeCapabilityFromRole(String role, String capability) throws RBACException {
		RBACManager.removeCapability(getProject(), role, capability);
	}
	
	/**
	 * 
	 * @param role
	 * @param oldCapability
	 * @param newCapability
	 * @throws RBACException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('rbac(role, capability)', 'U')")
	public void updateCapabilityForRole(String role, String oldCapability, String newCapability) throws RBACException {
		RBACManager.removeCapability(getProject(), role, oldCapability);
		RBACManager.addCapability(getProject(), role, newCapability);
	}
	
	@STServiceOperation
	public void downloadPrivacyStatement(HttpServletResponse oRes) throws IOException {
		File psFile = new File(Resources.getDocsDir(), "privacy_statement.pdf");
		oRes.setHeader("Content-Disposition", "attachment; " + psFile.getName());
		oRes.setContentType("application/pdf");
		oRes.setContentLength((int) psFile.length());
		try (InputStream is = new FileInputStream(psFile)) {
			IOUtils.copy(is, oRes.getOutputStream());
		}
		oRes.flushBuffer();
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void clonePUBinding(IRI sourceUserIri, String sourceProjectName,
			@Optional IRI targetUserIri, String targetProjectName) throws InvalidProjectNameException,
			ProjectInexistentException, ProjectAccessException, ProjectBindingException, UserException {
		Project sourceProject = ProjectManager.getProjectDescription(sourceProjectName);
		Project targetProject = ProjectManager.getProjectDescription(targetProjectName);
		STUser sourceUser = UsersManager.getUser(sourceUserIri);
		STUser targetUser;
		if (targetUserIri != null) {
			targetUser = UsersManager.getUser(targetUserIri);
		} else { //if target user is not provided, the target user is the same source user
			targetUser = sourceUser;
		}
		ProjectUserBindingsManager.clonePUBinding(sourceUser, sourceProject, targetUser, targetProject);
	}
	
}
