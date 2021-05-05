package it.uniroma2.art.semanticturkey.email;

import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.settings.core.CoreSystemSettings;
import it.uniroma2.art.semanticturkey.settings.core.MailSettings;
import it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class EmailSender {

	public static void sendMail(String toEmail, String subject, String text)
			throws MessagingException, UnsupportedEncodingException, STPropertyAccessException {

		CoreSystemSettings systemSettings = STPropertiesManager.getSystemSettings(CoreSystemSettings.class, SemanticTurkeyCoreSettingsManager.class.getName());
		MailSettings mailSettings = systemSettings.mail;

		String mailFromAddress = mailSettings.from.address;
		String mailFromPassword = mailSettings.from.password;
		String mailFromAlias = mailSettings.from.alias;

		String mailSmtpHost = mailSettings.smtp.host;
		int mailSmtpPort = mailSettings.smtp.port;
		boolean mailSmtpAuth = mailSettings.smtp.auth;
		boolean sslEnabled = mailSettings.smtp.sslEnabled;
		boolean starttlsEnabled = mailSettings.smtp.starttlsEnabled;

		if (mailFromAddress == null || mailSmtpHost == null || mailSmtpPort == 0) {
			throw new MessagingException("Wrong mail configuration, impossible to send a confirmation e-mail");
		}

		Properties props = new Properties();
		props.put("mail.smtp.host", mailSmtpHost);
		props.put("mail.smtp.port", mailSmtpPort);
		props.put("mail.smtp.auth", mailSmtpAuth+"");

		if (sslEnabled) {
			props.put("mail.smtp.ssl.enable", "true");
		} else if (starttlsEnabled) {
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
		message.setContent(text, "text/html; charset=utf-8");

		Transport.send(message);
	}
}
