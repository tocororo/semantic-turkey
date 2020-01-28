package it.uniroma2.art.semanticturkey.email;

import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

public class VbEmailService implements EmailService{

	/**
	 * Sends an email to the registered user
	 * @param user
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 * @throws STPropertyAccessException
	 */
	public void sendRegistrationMailToUser(STUser user)
			throws MessagingException, UnsupportedEncodingException, STPropertyAccessException {
		Collection<String> adminEmails = UsersManager.getAdminEmailList();
		String adminEmailsMsg = (adminEmails.size() == 1) ?
				adminEmails.iterator().next() :
				" one of the following address: " + String.join(", ", adminEmails);
		String text = "Dear " + user.getGivenName() + " " + user.getFamilyName() + ","
				+ "<br>thank you for registering to VocBench 3."
				+ " Your request has been received. Please wait for the administrator to approve it."
				+ " After the approval, you can log into VocBench with the e-mail " + user.getEmail() + " and your chosen password."
				+ "<br>Thanks for your interest."
				+ "<br>If you want to unregister, please send an email with your e-mail address and the subject:"
				+ " 'VocBench - Unregister' to " + adminEmailsMsg + "."
				+ "<br><br>Regards,<br>The VocBench Team.";
		EmailSender.sendMail(user.getEmail(), "VocBench registration", text);
	}

	/**
	 * Sends an email to a user to inform that his/her account has been enabled
	 * @param user
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 * @throws STPropertyAccessException
	 */
	public void sendEnabledMailToUser(STUser user)
			throws MessagingException, UnsupportedEncodingException, STPropertyAccessException {
		String text = "Dear " + user.getGivenName() + " " + user.getFamilyName() + ","
				+ "<br>the administrator has enabled your account."
				+ " You can now log into VocBench with the e-mail " + user.getEmail() + " and your chosen password."
				+ "<br><br>Regards,<br>The VocBench Team.";
		EmailSender.sendMail(user.getEmail(), "VocBench account enabled", text);
	}

	/**
	 * Sends an email to the system administrator to inform about a new user registration request
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 * @throws STPropertyAccessException
	 */
	public void sendRegistrationMailToAdmin(STUser user)
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		for (String adminEmail: UsersManager.getAdminEmailList()) {
			String text = "Dear VocBench administrator,"
					+ "<br>there is a new user registration request for VocBench."
					+ "<br>Given Name: " + user.getGivenName()
					+ "<br>Family Name: " + user.getFamilyName()
					+ "<br>E-mail: " + user.getEmail()
					+ "<br>Please activate the account.<br><br>Regards,<br>The VocBench Team.";
			EmailSender.sendMail(adminEmail, "VocBench registration", text);
		}
	}

	@Override
	public void sendMailConfigurationTest(String mailTo) throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String text = "This message has been sent in order to check the VocBench e-mail configuration.<br>"
				+ "If you did not request to send this e-mail, please ignore it.";
		EmailSender.sendMail(mailTo, "VocBench e-mail configuration check", text);
	}

	@Override
	public void sendResetPasswordMail(STUser user, String tempPassword)
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String text = "Dear " + user.getGivenName() + " " + user.getFamilyName() + ","
				+ "<br>we confirm you that your password has been reset."
				+ "<br>This is your new temporary password:"
				+ "<br><br>"+ tempPassword
				+ "<br><br>After the login we strongly recommend you to change the password.";
		EmailSender.sendMail(user.getEmail(), "VocBench password reset", text);
	}

	@Override
	public void sendForgotPasswordMail(STUser user, String forgotPasswordLink)
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String text = "Dear " + user.getGivenName() + " " + user.getFamilyName() + ","
				+ "<br>we've received a request to reset the password for the"
				+ " VocBench account associated to this email address."
				+ "<br>Click the link below to be redirected to the reset password page."
				+ " This password reset is only valid for a limited time."
				+ "<br><br>" + forgotPasswordLink
				+ "<br><br>If you did not request a password reset, please ignore this email"
				+ " or report this to the system administrator.";
		EmailSender.sendMail(user.getEmail(), "VocBench password reset", text);
	}

}
