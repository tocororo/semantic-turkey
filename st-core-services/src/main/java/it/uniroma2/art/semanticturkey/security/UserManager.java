package it.uniroma2.art.semanticturkey.security;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.exceptions.UserCreationException;
import it.uniroma2.art.semanticturkey.resources.Config;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserRepoHelper;
import it.uniroma2.art.semanticturkey.user.UserRolesEnum;

@Component
public class UserManager {
	
	private static final String USERS_FOLDER_NAME = "users";
	private static final String USERS_DETAILS_FILE_NAME = "details.ttl";
	
	private UserRepoHelper usersRepository;
	private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	
	public UserManager() throws RDFParseException, RepositoryException, IOException, UserCreationException {
		usersRepository = new UserRepoHelper();
		File usersFolder = getUsersFolder();
		if (!usersFolder.exists()) {
			initializeUsersFileStructure();
		}
		initUserRepository();
	}
	
	/**
	 * Adds a user.
	 * @param email
	 * @param password
	 * @param firstName
	 * @param lastName
	 * @throws UserCreationException
	 */
	public void registerUser(String email, String password, String firstName, String lastName,
			String birthday, String gender, String country, String address, String affiliation,
			String url, String phone) throws UserCreationException {
		try {
			if (!isEmailAvailable(email)) {
				throw new UserCreationException("E-mail address " + email + " already used by another user");
			} else {
				STUser newUser = new STUser(email, passwordEncoder.encode(password), firstName, lastName);
				if (birthday != null) {
					newUser.setBirthday(birthday);
				}
				if (gender != null) {
					newUser.setGender(gender);
				}
				if (country != null) {
					newUser.setCountry(country);
				}
				if (address != null) {
					newUser.setAddress(address);
				}
				if (affiliation != null) {
					newUser.setAffiliation(affiliation);
				}
				if (url != null) {
					newUser.setUrl(url);
				}
				if (phone != null) {
					newUser.setPhone(phone);
				}
				newUser.addAuthority(new SimpleGrantedAuthority(UserRolesEnum.ROLE_USER.name()));
				usersRepository.insertUser(newUser);
				createUserDetailsFolder(newUser);
			}
		} catch (ParseException e) {
			throw new UserCreationException(e);
		}
	}
	
	/**
	 * Returns a list of all the registered users
	 * @return
	 * @throws ParseException
	 */
	public List<STUser> listUsers() throws ParseException {
		return usersRepository.listUsers();
	}
	
	/**
	 * Checks if a user with the same e-mail address is already present. Useful during registration procedure.
	 * @param email
	 * @return
	 * @throws ParseException 
	 */
	public boolean isEmailAvailable(String email) throws ParseException {
		Map<String, String> filter = new HashMap<String, String>();
		filter.put(UserRepoHelper.BINDING_EMAIL, email);
		List<STUser> users = usersRepository.searchUsers(filter);
		return users.isEmpty();
	}
	
	/**
	 * Searches and returns a list of users that respect the filters
	 * @param filters Map of key value where the key is the field that the user should have and the 
	 * value is the value of that field
	 * @return
	 * @throws ParseException
	 */
	public List<STUser> searchUsers(Map<String, String> filters) throws ParseException {
		return usersRepository.searchUsers(filters);
	}
	
	/**
	 * Returns the user with the given email. Useful during the login procedure
	 * @param email
	 * @return
	 * @throws ParseException
	 */
	public STUser getUserByEmail(String email) throws ParseException {
		STUser user = null;
		Map<String, String> filters = new HashMap<String, String>();
		filters.put(UserRepoHelper.BINDING_EMAIL, email);
		List<STUser> users = usersRepository.searchUsers(filters);
		if (!users.isEmpty()) {
			user = users.get(0);
		}
		return user;
	}
	
	/**
	 * Delete the given user. 
	 * Note, since e-mail should be unique, delete the user with the same e-mail of the given user.
	 * @param user
	 * @throws IOException 
	 */
	public void deleteUser(STUser user) throws IOException {
		//remove user from repository
		usersRepository.deleteUser(user);
		//and delete its folder from server data
		deleteUserDetailsFolder(user);
	}
	
	private void initUserRepository() throws RDFParseException, RepositoryException, IOException {
		File usersFolder = getUsersFolder();
		//get all subfolder of "users" folder (one subfolder for each user) and load user in repository		
		String[] userDirectories = usersFolder.list(new FilenameFilter() {
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		for (int i = 0; i < userDirectories.length; i++) {
			// users/<userFolder>/details.ttl
			File userDetailsFile = new File(usersFolder + File.separator + userDirectories[i] + File.separator + USERS_DETAILS_FILE_NAME);
			usersRepository.loadUserDetails(userDetailsFile);
		}
	}
	
	private void initializeUsersFileStructure() throws UserCreationException{
		File usersFolder = getUsersFolder();
		usersFolder.mkdir();
		
		//create and register admin user
		STUser admin = new STUser("admin@admin.com", passwordEncoder.encode("admin"), "Admin", "Admin");
		admin.addAuthority(new SimpleGrantedAuthority(UserRolesEnum.ROLE_ADMIN.name()));
		admin.addAuthority(new SimpleGrantedAuthority(UserRolesEnum.ROLE_USER.name()));
		usersRepository.insertUser(admin);
		createUserDetailsFolder(admin);
	}
	
	private void createUserDetailsFolder(STUser user) {
		UserRepoHelper newUserTempRepo = new UserRepoHelper();
		newUserTempRepo.insertUser(user);
		newUserTempRepo.saveUserDetailsFile(getUserDetailsFile(user));
	}
	
	private void deleteUserDetailsFolder(STUser user) throws IOException {
		FileUtils.deleteDirectory(getUserDetailsFile(user));
	}
	
	private File getUsersFolder(){
		return new File(Config.getDataDir() + File.separator + USERS_FOLDER_NAME);
	}
	
	private File getUserDetailsFile(STUser user) {
		String email = user.getEmail();
		email = email.replace("@", "AT");
		email = email.replace(".", "_");
		File userFolder = new File(getUsersFolder() + File.separator + email);
		if (!userFolder.exists()) {
			userFolder.mkdir();
		}
		return new File(userFolder + File.separator + USERS_DETAILS_FILE_NAME);
	}

}
