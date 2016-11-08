package it.uniroma2.art.semanticturkey.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 
 * @author Tiziano
 * We need a custom authenticationEntryPoint because default Spring-Security config will
 * redirect to login page when a not authenticated user performs a request.
 * In our case we need just a https status 401 and a json response.
 * (Referenced in WEB-INF/spring-security.xml)
 */
@Component
public class STAuthenticationEntryPoint implements AuthenticationEntryPoint {
	
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		ServletOutputStream out = response.getOutputStream();
		
		//retrieve service method's name
//		String[] pathElements = request.getServletPath().split("/");
//		String methodName = pathElements[pathElements.length - 1];
//		
//		//create json response with the same structure of STResponse
//		response.setContentType("application/json");
//		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
//				.createReplyResponse(methodName, RepliesStatus.fail, SerializationType.json);
//		try {
//			jsonResp.getDataElement().put("msg", "Access denied. You need to be logged in.");
//		} catch (JSONException e) {
//			throw new ServletException(e);
//		}
//		out.print(jsonResp.toString());
		
		out.print("Access denied. You need to be logged in");
	}

}
