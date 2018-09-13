package it.uniroma2.art.semanticturkey.utilities;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.user.STUser;

public class EmailSender {
	
	/**
	 * Sends an email to the registered user
	 * @param toEmail
	 * @param givenName
	 * @param familyName
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 * @throws STPropertyAccessException 
	 */
	public static void sendRegistrationMailToUser(STUser user) 
			throws MessagingException, UnsupportedEncodingException, STPropertyAccessException {
		String emailAdminAddress = STPropertiesManager.getSystemSetting(
				STPropertiesManager.SETTING_ADMIN_ADDRESS);
		String text = "Dear " + user.getGivenName() + " " + user.getFamilyName() + ","
				+ "\nthank you for registering as a user of VocBench 3."
				+ " Your request has been received. Please wait for the administrator to approve it."
				+ " After approval, you can log into VocBench with the e-mail " + user.getEmail() + " and your chosen password."
				+ "\nThanks for your interest."
				+ "\nIf you want to unregister, please send an email with your e-mail address and the subject:"
				+ " 'VocBench - Unregister' to " + emailAdminAddress + "."
				+ "\nRegards,\nThe VocBench Team.";
		sendMail(user.getEmail(), "VocBench registration", text);
	}
	
	/**
	 * Sends an email to a user to inform that his/her account has been enabled
	 * @param user
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 * @throws STPropertyAccessException
	 */
	public static void sendEnabledMailToUser(STUser user) 
			throws MessagingException, UnsupportedEncodingException, STPropertyAccessException {
		String text = "Dear " + user.getGivenName() + " " + user.getFamilyName() + ","
				+ "\nthe administrator has enabled your account."
				+ " You can now log into VocBench with the e-mail " + user.getEmail() + " and your chosen password."
				+ "\nRegards,\nThe VocBench Team.";
		sendMail(user.getEmail(), "VocBench account enabled", text);
	}
	
	/**
	 * Sends an email to the system administrator to inform about a new user registration request
	 * @throws MessagingException 
	 * @throws UnsupportedEncodingException 
	 * @throws STPropertyAccessException 
	 */
	public static void sendRegistrationMailToAdmin(STUser user)
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String emailAdminAddress = STPropertiesManager.getSystemSetting(
				STPropertiesManager.SETTING_ADMIN_ADDRESS);
		String text = "Dear VocBench administrator,"
				+ "\nthere is a new user registration request for VocBench."
				+ "\nGiven Name: " + user.getGivenName()
				+ "\nFamily Name: " + user.getFamilyName()
				+ "\nE-mail: " + user.getEmail()
				+ "\nPlease activate the account.\nRegards,\nThe VocBench Team.";
		sendMail(emailAdminAddress, "VocBench registration", text);
	}
	
	public static void sendForgotPasswordMail(STUser user, String forgotPasswordLink)
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String text = "Dear " + user.getGivenName() + " " + user.getFamilyName() + ","
				+ "\nwe've received a request to reset the password for the"
				+ " VocBench account associated to this email address."
				+ "\nClick the link below to be redirected to the reset password page."
				+ " This password reset is only valid for a limited time."
				+ "\n\n" + forgotPasswordLink
				+ "\n\nIf you did not request a password reset, please ignore this email"
				+ " or report this to the system administrator."
				+ "\nRegards,\nThe VocBench team";
		sendMail(user.getEmail(), "VocBench password reset", text);
	}
	
	public static void sendResetPasswordMail(STUser user, String tempPassword)
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String text = "Dear " + user.getGivenName() + " " + user.getFamilyName() + ","
				+ "\nwe confirm you that your password has been reset."
				+ "\nThis is your new temporary password:"
				+ "\n\n"+ tempPassword
				+ "\n\nAfter the login we strongly recommend you to change the password."
				+ "\nRegards,\nThe VocBench team";
		sendMail(user.getEmail(), "VocBench password reset", text);
	}
	
	public static void sendTestMailConfig(String mailTo) 
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String text = "This message has been sent in order to check the VocBench e-mail configuration.\n"
				+ "If you did not request to send this e-mail, please ignore it.\n"
				+ "Regards,\nThe VocBench team";
		sendMail(mailTo, "VocBench e-mail configuration check", text);
	}
	
	private static void sendMail(String toEmail, String subject, String text) 
			throws MessagingException, UnsupportedEncodingException, STPropertyAccessException {
		String mailFromAddress = STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_FROM_ADDRESS);
		String mailFromPassword = STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_FROM_PASSWORD);
		String mailFromAlias = STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_FROM_ALIAS);
		
		String mailSmtpHost = STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_HOST);
		String mailSmtpPort = STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_PORT);
		boolean mailSmtpAuth = Boolean.parseBoolean(STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_AUTH));
		boolean mailSmtpSsl = Boolean.parseBoolean(STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_SSL_ENABLE));
		boolean mailSmtpTls = Boolean.parseBoolean(STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_MAIL_SMTP_STARTTLS_ENABLE));
		
		if (mailFromAddress == null || mailSmtpHost == null || mailSmtpPort == null) {
			throw new MessagingException("Wrong mail configuration, impossible to send a confirmation e-mail");
		}
		
		Properties props = new Properties();
		props.put("mail.smtp.host", mailSmtpHost);
		props.put("mail.smtp.port", mailSmtpPort);
		props.put("mail.smtp.auth", mailSmtpAuth+"");
		
		if (mailSmtpSsl) {
			props.put("mail.smtp.ssl.enable", "true");
		} else if (mailSmtpTls) {
			props.put("mail.smtp.starttls.enable", "true");
		}
		
		Session session;
		if (mailSmtpAuth) {
			session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(mailFromAddress, mailFromPassword);
				}
			});
		} else {
			session = Session.getInstance(props);
		}
		
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(mailFromAddress, mailFromAlias));
		message.setSubject(subject);
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
		message.setText(text);
		Transport.send(message);
	}

}
