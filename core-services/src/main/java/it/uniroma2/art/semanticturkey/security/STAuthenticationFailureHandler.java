package it.uniroma2.art.semanticturkey.security;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 
 * @author Tiziano
 * Specifies what happen when a user tries to login with wrong credentials
 * Simply returns a 403 response (forbidden) with text content "Authentication failed"
 * (Referenced in WEB-INF/spring-security.xml)
 *
 */
public class STAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("text/plain");
		ServletOutputStream out = response.getOutputStream();
		if (exception.getClass().isAssignableFrom(DisabledException.class)) {
			out.print("Authentication failed. User not yet enabled, please contact the system administrator");
		} else {
			out.print("Authentication failed. Wrong username and password combination");
		}
	}

}
