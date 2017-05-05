package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.resources.Config;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.servlet.JSONResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.user.PUBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserCreationException;
import it.uniroma2.art.semanticturkey.user.UserStatus;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.Utilities;

@Validated
@Controller
public class Users extends STServiceAdapter {
	
	private static Logger logger = LoggerFactory.getLogger(Users.class);
	
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
	@PreAuthorize("@auth.isAuthorized('um(user)', 'R')")
	public String listUsers() throws JSONException {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("listUsers", RepliesStatus.ok, SerializationType.json);
		Collection<STUser> users = UsersManager.listUsers();
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
	 * @throws ProjectAccessException 
	 * @throws ProjectInexistentException 
	 * @throws InvalidProjectNameException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/listUsersBoundToProject", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	@PreAuthorize("@auth.isAuthorized('um(user, project)', 'R')")
	public String listUsersBoundToProject(@RequestParam("projectName") String projectName) throws JSONException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("listUsersBoundToProject", RepliesStatus.ok, SerializationType.json);
		AbstractProject project = ProjectManager.getProjectDescription(projectName);
		Collection<ProjectUserBinding> puBindings = ProjectUserBindingsManager.listPUBindingsOfProject(project);
		JSONArray usersJson = new JSONArray();
		for (ProjectUserBinding pub : puBindings) {
			if (!pub.getRoles().isEmpty()) {
				usersJson.put(UsersManager.getUserByEmail(pub.getUser().getEmail()).getAsJSONObject());
			}
		}
		jsonResp.getDataElement().put("users", usersJson);
		return jsonResp.toString();
	}
	
	/**
	 * Registers a new user
	 * @param email this will be used as id
	 * @param password password not encoded, it is encoded here
	 * @param givenName
	 * @param familyName
	 * @param birthday
	 * @param gender
	 * @param country
	 * @param address
	 * @param affiliation
	 * @param url
	 * @param phone
	 * @return
	 * @throws MessagingException 
	 * @throws ProjectAccessException 
	 * @throws UserCreationException 
	 * @throws ParseException 
	 * @throws PUBindingException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/registerUser", 
			method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public String registerUser(
			@RequestParam("email") String email, @RequestParam("password") String password,
			@RequestParam("givenName") String givenName, @RequestParam("familyName") String familyName,
			@RequestParam(value = "birthday", required = false) String birthday,
			@RequestParam(value = "gender", required = false) String gender,
			@RequestParam(value = "country", required = false) String country,
			@RequestParam(value = "address", required = false) String address,
			@RequestParam(value = "affiliation", required = false) String affiliation,
			@RequestParam(value = "url", required = false) String url,
			@RequestParam(value = "phone", required = false) String phone) throws ProjectAccessException, 
				UserCreationException, ParseException, PUBindingException {
		try {
			STUser user = new STUser(email, password, givenName, familyName);
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
			UsersManager.registerUser(user);
			ProjectUserBindingsManager.createPUBindingsOfUser(user);
			
			try {
				EmailSender.sendRegistrationMailToUser(email, givenName, familyName);
				EmailSender.sendRegistrationMailToAdmin(email, givenName, familyName);
			} catch (UnsupportedEncodingException | MessagingException e) {
				logger.error(Utilities.printFullStackTrace(e));
				e.printStackTrace();
			}
			
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
	 * @param givenName
	 * @return
	 * @throws JSONException 
	 * @throws IOException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserGivenName", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserGivenName(@RequestParam("email") String email, @RequestParam("givenName") String givenName) throws JSONException, IOException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserGivenName(user, givenName);
		updateUserInSecurityContext(user);
		
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("updateUserGivenName", RepliesStatus.ok, SerializationType.json);
		jsonResp.getDataElement().put("user", user.getAsJSONObject());
		return jsonResp.toString();
	}
	
	/**
	 * Update last name of the given user.
	 * @param email
	 * @param familyName
	 * @return
	 * @throws JSONException 
	 * @throws IOException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserFamilyName", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserFamilyName(@RequestParam("email") String email, @RequestParam("familyName") String familyName) throws JSONException, IOException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserFamilyName(user, familyName);
		updateUserInSecurityContext(user);
		
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("updateUserFamilyName", RepliesStatus.ok, SerializationType.json);
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
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserPhone(user, phone);
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
	 * @throws ParseException 
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/updateUserBirthday", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String updateUserBirthday(@RequestParam("email") String email, @RequestParam("birthday") String birthday) throws JSONException, IOException, ParseException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserBirthday(user, birthday);
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
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserGender(user, gender);
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
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserCountry(user, country);
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
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserAddress(user, address);
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
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserAffiliation(user, affiliation);
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
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserUrl(user, url);
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
	 * @throws PUBindingException 
	 */
	//TODO move to Administration?
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/enableUser", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	@PreAuthorize("@auth.isAuthorized('um(user)', 'C')")
	public String enableUser(@RequestParam("email") String email, @RequestParam("enabled") boolean enabled)
			throws IOException, JSONException, PUBindingException  {
		if (UsersManager.getLoggedUser().getEmail().equals(email)) {
			throw new PUBindingException("Cannot disable current logged user");
		}
		STUser user = UsersManager.getUserByEmail(email);
		if (enabled) {
			user = UsersManager.updateUserStatus(user, UserStatus.ACTIVE);
		} else {
			user = UsersManager.updateUserStatus(user, UserStatus.INACTIVE);
		}
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("enableUser", RepliesStatus.ok, SerializationType.json);
		jsonResp.getDataElement().put("user", user.getAsJSONObject());
		return jsonResp.toString();
	}
	
	/**
	 * Deletes an user
	 * @param email
	 * @throws Exception 
	 */
	//TODO move to Administration?
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/deleteUser", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	@PreAuthorize("@auth.isAuthorized('um(user)', 'D')")
	public String deleteUser(@RequestParam("email") String email) throws Exception {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new IllegalArgumentException("User with email " + email + " doesn't exist");
		}
		if (UsersManager.getLoggedUser().getEmail().equals(email)) {
			throw new Exception("A user cannot delete himself"); //TODO create a more specific exception
		}
		UsersManager.deleteUser(email);
		ProjectUserBindingsManager.deletePUBindingsOfUser(user);
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
		 * @param givenName
		 * @param familyName
		 * @throws MessagingException
		 * @throws UnsupportedEncodingException
		 */
		public static void sendRegistrationMailToUser(String toEmail, String givenName, String familyName) throws MessagingException, UnsupportedEncodingException {
			String text = "Dear " + givenName + " " + familyName + ","
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
		public static void sendRegistrationMailToAdmin(String userEmail, String userGivenName, String userFamilyName) throws UnsupportedEncodingException, MessagingException {
			String text = "Dear VocBench administrator,"
					+ "\nthere is a new user registration request for VocBench."
					+ "\nGiven Name: " + userGivenName
					+ "\nFamily Name: " + userFamilyName
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
