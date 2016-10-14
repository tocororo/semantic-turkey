package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.servlet.JSONResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;

@GenerateSTServiceController
@Validated
@Component
@Controller //needed for getUser method
public class Users extends STServiceAdapter {
	
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/getUser", 
			method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public String getUser(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {

		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("getUser", RepliesStatus.ok, SerializationType.json);
		
		Principal loggedUser = request.getUserPrincipal();
		if (loggedUser != null) {
			List<String> roles = new ArrayList<String>();
			Collection<? extends GrantedAuthority> authorities = 
					SecurityContextHolder.getContext().getAuthentication().getAuthorities();
			Iterator<? extends GrantedAuthority> authIt = authorities.iterator();
			while (authIt.hasNext()) {
				roles.add(authIt.next().getAuthority());
			}
			
			jsonResp.getDataElement().put("user", loggedUser.getName());
			jsonResp.getDataElement().put("roles", new JSONArray(roles));
		} else {
			//this should never be executed since getUser service is secured, so it cannot be invoked if no user is logged
			jsonResp.getDataElement().put("user", "none");
		}
		return jsonResp.toString();
	}
	
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/testRequiredAdmin", 
			method = RequestMethod.GET, produces = "application/json")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@ResponseBody
	public String testRequiredAdmin(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONResponseREPLY jsonResp = (JSONResponseREPLY) ServletUtilities.getService()
				.createReplyResponse("testRequiredAdmin", RepliesStatus.ok, SerializationType.json);
		return jsonResp.toString();
	}
	
//	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Users/testRequiredAdmin", 
//			method = RequestMethod.GET, produces = "application/xml")
//	@PreAuthorize("hasRole('ROLE_ADMIN')")
//	@ResponseBody
//	public String testRequiredAdmin(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
//		XMLResponseREPLY xmlResp = (XMLResponseREPLY) ServletUtilities.getService()
//				.createReplyResponse("testRequiredAdmin", RepliesStatus.ok, SerializationType.xml);
//		return xmlResp.toString();
//	}
	
	
	
}
