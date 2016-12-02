package it.uniroma2.art.semanticturkey.services.core;

import java.util.Arrays;

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
import it.uniroma2.art.semanticturkey.user.STUser;

@Validated
@Controller
public class Administration extends STServiceAdapter {
	
	@Autowired
	UsersManager usersMgr;
	
	@Autowired
	RolesManager rolesMgr;
	
	@Autowired
	ProjectUserBindingManager puBindingMgr;
	
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
	
	
	
	
	
	
}
