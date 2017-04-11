package it.uniroma2.art.semanticturkey.customform;

import java.util.HashMap;
import java.util.Map;

public class StandardForm {
	
	public static final class Prompt {
		public static final String resource = "resource";
		public static final String label = "label";
		public static final String labelLang = "labelLang";
		public static final String xLabel = "xLabel";
		public static final String lexicalForm = "lexicalForm"; //TODO check
		public static final String schemes = "schemes";
	}
	
	Map<String, Object> form;
	
	public StandardForm() {
		form = new HashMap<>();
	}
	
	public void addFormEntry(String userPrompt, String value) {
		form.put(userPrompt, value);
	}
	
	public void removeFormEntry(String userPrompt) {
		form.remove(userPrompt);
	}

	public Map<String, Object> asMap() {
		return form;
	}
	
}

