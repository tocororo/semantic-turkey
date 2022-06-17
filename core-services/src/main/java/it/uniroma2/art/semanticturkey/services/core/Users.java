package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.email.EmailApplicationContext;
import it.uniroma2.art.semanticturkey.email.EmailService;
import it.uniroma2.art.semanticturkey.email.EmailServiceFactory;
import it.uniroma2.art.semanticturkey.email.VbEmailService;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.OldPasswordMismatchException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.exceptions.UserSelfDeletionException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.settings.core.CoreSystemSettings;
import it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager;
import it.uniroma2.art.semanticturkey.user.ProjectBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UpdateEmailAlreadyUsedException;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UserFormCustomField;
import it.uniroma2.art.semanticturkey.user.UserStatus;
import it.uniroma2.art.semanticturkey.user.UsersGroup;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.Utilities;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
    public JsonNode getUser() throws UserException {
        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        ObjectNode respNode = jsonFactory.objectNode();
        ObjectNode userNode = jsonFactory.objectNode();
        STUser loggedUser = UsersManager.getLoggedUser();
        if (loggedUser != null) {
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
     *
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
                        if (now.getTime() - lastRequest.getTime() <= 5 * 60 * 1000) { //lastRequest done less than 5 minutes ago
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
     *
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
     *
     * @param projectName
     * @return
     * @throws ProjectAccessException
     * @throws ProjectInexistentException
     * @throws InvalidProjectNameException
     */
    @STServiceOperation
    public JsonNode listUsersBoundToProject(String projectName, @Optional @JsonSerialized UserFilter requiredRoles,
            @Optional @JsonSerialized UserFilter requiredGroups, @Optional @JsonSerialized UserFilter requiredLanguages)
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
                if (requiredRoles != null && !requiredRoles.filters.isEmpty()) {
                    List<String> assignedRoleNames = pub.getRoles().stream().map(r -> r.getName()).collect(Collectors.toList());
                    if (requiredRoles.and) { //AND => all required roles must be assigned
                        if (!assignedRoleNames.containsAll(requiredRoles.filters)) {
                            continue;
                        }
                    } else { //OR => it's enough just one required role
                        if (requiredRoles.filters.stream().noneMatch(r -> assignedRoleNames.contains(r))) {
                            //user has none of the required roles => skip it
                            continue;
                        }
                    }
                }
                if (requiredGroups != null && !requiredGroups.filters.isEmpty()) {
                    UsersGroup assignedGroup = pub.getGroup();
                    //Do not check for AND/OR since a user can be bound to only one group
                    if (assignedGroup == null || !requiredGroups.filters.contains(assignedGroup.getShortName())) {
                        continue; //user doesn't belong to any of the required group => skip it
                    }
                }
                if (requiredLanguages != null && !requiredLanguages.filters.isEmpty()) {
                    Collection<String> assignedLanguages = pub.getLanguages();
                    if (requiredLanguages.and) { //AND => all required languages must be assigned
                        if (!assignedLanguages.containsAll(requiredLanguages.filters)) {
                            continue;
                        }
                    } else { //OR => it's enough just one required language
                        if (assignedLanguages == null || requiredLanguages.filters.stream().noneMatch(l -> assignedLanguages.contains(l))) {
                            //user has none of the required languages => skip user
                            continue;
                        }
                    }
                }
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
     * Create a new user
     *
     * @param email       this will be used as id
     * @param password    password not encoded, it is encoded here
     * @param givenName
     * @param familyName
     * @param address
     * @param affiliation
     * @param url
     * @param phone
     * @return
     * @throws ProjectAccessException
     * @throws UserException
     */
    @PreAuthorize("@auth.isAdmin()")
    @STServiceOperation(method = RequestMethod.POST)
    public JsonNode createUser(String email, String password, String givenName, String familyName, @Optional IRI iri,
                           @Optional String address, @Optional String affiliation, @Optional String url, @Optional String avatarUrl,
                           @Optional String phone, @Optional Collection<String> languageProficiencies, @Optional Map<IRI, String> customProperties)
            throws ProjectAccessException, UserException, IOException, InterruptedException, STPropertyUpdateException, STPropertyAccessException {
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
        UsersManager.clearExpiredUnverifiedUser();
        user.setStatus(UserStatus.ACTIVE);
        UsersManager.registerUser(user);

        return user.getAsJsonObject();
    }

    /**
     * @param email
     * @param password
     * @param givenName
     * @param familyName
     * @param iri
     * @param address
     * @param affiliation
     * @param url
     * @param avatarUrl
     * @param phone
     * @param languageProficiencies
     * @param customProperties
     * @param vbHostAddress         required if it is going to register a "standard" user (not admin) so it needs to send the
     *                              verification link in the email
     * @throws ProjectAccessException
     * @throws UserException
     * @throws STPropertyUpdateException
     * @throws IOException
     * @throws STPropertyAccessException
     * @throws MessagingException
     * @throws InterruptedException
     */
    @STServiceOperation(method = RequestMethod.POST)
    public JsonNode registerUser(String email, String password, String givenName, String familyName, @Optional IRI iri,
                             @Optional String address, @Optional String affiliation, @Optional String url, @Optional String avatarUrl,
                             @Optional String phone, @Optional Collection<String> languageProficiencies, @Optional Map<IRI, String> customProperties,
                             @Optional String vbHostAddress)
            throws ProjectAccessException, UserException, STPropertyUpdateException, IOException,
            STPropertyAccessException, MessagingException, InterruptedException {

        STUser user;
        if (iri != null) {
            user = new STUser(iri, email, password, givenName, familyName);
        } else {
            user = new STUser(email, password, givenName, familyName);
        }
        user.setAddress(address);
        user.setAffiliation(affiliation);
        user.setUrl(url);
        user.setAvatarUrl(avatarUrl);
        user.setPhone(phone);
        if (languageProficiencies != null) {
            user.setLanguageProficiencies(languageProficiencies);
        }
        if (customProperties != null) {
            for (Entry<IRI, String> entry : customProperties.entrySet()) {
                user.setCustomProperty(entry.getKey(), entry.getValue());
            }
        }
        // in case of Login with SAML the logged user is the mock up user set in SAMLUserDetails
        STUser loggedUser = UsersManager.getLoggedUser();

        //if no user registered yet, it means that it is the first access, so activate it and set it as admin
        if (UsersManager.listUsers().isEmpty()) {
            user.setStatus(UserStatus.ACTIVE);
            UsersManager.registerUser(user);
            UsersManager.addAdmin(user);
            setSamlUserInSecurityContext(loggedUser, user);
        } else { //not the first user
            /*
			New registered user, two possible workflows:
			1) email verification enabled: user is set as UNVERIFIED and an email is sent to the user for the
			email address verification
			2) email verification not enabled: user is set as NEW, an email is sent to the admin requiring
			the user activation and another is sent to user informing to wait for the activation
			 */
            VbEmailService emailService = new VbEmailService();
            boolean emailVerification = STPropertiesManager.getSystemSettings(
                    CoreSystemSettings.class, SemanticTurkeyCoreSettingsManager.class.getName()).emailVerification;

            emailVerification = emailVerification && (loggedUser == null || !loggedUser.isSamlUser());
            if (emailVerification) {
                UsersManager.clearExpiredUnverifiedUser();
                user.setStatus(UserStatus.UNVERIFIED);
                user.setVerificationToken(new BigInteger(130, new SecureRandom()).toString(32));
                UsersManager.registerUser(user);
                //only if registration succeeds (no duplicated email/iri exception) sends verification email
                try {
                    emailService.sendRegistrationMailToUser(user, vbHostAddress, true);
                } catch (Exception e) {
                    //if exception is raised while sending the verification email,
                    //undo the user creation so that no unverified user is pending
                    UsersManager.deleteUser(user);
                    //and rethrow the exception since in this case is necessary to inform the client
                    throw e;
                }
            } else {
                user.setStatus(UserStatus.NEW);
                user.setActivationToken(new BigInteger(130, new SecureRandom()).toString(32));
                UsersManager.registerUser(user);
                //only if registration succeeds (no duplicated email/iri exception) sends emails
                try {
                    emailService.sendRegistrationMailToUser(user, vbHostAddress, false);
                    emailService.sendRegistrationMailToAdmin(user, vbHostAddress);
                } catch (Exception e) { //catch generic Exception in order to avoid annoying exception raised to the client when the configuration is invalid
                    logger.error(Utilities.printFullStackTrace(e));
                }
            }
            setSamlUserInSecurityContext(loggedUser, user);
        }
        return user.getAsJsonObject();
    }

    /**
     * Replace the mockup SAML user with an actual user stored in ST
     * @param loggedUser
     * @param registeringUser
     */
    private void setSamlUserInSecurityContext(STUser loggedUser, STUser registeringUser) {
        if (loggedUser != null && loggedUser.isSamlUser() && loggedUser.getEmail().equalsIgnoreCase(registeringUser.getEmail())) {
            Authentication auth = new UsernamePasswordAuthenticationToken(registeringUser, registeringUser.getPassword(), null);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }


    @STServiceOperation(method = RequestMethod.POST)
    public void verifyUserEmail(String email, String token, String vbHostAddress)
            throws UserException, IOException, MessagingException, STPropertyAccessException, InterruptedException, STPropertyUpdateException {
        UsersManager.clearExpiredUnverifiedUser();
        UsersManager.verifyUser(email, token);
        STUser user = UsersManager.getUser(email);
        //user now verified => notify the admin and the user itself
        VbEmailService emailService = new VbEmailService();
        emailService.sendVerifiedMailToUser(user);
        emailService.sendRegistrationMailToAdmin(user, vbHostAddress);
    }

    @STServiceOperation(method = RequestMethod.POST)
    public void activateRegisteredUser(String email, String token) throws UserException {
        UsersManager.activateNewRegisteredUser(email, token);
        STUser user = UsersManager.getUser(email);
        VbEmailService emailService = new VbEmailService();
        try {
            emailService.sendEnabledMailToUser(user);
        } catch (Exception e) { //catch generic Exception in order to avoid annoying exception raised to the client when the configuration is invalid
            logger.error(Utilities.printFullStackTrace(e));
        }
    }

    /**
     * Update first name of the given user.
     *
     * @param email
     * @param givenName
     * @return
     * @throws IOException
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
    public ObjectNode updateUserGivenName(String email, String givenName) throws UserException {
        STUser user = UsersManager.getUser(email);
        UsersManager.updateUserGivenName(user, givenName);
        updateUserInSecurityContext(user);
        return user.getAsJsonObject();
    }

    /**
     * Update last name of the given user.
     *
     * @param email
     * @param familyName
     * @return
     * @throws IOException
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
    public ObjectNode updateUserFamilyName(String email, String familyName) throws UserException {
        STUser user = UsersManager.getUser(email);
        UsersManager.updateUserFamilyName(user, familyName);
        updateUserInSecurityContext(user);
        return user.getAsJsonObject();
    }

    /**
     * Update email of the given user.
     *
     * @param email
     * @param newEmail
     * @return
     * @throws IOException
     * @throws STPropertyUpdateException
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
    public ObjectNode updateUserEmail(String email, String newEmail) throws UserException {
        STUser user = UsersManager.getUser(email);
        //check if there is already a user that uses the newEmail
        if (UsersManager.isEmailUsed(newEmail)) {
            throw new UpdateEmailAlreadyUsedException(email, newEmail);
        }
        UsersManager.updateUserEmail(user, newEmail);
        updateUserInSecurityContext(user);
        return user.getAsJsonObject();
    }

    /**
     * Update the phone of the given user.
     *
     * @param email email of the user
     * @param phone phone number
     * @return
     * @throws IOException
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
    public ObjectNode updateUserPhone(String email, @Optional String phone) throws UserException {
        STUser user = UsersManager.getUser(email);
        UsersManager.updateUserPhone(user, phone);
        updateUserInSecurityContext(user);
        return user.getAsJsonObject();
    }

    /**
     * Update address of the given user.
     *
     * @param email   email of the user
     * @param address address
     * @return
     * @throws IOException
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
    public ObjectNode updateUserAddress(String email, @Optional String address) throws UserException {
        STUser user = UsersManager.getUser(email);
        UsersManager.updateUserAddress(user, address);
        updateUserInSecurityContext(user);
        return user.getAsJsonObject();
    }

    /**
     * Update gender of the given user.
     *
     * @param email
     * @param affiliation
     * @return
     * @throws IOException
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
    public ObjectNode updateUserAffiliation(String email, @Optional String affiliation) throws UserException {
        STUser user = UsersManager.getUser(email);
        UsersManager.updateUserAffiliation(user, affiliation);
        updateUserInSecurityContext(user);
        return user.getAsJsonObject();
    }

    /**
     * Update url of the given user.
     *
     * @param email
     * @param url
     * @return
     * @throws IOException
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
    public ObjectNode updateUserUrl(String email, @Optional String url) throws UserException {
        STUser user = UsersManager.getUser(email);
        UsersManager.updateUserUrl(user, url);
        updateUserInSecurityContext(user);
        return user.getAsJsonObject();
    }

    /**
     * Update avatarUrl of the given user.
     *
     * @param email
     * @param avatarUrl
     * @return
     * @throws IOException
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
    public ObjectNode updateUserAvatarUrl(String email, @Optional String avatarUrl) throws UserException {
        STUser user = UsersManager.getUser(email);
        UsersManager.updateUserAvatarUrl(user, avatarUrl);
        updateUserInSecurityContext(user);
        return user.getAsJsonObject();
    }


    /**
     * Update language proficiencies of the given user
     *
     * @param email
     * @param languageProficiencies
     * @return
     * @throws IOException
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('um(user)', 'U') || @auth.isLoggedUser(#email)")
    public ObjectNode updateUserLanguageProficiencies(String email, Collection<String> languageProficiencies) throws UserException {
        STUser user = UsersManager.getUser(email);
        UsersManager.updateUserLanguageProficiencies(user, languageProficiencies);
        updateUserInSecurityContext(user);
        return user.getAsJsonObject();
    }

    /**
     * Enables or disables the given user
     *
     * @param email
     * @param enabled
     * @return
     * @throws IOException
     * @throws ProjectBindingException
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('um(user)', 'C')")
    public ObjectNode enableUser(String email, boolean enabled) throws UserException, ProjectBindingException {
        if (UsersManager.getLoggedUser().getEmail().equalsIgnoreCase(email)) {
            throw new ProjectBindingException(Users.class.getName() + ".messages.cant_disable_current_user", null);
        }
        STUser user = UsersManager.getUser(email);
        if (enabled) {
            UserStatus oldStatus = user.getStatus();
            UsersManager.updateUserStatus(user, UserStatus.ACTIVE);
            if (oldStatus.equals(UserStatus.NEW)) { //if enabled for the first time send notification
                try {
                    new VbEmailService().sendEnabledMailToUser(user);
                } catch (Exception e) { //catch generic Exception in order to avoid annoying exception raised to the client when the configuration is invalid
                    logger.error(Utilities.printFullStackTrace(e));
                }
            }
        } else {
            UsersManager.updateUserStatus(user, UserStatus.INACTIVE);
        }
        return user.getAsJsonObject();
    }

    /**
     * Deletes an user
     *
     * @param email
     * @throws Exception
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('um(user)', 'D')")
    public void deleteUser(@RequestParam("email") String email) throws Exception {
        STUser user = UsersManager.getUser(email);
        if (UsersManager.getLoggedUser().getEmail().equalsIgnoreCase(email)) {
            throw new UserSelfDeletionException();
        }
        UsersManager.deleteUser(user);
    }

    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isLoggedUser(#email)")
    public void changePassword(String email, String oldPassword, String newPassword) throws Exception {
        STUser user = UsersManager.getUser(email);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            UsersManager.updateUserPassword(user, newPassword);
        } else {
            throw new OldPasswordMismatchException();
        }
    }

    /**
     * Allows the admin to force the password
     *
     * @param email
     * @param password
     * @throws Exception
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public void forcePassword(String email, String password) throws Exception {
        STUser user = UsersManager.getUser(email);
        UsersManager.updateUserPassword(user, password);
    }

    @STServiceOperation(method = RequestMethod.POST)
    public void forgotPassword(HttpServletRequest request, String email, String vbHostAddress, @Optional EmailApplicationContext appCtx) throws Exception {
        STUser user = UsersManager.getUser(email);
        //generate a random token
        SecureRandom random = new SecureRandom();
        String token = new BigInteger(130, random).toString(32);
        //use the token to generate the reset password link
        String resetLink = vbHostAddress + "/#/ResetPassword/" + token;
        //try to send the e-mail containing the link. If it fails, throw an exception
        try {
            request.getSession().setAttribute("reset_password_token", email + token);
            EmailService emailService = EmailServiceFactory.getService(appCtx);
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
            EmailService emailService = EmailServiceFactory.getService(appCtx);
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
     *
     * @return
     */
    @STServiceOperation
    public JsonNode getUserFormFields() {
        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

        ObjectNode respJson = jsonFactory.objectNode();

        //optional fields
        ArrayNode optionalFieldsJson = jsonFactory.arrayNode();
        for (Entry<IRI, Boolean> f : UsersManager.getUserForm().getOptionalFields().entrySet()) {
            ObjectNode fieldJson = jsonFactory.objectNode();
            fieldJson.set("iri", jsonFactory.textNode(f.getKey().stringValue()));
            fieldJson.set("visible", jsonFactory.booleanNode(f.getValue()));
            optionalFieldsJson.add(fieldJson);
        }
        respJson.set("optionalFields", optionalFieldsJson);

        //custom fields
        ArrayNode customFieldsJson = jsonFactory.arrayNode();
        for (UserFormCustomField field : UsersManager.getUserForm().getOrderedCustomFields()) {
            ObjectNode fieldJson = jsonFactory.objectNode();
            fieldJson.set("iri", jsonFactory.textNode(field.getIri().stringValue()));
            fieldJson.set("label", jsonFactory.textNode(field.getLabel()));
            fieldJson.set("description", jsonFactory.textNode(field.getDescription()));
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
    public void addUserFormCustomField(String field, @Optional String description) throws UserException {
        UsersManager.addUserFormCustomField(field, description);
    }

    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public void updateUserFormCustomField(IRI fieldIri, String label, @Optional String description) throws UserException {
        UsersManager.updateUserFormCustomField(fieldIri, label, description);
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
        UsersManager.updateUserCustomProperty(user, property, value);
        updateUserInSecurityContext(user);
        return user.getAsJsonObject();
    }

    /**
     * Updates the user stored in the security context whenever there is a change to the current logged user
     *
     * @param user
     */
    private void updateUserInSecurityContext(STUser user) {
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }


    public static class UserFilter {
        private List<String> filters;
        private boolean and;
        public UserFilter() {}
        public List<String> getFilters() {
            return filters;
        }
        public void setFilters(List<String> filters) {
            this.filters = filters;
        }
        public boolean isAnd() {
            return and;
        }
        public void setAnd(boolean and) {
            this.and = and;
        }
    }

}
