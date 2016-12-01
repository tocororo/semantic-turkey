package it.uniroma2.art.semanticturkey.security;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.user.AccessContolUtils;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserCreationException;
import it.uniroma2.art.semanticturkey.user.UserStatus;
import it.uniroma2.art.semanticturkey.user.UsersRepoHelper;

@Component("usersMgr")
@DependsOn("acRepoHolder")
public class UsersManager {
	
	private UsersRepoHelper userRepoHelper;
	
	@Autowired 
	public UsersManager(AccessControlRepositoryHolder acRepoHolder) {
		userRepoHelper = new UsersRepoHelper(acRepoHolder.getRepository());
	}
	
	/**
	 * Loads all the users into the repository
	 * @throws RDFParseException
	 * @throws RepositoryException
	 * @throws IOException
	 */
	public void loadUsers() throws RDFParseException, RepositoryException, IOException {
		Collection<File> userDetailsFolders = AccessContolUtils.getAllUserDetailsFiles();
		for (File f : userDetailsFolders) {
			userRepoHelper.loadUserDetails(f);
		}
	}
	
	/**
	 * Registers a user 
	 * @param email
	 * @param password
	 * @param firstName
	 * @param lastName
	 * @throws UserCreationException
	 * @throws IOException 
	 */
	public void registerUser(STUser user) throws UserCreationException, IOException {
		if (!isEmailAvailable(user.getEmail())) {
			throw new UserCreationException("E-mail address " + user.getEmail() + " already used by another user");
		} else {
			user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword())); //encode password
			userRepoHelper.insertUser(user); // insert user in the repo
			createOrUpdateUserDetailsFolder(user); // serialize user detials
		}
	}
	
	/**
	 * Returns a list of all the registered users
	 * @return
	 */
	public Collection<STUser> listUsers() {
		return userRepoHelper.listUsers();
	}
	
	/**
	 * Checks if a user with the same e-mail address is already present. Useful during registration procedure.
	 * @param email
	 * @return
	 */
	public boolean isEmailAvailable(String email) {
		Map<String, String> filter = new HashMap<String, String>();
		filter.put(UsersRepoHelper.BINDING_EMAIL, email);
		Collection<STUser> users = userRepoHelper.searchUsers(filter);
		return users.isEmpty();
	}
	
	/**
	 * Searches and returns a list of users that respect the filters
	 * @param filters Map of key value where the key is the field that the user should have and the 
	 * value is the value of that field
	 * @return
	 */
	public Collection<STUser> searchUsers(Map<String, String> filters) {
		return userRepoHelper.searchUsers(filters);
	}
	
	/**
	 * Returns the user with the given email. Useful during the login procedure
	 * @param email
	 * @return
	 */
	public STUser getUserByEmail(String email) {
		STUser user = null;
		Map<String, String> filters = new HashMap<String, String>();
		filters.put(UsersRepoHelper.BINDING_EMAIL, email);
		Collection<STUser> users = userRepoHelper.searchUsers(filters);
		if (!users.isEmpty()) {
			user = users.iterator().next();
		}
		return user;
	}
	
	/**
	 * Returns the users with the given status. Useful to get the users waiting for enabling after registration
	 * @param status
	 * @return
	 */
	public Collection<STUser> listUsersByStatus(UserStatus status) {
		Map<String, String> filters = new HashMap<String, String>();
		filters.put(UsersRepoHelper.BINDING_STATUS, status.name());
		Collection<STUser> users = userRepoHelper.searchUsers(filters);
		return users;
	}
	
	/**
	 * Delete the given user. 
	 * Note, since e-mail should be unique, delete the user with the same e-mail of the given user.
	 * @param email
	 * @throws IOException 
	 */
	public void deleteUser(String email) throws IOException {
		//remove user from repository
		userRepoHelper.deleteUser(email);
		//and delete its folder from server data
		FileUtils.deleteDirectory(AccessContolUtils.getUserFolder(email));
	}
	
	/**
	 * Updates the first name of the given user and returns it updated
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException 
	 */
	public STUser updateUserFirstName(STUser user, String newValue) throws IOException {
		userRepoHelper.updateUserInfo(user.getEmail(), UsersRepoHelper.BINDING_FIRST_NAME, newValue);
		user.setFirstName(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}
	
	/**
	 * Updates the last name of the given user and returns it updated
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException 
	 */
	public STUser updateUserLastName(STUser user, String newValue) throws IOException {
		userRepoHelper.updateUserInfo(user.getEmail(), UsersRepoHelper.BINDING_LAST_NAME, newValue);
		user.setLastName(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}
	
	/**
	 * Updates the phone number of the given user and returns it updated
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException 
	 */
	public STUser updateUserPhone(STUser user, String newValue) throws IOException {
		userRepoHelper.updateUserInfo(user.getEmail(), UsersRepoHelper.BINDING_PHONE, newValue);
		user.setPhone(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}
	
	/**
	 * Updates the last name of the given user and returns it updated
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException 
	 */
	public STUser updateUserBirthday(STUser user, String newValue) throws IOException {
		userRepoHelper.updateUserInfo(user.getEmail(), UsersRepoHelper.BINDING_BIRTHDAY, newValue);
		user.setBirthday(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}
	
	/**
	 * Updates the gender of the given user and returns it updated
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException 
	 */
	public STUser updateUserGender(STUser user, String newValue) throws IOException {
		userRepoHelper.updateUserInfo(user.getEmail(), UsersRepoHelper.BINDING_GENDER, newValue);
		user.setGender(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}
	
	/**
	 * Updates the country of the given user and returns it updated
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException 
	 */
	public STUser updateUserCountry(STUser user, String newValue) throws IOException {
		userRepoHelper.updateUserInfo(user.getEmail(), UsersRepoHelper.BINDING_COUNTRY, newValue);
		user.setCountry(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}
	
	/**
	 * Updates the address of the given user and returns it updated
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException 
	 */
	public STUser updateUserAddress(STUser user, String newValue) throws IOException {
		userRepoHelper.updateUserInfo(user.getEmail(), UsersRepoHelper.BINDING_ADDRESS, newValue);
		user.setAddress(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}
	
	/**
	 * Updates the affiliation of the given user and returns it updated
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException 
	 */
	public STUser updateUserAffiliation(STUser user, String newValue) throws IOException {
		userRepoHelper.updateUserInfo(user.getEmail(), UsersRepoHelper.BINDING_AFFILIATION, newValue);
		user.setAffiliation(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}
	
	/**
	 * Updates the url of the given user and returns it updated
	 * @param user
	 * @param newValue
	 * @return
	 * @throws IOException 
	 */
	public STUser updateUserUrl(STUser user, String newValue) throws IOException {
		userRepoHelper.updateUserInfo(user.getEmail(), UsersRepoHelper.BINDING_URL, newValue);
		user.setUrl(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}
	
	/**
	 * Updates the status of the given user and returns it update
	 * @param user
	 * @param newStatus
	 * @return
	 * @throws IOException
	 */
	public STUser updateUserStatus(STUser user, UserStatus newStatus) throws IOException {
		userRepoHelper.updateUserInfo(user.getEmail(), UsersRepoHelper.BINDING_STATUS, newStatus.name());
		user.setStatus(UserStatus.ENABLED);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}
	
	//Utility methods to manage files
	
	/**
	 * Creates a folder for the given user and serializes the details about the user in a file.
	 * If the folder is already created, simply update the info in the user details file.
	 * @param user
	 * @throws IOException 
	 */
	private void createOrUpdateUserDetailsFolder(STUser user) throws IOException {
		//creates a temporary not persistent repository
		MemoryStore memStore = new MemoryStore();
		memStore.setPersist(false);
		SailRepository tempRepo = new SailRepository(memStore);
		tempRepo.initialize();
		
		UsersRepoHelper tempUserRepoHelper = new UsersRepoHelper(tempRepo);
		tempUserRepoHelper.insertUser(user);
		tempUserRepoHelper.saveUserDetailsFile(AccessContolUtils.getUserDetailsFile(user.getEmail()));
		tempUserRepoHelper.shutDownRepository();
	}
	
}
