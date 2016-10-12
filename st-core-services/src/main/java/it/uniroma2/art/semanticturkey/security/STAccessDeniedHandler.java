package it.uniroma2.art.semanticturkey.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.servlet.JSONResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;

/**
 * 
 * @author Tiziano
 * Specifies what happen when an authenticated user perform a request for which has not the required role
 * Simply returns a 403 response (forbidden) with a json content
 * (Referenced in WEB-INF/spring-security.xml)
 *
 */
@Component
public class STAccessDeniedHandler implements AccessDeniedHandler {

	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
	        throws IOException, ServletException {
		
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		ServletOutputStream out = response.getOutputStream();
		
//		//retrieve service method's name
//		String[] pathElements = request.getServletPath().split("/");
//		String methodName = pathElements[pathElements.length - 1];
//		
//		//create json response with the same structure of STResponse
//		response.setContentType("application/json");
//		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
//				.createReplyResponse(methodName, RepliesStatus.fail, SerializationType.json);
//		try {
//			jsonResp.getDataElement().put("msg", "Access denied. You don't have enough privileges for this operation.");
//		} catch (JSONException e) {
//			throw new ServletException(e);
//		}
//		out.print(jsonResp.toString());
		
		out.print("Access denied. You don't have enough privileges for this operation");
	}

}
