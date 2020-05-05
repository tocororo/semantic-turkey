package it.uniroma2.art.semanticturkey.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * @author Tiziano
 * Specifies what happen when a user successes to login.
 * Simply returns a 200 response (ok) with a user Json object
 * (Referenced in WEB-INF/spring-security.xml)
 */
public class STAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		ServletOutputStream out = response.getOutputStream();
		
		STUser loggedUser = (STUser) authentication.getPrincipal();
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode responseNode = jsonFactory.objectNode();
		responseNode.set("result", loggedUser.getAsJsonObject());
		
		out.print(responseNode.toString());
		
	}
}