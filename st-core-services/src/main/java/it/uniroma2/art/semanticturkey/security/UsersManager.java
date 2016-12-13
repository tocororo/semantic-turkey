package it.uniroma2.art.semanticturkey.security;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.user.AccessContolUtils;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserCreationException;
import it.uniroma2.art.semanticturkey.user.UserStatus;
import it.uniroma2.art.semanticturkey.user.UsersRepoHelper;

@Component("usersMgr")
public class UsersManager {
	
	private Collection<STUser> userList;
	
	public UsersManager() {
		userList = new ArrayList<>();
	}
	
	/**
	 * Loads all the users into the repository
	 * @throws RDFParseException
	 * @throws RepositoryException
	 * @throws IOException
	 */
	public void loadUsers() throws RDFParseException, RepositoryException, IOException {
		UsersRepoHelper userRepoHelper = new UsersRepoHelper();
		Collection<File> userDetailsFolders = AccessContolUtils.getAllUserDetailsFiles();
		for (File f : userDetailsFolders) {
			userRepoHelper.loadUserDetails(f);
		}
		this.userList = userRepoHelper.listUsers();
		userRepoHelper.shutDownRepository();
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
		if (getUserByEmail(user.getEmail()) != null) {
			throw new UserCreationException("E-mail address " + user.getEmail() + " already used by another user");
		} else {
			user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword())); //encode password
			user.setRegistrationDate(new Date());
			userList.add(user);
			createOrUpdateUserDetailsFolder(user); // serialize user detials
		}
	}
	
	/**
	 * Returns a list of all the registered users
	 * @return
	 */
	public Collection<STUser> listUsers() {
		return this.userList;
	}
	
	/**
	 * Returns the user with the given email. Null if there is no user with the given email.
	 * @param email
	 * @return
	 */
	public STUser getUserByEmail(String email) {
		STUser user = null;
		for (STUser u : this.userList) {
			if (u.getEmail().equals(email)) {
				user = u;
			}
		}
		return user;
	}
	
	/**
	 * Delete the user with the given email 
	 * @param email
	 * @throws IOException 
	 */
	public void deleteUser(String email) throws IOException {
		userList.remove(getUserByEmail(email));
		//delete its folder from server data
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
		user = getUserByEmail(user.getEmail());
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
		user = getUserByEmail(user.getEmail());
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
		user = getUserByEmail(user.getEmail());
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
	 * @throws ParseException 
	 */
	public STUser updateUserBirthday(STUser user, String newValue) throws IOException, ParseException {
		user = getUserByEmail(user.getEmail());
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
		user = getUserByEmail(user.getEmail());
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
		user = getUserByEmail(user.getEmail());
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
		user = getUserByEmail(user.getEmail());
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
		user = getUserByEmail(user.getEmail());
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
		user = getUserByEmail(user.getEmail());
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
	public STUser updateUserStatus(STUser user, UserStatus newValue) throws IOException {
		user = getUserByEmail(user.getEmail());
		user.setStatus(newValue);
		createOrUpdateUserDetailsFolder(user);
		return user;
	}
	
	/**
	 * Creates a folder for the given user and serializes the details about the user in a file.
	 * If the folder is already created, simply update the info in the user details file.
	 * @param user
	 * @throws IOException 
	 */
	private void createOrUpdateUserDetailsFolder(STUser user) throws IOException {
		//creates a temporary not persistent repository
		UsersRepoHelper tempUserRepoHelper = new UsersRepoHelper();
		tempUserRepoHelper.insertUser(user);
		tempUserRepoHelper.saveUserDetailsFile(AccessContolUtils.getUserDetailsFile(user.getEmail()));
		tempUserRepoHelper.shutDownRepository();
	}
	
}
