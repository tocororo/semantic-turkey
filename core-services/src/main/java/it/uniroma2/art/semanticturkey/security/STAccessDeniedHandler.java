package it.uniroma2.art.semanticturkey.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 
 * @author Tiziano
 * Specifies what happen when an authenticated user perform a request for which has not the required role
 * Simply returns a 403 response (forbidden) with a json content
 * (Referenced in WEB-INF/spring-security.xml)
 *
 */
public class STAccessDeniedHandler implements AccessDeniedHandler {

	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
	        throws IOException, ServletException {
		
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		ServletOutputStream out = response.getOutputStream();

		out.print("Access denied. You don't have enough privileges for this operation");
	}

}
