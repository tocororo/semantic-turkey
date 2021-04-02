package it.uniroma2.art.semanticturkey.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 
 * @author Tiziano
 * We need a custom authenticationEntryPoint because default Spring-Security config will
 * redirect to login page when a not authenticated user performs a request.
 * In our case we need just a https status 401 and a json response.
 * (Referenced in WEB-INF/spring-security.xml)
 */
public class STAuthenticationEntryPoint implements AuthenticationEntryPoint {
	
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("text/plain");
		ServletOutputStream out = response.getOutputStream();
		
		out.print("Access denied. You need to be logged in");
	}

}
