package it.uniroma2.art.semanticturkey.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.servlet.JSONResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;

/**
 * @author Tiziano
 * Specifies what happen when a user successes to login.
 * Simply returns a 200 response (ok) with a user Json object
 * (Referenced in WEB-INF/spring-security.xml)
 */
@Component
public class STAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		ServletOutputStream out = response.getOutputStream();
		
		List<String> roles = new ArrayList<String>();
		Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
		Iterator<? extends GrantedAuthority> authIt = authorities.iterator();
		while (authIt.hasNext()) {
			roles.add(authIt.next().getAuthority());
		}
		
		//create json response with the same structure of STResponse
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("login", RepliesStatus.ok, SerializationType.json);
		try {
			jsonResp.getDataElement().put("user", authentication.getName());
			jsonResp.getDataElement().put("roles", new JSONArray(roles));
		} catch (JSONException e) {
			throw new ServletException(e);
		}
		out.print(jsonResp.toString());
		
	}
}