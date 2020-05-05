package it.uniroma2.art.semanticturkey.customform;

import java.util.HashMap;
import java.util.Map;

public class SessionFormData {
	
	public static final class Data {
		public static final String user = "user";
	}
	
	Map<String, Object> featureStruct;
	
	public SessionFormData() {
		featureStruct = new HashMap<>();
	}
	
	public void addSessionParameter(String sessionParam, String value) {
		featureStruct.put(sessionParam, value);
	}
	
	public void removeSessionParameter(String userPrompt) {
		featureStruct.remove(userPrompt);
	}

	public Map<String, Object> asMap() {
		return featureStruct;
	}
	

}
