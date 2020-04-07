package it.uniroma2.art.semanticturkey.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.resources.Resources;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class UsersManager {

	private static final String USERS_DETAILS_FILE_NAME = "details.ttl";
	private static final String USER_FORM_FIELDS_FILE_NAME = "fields.ttl";

	private static Collection<STUser> userList = new ArrayList<>();
	private static Set<String> adminSet = new HashSet<>();

	private static UserForm userForm;

	/**
	 * Loads all the users into the repository Protected since the load should be done just once by
	 * AccessControlManager during its initialization
	 *
	 * @throws RDFParseException
	 * @throws RepositoryException
	 * @throws IOException
	 */
	public static void loadUsers() throws RDFParseException, RepositoryException, IOException, STPropertyAccessException {
		UsersRepoHelper userRepoHelper = new UsersRepoHelper();
		Collection<File> userDetailsFolders = getAllUserDetailsFiles();
		for (File f : userDetailsFolders) {
			userRepoHelper.loadUserDetails(f);
		}
		userList = userRepoHelper.listUsers();

		//load also the custom fields of the user form
		userRepoHelper.loadUserFormFields(getUserFormFieldsFile());
		userForm = userRepoHelper.initUserForm();

		userRepoHelper.shutDownRepository();

		initAdminList();
	}

	private static void initAdminList() throws STPropertyAccessException, IOException {
		String adminEmailsSetting = STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_ADMIN_ADDRESS);
		//adminEmails could be a plain string for a single address (ST < 6.1.0) or a json serialized list => handle both cases
		adminSet = adminEmailsSetting.startsWith("[") ?
				new ObjectMapper().readValue(adminEmailsSetting, new TypeReference<Set<String>>(){}) :
				new HashSet<>(Collections.singletonList(adminEmailsSetting));
	}

	/**
	 * Registers a user
	 *
	 * @param user
	 * @throws UserException
	 * @throws IOException
	 */
	public static void registerUser(STUser user) throws UserException, ProjectAccessException {
		if (isEmailUsed(user.getEmail())) {
			throw new UserException("E-mail address " + user.getEmail() + " already used by another user");
		}
		if (isIriUsed(user.getIRI())) {
			throw new UserException("IRI " + user.getIRI().stringValue() + " already used by another user");
		}
		user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword())); // encode password
		user.setRegistrationDate(new Date());
		userList.add(user);
		createOrUpdateUserDetailsFolder(user); // serialize user detials
		ProjectUserBindingsManager.createPUBindingsOfUser(user);
	}

	/**
	 * Returns a list of all the registered users
	 *
	 * @return
	 */
	public static Collection<STUser> listUsers() {
		return userList;
	}

	/**
	 * Returns the list of the administrators' email
	 * @return
	 */
	public static Collection<String> getAdminEmailList() {
		return adminSet;
	}

	/**
	 * Returns the admin
	 * @return
	 */
	public static List<STUser> getAdminUsers() {
		List<STUser> admins = new ArrayList<>();
		for (STUser user : userList) {
			if (user.isAdmin()) {
				admins.add(user);
			}
		}
		return admins;
	}

	public static void addAdmin(STUser user) throws STPropertyUpdateException, JsonProcessingException {
		if (!user.getStatus().equals(UserStatus.ACTIVE)) {
			throw new IllegalStateException("Cannot grant administrator authority to a non-active user");
		}
		adminSet.add(user.getEmail());
		updateAdminSetting();
	}

	public static void removeAdmin(STUser user) throws STPropertyUpdateException, JsonProcessingException {
		if (adminSet.size() == 1 && adminSet.contains(user.getEmail())) {
			throw new IllegalStateException("Cannot remove the sole administrator");
		} else {
			adminSet.remove(user.getEmail());
			updateAdminSetting();
		}
	}

	private static void updateAdminSetting() throws JsonProcessingException, STPropertyUpdateException {
		ObjectMapper mapper = new ObjectMapper();
		String adminsJson = mapper.writeValueAsString(adminSet);
		STPropertiesManager.setSystemSetting(STPropertiesManager.SETTING_ADMIN_ADDRESS, adminsJson);
	}

	/**
	 * Returns the user with the given email. If no user with the given email exists, throws a UserException
	 *
	 * @param email
	 * @return
	 */
	public static STUser getUser(String email) throws UserException {
		STUser user = null;
		for (STUser u : userList) {
			if (u.getEmail().equals(email)) {
				user = u;
			}
		}
		if (user == null) {
			throw new UserException("User with email " + email + " doesn't exist");
		}
		return user;
	}

	/**
	 * Returns the user with the given IRI. If no user with the given IRI exists, throws a UserException
	 *
	 * @param iri
	 * @return
	 */
	public static STUser getUser(IRI iri) throws UserException {
		STUser user = null;
		for (STUser u : userList) {
			if (u.getIRI().equals(iri)) {
				user = u;
			}
		}
		if (user == null) {
			throw new UserException("User with IRI " + iri.stringValue() + " doesn't exist");
		}
		return user;
	}

	/**
	 * Check if the given email is already used
	 * @param email
	 * @return
	 */
	public static boolean isEmailUsed(String email) {
		for (STUser u : userList) {
			if (u.getEmail().equals(email)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the given iri is already used
	 * @param iri
	 * @return
	 */
	public static boolean isIriUsed(IRI iri) {
		for (STUser u : userList) {
			if (u.getIRI().equals(iri)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Delete the user with the given email
	 *
	 * @param user
	 * @throws IOException
	 */
	public static void deleteUser(STUser user) throws IOException {
		userList.remove(user);
		// delete its folder from server data
		FileUtils.deleteDirectory(getUserFolder(user));
		// delete the bindings
		ProjectUserBindingsManager.deletePUBindingsOfUser(user);
	}

	/**
	 * Updates the password of the given user and returns it updated
	 *
	 * @param user
	 * @param newPassword
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserPassword(STUser user, String newPassword) throws UserException {
		user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
		createOrUpdateUserDetailsFolder(user);
		return user;
	}

	/**
	 * Updates the first name of the given user and returns it updated
	 *
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserGivenName(STUser user, String newValue) throws UserException {
		user.setGivenName(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}

	/**
	 * Updates the last name of the given user and returns it updated
	 *
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserFamilyName(STUser user, String newValue) throws UserException {
		user.setFamilyName(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}

	/**
	 * Updates the email of the given user and returns it updated
	 *
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserEmail(STUser user, String newValue) throws UserException {
		user.setEmail(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}

	/**
	 * Updates the phone number of the given user and returns it updated
	 *
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserPhone(STUser user, String newValue) throws UserException {
		user.setPhone(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}

	/**
	 * Updates the address of the given user and returns it updated
	 *
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserAddress(STUser user, String newValue) throws UserException {
		user.setAddress(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}

	/**
	 * Updates the affiliation of the given user and returns it updated
	 *
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserAffiliation(STUser user, String newValue) throws UserException {
		user.setAffiliation(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}

	/**
	 * Updates the url of the given user and returns it updated
	 *
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserUrl(STUser user, String newValue) throws UserException {
		user.setUrl(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}

	/**
	 * Updates the url of the given user and returns it updated
	 *
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserAvatarUrl(STUser user, String newValue) throws UserException {
		user.setAvatarUrl(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}

	/**
	 * Updates the language proficiencies of the given user and returns it updated
	 *
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserLanguageProficiencies(STUser user, Collection<String> newValue) throws UserException {
		user.setLanguageProficiencies(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}

	/**
	 * Updates the status of the given user and returns it update
	 *
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserStatus(STUser user, UserStatus newValue) throws UserException {
		user.setStatus(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}

	/**
	 * Creates a folder for the given user and serializes the details about the user in a file. If the folder
	 * is already created, simply update the info in the user details file.
	 *
	 * @param user
	 * @throws IOException
	 */
	private static void createOrUpdateUserDetailsFolder(STUser user) throws UserException {
		try {
			// creates a temporary not persistent repository
			UsersRepoHelper tempUserRepoHelper = new UsersRepoHelper();
			tempUserRepoHelper.insertUser(user);
			tempUserRepoHelper.serializeRepoContent(getUserDetailsFile(user));
			tempUserRepoHelper.shutDownRepository();
		} catch (IOException e) {
			throw new UserException(e);
		}
	}

	/**
	 * This method should never return <code>null</code>, since if the user is not logged, the services are
	 * intercepted by the security filter. However, if for whatever reason no user is logged in, an
	 * <code>IllegalStateException</code> is thrown.
	 *
	 * @return
	 */
	public static STUser getLoggedUser() throws IllegalStateException {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (!(auth instanceof AnonymousAuthenticationToken)) {// if there's a user authenticated
			return (STUser) auth.getPrincipal();
		}

		throw new IllegalStateException("No user is logged in");
	}

	/**
	 * Returns the user folder under <STData>/users/ for the given user
	 * @param user
	 * @return
	 */
	public static File getUserFolder(STUser user) {
		return new File(Resources.getUsersDir() + File.separator + STUser.encodeUserIri(user.getIRI()));
	}

	/**
	 * Returns all the user folders under <STData>/users/
	 * @return
	 */
	public static Collection<File> getAllUserFolders() {
		Collection<File> userFolders = new ArrayList<>();
		File usersFolder = Resources.getUsersDir();
		//get all subfolder of "users" folder (one subfolder for each user)		
		String[] userDirectories = usersFolder.list(new FilenameFilter() {
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		for (String userDirectory : userDirectories) {
			userFolders.add(new File(usersFolder + File.separator + userDirectory));
		}
		return userFolders;
	}

	/**
	 * Returns the user details file for the given user
	 * @param user
	 * @return
	 */
	private static File getUserDetailsFile(STUser user) {
		File userFolder = new File(Resources.getUsersDir() + File.separator + STUser.encodeUserIri(user.getIRI()));
		if (!userFolder.exists()) {
			userFolder.mkdir();
		}
		return new File(userFolder + File.separator + USERS_DETAILS_FILE_NAME);
	}

	/**
	 * Returns the user details files for all the users
	 * @return
	 */
	private static Collection<File> getAllUserDetailsFiles() {
		Collection<File> userDetailsFolders = new ArrayList<>();
		Collection<File> userFolders = getAllUserFolders();
		for (File f : userFolders) {
			userDetailsFolders.add(new File(f + File.separator + USERS_DETAILS_FILE_NAME));
		}
		return userDetailsFolders;
	}

	/*
	 * User Form fields methods
	 */

	public static UserForm getUserForm() {
		return userForm;
	}

	public static void setUserFormOptionalFieldVisibility(IRI fieldIri, boolean visibility) throws UserException {
		userForm.setOptionalFieldVisibility(fieldIri, visibility);
		updateUserFormFieldsFile();
	}

	public static void addUserFormCustomField(String field) throws UserException {
		IRI p = userForm.getFirstAvailableProperty();
		if (p == null) { //this should never happen, the UI should prevent to add new fields when there are no more fields available 
			throw new IllegalStateException("Cannot add a field, the form is already filled");
		}
		userForm.addField(new UserFormCustomField(p, userForm.getOrderedCustomFields().size(), field));
		updateUserFormFieldsFile();
	}
	public static void renameUserFormCustomField(IRI fieldIri, String newLabel) throws UserException {
		UserFormCustomField field = userForm.getCustomField(fieldIri);
		field.setLabel(newLabel);
		updateUserFormFieldsFile();
	}
	public static void removeUserFormCustomField(IRI field) throws UserException {
		userForm.removeCustomField(field);
		removeCustomPropertyFromUsers(field);
		updateUserFormFieldsFile();
	}
	public static void swapUserFormCustomField(IRI field1, IRI field2) throws UserException {
		UserFormCustomField f1 = userForm.getCustomField(field1);
		int pos1 = f1.getPosition();
		UserFormCustomField f2 = userForm.getCustomField(field2);
		int pos2 = f2.getPosition();
		f1.setPosition(pos2);
		f2.setPosition(pos1);
		updateUserFormFieldsFile();
	}

	/**
	 * Updates the value of a custom property of the given user and returns it updated
	 *
	 * @param user
	 * @param property
	 * @param value
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserCustomProperty(STUser user, IRI property, String value) throws UserException {
		user.setCustomProperty(property, value);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}

	/**
	 * To invoke when a custom property is deleted from the form definition
	 * @param field
	 * @throws UserException
	 */
	private static void removeCustomPropertyFromUsers(IRI field) throws UserException {
		for (STUser u: userList) {
			u.removeCustomProperty(field);
			createOrUpdateUserDetailsFolder(u);
		}
	}


	private static void updateUserFormFieldsFile() throws UserException {
		try {
			// creates a temporary not persistent repository
			UsersRepoHelper tempUserRepoHelper = new UsersRepoHelper();
			for (UserFormCustomField f : userForm.getCustomFields()) {
				tempUserRepoHelper.insertUserFormCustomField(f);
			}
			for (Entry<IRI, Boolean> f: userForm.getOptionalFields().entrySet()) {
				tempUserRepoHelper.insertUserFormOptionalField(f.getKey(), f.getValue());
			}
			tempUserRepoHelper.serializeRepoContent(getUserFormFieldsFile());
			tempUserRepoHelper.shutDownRepository();
		} catch (IOException e) {
			throw new UserException(e);
		}
	}

	private static File getUserFormFieldsFile() throws IOException {
		File f = new File(Resources.getUsersDir() + File.separator + USER_FORM_FIELDS_FILE_NAME);
		if (!f.exists()) {
			f.createNewFile();
		}
		return f;
	}

}
