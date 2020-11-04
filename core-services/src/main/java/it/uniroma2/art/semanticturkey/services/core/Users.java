package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.email.EmailApplicationContext;
import it.uniroma2.art.semanticturkey.email.EmailService;
import it.uniroma2.art.semanticturkey.email.PmkiEmailService;
import it.uniroma2.art.semanticturkey.email.VbEmailService;
import it.uniroma2.art.semanticturkey.exceptions.DeniedOperationException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
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
import it.uniroma2.art.semanticturkey.user.UserFormCustomField;
import it.uniroma2.art.semanticturkey.user.UserStatus;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.Utilities;
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

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	 * @throws RBACException
	 */
	@STServiceOperation
	public JsonNode listUserCapabilities() throws RBACException {
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

		//admins are always bound to a project even if he has no role in it
		Collection<STUser> boundUsers = new ArrayList<>(UsersManager.getAdminUsers());
		//add user only if has a role in project
		for (ProjectUserBinding pub : puBindings) {
			if (!pub.getRoles().isEmpty() || !pub.getLanguages().isEmpty() || pub.getGroup() != null) {
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

	@STServiceOperation
	@PreAuthorize("@auth.isAdmin()")
	public List<String> listProjectsBoundToUser(IRI userIri) throws ProjectAccessException, UserException {
		List<String> listProj = new ArrayList<>();
		STUser user = UsersManager.getUser(userIri);
		Collection<AbstractProject> projects = ProjectManager.listProjects();
		for (AbstractProject absProj : projects) {
			if (absProj instanceof Project) {
				Project project = (Project) absProj;
				if (ProjectUserBindingsManager.hasUserAccessToProject(user, project)) {
					listProj.add(project.getName());
				}
			}
		}
		return listProj;
	}
	
	/**
	 * Registers a new user
	 * @param email this will be used as id
	 * @param password password not encoded, it is encoded here
	 * @param givenName
	 * @param familyName
	 * @param address
	 * @param affiliation
	 * @param url
	 * @param phone
	 * @return
	 * @throws MessagingException 
	 * @throws ProjectAccessException 
	 * @throws ParseException
	 * @throws ProjectBindingException 
	 * @throws STPropertyUpdateException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void registerUser(String email, String password, String givenName, String familyName, @Optional IRI iri,
			@Optional String address, @Optional String affiliation, @Optional String url, @Optional String avatarUrl,
			@Optional String phone, @Optional Collection<String> languageProficiencies, @Optional Map<IRI, String> customProperties,
			@Optional (defaultValue = "true") boolean sendNotification)
			throws ProjectAccessException, UserException, STPropertyUpdateException, JsonProcessingException {
		STUser user;
		if (iri != null) {
			user = new STUser(iri, email, password, givenName, familyName);
		} else {
			user = new STUser(email, password, givenName, familyName);
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
		if (customProperties != null) {
			for (Entry<IRI, String> entry : customProperties.entrySet()) {
				user.setCustomProperty(entry.getKey(), entry.getValue());
			}
		}
		
		if (UsersManager.listUsers().isEmpty()) {
			//if this is the first registered user, it means that it is the first access, so activate it and set it as admin
			user.setStatus(UserStatus.ACTIVE);
			UsersManager.registerUser(user);
			UsersManager.addAdmin(user);
		} else {
			//otherwise activate it and send the email notifications
			UsersManager.registerUser(user);
			if (sendNotification) {
				try {
					VbEmailService vbEmailService = new VbEmailService();
					vbEmailService.sendRegistrationMailToUser(user);
					vbEmailService.sendRegistrationMailToAdmin(user);
				} catch (UnsupportedEncodingException | MessagingException | STPropertyAccessException e) {
					logger.error(Utilities.printFullStackTrace(e));
				}
			}
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
		STUser user = UsersManager.getUser(email);
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
		STUser user = UsersManager.getUser(email);
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
	public ObjectNode updateUserEmail(String email, String newEmail) throws UserException, STPropertyUpdateException, JsonProcessingException {
		STUser user = UsersManager.getUser(email);
		//check if there is already a user that uses the newEmail
		if (UsersManager.isEmailUsed(newEmail)) {
			throw new UserException("Cannot update the email for the user " + email + ". The email " + newEmail + 
					" is already used by another user");
		}
		user = UsersManager.updateUserEmail(user, newEmail);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}

	/**
	 * Update the phone of the given user.
	 * @param email email of the user
	 * @param phone phone number
	 * @return
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
	public ObjectNode updateUserPhone(String email, @Optional String phone) throws UserException {
		STUser user = UsersManager.getUser(email);
		user = UsersManager.updateUserPhone(user, phone);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	/**
	 * Update address of the given user.
	 * @param email email of the user
	 * @param address address
	 * @return
	 * @throws IOException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
	public ObjectNode updateUserAddress(String email, @Optional String address) throws UserException {
		STUser user = UsersManager.getUser(email);
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
		STUser user = UsersManager.getUser(email);
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
		STUser user = UsersManager.getUser(email);
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
		STUser user = UsersManager.getUser(email);
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
		STUser user = UsersManager.getUser(email);
		user = UsersManager.updateUserLanguageProficiencies(user, languageProficiencies);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
	}
	
	/**
	 * Enables or disables the given user
	 * @param email
	 * @param enabled
	 * @return
	 * @throws IOException 
	 * @throws ProjectBindingException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('um(user)', 'C')")
	public ObjectNode enableUser(String email, boolean enabled, @Optional (defaultValue = "true") boolean sendNotification)
			throws UserException, ProjectBindingException {
		if (UsersManager.getLoggedUser().getEmail().equals(email)) {
			throw new ProjectBindingException("Cannot disable current logged user");
		}
		STUser user = UsersManager.getUser(email);
		if (enabled) {
			user = UsersManager.updateUserStatus(user, UserStatus.ACTIVE);
			if (sendNotification) {
				try {
					new VbEmailService().sendEnabledMailToUser(user);
				} catch (UnsupportedEncodingException | MessagingException | STPropertyAccessException e) {
					logger.error(Utilities.printFullStackTrace(e));
				}
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
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('um(user)', 'D')")
	public void deleteUser(@RequestParam("email") String email) throws Exception {
		STUser user = UsersManager.getUser(email);
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
		STUser user = UsersManager.getUser(email);
		if (user == null) {
			throw new IllegalArgumentException("User with email " + email + " doesn't exist");
		}
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		if (passwordEncoder.matches(oldPassword, user.getPassword())) {
			UsersManager.updateUserPassword(user, newPassword);
		} else {
			throw new DeniedOperationException("Old password is wrong");
		}
	}
	
	/**
	 * Allows the admin to force the password
	 * @param email
	 * @param password
	 * @throws Exception
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void forcePassword(String email, String password) throws Exception {
		STUser user = UsersManager.getUser(email);
		if (user == null) {
			throw new IllegalArgumentException("User with email " + email + " doesn't exist");
		}
		UsersManager.updateUserPassword(user, password);
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	public void forgotPassword(HttpServletRequest request, String email, String vbHostAddress, @Optional EmailApplicationContext appCtx) throws Exception {
		STUser user = UsersManager.getUser(email);
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
			EmailService emailService = (appCtx == EmailApplicationContext.PMKI) ?  new PmkiEmailService() : new VbEmailService();
			emailService.sendResetPasswordRequestedMail(user, resetLink);
		} catch (UnsupportedEncodingException | MessagingException e) {
			logger.error(Utilities.printFullStackTrace(e));
			Collection<String> adminEmails = UsersManager.getAdminEmailList();
			String adminEmailsMsg = (adminEmails.size() == 1) ? adminEmails.iterator().next() :
					" one of the following address: " + String.join(", ", adminEmails);
			throw new Exception("Failed to send an e-mail for resetting the password. Please contact the "
					+ "system administration at " + adminEmailsMsg);
		}
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	public void resetPassword(HttpServletRequest request, String email, String token, @Optional EmailApplicationContext appCtx) throws Exception {
		STUser user = UsersManager.getUser(email);
		if (user == null) {
			throw new IllegalArgumentException("User with email " + email + " doesn't exist");
		}
		String storedToken = (String) request.getSession().getAttribute("reset_password_token");
		if (!(email + token).equals(storedToken)) {
			throw new Exception("Cannot reset password for email " + email + 
					". The time limit for resetting the password might be expired, please retry.");
		}
		
		//Reset the password and send it to the user via e-mail
		SecureRandom random = new SecureRandom();
		String tempPwd = new BigInteger(130, random).toString(32);
		tempPwd = tempPwd.substring(0, 8); //limit the pwd to 8 chars
		
		try {
			UsersManager.updateUserPassword(user, tempPwd);
			EmailService emailService = (appCtx == EmailApplicationContext.PMKI) ?  new PmkiEmailService() : new VbEmailService();
			emailService.sendResetPasswordConfirmedMail(user, tempPwd);
		} catch (IOException e) {
			throw new Exception(e);
		}
	}
	
	/*
	 * Custom user form field management
	 */

	/**
	 * Returns the optional and the custom fields of the user form
	 * @return
	 */
	@STServiceOperation
	public JsonNode getUserFormFields() {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

		ObjectNode respJson = jsonFactory.objectNode();

		//optional fields
		ArrayNode optionalFieldsJson = jsonFactory.arrayNode();
		for (Entry<IRI, Boolean> f: UsersManager.getUserForm().getOptionalFields().entrySet()) {
			ObjectNode fieldJson = jsonFactory.objectNode();
			fieldJson.set("iri", jsonFactory.textNode(f.getKey().stringValue()));
			fieldJson.set("visible", jsonFactory.booleanNode(f.getValue()));
			optionalFieldsJson.add(fieldJson);
		}
		respJson.set("optionalFields", optionalFieldsJson);

		//custom fields
		ArrayNode customFieldsJson = jsonFactory.arrayNode();
		for (UserFormCustomField field: UsersManager.getUserForm().getOrderedCustomFields()) {
			ObjectNode fieldJson = jsonFactory.objectNode();
			fieldJson.set("iri", jsonFactory.textNode(field.getIri().stringValue()));
			fieldJson.set("label", jsonFactory.textNode(field.getLabel()));
//			fieldJson.set("position", jsonFactory.numberNode(field.getPosition()));
			customFieldsJson.add(fieldJson);
		}
		respJson.set("customFields", customFieldsJson);
		return respJson;
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void updateUserFormOptionalFieldVisibility(IRI field, boolean visibility) throws UserException {
		UsersManager.setUserFormOptionalFieldVisibility(field, visibility);
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void addUserFormCustomField(String field) throws UserException {
		UsersManager.addUserFormCustomField(field);
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void renameUserFormCustomField(IRI fieldIri, String newLabel) throws UserException {
		UsersManager.renameUserFormCustomField(fieldIri, newLabel);
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void swapUserFormCustomFields(IRI field1, IRI field2) throws UserException {
		UsersManager.swapUserFormCustomField(field1, field2);
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void removeUserFormCustomField(IRI field) throws UserException {
		UsersManager.removeUserFormCustomField(field);
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
	public ObjectNode updateUserCustomField(String email, IRI property, @Optional String value) throws UserException {
		STUser user = UsersManager.getUser(email);
		user = UsersManager.updateUserCustomProperty(user, property, value);
		updateUserInSecurityContext(user);
		return user.getAsJsonObject();
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
