package it.uniroma2.art.semanticturkey.services;

import java.util.Set;

public interface STRequest {

	String getServiceClass();
	String getServiceMethod();
	Set<String> getParams();
	String getParamValue(String param);
	
}
