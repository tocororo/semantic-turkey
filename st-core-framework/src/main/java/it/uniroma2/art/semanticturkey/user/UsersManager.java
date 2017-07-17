package it.uniroma2.art.semanticturkey.user;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import it.uniroma2.art.semanticturkey.resources.Resources;

public class UsersManager {
	
	private static final String USERS_DETAILS_FILE_NAME = "details.ttl";

	private static Collection<STUser> userList = new ArrayList<>();

	/**
	 * Loads all the users into the repository Protected since the load should be done just once by
	 * AccessControlManager during its initialization
	 * 
	 * @throws RDFParseException
	 * @throws RepositoryException
	 * @throws IOException
	 */
	public static void loadUsers() throws RDFParseException, RepositoryException, IOException {
		UsersRepoHelper userRepoHelper = new UsersRepoHelper();
		Collection<File> userDetailsFolders = getAllUserDetailsFiles();
		for (File f : userDetailsFolders) {
			userRepoHelper.loadUserDetails(f);
		}
		userList = userRepoHelper.listUsers();
		userRepoHelper.shutDownRepository();
	}

	/**
	 * Registers a user
	 * 
	 * @param user
	 * @throws UserException
	 * @throws IOException
	 */
	public static void registerUser(STUser user) throws UserException {
		if (getUserByIRI(user.getIRI()) != null) {
			throw new UserException("IRI " + user.getIRI().stringValue() + " already used by another user");
		}
		if (getUserByEmail(user.getEmail()) != null) {
			throw new UserException("E-mail address " + user.getEmail() + " already used by another user");
		}
		user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword())); // encode password
		user.setRegistrationDate(new Date());
		userList.add(user);
		createOrUpdateUserDetailsFolder(user); // serialize user detials
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
	 * Returns the user with the given email. Null if there is no user with the given email.
	 * 
	 * @param email
	 * @return
	 */
	public static STUser getUserByEmail(String email) {
		STUser user = null;
		for (STUser u : userList) {
			if (u.getEmail().equals(email)) {
				user = u;
			}
		}
		return user;
	}
	
	/**
	 * Returns the user with the given IRI. Null if there is no user with the given IRI.
	 * 
	 * @param iri
	 * @return
	 */
	public static STUser getUserByIRI(IRI iri) {
		STUser user = null;
		for (STUser u : userList) {
			if (u.getIRI().equals(iri)) {
				user = u;
			}
		}
		return user;
	}

	/**
	 * Delete the user with the given email
	 * 
	 * @param email
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
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserPassword(STUser user, String newPassword) throws UserException {
		String crypted = new BCryptPasswordEncoder().encode(newPassword);
		System.out.println("from " + newPassword + " to " + crypted);
		user.setPassword(crypted);
//		user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
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
	 * Updates the last name of the given user and returns it updated
	 * 
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public static STUser updateUserBirthday(STUser user, String newValue) throws UserException, ParseException {
		user.setBirthday(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}

	/**
	 * Updates the gender of the given user and returns it updated
	 * 
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserGender(STUser user, String newValue) throws UserException {
		user.setGender(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}

	/**
	 * Updates the country of the given user and returns it updated
	 * 
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	public static STUser updateUserCountry(STUser user, String newValue) throws UserException {
		user.setCountry(newValue);
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
	 * @param newStatus
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
			tempUserRepoHelper.saveUserDetailsFile(getUserDetailsFile(user));
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
	 * @param userEmail
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
		for (int i = 0; i < userDirectories.length; i++) {
			userFolders.add(new File(usersFolder + File.separator + userDirectories[i]));
		}
		return userFolders;
	}
	
	/**
	 * Returns the user details file for the given user
	 * @param userEmail
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
	
}
