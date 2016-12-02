package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.resources.Config;
import it.uniroma2.art.semanticturkey.security.ProjectUserBindingManager;
import it.uniroma2.art.semanticturkey.security.UsersManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.servlet.JSONResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserCreationException;
import it.uniroma2.art.semanticturkey.user.UserStatus;

@Validated
@Controller
public class Users extends STServiceAdapter {
	
	@Autowired
	UsersManager usersMgr;
	
	@Autowired
	ProjectUserBindingManager puBindingMgr;
	
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
		} else {
			jsonResp.getDataElement().put("user", new JSONObject()); //empty object
		}
		return jsonResp.toString();
	}
	
	/**
	 * Returns all the users registered
	 * @return
	 * @throws JSONException
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/listUsers", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String listUsers() throws JSONException {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("listUsers", RepliesStatus.ok, SerializationType.json);
		Collection<STUser> users = usersMgr.listUsers();
		JSONArray usersJson = new JSONArray();
		for (STUser user : users) {
			usersJson.put(user.getAsJSONObject());
		}
		jsonResp.getDataElement().put("users", usersJson);
		return jsonResp.toString();
	}
	
	/**
	 * Returns all the users that have at least a role in the given project
	 * @param projectName
	 * @return
	 * @throws JSONException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/listUsersBoundToProject", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String listUsersBoundToProject(@RequestParam("projectName") String projectName) throws JSONException {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("listUsersBoundToProject", RepliesStatus.ok, SerializationType.json);
		Collection<ProjectUserBinding> puBindings = puBindingMgr.listPUBindingsOfProject(projectName);
		JSONArray usersJson = new JSONArray();
		for (ProjectUserBinding pub : puBindings) {
			if (!pub.getRolesName().isEmpty()) {
				usersJson.put(usersMgr.getUserByEmail(pub.getUserEmail()).getAsJSONObject());
			}
		}
		jsonResp.getDataElement().put("users", usersJson);
		return jsonResp.toString();
	}
	
	/**
	 * Just an example to test a service for which a capability is required. If the current logged user
	 * has no the capability determined in auth.isAuthorized, then a denied response is returned
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/testRequiredAdmin", 
			method = RequestMethod.GET, produces = "application/json")
	@PreAuthorize("@auth.isAuthorized('concept', 'lexicalization', 'update')")
	@ResponseBody
	public String testRequiredAdmin(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("testRequiredAdmin", RepliesStatus.ok, SerializationType.json);
		return jsonResp.toString();
	}
	
	/**
	 * Registers a new user
	 * @param email this will be used as id
	 * @param password password not encoded, it is encoded here
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
	 * @throws IOException 
	 * @throws MessagingException 
	 * @throws ProjectAccessException 
	 * @throws UserCreationException 
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
			@RequestParam(value = "phone", required = false) String phone) throws IOException, MessagingException, ProjectAccessException, UserCreationException {
		try {
			STUser user = new STUser(email, password, firstName, lastName);
			if (birthday != null) {
				user.setBirthday(birthday);
			}
			if (gender != null) {
				user.setGender(gender);
			}
			if (country != null) {
				user.setCountry(country);
			}
			if (address != null) {
				user.setAddress(address);
			}
			if (affiliation != null) {
				user.setAffiliation(affiliation);
			}
			if (url != null) {
				user.setUrl(url);
			}
			if (phone != null) {
				user.setPhone(phone);
			}
			usersMgr.registerUser(user);
			puBindingMgr.createPUBindingsOfUser(email);
			
			//TODO to enable when AccessControl role-base is completed (so the admin can set a valid admin email config) 
//			EmailSender.sendRegistrationMail(email, firstName, lastName);
			//TODO send mail to system administrator too in order to enable new user
//			EmailSender.sendRegistrationMailToAdmin(email, firstName, lastName);
			
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
	 * @throws JSONException 
	 * @throws IOException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserFirstName", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserFirstName(@RequestParam("email") String email, @RequestParam("firstName") String firstName) throws JSONException, IOException {
		STUser user = usersMgr.getUserByEmail(email);
		user = usersMgr.updateUserFirstName(user, firstName);
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
	 * @throws JSONException 
	 * @throws IOException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserLastName", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserLastName(@RequestParam("email") String email, @RequestParam("lastName") String lastName) throws JSONException, IOException {
		STUser user = usersMgr.getUserByEmail(email);
		user = usersMgr.updateUserLastName(user, lastName);
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
	 * @throws JSONException 
	 * @throws IOException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserPhone", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserPhone(@RequestParam("email") String email, @RequestParam("phone") String phone) throws JSONException, IOException {
		STUser user = usersMgr.getUserByEmail(email);
		user = usersMgr.updateUserPhone(user, phone);
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
	 * @throws JSONException 
	 * @throws IOException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserBirthday", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserBirthday(@RequestParam("email") String email, @RequestParam("birthday") String birthday) throws JSONException, IOException {
		STUser user = usersMgr.getUserByEmail(email);
		user = usersMgr.updateUserBirthday(user, birthday);
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
	 * @throws JSONException 
	 * @throws IOException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserGender", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserGender(@RequestParam("email") String email, @RequestParam("gender") String gender) throws JSONException, IOException {
		STUser user = usersMgr.getUserByEmail(email);
		user = usersMgr.updateUserGender(user, gender);
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
	 * @throws JSONException 
	 * @throws IOException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserCountry", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserCountry(@RequestParam("email") String email, @RequestParam("country") String country) throws JSONException, IOException {
		STUser user = usersMgr.getUserByEmail(email);
		user = usersMgr.updateUserCountry(user, country);
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
	 * @throws JSONException 
	 * @throws IOException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserAddress", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserAddress(@RequestParam("email") String email, @RequestParam("address") String address) throws JSONException, IOException {
		STUser user = usersMgr.getUserByEmail(email);
		user = usersMgr.updateUserAddress(user, address);
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
	 * @throws JSONException 
	 * @throws IOException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserAffiliation", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserAffiliation(@RequestParam("email") String email, @RequestParam("affiliation") String affiliation) throws JSONException, IOException {
		STUser user = usersMgr.getUserByEmail(email);
		user = usersMgr.updateUserAffiliation(user, affiliation);
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
	 * @throws JSONException 
	 * @throws IOException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserUrl", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserUrl(@RequestParam("email") String email, @RequestParam("url") String url) throws JSONException, IOException {
		STUser user = usersMgr.getUserByEmail(email);
		user = usersMgr.updateUserUrl(user, url);
		updateUserInSecurityContext(user);
		
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("updateUrl", RepliesStatus.ok, SerializationType.json);
		jsonResp.getDataElement().put("user", user.getAsJSONObject());
		return jsonResp.toString();
	}
	
	/**
	 * Enables or disables the given user
	 * @param email
	 * @param enable
	 * @return
	 * @throws IOException 
	 * @throws JSONException 
	 */
	//TODO move to Administration?
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/enableUser", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String enableUser(@RequestParam("email") String email, @RequestParam("enabled") boolean enabled) throws IOException, JSONException  {
		STUser user = usersMgr.getUserByEmail(email);
		if (enabled) {
			user = usersMgr.updateUserStatus(user, UserStatus.ENABLED);
		} else {
			user = usersMgr.updateUserStatus(user, UserStatus.DISABLED);
		}
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("enableUser", RepliesStatus.ok, SerializationType.json);
		jsonResp.getDataElement().put("user", user.getAsJSONObject());
		return jsonResp.toString();
	}
	
	/**
	 * Deletes an user
	 * @param email
	 * @throws IOException 
	 */
	//TODO move to Administration?
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/deleteUser", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String deleteUser(@RequestParam("email") String email) throws IOException {
		usersMgr.deleteUser(email);
		puBindingMgr.deletePUBindingsOfUser(email);
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("deleteUser", RepliesStatus.ok, SerializationType.json);
		return jsonResp.toString();
	}
	
	private void updateUserInSecurityContext(STUser user) {
		Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);
	}
	
	
	private static class EmailSender {
		
		/**
		 * Sends an email to the registered user
		 * @param toEmail
		 * @param firstName
		 * @param lastName
		 * @throws MessagingException
		 * @throws UnsupportedEncodingException
		 */
		public static void sendRegistrationMailToUser(String toEmail, String firstName, String lastName) throws MessagingException, UnsupportedEncodingException {
			String text = "Dear " + firstName + " " + lastName + ","
					+ "\nthank you for registering as a user of VocBench 3."
					+ " Your request has been received. Please wait for the administrator to approve it."
					+ " You will be informed when you can start to login in the system."
					+ " After approval, you can log into VocBench with the e-mail " + toEmail + " and your chosen password."
					+ "\nThanks for your interest."
					+ "\nIf you want to unregister, please send an email with your e-mail address and the subject:"
					+ " 'VocBench - Unregister' to " + Config.getEmailAdminAddress() + "."
					+ "\nRegards,\nThe VocBench Team.";
			sendMail(toEmail, text);
		}
		
		/**
		 * Sends an email to the system administrator to inform about a new user registration request
		 * @throws MessagingException 
		 * @throws UnsupportedEncodingException 
		 */
		public static void sendRegistrationMailToAdmin(String userEmail, String userFirstName, String userLastName) throws UnsupportedEncodingException, MessagingException {
			String text = "Dear VocBench administrator,"
					+ "\nthere is a new user registration request for VocBench."
					+ "\nFirst Name: " + userFirstName
					+ "\nLast Name: " + userLastName
					+ "\nE-mail: " + userEmail
					+ "\nPlease activate the account.\nRegards,\nThe VocBench Team.";
			sendMail(Config.getEmailAdminAddress(), text);
		}
		
		private static void sendMail(String toEmail, String text) throws MessagingException, UnsupportedEncodingException {
			String emailAddress = Config.getEmailFromAddress();
			String emailPassword = Config.getEmailFromPassword();
			String emailAlias = Config.getEmailFromAlias();
			String emailHost = Config.getEmailFromHost();
			String emailPort = Config.getEmailFromPort();
			
			if (emailAddress == null || emailPassword == null || emailAlias == null || emailHost == null || emailPort == null) {
				throw new MessagingException("Wrong mail configuration, impossible to send a confirmation e-mail");
			}
			
			Properties props = new Properties();
			props.put("mail.smtp.host", emailHost);
			props.put("mail.smtp.socketFactory.port", emailPort);
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.port", emailPort);
			
			Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(emailAddress, emailPassword);
				}
			});
			
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("itartartemide@gmail.com", "VocBench Admin"));
			message.setSubject("VocBench3 registration");
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
			message.setText(text);
			Transport.send(message);
			
		}
		
	}
	
}
