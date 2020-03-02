package it.uniroma2.art.semanticturkey.email;

import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.user.STUser;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;

public abstract class EmailService {

	private String app;

	EmailService(EmailApplicationContext ctx) {
		app = ctx == EmailApplicationContext.VB ? "VocBench" : "PMKI";
	}

	/**
	 * The following send email methods are in common for both VB and PMKI EmailService.
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
		String text = "This message has been sent in order to check the configuration of the " + app + " email service.<br>" +
				"If you did not request to send this email, please ignore it.<br><br>Regards.";
		EmailSender.sendMail(mailTo, app + " email service configuration check", text);
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
		EmailSender.sendMail(user.getEmail(), app + " password reset", text);
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
				+ "<br>we've received a request to reset the password for the " + app
				+ " account associated to this email address."
				+ "<br>Click the link below to be redirected to the reset password page."
				+ " This password reset is only valid for a limited time."
				+ "<br><br>" + forgotPasswordLink
				+ "<br><br>If you did not request a password reset, please ignore this email"
				+ " or report this to the system administrator.";
		EmailSender.sendMail(user.getEmail(), app + " password reset", text);
	}


	protected static String formatBold(String text) {
		return "<b>" + text + "</b>";
	}
	protected static String formatItalic(String text) {
		return "<i>" + text + "</i>";
	}
}
