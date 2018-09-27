package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import it.uniroma2.art.semanticturkey.exceptions.DeniedOperationException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.ProjectBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UserStatus;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.EmailSender;
import it.uniroma2.art.semanticturkey.utilities.Utilities;

@STService
public class Users extends STServiceAdapter {
	
	private static Logger logger = LoggerFactory.getLogger(Users.class);
	
	@Autowired
	private SessionRegistry sessionRegistry;
	
	/**
	 * If there are no registered users return an empty response;
	 * If there is a user logged, returns a user object that is a json representation of the logged user.
	 * If there is no logged user, returns an empty user object
	 */	
	@STServiceOperation
	public JsonNode getUser() {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
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
		List<Object> principals = sessionRegistry.getAllPrincipals();
		Collection<STUser> users = UsersManager.listUsers();
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode userArrayNode = jsonFactory.arrayNode();
		for (STUser user : users) {
			ObjectNode userJson = user.getAsJsonObject();
			userJson.set("online", jsonFactory.booleanNode(false));
			for (Object principal : principals) {
				if (principal instanceof STUser && user.getIRI().equals(((STUser) principal).getIRI())) {
					for (SessionInformation sessionInfo : sessionRegistry.getAllSessions(principal, false)) {
						Date lastRequest = sessionInfo.getLastRequest();
						Date now = new Date();
						if (now.getTime() - lastRequest.getTime() <= 5*60*1000) { //lastRequest done less than 5 minutes ago
							userJson.set("online", jsonFactory.booleanNode(true));
						}
					}
				}
			}
			userArrayNode.add(userJson);
		}
		return userArrayNode;
	}
	
	/**
	 * Returns the capabilities of the current logged user in according the roles he has in the current project
	 * @return
	 * @throws NoMoreSolutionException 
	 * @throws NoSolutionException 
	 * @throws MalformedGoalException 
	 * @throws RBACException 
	 */
	@STServiceOperation
	public JsonNode listUserCapabilities() throws MalformedGoalException, NoSolutionException, NoMoreSolutionException, RBACException {
		ArrayNode capabilitiesJsonArray = JsonNodeFactory.instance.arrayNode();
		Project project = getProject();
		STUser user = UsersManager.getLoggedUser();
		ProjectUserBinding puBinding = ProjectUserBindingsManager.getPUBinding(user, project);
		for (Role role : puBinding.getRoles()) {
			for (String c : RBACManager.getRoleCapabilities(project, role.getName())) {
				capabilitiesJsonArray.add(c);
			}
		}
		return capabilitiesJsonArray;
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
//	@PreAuthorize("@auth.isAuthorized('um(user, project)', 'R')")
	public JsonNode listUsersBoundToProject(String projectName)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		AbstractProject project = ProjectManager.getProjectDescription(projectName);
		Collection<ProjectUserBinding> puBindings = ProjectUserBindingsManager.listPUBindingsOfProject(project);
		
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode userArrayNode = jsonFactory.arrayNode();
		
		Collection<STUser> boundUsers = new ArrayList<STUser>();
		//admin is always bound to a project even if he has no role in it
		boundUsers.add(UsersManager.getAdminUser()); 
		//add user only if has a role in project
		for (ProjectUserBinding pub : puBindings) {
			if (!pub.getRoles().isEmpty()) {
				STUser user = pub.getUser();
				if (!boundUsers.contains(user)) {
					boundUsers.add(user);
				}
			}
		}
		//serialize bound user in json response
		for (STUser u : boundUsers) {
			userArrayNode.add(u.getAsJsonObject());
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
	 * @throws ProjectBindingException 
	 * @throws STPropertyUpdateException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void registerUser(String email, String password, String givenName, String familyName, @Optional IRI iri,
			@Optional String birthday, @Optional String gender, @Optional String country, @Optional String address,
			@Optional String affiliation, @Optional String url, @Optional String avatarUrl, @Optional String phone, 
			@Optional Collection<String> languageProficiencies)
					throws ProjectAccessException, UserException, ParseException, ProjectBindingException, STPropertyUpdateException {
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
		if (avatarUrl != null) {
			user.setAvatarUrl(avatarUrl);
		}
		if (phone != null) {
			user.setPhone(phone);
		}
		if (languageProficiencies != null) {
			user.setLanguageProficiencies(languageProficiencies);
		}
		//if this is the first registered user, it means that it is the first access, so set it as admin 
		if (UsersManager.listUsers().isEmpty()) {
			STPropertiesManager.setSystemSetting(STPropertiesManager.SETTING_ADMIN_ADDRESS, email);
			user.setStatus(UserStatus.ACTIVE);
		}
		UsersManager.registerUser(user);
		ProjectUserBindingsManager.createPUBindingsOfUser(user);
		
		try {
			EmailSender.sendRegistrationMailToUser(user);
			EmailSender.sendRegistrationMailToAdmin(user);
		} catch (UnsupportedEncodingException | MessagingException | STPropertyAccessException e) {
			logger.error(Utilities.printFullStackTrace(e));
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
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
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
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
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
	 * @throws STPropertyUpdateException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
	public ObjectNode updateUserEmail(String email, String newEmail) throws UserException, STPropertyUpdateException {
		STUser user = UsersManager.getUserByEmail(email);
		//check if there is already a user that uses the newEmail
		if (UsersManager.getUserByEmail(newEmail) != null) {
			throw new UserException("Cannot update the email for the user " + email + ". The email " + newEmail + 
					" is already used by another user");
		}
		boolean wasAdmin = user.isAdmin(); 
		user = UsersManager.updateUserEmail(user, newEmail);
		if (wasAdmin) { //if user was admin, update the admin email in the configuration file
			STPropertiesManager.setSystemSetting(STPropertiesManager.SETTING_ADMIN_ADDRESS, user.getEmail());
		}
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
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
	public ObjectNode updateUserPhone(String email, @Optional String phone) throws UserException {
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
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
	public ObjectNode updateUserBirthday(String email, String birthday) throws UserException, ParseException {
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
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
	public ObjectNode updateUserGender(String email, @Optional String gender) throws UserException {
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
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
	public ObjectNode updateUserCountry(String email, String country) throws UserException {
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
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
	public ObjectNode updateUserAddress(String email, @Optional String address) throws UserException {
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
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
	public ObjectNode updateUserAffiliation(String email, @Optional String affiliation) throws UserException {
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
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
	public ObjectNode updateUserUrl(String email, @Optional String url) throws UserException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserUrl(user, url);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	/**
	 * Update avatarUrl of the given user.
	 * @param email
	 * @param avatarUrl
	 * @return
	 * @throws IOException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
	public ObjectNode updateUserAvatarUrl(String email, @Optional String avatarUrl) throws UserException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserAvatarUrl(user, avatarUrl);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	
	/**
	 * Update language proficiencies of the given user
	 * @param email
	 * @param languageProficiencies
	 * @return
	 * @throws IOException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
	public ObjectNode updateUserLanguageProficiencies(String email, Collection<String> languageProficiencies) throws UserException {
		STUser user = UsersManager.getUserByEmail(email);
		user = UsersManager.updateUserLanguageProficiencies(user, languageProficiencies);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	/**
	 * Enables or disables the given user
	 * @param email
	 * @param enable
	 * @return
	 * @throws IOException 
	 * @throws ProjectBindingException 
	 */
	//TODO move to Administration?
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('um(user)', 'C')")
	public ObjectNode enableUser(@RequestParam("email") String email, @RequestParam("enabled") boolean enabled)
			throws UserException, ProjectBindingException {
		if (UsersManager.getLoggedUser().getEmail().equals(email)) {
			throw new ProjectBindingException("Cannot disable current logged user");
		}
		STUser user = UsersManager.getUserByEmail(email);
		if (enabled) {
			user = UsersManager.updateUserStatus(user, UserStatus.ACTIVE);
			try {
				EmailSender.sendEnabledMailToUser(user);
			} catch (UnsupportedEncodingException | MessagingException | STPropertyAccessException e) {
				logger.error(Utilities.printFullStackTrace(e));
			}
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
			throw new DeniedOperationException("A user cannot delete himself");
		}
		UsersManager.deleteUser(user);
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isLoggedUser(#email)")
	public void changePassword(String email, String oldPassword, String newPassword) throws Exception {
		STUser user = UsersManager.getUserByEmail(email);
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		if (passwordEncoder.matches(oldPassword, user.getPassword())) {
			UsersManager.updateUserPassword(user, newPassword);
		} else {
			throw new DeniedOperationException("Old password is wrong");
		}
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
			String emailAdminAddress = STPropertiesManager.getSystemSetting(
					STPropertiesManager.SETTING_ADMIN_ADDRESS);
			throw new Exception("Failed to send an e-mail for resetting the password. Please contact the "
					+ "system administration at " + emailAdminAddress);
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
	
}
