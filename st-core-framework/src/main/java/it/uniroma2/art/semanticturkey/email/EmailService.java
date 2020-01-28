package it.uniroma2.art.semanticturkey.email;

import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.user.STUser;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;

public interface EmailService {

	/**
	 *
	 * @param mailTo
	 */
	void sendMailConfigurationTest(String mailTo) throws UnsupportedEncodingException, MessagingException, STPropertyAccessException;

	/**
	 *
	 * @param user
	 * @param tempPassword
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	void sendResetPasswordMail(STUser user, String tempPassword) throws UnsupportedEncodingException, MessagingException, STPropertyAccessException;

	/**
	 *
	 * @param user
	 * @param forgotPasswordLink
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	void sendForgotPasswordMail(STUser user, String forgotPasswordLink) throws UnsupportedEncodingException, MessagingException, STPropertyAccessException;

}
