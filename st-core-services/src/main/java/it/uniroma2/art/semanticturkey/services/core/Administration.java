package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Term;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.Language;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesUtils;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.rbac.RBACProcessor;
import it.uniroma2.art.semanticturkey.rbac.TheoryNotFoundException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.ProjectBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.user.RoleCreationException;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.EmailSender;

@STService
@Controller
public class Administration extends STServiceAdapter {
	
	/**
	 * Gets the administration config: a map with key value of configuration parameters
	 * @return
	 * @throws JSONException
	 * @throws STPropertyAccessException 
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAdmin()")
	public JsonNode getAdministrationConfig() throws JSONException, STPropertyAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode configNode = jsonFactory.objectNode();
		configNode.set("adminAddress", jsonFactory.textNode(
				STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_ADMIN_ADDRESS)));
		configNode.set("mailFromAddress", jsonFactory.textNode(
				STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_FROM_ADDRESS)));
		configNode.set("mailFromPassword", jsonFactory.textNode(
				STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_FROM_PASSWORD)));
		configNode.set("mailFromAlias", jsonFactory.textNode(
				STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_FROM_ALIAS)));
		configNode.set("mailSmtpAuth", jsonFactory.textNode(
				STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_AUTH)));
		configNode.set("mailSmtpHost", jsonFactory.textNode(
				STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_HOST)));
		configNode.set("mailSmtpPort", jsonFactory.textNode(
				STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_PORT)));
		configNode.set("mailSmtpSslEnable", jsonFactory.textNode(
				STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_SSL_ENABLE)));
		configNode.set("mailSmtpStarttlsEnable", jsonFactory.textNode(
				STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_STARTTLS_ENABLE)));
		return configNode;
	}
	
	/**
	 * 
	 * @param emailAdminAddress
	 * @throws STPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void updateAdministrator(String adminEmailAddress) throws STPropertyUpdateException {
		STPropertiesManager.setSystemSetting(STPropertiesManager.SETTING_ADMIN_ADDRESS, adminEmailAddress);
	}
	
	/**
	 * 
	 * @param mailSmtpHost
	 * @param mailSmtpPort
	 * @param mailSmtpAuth
	 * @param mailSmtpSsl
	 * @param mailSmtpTls
	 * @param mailFromAddress
	 * @param mailFromAlias
	 * @param mailFromPassword
	 * @throws STPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void updateEmailConfig(String mailSmtpHost, String mailSmtpPort, boolean mailSmtpAuth,
			boolean mailSmtpSsl, boolean mailSmtpTls, String mailFromAddress, String mailFromAlias, 
			@Optional String mailFromPassword) throws STPropertyUpdateException {
		STPropertiesManager.setSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_HOST, mailSmtpHost);
		STPropertiesManager.setSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_PORT, mailSmtpPort);
		STPropertiesManager.setSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_AUTH, mailSmtpAuth+"");
		STPropertiesManager.setSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_SSL_ENABLE, mailSmtpSsl+"");
		STPropertiesManager.setSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_STARTTLS_ENABLE, mailSmtpTls+"");
		STPropertiesManager.setSystemSetting(STPropertiesManager.SETTING_MAIL_FROM_ADDRESS, mailFromAddress);
		STPropertiesManager.setSystemSetting(STPropertiesManager.SETTING_MAIL_FROM_ALIAS, mailFromAlias);
		STPropertiesManager.setSystemSetting(STPropertiesManager.SETTING_MAIL_FROM_PASSWORD, mailFromPassword);
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
	public void testEmailConfig(String mailTo) throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		EmailSender.sendTestMailConfig(mailTo);
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
	public JsonNode getProjectUserBinding(String projectName, String email) throws ProjectBindingException, JSONException, 
		InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, STPropertyAccessException {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new ProjectBindingException("No user found with email " + email);
		}
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
		/* special case:
		 * the administrator as default has no languages, but he should have permission to use all the langs,
		 * so if in its project-user binding there is no language assigned, assign all the language of the project
		 */
		if (boundLangs.isEmpty() && user.isAdmin()) {
			Collection<Language> projectLangs = STPropertiesUtils.parseLanguages(
					STPropertiesManager.getProjectSetting(STPropertiesManager.SETTING_PROJ_LANGUAGES, project));
			for (Language l : projectLangs) {
				languagesArrayNode.add(l.getTag());
			}
		} else {
			for (String lang: boundLangs) {
				languagesArrayNode.add(lang);
			}
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
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('rbac(user, role)', 'C')")
	public void addRolesToUser(String projectName, String email, String[] roles) throws ProjectBindingException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new ProjectBindingException("No user found with email " + email);
		}
		Project project = ProjectManager.getProjectDescription(projectName);
		if (project == null) {
			throw new ProjectBindingException("Project " + projectName + " doesn't exist");
		}
		Collection<Role> roleList = new ArrayList<>();
		for (String r : roles) {
			Role role = RBACManager.getRole(project, r);
			if (role == null) {
				throw new ProjectBindingException("No role '" + r + "' found");
			} else {
				roleList.add(role);
			}
		}
		ProjectUserBindingsManager.addRolesToPUBinding(user, project, roleList);
	}
	
	/**
	 * Removes all roles from the user in the given project
	 * @throws ProjectBindingException 
	 * @throws ProjectAccessException 
	 * @throws ProjectInexistentException 
	 * @throws InvalidProjectNameException 
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('rbac(user, role)', 'D')")
	public void removeAllRolesFromUser(String projectName, String email) throws ProjectBindingException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new ProjectBindingException("No user found with email " + email);
		}
		Project project = ProjectManager.getProjectDescription(projectName);
		if (project == null) {
			throw new ProjectBindingException("Project " + projectName + " doesn't exist");
		}
		//removes all role from the binding
		ProjectUserBindingsManager.removeAllRoleFromPUBinding(user, project);
	}
	
	/**
	 * @throws ProjectBindingException 
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('rbac(user, role)', 'D')")
	public void removeRoleFromUser(String projectName, String email, String role) throws ProjectBindingException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new ProjectBindingException("No user found with email " + email);
		}
		Project project = ProjectManager.getProjectDescription(projectName);
		if (project == null) {
			throw new ProjectBindingException("Project " + projectName + " doesn't exist");
		}
		Role aRole = RBACManager.getRole(project, role);
		if (aRole == null) {
			throw new ProjectBindingException("No role '" + role + "' found");
		}
		ProjectUserBindingsManager.removeRoleFromPUBinding(user, project, aRole);
	}
	
	/**
	 * @throws ProjectBindingException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('rbac(user, role)', 'U')")
	public void updateLanguagesOfUserInProject(String projectName, String email, Collection<String> languages) throws ProjectBindingException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new ProjectBindingException("No user found with email " + email);
		}
		Project project = ProjectManager.getProjectDescription(projectName);
		if (project == null) {
			throw new ProjectBindingException("Project " + projectName + " doesn't exist");
		}
		ProjectUserBindingsManager.updateLanguagesToPUBinding(user, project, languages);
	}
	
	//ROLES AND CAPABILITIES SERVICES
	
	/**
	 * 
	 * @return
	 * @throws JSONException
	 * @throws RBACException 
	 * @throws ProjectAccessException 
	 * @throws ProjectInexistentException 
	 * @throws InvalidProjectNameException 
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('rbac(role)', 'R')")
	public JsonNode listRoles(@Optional String projectName) throws JSONException, RBACException, InvalidProjectNameException,
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
	@STServiceOperation
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
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('rbac(role)', 'D')")
	public void deleteRole(String roleName) throws ProjectBindingException {
		Project project = null;
		if (stServiceContext.hasContextParameter("project")) {
			project = getProject();
		}
		Role aRole = RBACManager.getRole(project, roleName);
		if (aRole == null) {
			throw new ProjectBindingException("No role '" + roleName + "' found");
		}
		RBACManager.deleteRole(getProject(), roleName);
		ProjectUserBindingsManager.removeRoleFromPUBindings(getProject(), aRole);
	}
	
	/**
	 * Exports the {@link CustomForm} with the given id
	 * @param oRes
	 * @param id
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
			throw new RBACException("Impossible to export role '" + roleName + "'."
					+ " A role with that name doesn't exist");
		}
		File roleFile = RBACManager.getRoleFile(project, roleName);
		if (!roleFile.exists()) { //in case the role file was not found at project level)
			roleFile = RBACManager.getRoleFile(null, roleName); //TODO seems not to work
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
	 * @throws CustomFormException 
	 * @throws RBACException 
	 * @throws RoleCreationException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('rbac(role)', 'C')")
	public void importRole(MultipartFile inputFile, String newRoleName) 
			throws IOException, CustomFormException, RBACException, RoleCreationException {
		if (RBACManager.getRBACProcessor(getProject(), newRoleName) != null) {
			throw new RBACException("Cannot import role '" + newRoleName + "'."
					+ " A role with that name already exists in project '" + getProject().getName() + "'");
		}
		File tempServerFile = File.createTempFile("roleImport", ".pl");
		try {
			inputFile.transferTo(tempServerFile);
			try {
				RBACProcessor rbac = new RBACProcessor(tempServerFile);
				RBACManager.createRole(getProject(), newRoleName);
				List<Term> capAsTerms = rbac.getCapabilitiesAsTermList();
				Collection<String> capabilities = new ArrayList<>();
				for (Term t: capAsTerms) {
					capabilities.add(t.toString());
				}
				RBACManager.addCapabilities(getProject(), newRoleName, capabilities);
			} catch (InvalidTheoryException | TheoryNotFoundException | 
					MalformedGoalException | NoSolutionException | NoMoreSolutionException e) {
				throw new RBACException("Invalid role file", e);
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
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('rbac(role)', 'CR')")
	public void cloneRole(String sourceRoleName, String targetRoleName) throws RoleCreationException, RBACException {
		Project project = getProject();
		if (stServiceContext.hasContextParameter("project")) {
			project = getProject();
		}
		Role sourceRole = RBACManager.getRole(project, sourceRoleName);
		if (sourceRole == null) {
			throw new RoleCreationException("No role '" + sourceRoleName + "' found");
		}
		//doesn't check the existence of targetRoleName since it is already done by createRole()
		RBACManager.createRole(getProject(), targetRoleName);
		RBACManager.addCapabilities(getProject(), targetRoleName, RBACManager.getRoleCapabilities(project, sourceRoleName));
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
	@STServiceOperation
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
	
}
