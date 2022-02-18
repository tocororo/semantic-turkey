package it.uniroma2.art.semanticturkey.email;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UsersManager;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class EmailService {

	private String appName;

	//placeholders
	private static final String USER_EMAIL_PLACEHOLDER = "{{user.email}}";
	private static final String USER_GIVEN_NAME_PLACEHOLDER = "{{user.givenName}}";
	private static final String USER_FAMILY_NAME_PLACEHOLDER = "{{user.familyName}}";
	private static final String ADMIN_EMAIL_PLACEHOLDER = "{{admin.email}}";
	private static final String ADMIN_GIVEN_NAME_PLACEHOLDER = "{{admin.givenName}}";
	private static final String ADMIN_FAMILY_NAME_PLACEHOLDER = "{{admin.familyName}}";
	private static final String ADMIN_LIST_PLACEHOLDER = "{{adminList}}";

	EmailService(EmailApplicationContext ctx) {
		appName = ctx == EmailApplicationContext.VB ? "VocBench" : "ShowVoc";
	}

	/**
	 * The following send email methods are in common for both VB and ShowVoc EmailService.
	 * - mail service configuration test
	 * - reset password confirmed
	 * - reset password requested
	 * All these email are not configurable. The reset password confirmed is not configurable because it must contains the
	 * new generated temporary password. The same for the reset password requested that must contains the link to
	 * complete the procedure.
	 * Both of these two mail could be made configurable with dedicated placeholders (e.g. {{newPwd}} and {{resetPwdLink}})
	 * in the setting value, but this could be risky, in fact if the placeholders were removed, the reset password
	 * procedure would be corrupted.
	 */

	/**
	 * Sends an email to the given address just for testing the email service configuration
	 * @param mailTo
	 */
	public void sendMailServiceConfigurationTest(String mailTo) throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String text = "This message has been sent in order to check the configuration of the " + appName + " email service.<br>" +
				"If you did not request to send this email, please ignore it.<br><br>Regards.";
		EmailSender.sendMail(mailTo, appName + " email service configuration check", text);
	}

	/**
	 * Sends an email that informs the given user that its password has been replaced with the tempPassword
	 * @param user
	 * @param tempPassword
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	public void sendResetPasswordConfirmedMail(STUser user, String tempPassword) throws UnsupportedEncodingException,
			MessagingException, STPropertyAccessException {
		String text = "Dear " + user.getGivenName() + " " + user.getFamilyName() + ","
				+ "<br>we confirm you that your password has been reset."
				+ "<br>This is your new temporary password:"
				+ "<br><br>"+ tempPassword
				+ "<br><br>After the login we strongly recommend you to change the password.";
		EmailSender.sendMail(user.getEmail(), appName + " password reset", text);
	}

	/**
	 * Sends an email that provides to the given user the info for resetting the password
	 * @param user
	 * @param forgotPasswordLink
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	public void sendResetPasswordRequestedMail(STUser user, String forgotPasswordLink)
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String text = "Dear " + user.getGivenName() + " " + user.getFamilyName() + ","
				+ "<br>we've received a request to reset the password for the " + appName
				+ " account associated to this email address."
				+ "<br>Click the link below to be redirected to the reset password page."
				+ " This password reset is only valid for a limited time."
				+ "<br><br>" + forgotPasswordLink
				+ "<br><br>If you did not request a password reset, please ignore this email"
				+ " or report this to the system administrator.";
		EmailSender.sendMail(user.getEmail(), appName + " password reset", text);
	}


	/**
	 * Sends an email to tha administrator users for notifying them that a project has been created
	 * (it doesn't send to the user who created)
	 * @param creator
	 * @param project
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 * @throws UserException
	 */
	public void sendProjCreationMailToAdmin(STUser creator, Project project)
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException, UserException {
		List<String> adminEmailList = UsersManager.getAdminEmailList().stream()
				.filter(email -> !email.equals(creator.getEmail())).collect(Collectors.toList());
		for (String adminEmail: adminEmailList) {
			STUser admin = UsersManager.getUser(adminEmail);

			String text = "Dear {{admin.givenName}} {{admin.familyName}},<br>" +
					"a new project with name " + formatItalic(project.getName()) + " has been created by " +
					"{{user.givenName}} {{user.familyName}} ({{user.email}}).<br><br>" +
					"Regards";
			text = replaceUserPlaceholders(text, creator);
			text = replaceAdminPlaceholders(text, admin);
			text = replaceGenericPlaceholders(text);

			EmailSender.sendMail(adminEmail, "VocBench: new project created", text);
		}
	}


	protected static String formatBold(String text) {
		return "<b>" + text + "</b>";
	}
	protected static String formatItalic(String text) {
		return "<i>" + text + "</i>";
	}

	/**
	 * Replaces references to user placeholders with the info about the given user.
	 * This must be used only in those email messages which content involves the user
	 * (e.g. user registered, account enabled, ..., so not in the email-service test message)
	 * @param text
	 * @param user
	 * @return
	 */
	protected static String replaceUserPlaceholders(String text, STUser user) {
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
	protected static String replaceAdminPlaceholders(String text, STUser admin) {
		String replaced = text;
		replaced = replaced.replace(ADMIN_EMAIL_PLACEHOLDER, admin.getEmail());
		replaced = replaced.replace(ADMIN_GIVEN_NAME_PLACEHOLDER, admin.getGivenName());
		replaced = replaced.replace(ADMIN_FAMILY_NAME_PLACEHOLDER, admin.getFamilyName());
		return replaced;
	}

	protected static String replaceGenericPlaceholders(String text) {
		String replaced = text;
		if (replaced.contains(ADMIN_LIST_PLACEHOLDER)) {
			Collection<String> adminEmails = UsersManager.getAdminEmailList();
			replaced = replaced.replace(ADMIN_LIST_PLACEHOLDER, String.join(", ", adminEmails));
		}
		return replaced;
	}
}
