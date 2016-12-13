package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import it.uniroma2.art.semanticturkey.security.ProjectUserBindingManager;
import it.uniroma2.art.semanticturkey.security.RolesManager;
import it.uniroma2.art.semanticturkey.security.UsersManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.servlet.JSONResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.RoleCreationException;
import it.uniroma2.art.semanticturkey.user.STRole;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserCapabilitiesEnum;

@Validated
@Controller
public class Administration extends STServiceAdapter {
	
	@Autowired
	UsersManager usersMgr;
	
	@Autowired
	RolesManager rolesMgr;
	
	@Autowired
	ProjectUserBindingManager puBindingMgr;
	
	//PROJECT-USER BINDING SERVICES
	
	/**
	 * @throws Exception 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Administration/getProjectUserBinding", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String getProjectUserBinding(
			@RequestParam("projectName") String projectName,
			@RequestParam("email") String email) throws Exception {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("addRoleToUserInProject", RepliesStatus.ok, SerializationType.json);
		STUser user = usersMgr.getUserByEmail(email);
		if (user == null) {
			throw new Exception("No user found with email " + email); //TODO create a valid exception
		}
		ProjectUserBinding puBinding = puBindingMgr.getPUBinding(user, projectName);
		if (puBinding == null) {
			throw new Exception("No binding found for user with email " + email + " and project " + projectName); //TODO create a valid exception
		}
		jsonResp.getDataElement().put("binding", puBinding.getAsJSONObject());
		return jsonResp.toString();
	}
	
	/**
	 * @throws Exception 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Administration/addProjectUserBinding", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String addProjectUserBinding(@RequestParam("projectName") String projectName,
			@RequestParam("email") String email, @RequestParam("roles") String[] roles) throws Exception {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("addProjectUserBinding", RepliesStatus.ok, SerializationType.json);
		
		STUser user = usersMgr.getUserByEmail(email);
		if (user == null) {
			throw new Exception("No user found with email " + email); //TODO create a valid exception
		}

		ProjectUserBinding puBinding = puBindingMgr.getPUBinding(user, projectName);
		if (puBinding == null) {
			throw new Exception("No binding found for user with email " + email + " and project " + projectName); //TODO create a valid exception
		}
		
		puBindingMgr.addRolesToPUBinding(email, projectName, Arrays.asList(roles));
		
		return jsonResp.toString();
	}
	
	/**
	 * @throws Exception 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Administration/addRoleToUserInProject", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String addRoleToUserInProject(@RequestParam("projectName") String projectName,
			@RequestParam("email") String email, @RequestParam("role") String role) throws Exception {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("addRoleToUserInProject", RepliesStatus.ok, SerializationType.json);
		STUser user = usersMgr.getUserByEmail(email);
		if (user == null) {
			throw new Exception("No user found with email " + email); //TODO create a valid exception
		}
		ProjectUserBinding puBinding = puBindingMgr.getPUBinding(user, projectName);
		if (puBinding == null) {
			throw new Exception("No binding found for user with email " + email + " and project " + projectName); //TODO create a valid exception
		}
		puBindingMgr.addRolesToPUBinding(email, projectName, Arrays.asList(new String[]{role}));
		return jsonResp.toString();
	}
	
	/**
	 * @throws Exception 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Administration/removeRoleToUserInProject", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String removeRoleToUserInProject(@RequestParam("projectName") String projectName,
			@RequestParam("email") String email, @RequestParam("role") String role) throws Exception {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("addRoleToUserInProject", RepliesStatus.ok, SerializationType.json);
		STUser user = usersMgr.getUserByEmail(email);
		if (user == null) {
			throw new Exception("No user found with email " + email); //TODO create a valid exception
		}
		ProjectUserBinding puBinding = puBindingMgr.getPUBinding(user, projectName);
		if (puBinding == null) {
			throw new Exception("No binding found for user with email " + email + " and project " + projectName); //TODO create a valid exception
		}
		puBindingMgr.removeRoleFromPUBinding(email, projectName, role);
		return jsonResp.toString();
	}
	
	//ROLES AND CAPABILITIES SERVICES
	
	/**
	 * 
	 * @return
	 * @throws JSONException
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Administration/listRoles", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String listRoles() throws JSONException {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("listRoles", RepliesStatus.ok, SerializationType.json);
		Collection<STRole> roles = rolesMgr.listRoles();
		JSONArray rolesJson = new JSONArray();
		for (STRole r : roles) {
			rolesJson.put(r.getAsJSONObject());
		}
		jsonResp.getDataElement().put("roles", rolesJson);
		return jsonResp.toString();
	}
	
	/**
	 * 
	 * @param roleName
	 * @return
	 * @throws RoleCreationException
	 * @throws IOException
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Administration/createRole", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String createRole(@RequestParam("roleName") String roleName) throws RoleCreationException, IOException {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("createRole", RepliesStatus.ok, SerializationType.json);
		rolesMgr.createRole(new STRole(roleName));
		return jsonResp.toString();
	}
	
	/**
	 * 
	 * @param roleName
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Administration/deleteRole", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String deleteRole(@RequestParam("roleName") String roleName) throws IOException {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("deleteRole", RepliesStatus.ok, SerializationType.json);
		rolesMgr.deleteRole(roleName);
		puBindingMgr.removeRoleFromAllPUBindings(roleName);
		return jsonResp.toString();
	}
	
	/**
	 * 
	 * @return
	 * @throws JSONException
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Administration/listCapabilities", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String listCapabilities() throws JSONException {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("listCapabilities", RepliesStatus.ok, SerializationType.json);
		
		JSONArray capabilitiesJson = new JSONArray();
		UserCapabilitiesEnum[] capabilities = UserCapabilitiesEnum.values();
		for (int i = 0; i < capabilities.length; i++) {
			capabilitiesJson.put(capabilities[i].name());
		}
		jsonResp.getDataElement().put("capabilities", capabilitiesJson);
		return jsonResp.toString();
	}
	
	/**
	 * 
	 * @param role
	 * @param capability
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Administration/addCapabilityToRole", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String addCapabilityToRole(@RequestParam("role") String role,
			@RequestParam("capability") String capability) throws Exception {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("addCapabilityToRole", RepliesStatus.ok, SerializationType.json);
		
		UserCapabilitiesEnum capEnum = UserCapabilitiesEnum.valueOf(capability);
		STRole stRole = rolesMgr.searchRole(role);
		if (stRole == null) {
			throw new Exception("No role found with name " + role); //TODO create a valid exception
		}
		rolesMgr.addCapability(stRole, capEnum);
		return jsonResp.toString();
	}

	/**
	 * 
	 * @param role
	 * @param capability
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Administration/removeCapabilityFromRole", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String removeCapabilityFromRole(@RequestParam("role") String role,
			@RequestParam("capability") String capability) throws Exception {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("removeCapabilityFromRole", RepliesStatus.ok, SerializationType.json);
		UserCapabilitiesEnum capEnum = UserCapabilitiesEnum.valueOf(capability);
		STRole stRole = rolesMgr.searchRole(role);
		if (stRole == null) {
			throw new Exception("No role found with name " + role); //TODO create a valid exception
		}
		rolesMgr.removeCapability(stRole, capEnum);
		return jsonResp.toString();
	}
	
	
	
}
