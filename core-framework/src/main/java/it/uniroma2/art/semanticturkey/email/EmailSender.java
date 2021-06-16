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
			/*
			the following is required since, if not specified, it seems that the mail API uses TLSv1 as default SSL
			protocol which in some cases is disabled in jre\lib\security\java.security (prop: jdk.tls.disabledAlgorithms)
			causing
			javax.net.ssl.SSLHandshakeException: No appropriate protocol (protocol is disabled or cipher suites are inappropriate)
			 */
			props.put("mail.smtp.ssl.protocols", "TLSv1.2");
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
		System.out.println(session.getProperties().getProperty("mail.smtp.ssl.protocols"));

		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(mailFromAddress, mailFromAlias));
		message.setSubject(subject);
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
		message.setContent(text, "text/html; charset=utf-8");

		Transport.send(message);
	}
}
