package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SecureRandom;
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

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.resources.Config;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.PUBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UserStatus;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.Utilities;

@STService
public class Users extends STServiceAdapter {
	
	private static Logger logger = LoggerFactory.getLogger(Users.class);
	
	/**
	 * If there are no registered users return an empty response;
	 * If there is a user logged, returns a user object that is a json representation of the logged user.
	 * If there is no logged user, returns an empty user object
	 */	
	@STServiceOperation
	public JsonNode getUser() {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
//		ObjectNode userNode = jsonFactory.objectNode();
//		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//		if (!(auth instanceof AnonymousAuthenticationToken)) {//if there's a user authenticated
//			STUser loggedUser = (STUser) auth.getPrincipal();
//			userNode = loggedUser.getAsJsonObject();
//		}
//		return userNode;
		ObjectNode respNode = jsonFactory.objectNode();
		ObjectNode userNode = jsonFactory.objectNode();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (!(auth instanceof AnonymousAuthenticationToken)) {//if there's a user authenticated
			STUser loggedUser = (STUser) auth.getPrincipal();
			userNode = loggedUser.getAsJsonObject();
			respNode.set("user", userNode);
		} else {
			if (!UsersManager.listUsers().isEmpty()) { //check if there is at least a user registered
				respNode.set("user", userNode); //set an empty node as "user"
			}
		}
		return respNode;
	}
	
	/**
	 * Returns all the users registered
	 * @return
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('um(user)', 'R')")
	public JsonNode listUsers() {
		Collection<STUser> users = UsersManager.listUsers();
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode userArrayNode = jsonFactory.arrayNode();
		for (STUser user : users) {
			userArrayNode.add(user.getAsJsonObject());
		}
		return userArrayNode;
	}
	
	/**
	 * Returns all the users that have at least a role in the given project
	 * @param projectName
	 * @return
	 * @throws ProjectAccessException 
	 * @throws ProjectInexistentException 
	 * @throws InvalidProjectNameException 
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('um(user, project)', 'R')")
	public JsonNode listUsersBoundToProject(String projectName)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		AbstractProject project = ProjectManager.getProjectDescription(projectName);
		Collection<ProjectUserBinding> puBindings = ProjectUserBindingsManager.listPUBindingsOfProject(project);
		
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode userArrayNode = jsonFactory.arrayNode();
		
		for (ProjectUserBinding pub : puBindings) {
			if (!pub.getRoles().isEmpty()) {
				userArrayNode.add(UsersManager.getUserByEmail(pub.getUser().getEmail()).getAsJsonObject());
			}
		}
		return userArrayNode;
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
	@STServiceOperation(method = RequestMethod.POST)
	public void registerUser(String email, String password, String givenName, String familyName, @Optional IRI iri,
			@Optional String birthday, @Optional String gender, @Optional String country, @Optional String address,
			@Optional String affiliation, @Optional String url, @Optional String phone)
					throws ProjectAccessException, UserException, ParseException, PUBindingException {
		STUser user;
		if (iri != null) {
			user = new STUser(iri, email, password, givenName, familyName);
		} else {
			user = new STUser(email, password, givenName, familyName);
		}
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
		//if this is the first registered user, it means that it is the first access, 
		//so set it as admin (TODO I'm not sure this is completely safe, check it) 
		if (UsersManager.listUsers().isEmpty()) {
			Config.setEmailAdminAddress(email);
			user.setStatus(UserStatus.ACTIVE);
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
	}
	
	/**
	 * Update first name of the given user.
	 * @param email
	 * @param givenName
	 * @return
	 * @throws IOException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public ObjectNode updateUserGivenName(String email, String givenName) throws UserException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserGivenName(user, givenName);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	/**
	 * Update last name of the given user.
	 * @param email
	 * @param familyName
	 * @return
	 * @throws IOException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public ObjectNode updateUserFamilyName(String email, String familyName) throws UserException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserFamilyName(user, familyName);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	/**
	 * Update email of the given user.
	 * @param email
	 * @param newEmail
	 * @return
	 * @throws IOException 
	 * @throws UserException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public ObjectNode updateUserEmail(String email, String newEmail) throws IOException, UserException {
		STUser user = UsersManager.getUserByEmail(email);
		//check if there is already a user that uses the newEmail
		if (UsersManager.getUserByEmail(newEmail) != null) {
			throw new UserException("Cannot update the email for the user " + email + ". The email " + newEmail + 
					" is already used by another user");
		}
		user = UsersManager.updateUserFamilyName(user, newEmail);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	/**
	 * Update first name of the given user.
	 * @param email
	 * @param phone
	 * @return
	 * @throws IOException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public ObjectNode updateUserPhone(@RequestParam("email") String email, @RequestParam("phone") String phone) throws UserException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserPhone(user, phone);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	/**
	 * Update birthday of the given user.
	 * @param email
	 * @param birthday
	 * @return
	 * @throws IOException 
	 * @throws ParseException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public ObjectNode updateUserBirthday(@RequestParam("email") String email, @RequestParam("birthday") String birthday) throws UserException, ParseException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserBirthday(user, birthday);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	/**
	 * Update gender of the given user.
	 * @param email
	 * @param gender
	 * @return
	 * @throws IOException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public ObjectNode updateUserGender(@RequestParam("email") String email, @RequestParam("gender") String gender) throws UserException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserGender(user, gender);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	/**
	 * Update country of the given user.
	 * @param email
	 * @param country
	 * @return
	 * @throws IOException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public ObjectNode updateUserCountry(@RequestParam("email") String email, @RequestParam("country") String country) throws UserException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserCountry(user, country);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	/**
	 * Update address of the given user.
	 * @param email
	 * @param address
	 * @return
	 * @throws IOException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public ObjectNode updateUserAddress(@RequestParam("email") String email, @RequestParam("address") String address) throws UserException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserAddress(user, address);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	/**
	 * Update gender of the given user.
	 * @param email
	 * @param affiliation
	 * @return
	 * @throws IOException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public ObjectNode updateUserAffiliation(@RequestParam("email") String email, @RequestParam("affiliation") String affiliation) throws UserException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserAffiliation(user, affiliation);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	/**
	 * Update url of the given user.
	 * @param email
	 * @param url
	 * @return
	 * @throws IOException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public ObjectNode updateUserUrl(@RequestParam("email") String email, @RequestParam("url") String url) throws UserException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserUrl(user, url);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	/**
	 * Enables or disables the given user
	 * @param email
	 * @param enable
	 * @return
	 * @throws IOException 
	 * @throws PUBindingException 
	 */
	//TODO move to Administration?
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('um(user)', 'C')")
	public ObjectNode enableUser(@RequestParam("email") String email, @RequestParam("enabled") boolean enabled)
			throws UserException, PUBindingException  {
		if (UsersManager.getLoggedUser().getEmail().equals(email)) {
			throw new PUBindingException("Cannot disable current logged user");
		}
		STUser user = UsersManager.getUserByEmail(email);
		if (enabled) {
			user = UsersManager.updateUserStatus(user, UserStatus.ACTIVE);
		} else {
			user = UsersManager.updateUserStatus(user, UserStatus.INACTIVE);
		}
		return user.getAsJsonObject();
	}
	
	/**
	 * Deletes an user
	 * @param email
	 * @throws Exception 
	 */
	//TODO move to Administration?
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('um(user)', 'D')")
	public void deleteUser(@RequestParam("email") String email) throws Exception {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new IllegalArgumentException("User with email " + email + " doesn't exist");
		}
		if (UsersManager.getLoggedUser().getEmail().equals(email)) {
			throw new Exception("A user cannot delete himself"); //TODO create a more specific exception
		}
		UsersManager.deleteUser(user);
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	public void forgotPassword(HttpServletRequest request, String email, String vbHostAddress) throws Exception {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new IllegalArgumentException("User with email " + email + " doesn't exist");
		}
		//generate a random token
		SecureRandom random = new SecureRandom();
		String token = new BigInteger(130, random).toString(32);
		//use the token to generate the reset password link
		String resetLink = vbHostAddress + "/#/ResetPassword/" + token;
		//try to send the e-mail containing the link. If it fails, throw an exception
		try {
			request.getSession().setAttribute("reset_password_token", email+token);
			EmailSender.sendForgotPasswordMail(user, resetLink);
		} catch (UnsupportedEncodingException | MessagingException e) {
			logger.error(Utilities.printFullStackTrace(e));
			throw new Exception("Failed to send an e-mail for resetting the password. Please contact the "
					+ "system administration at " + Config.getEmailAdminAddress());
		}
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	public void resetPassword(HttpServletRequest request, String email, String token) throws Exception {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new IllegalArgumentException("User with email " + email + " doesn't exist");
		}
		String storedToken = (String) request.getSession().getAttribute("reset_password_token");
		if (!(email + token).equals(storedToken)) {
			throw new Exception("Cannot reset password for email " + email + 
					". The time limit for resetting the password may be expired, please retry.");
		}
		
		//Reset the password and send it to the user via e-mail
		SecureRandom random = new SecureRandom();
		String tempPwd = new BigInteger(130, random).toString(32);
		tempPwd = tempPwd.substring(0, 8); //limit the pwd to 8 chars
		
		try {
			UsersManager.updateUserPassword(user, tempPwd);
			EmailSender.sendResetPasswordMail(user, tempPwd);
		} catch (IOException e) {
			throw new Exception(e);
		}
	}
	
	
	/**
	 * Updates the user stored in the security context whenever there is a change to the current logged user
	 * @param user
	 */
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
		public static void sendRegistrationMailToUser(String toEmail, String givenName, String familyName) 
				throws MessagingException, UnsupportedEncodingException {
			String text = "Dear " + givenName + " " + familyName + ","
					+ "\nthank you for registering as a user of VocBench 3."
					+ " Your request has been received. Please wait for the administrator to approve it."
					+ " After approval, you can log into VocBench with the e-mail " + toEmail + " and your chosen password."
					+ "\nThanks for your interest."
					+ "\nIf you want to unregister, please send an email with your e-mail address and the subject:"
					+ " 'VocBench - Unregister' to " + Config.getEmailAdminAddress() + "."
					+ "\nRegards,\nThe VocBench Team.";
			sendMail(toEmail, "VocBench registration", text);
		}
		
		/**
		 * Sends an email to the system administrator to inform about a new user registration request
		 * @throws MessagingException 
		 * @throws UnsupportedEncodingException 
		 */
		public static void sendRegistrationMailToAdmin(String userEmail, String userGivenName, String userFamilyName)
				throws UnsupportedEncodingException, MessagingException {
			String text = "Dear VocBench administrator,"
					+ "\nthere is a new user registration request for VocBench."
					+ "\nGiven Name: " + userGivenName
					+ "\nFamily Name: " + userFamilyName
					+ "\nE-mail: " + userEmail
					+ "\nPlease activate the account.\nRegards,\nThe VocBench Team.";
			sendMail(Config.getEmailAdminAddress(), "VocBench registration", text);
		}
		
		public static void sendForgotPasswordMail(STUser user, String forgotPasswordLink)
				throws UnsupportedEncodingException, MessagingException {
			String text = "Dear " + user.getGivenName() + " " + user.getFamilyName() + ","
					+ "\nwe've received a request to reset the password for the"
					+ " VocBench account associated to this email address."
					+ "\nClick the link below to be redirected to the reset password page."
					+ " This password reset is only valid for a limited time."
					+ "\n\n" + forgotPasswordLink
					+ "\n\nIf you did not request a password reset, please ignore this email"
					+ " or report this to the system administrator."
					+ "\nRegards,\nThe VocBench team";
			sendMail(user.getEmail(), "VocBench password reset", text);
		}
		
		public static void sendResetPasswordMail(STUser user, String tempPassword)
				throws UnsupportedEncodingException, MessagingException {
			String text = "Dear " + user.getGivenName() + " " + user.getFamilyName() + ","
					+ "\nwe confirm you that your password has been reset."
					+ "\nThis is your new temporary password:"
					+ "\n\n"+ tempPassword
					+ "\n\nAfter the login we strongly recommend you to change the password."
					+ "\nRegards,\nThe VocBench team";
			sendMail(user.getEmail(), "VocBench password reset", text);
		}
		
		private static void sendMail(String toEmail, String subject, String text) throws MessagingException, UnsupportedEncodingException {
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
			
			Session session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(emailAddress, emailPassword);
				}
			});
			
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(emailAddress, "VocBench"));
			message.setSubject("VocBench3 registration");
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
			message.setText(text);
			Transport.send(message);
		}
		
	}

}
