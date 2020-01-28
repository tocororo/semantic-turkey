package it.uniroma2.art.semanticturkey.email;

import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

public class EmailSender {
	
	public static void sendMail(String toEmail, String subject, String text)
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
		message.setContent(text, "text/html; charset=utf-8");

		Transport.send(message);
	}


	/*
	 * UTILS
	 */

	/**
	 *
	 * @param timestamp
	 * @return
	 */
	public static String getDateFromTimestamp(long timestamp) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(new Date(timestamp));
	}

	public static String formatBold(String text) {
		return "<b>" + text + "</b>";
	}
	public static String formatItalic(String text) {
		return "<i>" + text + "</i>";
	}

}
