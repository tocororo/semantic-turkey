package it.uniroma2.art.semanticturkey.email;

import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UsersManager;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

public class VbEmailService extends EmailService {

	//placeholders
	private static final String USER_EMAIL_PLACEHOLDER = "{{user.email}}";
	private static final String USER_GIVEN_NAME_PLACEHOLDER = "{{user.givenName}}";
	private static final String USER_FAMILY_NAME_PLACEHOLDER = "{{user.familyName}}";
	private static final String ADMIN_EMAIL_PLACEHOLDER = "{{admin.email}}";
	private static final String ADMIN_GIVEN_NAME_PLACEHOLDER = "{{admin.givenName}}";
	private static final String ADMIN_FAMILY_NAME_PLACEHOLDER = "{{admin.familyName}}";
	private static final String ADMIN_LIST_PLACEHOLDER = "{{adminList}}";

	private static final String VB_ACTIONS_ROUTE = "UserActions";
	private static final String VB_ACTIONS_VERIFY = "verify";
	private static final String VB_ACTIONS_ACTIVATE = "activate";

	public VbEmailService() {
		super(EmailApplicationContext.VB);
	}

	/**
	 * Sends an email to a new registered user. Email asks for email verification if required, otherwise
	 * inform the user to wait to be activated by admin
	 * @param user
	 * @param vbHostAddress
	 * @param emailVerification
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 * @throws STPropertyAccessException
	 */
	public void sendRegistrationMailToUser(STUser user, String vbHostAddress, boolean emailVerification)
			throws MessagingException, UnsupportedEncodingException, STPropertyAccessException {

		String registrationText_NoVerification = "Dear {{user.givenName}} {{user.familyName}},<br>" +
			"thank you for registering to VocBench.<br>" +
			"Your registration has been now notified to the administrator. " +
			"Please wait for the administrator to approve it.<br>" +
			"After the approval, you can log into VocBench with the e-mail {{user.email}} and your chosen password.<br>" +
			"Thanks for your interest.<br><br>" +
			"If you want to unregister, please contact one of the administrators ({{adminList}}).<br><br>" +
			"Regards,<br>The VocBench team.";

		String registrationText_WithVeirification = "Dear {{user.givenName}} {{user.familyName}},<br>" +
			"thank you for registering to VocBench.<br>" +
			"In order to complete the registration you need to verify your email address by clicking on the link below:<br><br>" +
			"{{verificationLink}}<br><br>" +
			"The above link will expire after " + UsersManager.EMAIL_VERIFICATION_EXPIRATION_HOURS + " hours. " +
			"If this occurs, you can register again and then activate your account using the activation link in the new email.<br>" +
			"If you receive this email without signing up, please ignore this message<br><br>" +
			"Regards,<br>The VocBench team.";

		String text = emailVerification ? registrationText_WithVeirification : registrationText_NoVerification;
		text = replaceUserPlaceholders(text, user);

		if (emailVerification) {
			if (!vbHostAddress.endsWith("/")) {
				vbHostAddress += "/";
			}
			String verificationUrl = vbHostAddress + "#/" + VB_ACTIONS_ROUTE +
					"?action=" + VB_ACTIONS_VERIFY +
					"&token=" + user.getVerificationToken() +
					"&email=" + user.getEmail() ;
			String verificationLink = "<a href=\"" + verificationUrl + "\">Verifiy</a>";
			text = text.replace("{{verificationLink}}", verificationLink);
		}

		EmailSender.sendMail(user.getEmail(), "VocBench registration", text);
	}

	/**
	 * Sends an email to a verified user informing to wait to be enabled
	 * @param user
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 * @throws STPropertyAccessException
	 */
	public void sendVerifiedMailToUser(STUser user) throws MessagingException, UnsupportedEncodingException, STPropertyAccessException {
		String text = "Dear {{user.givenName}} {{user.familyName}},<br>" +
			"your email has been verified. Your registration has been now notified to the administrator. " +
			"Please wait for the administrator to approve it.<br>" +
			"After the approval, you can log into VocBench with the e-mail {{user.email}} and your chosen password.<br>" +
			"Thanks for your interest.<br><br>" +
			"If you want to unregister, please contact one of the administrators ({{adminList}}).<br><br>" +
			"Regards,<br>The VocBench team.";
		text = replaceUserPlaceholders(text, user);
		text = replaceGenericPlaceholders(text);
		EmailSender.sendMail(user.getEmail(), "VocBench email verified", text);
	}

	/**
	 * Sends an email to the system administrator to inform about a new user registration request and requiring
	 * to be activated
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 * @throws STPropertyAccessException
	 */
	public void sendRegistrationMailToAdmin(STUser user, String vbHostAddress)
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException, UserException {
		for (String adminEmail: UsersManager.getAdminEmailList()) {
			STUser admin = UsersManager.getUser(adminEmail);
			String text = "Dear {{admin.givenName}} {{admin.familyName}},<br>" +
					"there is a new user registered to VocBench.<br>" +
					"Given Name: <i>{{user.givenName}}</i><br>" +
					"Family Name: <i>{{user.familyName}}</i><br>" +
					"Email: <i>{{user.email}}</i><br>" +
					"You can activate the account from the administration page in VB or by clicking on the link below:<br><br>" +
					"{{activationLink}}<br><br>" +
					"Regards,<br>The VocBench team.";
			text = replaceUserPlaceholders(text, user);
			text = replaceAdminPlaceholders(text, admin);
			text = replaceGenericPlaceholders(text);

			if (!vbHostAddress.endsWith("/")) {
				vbHostAddress += "/";
			}
			String activationUrl = vbHostAddress + "#/" + VB_ACTIONS_ROUTE +
					"?action=" + VB_ACTIONS_ACTIVATE +
					"&token=" + user.getActivationToken() +
					"&email=" + user.getEmail();
			String activationLink = "<a href=\"" + activationUrl + "\">Activate</a>";
			text = text.replace("{{activationLink}}", activationLink);

			EmailSender.sendMail(adminEmail, "VocBench: new user registered", text);
		}
	}

	/**
	 * Sends an email to a user to inform that his/her account has been enabled
	 * @param user
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 * @throws STPropertyAccessException
	 */
	public void sendEnabledMailToUser(STUser user) throws MessagingException, UnsupportedEncodingException, STPropertyAccessException {
		String text = "Dear {{user.givenName}} {{user.familyName}},<br>" +
			"the administrator has enabled your account. You can now log into VocBench with the email " +
			"<i>{{user.email}}</i> and your chosen password.<br><br>" +
			"Regards,<br>The VocBench team.";
		text = replaceUserPlaceholders(text, user);
		text = replaceGenericPlaceholders(text);
		EmailSender.sendMail(user.getEmail(), "VocBench account enabled", text);
	}

	/**
	 * Replaces references to user placeholders with the info about the given user.
	 * This must be used only in those email messages which content involves the user
	 * (e.g. user registered, account enabled, ..., so not in the email-service test message)
	 * @param text
	 * @param user
	 * @return
	 */
	private static String replaceUserPlaceholders(String text, STUser user) {
		String replaced = text;
		replaced = replaced.replace(USER_EMAIL_PLACEHOLDER, user.getEmail());
		replaced = replaced.replace(USER_GIVEN_NAME_PLACEHOLDER, user.getGivenName());
		replaced = replaced.replace(USER_FAMILY_NAME_PLACEHOLDER, user.getFamilyName());
		return replaced;
	}

	/**
	 * Replaces references to (a specific) admin placeholders with the info about the given admin.
	 * This must be used only in those email messages which content involves the admin
	 * (e.g. user registered, ..., so not in emails toward the sole user (account enabled) or the email-service test message)
	 * @param text
	 * @param admin
	 * @return
	 */
	private static String replaceAdminPlaceholders(String text, STUser admin) {
		String replaced = text;
		replaced = replaced.replace(ADMIN_EMAIL_PLACEHOLDER, admin.getEmail());
		replaced = replaced.replace(ADMIN_GIVEN_NAME_PLACEHOLDER, admin.getGivenName());
		replaced = replaced.replace(ADMIN_FAMILY_NAME_PLACEHOLDER, admin.getFamilyName());
		return replaced;
	}

	private static String replaceGenericPlaceholders(String text) {
		String replaced = text;
		if (replaced.contains(ADMIN_LIST_PLACEHOLDER)) {
			Collection<String> adminEmails = UsersManager.getAdminEmailList();
			replaced = replaced.replace(ADMIN_LIST_PLACEHOLDER, String.join(", ", adminEmails));
		}
		return replaced;
	}

}
