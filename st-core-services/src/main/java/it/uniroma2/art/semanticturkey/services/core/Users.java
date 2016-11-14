package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
import it.uniroma2.art.semanticturkey.security.PermissionManager;
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
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/testRequiredAdmin", 
			method = RequestMethod.GET, produces = "application/json")
	@PreAuthorize("hasRole('CAPABILITY_1')")
	@ResponseBody
	public String testRequiredAdmin(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("testRequiredAdmin", RepliesStatus.ok, SerializationType.json);
		return jsonResp.toString();
	}
	
	/**
	 * Registers a new user
	 * @param email this will be used as id
	 * @param password password not encoded, it will be encoded lately
	 * @param firstName
	 * @param lastName
	 * @param birthday
	 * @param gender
	 * @param country
	 * @param address
	 * @param affiliation
	 * @param url
	 * @param phone
	 * @return
	 */
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
	
	/**
	 * Update first name of the given user.
	 * @param email
	 * @param firstName
	 * @return
	 * @throws ParseException 
	 * @throws JSONException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserFirstName", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserFirstName(@RequestParam("email") String email, @RequestParam("firstName") String firstName) throws ParseException, JSONException {
		STUser user = userMgr.getUserByEmail(email);
		user = userMgr.updateUserFirstName(user, firstName);
		updateUserInSecurityContext(user);
		
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("updateUserFirstName", RepliesStatus.ok, SerializationType.json);
		jsonResp.getDataElement().put("user", user.getAsJSONObject());
		return jsonResp.toString();
	}
	
	/**
	 * Update last name of the given user.
	 * @param email
	 * @param lastName
	 * @return
	 * @throws ParseException 
	 * @throws JSONException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserLastName", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserLastName(@RequestParam("email") String email, @RequestParam("lastName") String lastName) throws ParseException, JSONException {
		STUser user = userMgr.getUserByEmail(email);
		user = userMgr.updateUserLastName(user, lastName);
		updateUserInSecurityContext(user);
		
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("updateUserLastName", RepliesStatus.ok, SerializationType.json);
		jsonResp.getDataElement().put("user", user.getAsJSONObject());
		return jsonResp.toString();
	}
	
	/**
	 * Update first name of the given user.
	 * @param email
	 * @param phone
	 * @return
	 * @throws ParseException 
	 * @throws JSONException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserPhone", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserPhone(@RequestParam("email") String email, @RequestParam("phone") String phone) throws ParseException, JSONException {
		STUser user = userMgr.getUserByEmail(email);
		user = userMgr.updateUserPhone(user, phone);
		updateUserInSecurityContext(user);
		
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("updateUserPhone", RepliesStatus.ok, SerializationType.json);
		jsonResp.getDataElement().put("user", user.getAsJSONObject());
		return jsonResp.toString();
	}
	
	/**
	 * Update birthday of the given user.
	 * @param email
	 * @param birthday
	 * @return
	 * @throws ParseException 
	 * @throws JSONException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserBirthday", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserBirthday(@RequestParam("email") String email, @RequestParam("birthday") String birthday) throws ParseException, JSONException {
		STUser user = userMgr.getUserByEmail(email);
		user = userMgr.updateUserBirthday(user, birthday);
		updateUserInSecurityContext(user);
		
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("updateBirthday", RepliesStatus.ok, SerializationType.json);
		jsonResp.getDataElement().put("user", user.getAsJSONObject());
		return jsonResp.toString();
	}
	
	/**
	 * Update gender of the given user.
	 * @param email
	 * @param gender
	 * @return
	 * @throws ParseException 
	 * @throws JSONException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserGender", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserGender(@RequestParam("email") String email, @RequestParam("gender") String gender) throws ParseException, JSONException {
		STUser user = userMgr.getUserByEmail(email);
		user = userMgr.updateUserGender(user, gender);
		updateUserInSecurityContext(user);
		
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("updateGender", RepliesStatus.ok, SerializationType.json);
		jsonResp.getDataElement().put("user", user.getAsJSONObject());
		return jsonResp.toString();
	}
	
	/**
	 * Update country of the given user.
	 * @param email
	 * @param country
	 * @return
	 * @throws ParseException 
	 * @throws JSONException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserCountry", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserCountry(@RequestParam("email") String email, @RequestParam("country") String country) throws ParseException, JSONException {
		STUser user = userMgr.getUserByEmail(email);
		user = userMgr.updateUserCountry(user, country);
		updateUserInSecurityContext(user);
		
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("updateCountry", RepliesStatus.ok, SerializationType.json);
		jsonResp.getDataElement().put("user", user.getAsJSONObject());
		return jsonResp.toString();
	}
	
	/**
	 * Update address of the given user.
	 * @param email
	 * @param address
	 * @return
	 * @throws ParseException 
	 * @throws JSONException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserAddress", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserAddress(@RequestParam("email") String email, @RequestParam("address") String address) throws ParseException, JSONException {
		STUser user = userMgr.getUserByEmail(email);
		user = userMgr.updateUserAddress(user, address);
		updateUserInSecurityContext(user);
		
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("updateAddress", RepliesStatus.ok, SerializationType.json);
		jsonResp.getDataElement().put("user", user.getAsJSONObject());
		return jsonResp.toString();
	}
	
	/**
	 * Update gender of the given user.
	 * @param email
	 * @param affiliation
	 * @return
	 * @throws ParseException 
	 * @throws JSONException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserAffiliation", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserAffiliation(@RequestParam("email") String email, @RequestParam("affiliation") String affiliation) throws ParseException, JSONException {
		STUser user = userMgr.getUserByEmail(email);
		user = userMgr.updateUserAffiliation(user, affiliation);
		updateUserInSecurityContext(user);
		
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("updateAffiliation", RepliesStatus.ok, SerializationType.json);
		jsonResp.getDataElement().put("user", user.getAsJSONObject());
		return jsonResp.toString();
	}
	
	/**
	 * Update url of the given user.
	 * @param email
	 * @param url
	 * @return
	 * @throws ParseException 
	 * @throws JSONException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserUrl", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserUrl(@RequestParam("email") String email, @RequestParam("url") String url) throws ParseException, JSONException {
		STUser user = userMgr.getUserByEmail(email);
		user = userMgr.updateUserUrl(user, url);
		updateUserInSecurityContext(user);
		
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("updateUrl", RepliesStatus.ok, SerializationType.json);
		jsonResp.getDataElement().put("user", user.getAsJSONObject());
		return jsonResp.toString();
	}
	
	private void updateUserInSecurityContext(STUser user) {
		Authentication auth = new UsernamePasswordAuthenticationToken(user, null, PermissionManager.getAuthoritiesForRoles(user.getRoles()));
		SecurityContextHolder.getContext().setAuthentication(auth);
	}
	
}
