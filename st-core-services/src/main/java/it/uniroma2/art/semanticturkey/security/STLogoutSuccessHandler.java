package it.uniroma2.art.semanticturkey.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import it.uniroma2.art.semanticturkey.servlet.JSONResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;

/**
 * 
 * @author Tiziano
 * Implementation of the LogoutSuccessHandler. Returns an HTTP status code of 200.
 * This is useful in REST-type scenarios where a redirect upon a successful logout is not desired.
 * (Referenced in WEB-INF/spring-security.xml)
 * 
 */
public class STLogoutSuccessHandler implements LogoutSuccessHandler {
	
	@Override
	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		ServletOutputStream out = response.getOutputStream();
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("logout", RepliesStatus.ok, SerializationType.json);
		out.print(jsonResp.toString());
	}
	
}
