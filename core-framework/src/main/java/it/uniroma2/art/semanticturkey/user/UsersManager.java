package it.uniroma2.art.semanticturkey.user;

import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.settings.core.CoreSystemSettings;
import it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager;
import it.uniroma2.art.semanticturkey.user.notification.NotificationPreferencesAPI;
import it.uniroma2.art.semanticturkey.user.notification.UserNotificationsAPI;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class UsersManager {

    public static final String USERS_DETAILS_FILE_NAME = "details.ttl";
    private static final String USER_FORM_FIELDS_FILE_NAME = "fields.ttl";

    public static final int EMAIL_VERIFICATION_EXPIRATION_HOURS = 48;

    private static Collection<STUser> userList = new ArrayList<>();
    private static Set<String> adminSet = new HashSet<>(); //IRI of admin users
    private static Set<String> superUserSet = new HashSet<>(); //IRI of super users

    private static UserForm userForm;

    private static volatile ExtensionPointManager exptManager;

    /**
     * Loads all the users into the repository Protected since the load should be done just once by
     * AccessControlManager during its initialization
     *
     * @throws RDFParseException
     * @throws RepositoryException
     * @throws IOException
     */
    public static void loadUsers()
            throws RDFParseException, RepositoryException, IOException, STPropertyAccessException {
        UsersRepoHelper userRepoHelper = new UsersRepoHelper();
        Collection<File> userDetailsFolders = getAllUserDetailsFiles();
        for (File f : userDetailsFolders) {
            userRepoHelper.loadUserDetails(f);
        }
        userList = userRepoHelper.listUsers();

        // load also the custom fields of the user form
        userRepoHelper.loadUserFormFields(getUserFormFieldsFile());
        userForm = userRepoHelper.initUserForm();

        userRepoHelper.shutDownRepository();

        initSystemRolesLists();
    }

    private static void initSystemRolesLists() throws STPropertyAccessException {
        CoreSystemSettings systemSettings = STPropertiesManager.getSystemSettings(CoreSystemSettings.class, SemanticTurkeyCoreSettingsManager.class.getName());
        adminSet = new HashSet<>(systemSettings.adminList);
        if (systemSettings.superUserList != null) {
            superUserSet = new HashSet<>(systemSettings.superUserList);
        } else {
            superUserSet = new HashSet<>();
        }

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
            throw new EmailAlreadyUsedException(user.getEmail());
        }
        if (isIriUsed(user.getIRI())) {
            throw new IRIAlreadyUsedException(user.getIRI());
        }
        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword())); // encode password
        user.setRegistrationDate(new Date());
        userList.add(user);
        createOrUpdateUserDetailsFolder(user); // serialize user details
        ProjectUserBindingsManager.createPUBindingsOfUser(user);
    }

    /**
     * Returns a list of all the registered users
     *
     * @return
     */
    public static Collection<STUser> listUsers() {
        return userList.stream().filter(u -> !u.getStatus().equals(UserStatus.UNVERIFIED)).collect(Collectors.toList());
    }

    /**
     * Returns the list of the administrators' email
     *
     * @return
     */
    public static List<String> getAdminEmailList() {
        return getAdminUsers().stream().filter(STUser::isAdmin).map(STUser::getEmail).collect(Collectors.toList());
    }

    public static Set<String> getAdminIriSet() {
        return adminSet;
    }

    public static Set<String> getSuperUserIriSet() {
        return superUserSet;
    }

    /**
     * Returns the admin
     *
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

    public static void addAdmin(STUser user) throws STPropertyUpdateException, STPropertyAccessException {
        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            throw new IllegalArgumentException("Cannot grant administrator authority to a non-active user");
        }
        adminSet.add(user.getIRI().stringValue());
        updateAdminSetting();
    }

    public static void removeAdmin(STUser user) throws STPropertyUpdateException, STPropertyAccessException {
        if (adminSet.size() == 1 && adminSet.contains(user.getIRI().stringValue())) {
            throw new IllegalArgumentException("Cannot remove the sole administrator");
        } else {
            adminSet.remove(user.getIRI().stringValue());
            updateAdminSetting();
        }
    }

    private static void updateAdminSetting() throws STPropertyUpdateException, STPropertyAccessException {
        CoreSystemSettings systemSettings = STPropertiesManager.getSystemSettings(CoreSystemSettings.class, SemanticTurkeyCoreSettingsManager.class.getName());
        systemSettings.adminList = adminSet;
        STPropertiesManager.setSystemSettings(systemSettings, SemanticTurkeyCoreSettingsManager.class.getName());
    }

    public static List<STUser> getSuperUsers() {
        List<STUser> superUsers = new ArrayList<>();
        for (STUser user : userList) {
            if (user.isSuperUser()) {
                superUsers.add(user);
            }
        }
        return superUsers;
    }

    public static void addSuperUser(STUser user) throws STPropertyUpdateException, STPropertyAccessException {
        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            throw new IllegalArgumentException("Cannot grant superuser authority to a non-active user");
        }
        superUserSet.add(user.getIRI().stringValue());
        updateSuperUserSetting();
    }

    public static void removeSuperUser(STUser user) throws STPropertyUpdateException, STPropertyAccessException {
        superUserSet.remove(user.getIRI().stringValue());
        updateSuperUserSetting();
    }

    private static void updateSuperUserSetting() throws STPropertyUpdateException, STPropertyAccessException {
        CoreSystemSettings systemSettings = STPropertiesManager.getSystemSettings(CoreSystemSettings.class, SemanticTurkeyCoreSettingsManager.class.getName());
        systemSettings.superUserList = superUserSet;
        STPropertiesManager.setSystemSettings(systemSettings, SemanticTurkeyCoreSettingsManager.class.getName());
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
            if (u.getEmail().equalsIgnoreCase(email)) {
                user = u;
            }
        }
        if (user == null) {
            throw new UserNotFoundException(email);
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
            throw new UserNotFoundException(iri);
        }
        return user;
    }

    /**
     * Check if the given email is already used
     *
     * @param email
     * @return
     */
    public static boolean isEmailUsed(String email) {
        for (STUser u : userList) {
            if (u.getEmail().equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the given iri is already used
     *
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
    public static void deleteUser(STUser user) throws IOException, InterruptedException, STPropertyUpdateException, STPropertyAccessException {
        userList.remove(user);
        if (user.isAdmin()) { //if deleting user was an admin, remove it from admin list
            removeAdmin(user);
        }
        // delete its folder from server data
        FileUtils.deleteDirectory(getUserFolder(user));
        // delete the bindings
        ProjectUserBindingsManager.deletePUBindingsOfUser(user);
        // delete notification indexes
        NotificationPreferencesAPI.getInstance().removeUser(user);
        UserNotificationsAPI.getInstance().removeUser(user);
    }

    /**
     * Updates the password of the given user and returns it updated
     *
     * @param user
     * @param newPassword
     * @return
     * @throws IOException
     */
    public static void updateUserPassword(STUser user, String newPassword) throws UserException {
        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        createOrUpdateUserDetailsFolder(user);
    }

    /**
     * Updates the first name of the given user and returns it updated
     *
     * @param user
     * @param newValue
     * @return
     * @throws IOException
     */
    public static void updateUserGivenName(STUser user, String newValue) throws UserException {
        user.setGivenName(newValue);
        createOrUpdateUserDetailsFolder(user);
    }

    /**
     * Updates the last name of the given user and returns it updated
     *
     * @param user
     * @param newValue
     * @return
     * @throws IOException
     */
    public static void updateUserFamilyName(STUser user, String newValue) throws UserException {
        user.setFamilyName(newValue);
        createOrUpdateUserDetailsFolder(user);
    }

    /**
     * Updates the email of the given user and returns it updated
     *
     * @param user
     * @param newValue
     * @return
     * @throws IOException
     */
    public static void updateUserEmail(STUser user, String newValue) throws UserException {
        user.setEmail(newValue);
        createOrUpdateUserDetailsFolder(user);
    }

    /**
     * Updates the phone number of the given user and returns it updated
     *
     * @param user
     * @param newValue
     * @return
     * @throws IOException
     */
    public static void updateUserPhone(STUser user, String newValue) throws UserException {
        user.setPhone(newValue);
        createOrUpdateUserDetailsFolder(user);
    }

    /**
     * Updates the address of the given user and returns it updated
     *
     * @param user
     * @param newValue
     * @return
     * @throws IOException
     */
    public static void updateUserAddress(STUser user, String newValue) throws UserException {
        user.setAddress(newValue);
        createOrUpdateUserDetailsFolder(user);
    }

    /**
     * Updates the affiliation of the given user and returns it updated
     *
     * @param user
     * @param newValue
     * @return
     * @throws IOException
     */
    public static void updateUserAffiliation(STUser user, String newValue) throws UserException {
        user.setAffiliation(newValue);
        createOrUpdateUserDetailsFolder(user);
    }

    /**
     * Updates the url of the given user and returns it updated
     *
     * @param user
     * @param newValue
     * @return
     * @throws IOException
     */
    public static void updateUserUrl(STUser user, String newValue) throws UserException {
        user.setUrl(newValue);
        createOrUpdateUserDetailsFolder(user);
    }

    /**
     * Updates the url of the given user and returns it updated
     *
     * @param user
     * @param newValue
     * @return
     * @throws IOException
     */
    public static void updateUserAvatarUrl(STUser user, String newValue) throws UserException {
        user.setAvatarUrl(newValue);
        createOrUpdateUserDetailsFolder(user);
    }

    /**
     * Updates the language proficiencies of the given user and returns it updated
     *
     * @param user
     * @param newValue
     * @return
     * @throws IOException
     */
    public static void updateUserLanguageProficiencies(STUser user, Collection<String> newValue)
            throws UserException {
        user.setLanguageProficiencies(newValue);
        createOrUpdateUserDetailsFolder(user);
    }

    /**
     * Updates the status of the given user and returns it update
     *
     * @param user
     * @param newValue
     * @return
     * @throws IOException
     */
    public static void updateUserStatus(STUser user, UserStatus newValue) throws UserException {
        user.setStatus(newValue);
        createOrUpdateUserDetailsFolder(user);
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
     * Returns the logged user (if any), otherwise returns null
     *
     * @return
     */
    public static STUser getLoggedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !(auth instanceof AnonymousAuthenticationToken)) {// if there's a user authenticated
            STUser loggedUser = null;
            // In case of form login the STUser instance is stored into Auth.getPrincipal otherwise, in case of saml login, it's stored in Auth.getDetails
            if (auth.getPrincipal() instanceof STUser) {
                loggedUser = (STUser) auth.getPrincipal(); // Form login
            } else if (auth.getDetails() instanceof STUser) {
                loggedUser = (STUser) auth.getDetails(); // SAML login
            }
            return loggedUser;
        }
        return null;
    }

    /**
     * Returns the user folder under <STData>/users/ for the given user
     *
     * @param user
     * @return
     */
    public static File getUserFolder(STUser user) {
        return new File(Resources.getUsersDir() + File.separator + STUser.encodeUserIri(user.getIRI()));
    }

    /**
     * Returns all the user folders under <STData>/users/
     *
     * @return
     */
    public static Collection<File> getAllUserFolders() {
        Collection<File> userFolders = new ArrayList<>();
        File usersFolder = Resources.getUsersDir();
		if (!usersFolder.exists()) {
			usersFolder.mkdir();
		}
        // get all subfolder of "users" folder (one subfolder for each user)
        String[] userDirectories = usersFolder.list((current, name) -> new File(current, name).isDirectory());
        for (String userDirectory : userDirectories) {
            userFolders.add(new File(usersFolder + File.separator + userDirectory));
        }
        return userFolders;
    }

    /**
     * Returns the user details file for the given user
     *
     * @param user
     * @return
     */
    private static File getUserDetailsFile(STUser user) {
        File userFolder = new File(
                Resources.getUsersDir() + File.separator + STUser.encodeUserIri(user.getIRI()));
        if (!userFolder.exists()) {
            userFolder.mkdir();
        }
        return new File(userFolder + File.separator + USERS_DETAILS_FILE_NAME);
    }

    /**
     * Returns the user details files for all the users
     *
     * @return
     */
    public static Collection<File> getAllUserDetailsFiles() {
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

    public static void setUserFormOptionalFieldVisibility(IRI fieldIri, boolean visibility)
            throws UserException {
        userForm.setOptionalFieldVisibility(fieldIri, visibility);
        updateUserFormFieldsFile();
    }

    public static void addUserFormCustomField(String field, String description) throws UserException {
        IRI p = userForm.getFirstAvailableProperty();
        if (p == null) { // this should never happen, the UI should prevent to add new fields when there are
            // no more fields available
            throw new IllegalStateException("Cannot add a field, the form is already filled");
        }
        userForm.addField(new UserFormCustomField(p, userForm.getOrderedCustomFields().size(), field, description));
        updateUserFormFieldsFile();
    }

    public static void updateUserFormCustomField(IRI fieldIri, String label, String description) throws UserException {
        UserFormCustomField field = userForm.getCustomField(fieldIri);
        field.setLabel(label);
        field.setDescription(description);
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
    public static STUser updateUserCustomProperty(STUser user, IRI property, String value)
            throws UserException {
        user.setCustomProperty(property, value);
        createOrUpdateUserDetailsFolder(user);
        return user;
    }

    /**
     * To invoke when a custom property is deleted from the form definition
     *
     * @param field
     * @throws UserException
     */
    private static void removeCustomPropertyFromUsers(IRI field) throws UserException {
        for (STUser u : userList) {
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
            for (Entry<IRI, Boolean> f : userForm.getOptionalFields().entrySet()) {
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


    /*
     * Pending Users (waiting to be verified) methods
     */

    /**
     * Returns a list of users which email still need to be verified (status {@link UserStatus#UNVERIFIED})
     *
     * @return
     */
    public static Collection<STUser> listUnverifiedUsers() {
        return userList.stream().filter(u -> u.getStatus().equals(UserStatus.UNVERIFIED)).collect(Collectors.toList());
    }

    public static STUser getUnverifiedUser(String email, String token) {
        return userList.stream()
                .filter(u -> u.getStatus().equals(UserStatus.UNVERIFIED) && u.getEmail().equalsIgnoreCase(email) && u.getVerificationToken().equals(token))
                .findAny().orElse(null);
    }

    public static void verifyUser(String email, String token) throws UserException {
        STUser user = getUnverifiedUser(email, token);
        if (user != null) {
            user.setStatus(UserStatus.NEW);
            user.setVerificationToken(null);
            user.setActivationToken(new BigInteger(130, new SecureRandom()).toString(32));
            createOrUpdateUserDetailsFolder(user); // serialize user details
        } else {
            throw new EmailVerificationExpiredException(email);
        }
    }

    /**
     * Delete the user that have not been verified after 48 hours of the registration
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static void clearExpiredUnverifiedUser() throws IOException, InterruptedException, STPropertyUpdateException, STPropertyAccessException {
        long nowMs = new Date().getTime();
        Collection<STUser> unverifiedUsers = listUnverifiedUsers();
        for (STUser user : unverifiedUsers) {
            long registrationMs = user.getRegistrationDate().getTime();
            long diffHours = TimeUnit.HOURS.convert(nowMs - registrationMs, TimeUnit.MILLISECONDS);
            if (diffHours > EMAIL_VERIFICATION_EXPIRATION_HOURS) { //in the unverified user is registered more than 48h ago, delete it
                UsersManager.deleteUser(user);
            }
        }
    }

    public static void activateNewRegisteredUser(String email, String token) throws UserException {
        STUser user = userList.stream()
                .filter(u -> u.getStatus().equals(UserStatus.NEW) && u.getEmail().equalsIgnoreCase(email) && u.getActivationToken().equals(token))
                .findAny().orElse(null);
        if (user != null) {
            user.setStatus(UserStatus.ACTIVE);
            user.setActivationToken(null);
            createOrUpdateUserDetailsFolder(user); // serialize user details
        } else {
            throw new UserActivationExpiredException(email);
        }
    }
}
