package it.uniroma2.art.semanticturkey.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * 
 * @author Tiziano
 * Specifies what happen when a user tries to login with wrong credentials
 * Simply returns a 401 response (unauthorized) with text content "Authentication failed"
 * (Referenced in WEB-INF/spring-security.xml)
 *
 */
@Component
public class STAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		ServletOutputStream out = response.getOutputStream();
		
		//create json response with the same structure of STResponse
//		response.setContentType("application/json");
//		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
//				.createReplyResponse("login", RepliesStatus.fail, SerializationType.json);
//		try {
//			jsonResp.getDataElement().put("msg", "Authentication failed. Wrong username and password combination");
//		} catch (JSONException e) {
//			throw new ServletException(e);
//		}
//		out.print(jsonResp);
		
		out.print("Authentication failed. Wrong username and password combination");
	}

}
