package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import it.uniroma2.art.semanticturkey.exceptions.UserCreationException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.security.UserManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.servlet.JSONResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.user.STUser;

@GenerateSTServiceController
@Validated
@Component
@Controller //needed for getUser method
public class Users extends STServiceAdapter {
	
	@Autowired
	UserManager userMgr;
	
	/**
	 * Returns response containing a json representation of the logged user.
	 * An empty data element if no user is logged.
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/getUser", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String getUser(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {

		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("getUser", RepliesStatus.ok, SerializationType.json);
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (!(auth instanceof AnonymousAuthenticationToken)) {//if there's a user authenticated
			STUser loggedUser = (STUser) auth.getPrincipal();
			
			jsonResp.getDataElement().put("user", loggedUser.getAsJSONObject());
		}
		
		return jsonResp.toString();
	}
	
	/**
	 * Just an example to test a service for which ROLE_ADMIN is required. If the current logged user
	 * has no ROLE_ADMIN authority, a denied response is returned
	 * 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/testRequiredAdmin", 
			method = RequestMethod.GET, produces = "application/json")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@ResponseBody
	public String testRequiredAdmin(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("testRequiredAdmin", RepliesStatus.ok, SerializationType.json);
		return jsonResp.toString();
	}
	
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/registerUser", 
			method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public String registerUser(
			@RequestParam("email") String email, @RequestParam("password") String password,
			@RequestParam("firstName") String firstName, @RequestParam("lastName") String lastName,
			@RequestParam(value = "birthday", required = false) String birthday,
			@RequestParam(value = "gender", required = false) String gender,
			@RequestParam(value = "country", required = false) String country,
			@RequestParam(value = "address", required = false) String address,
			@RequestParam(value = "affiliation", required = false) String affiliation,
			@RequestParam(value = "url", required = false) String url,
			@RequestParam(value = "phone", required = false) String phone) {
		try {
			userMgr.registerUser(email, password, firstName, lastName, birthday, gender, country, address, affiliation, url, phone);
			JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
					.createReplyResponse("registerUser", RepliesStatus.ok, SerializationType.json);
			return jsonResp.toString();
		} catch (UserCreationException e) {
			JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
					.createReplyFAIL("registerUser", e.getMessage(), SerializationType.json);
			return jsonResp.toString();
		}
		
	}
	
	
}
