package it.uniroma2.art.semanticturkey.security;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import it.uniroma2.art.semanticturkey.servlet.JSONResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * 
 * @author Tiziano
 * Implementation of the LogoutSuccessHandler. Returns an HTTP status code of 200.
 * This is useful in REST-type scenarios where a redirect upon a successful logout is not desired.
 * (Referenced in WEB-INF/spring-security.xml)
 * 
 */
public class STLogoutSuccessHandler implements LogoutSuccessHandler {
	
	@Autowired
	private SessionRegistry sessionRegistry;

	@Override
	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		//Expires the session of the logged out user and removes it from the sessionRegistry
		STUser loggingOutUser = (STUser) authentication.getPrincipal();
		List<SessionInformation> sessions = sessionRegistry.getAllSessions(loggingOutUser, false);
    	for (SessionInformation info : sessions) {
			info.expireNow();
			sessionRegistry.removeSessionInformation(info.getSessionId());
		}
		
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		ServletOutputStream out = response.getOutputStream();
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("logout", RepliesStatus.ok, SerializationType.json);
		out.print(jsonResp.toString());
	}
	
}
