package it.uniroma2.art.semanticturkey.services.http;

import it.uniroma2.art.semanticturkey.services.STRequest;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

public class STHTTPRequest implements STRequest {
	
	private HttpServletRequest request;
	
	public STHTTPRequest(HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public String getServiceClass() {
		return getServiceClassFromRequest();
	}

	@Override
	public String getServiceMethod() {
		return getServiceMethodFromRequest();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<String> getParams() {
		Enumeration<String> params = request.getParameterNames();
		Set<String> set = new HashSet<String>();
		while (params.hasMoreElements()){
			set.add(params.nextElement());
		}
		return set;
	}

	@Override
	public String getParamValue(String param) {
		return request.getParameter(param);
	}
	
	private String getServiceClassFromRequest(){
		String[] pathElements = request.getServletPath().split("/");
		return pathElements[pathElements.length - 2];
	}
	
	private String getServiceMethodFromRequest(){
		String[] pathElements = request.getServletPath().split("/");
		return pathElements[pathElements.length - 1];
	}

}
