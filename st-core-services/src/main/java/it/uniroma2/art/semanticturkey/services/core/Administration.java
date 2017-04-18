package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
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
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.rbac.RBACProcessor;
import it.uniroma2.art.semanticturkey.rbac.TheoryNotFoundException;
import it.uniroma2.art.semanticturkey.resources.Config;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.PUBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.RoleCreationException;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;

@STService
@Controller
public class Administration extends STServiceAdapter {
	
	/**
	 * Gets the administration config: a map with key value of configuration parameters
	 * @return
	 * @throws JSONException
	 */
	@STServiceOperation
	public JsonNode getAdministrationConfig() throws JSONException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode configNode = jsonFactory.objectNode();
		configNode.set("emailAdminAddress", jsonFactory.textNode(Config.getEmailAdminAddress()));
		configNode.set("emailFromAddress", jsonFactory.textNode(Config.getEmailFromAddress()));
		configNode.set("emailFromPassword", jsonFactory.textNode(Config.getEmailFromPassword()));
		configNode.set("emailFromAlias", jsonFactory.textNode(Config.getEmailFromAlias()));
		configNode.set("emailFromHost", jsonFactory.textNode(Config.getEmailFromHost()));
		configNode.set("emailFromPort", jsonFactory.textNode(Config.getEmailFromPort()));
		return configNode;
	}
	
	/**
	 * Updates the administration config parameters
	 * @param emailAdminAddress
	 * @param emailFromAddress
	 * @param emailFromPassword
	 * @param emailFromAlias
	 * @param emailFromHost
	 * @param emailFromPort
	 * @return
	 */
	@STServiceOperation
	public void updateAdministrationConfig(
			String emailAdminAddress, String emailFromAddress, String emailFromPassword,
			String emailFromAlias, String emailFromHost, String emailFromPort) {
		Config.setEmailAdminAddress(emailAdminAddress);
		Config.setEmailFromAddress(emailFromAddress);
		Config.setEmailFromPassword(emailFromPassword);
		Config.setEmailFromAlias(emailFromAlias);
		Config.setEmailFromHost(emailFromHost);
		Config.setEmailFromPort(emailFromPort);
	}
	
	
	//PROJECT-USER BINDING SERVICES
	
	/**
	 * @throws RBACException
	 * @throws JSONException 
	 */
	@STServiceOperation
	public JsonNode getProjectUserBinding(String projectName, String email) throws RBACException, JSONException {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new RBACException("No user found with email " + email);
		}
		ProjectUserBinding puBinding = ProjectUserBindingsManager.getPUBinding(user, projectName);
		if (puBinding == null) {
			throw new RBACException("No binding found for user with email " + email + " and project " + projectName);
		}
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode bindingNode = jsonFactory.objectNode();
		bindingNode.set("userEmail", jsonFactory.textNode(puBinding.getUserEmail()));
		bindingNode.set("projectName", jsonFactory.textNode(puBinding.getProjectName()));
		ArrayNode rolesArrayNode = jsonFactory.arrayNode();
		for (String role: puBinding.getRolesName()) {
			rolesArrayNode.add(role);
		}
		bindingNode.set("roles", rolesArrayNode);
		return bindingNode;
	}
	
	/**
	 * @throws PUBindingException 
	 * @throws RBACException 
	 */
	@STServiceOperation
	public void addProjectUserBinding(String projectName, String email, String[] roles) throws RBACException, PUBindingException {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new RBACException("No user found with email " + email);
		}

		ProjectUserBinding puBinding = ProjectUserBindingsManager.getPUBinding(user, projectName);
		if (puBinding == null) {
			throw new RBACException("No binding found for user with email " + email + " and project " + projectName);
		}
		ProjectUserBindingsManager.addRolesToPUBinding(email, projectName, Arrays.asList(roles));
	}
	
	/**
	 * @throws RBACException 
	 * @throws PUBindingException 
	 */
	@STServiceOperation
	public void addRoleToUserInProject(String projectName, String email, String role) throws RBACException, PUBindingException {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new RBACException("No user found with email " + email);
		}
		ProjectUserBinding puBinding = ProjectUserBindingsManager.getPUBinding(user, projectName);
		if (puBinding == null) {
			throw new RBACException("No binding found for user with email " + email + " and project " + projectName);
		}
		ProjectUserBindingsManager.addRolesToPUBinding(email, projectName, Arrays.asList(new String[]{role}));
	}
	
	/**
	 * @throws PUBindingException 
	 * @throws RBACException 
	 */
	@STServiceOperation
	public void removeRoleToUserInProject(String projectName, String email, String role) throws RBACException, PUBindingException {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new RBACException("No user found with email " + email);
		}
		ProjectUserBinding puBinding = ProjectUserBindingsManager.getPUBinding(user, projectName);
		if (puBinding == null) {
			throw new RBACException("No binding found for user with email " + email + " and project " + projectName);
		}
		ProjectUserBindingsManager.removeRoleFromPUBinding(email, projectName, role);
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
	public JsonNode listRoles(@Optional String projectName) throws JSONException, RBACException, InvalidProjectNameException,
		ProjectInexistentException, ProjectAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode rolesArrayNode = jsonFactory.arrayNode();
		//system roles
		for (String role : RBACManager.getRoles(null)) {
			ObjectNode roleNode = jsonFactory.objectNode();
			roleNode.set("name", jsonFactory.textNode(role));
			roleNode.set("level", jsonFactory.textNode("system"));
			rolesArrayNode.add(roleNode);
		}
		//project roles
		if (projectName != null) {
			Project<?> project = ProjectManager.getProjectDescription(projectName);
			for (String role : RBACManager.getRoles(project)) {
				ObjectNode roleNode = jsonFactory.objectNode();
				roleNode.set("name", jsonFactory.textNode(role));
				roleNode.set("level", jsonFactory.textNode("project"));
				rolesArrayNode.add(roleNode);
			}
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
			Project<?> project = ProjectManager.getProjectDescription(projectName);
			for (Term c: RBACManager.getRoleCapabilities(project, role)) {
				capabilitiesArrayNode.add(c.toString());
			}
		} else {
			for (Term c: RBACManager.getRoleCapabilities(null, role)) {
				capabilitiesArrayNode.add(c.toString());
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
	public void createRole(String roleName) throws RoleCreationException {
		RBACManager.createRole(getProject(), roleName);
	}
	
	/**
	 * 
	 * @param roleName
	 * @return
	 * @throws PUBindingException
	 */
	@STServiceOperation
	public void deleteRole(String roleName) throws PUBindingException {
		RBACManager.deleteRole(getProject(), roleName);
		ProjectUserBindingsManager.removeRoleFromPUBindings(getProject(), roleName);
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
	public void exportRole(HttpServletResponse oRes, String roleName) throws RBACException, IOException {
		if (RBACManager.getRBACProcessor(getProject(), roleName) == null) {
			throw new RBACException("Impossible to export role '" + roleName + "'."
					+ " A role with that name doesn't exist in project '" + getProject().getName() + "'");
		}
		File roleFile = RBACManager.getRoleFile(getProject(), roleName);
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
	 * 
	 * @param role
	 * @param capability
	 * @return
	 * @throws RBACException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
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
	public void updateCapabilityForRole(String role, String oldCapability, String newCapability) throws RBACException {
		RBACManager.removeCapability(getProject(), role, oldCapability);
		RBACManager.addCapability(getProject(), role, newCapability);
	}
	
}
