package it.uniroma2.art.semanticturkey.customform;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;


public class CustomFormValue {

	private String customFormId;
	private Map<String, Object> userPromptMap;
	
	public CustomFormValue(@JsonProperty("customFormId") String customFormId, 
			@JsonProperty("userPromptMap") Map<String, Object> userPromptMap) {
		this.customFormId = customFormId;
		this.userPromptMap = userPromptMap;
	}
	
	public String getCustomFormId() {
		return this.customFormId;
	}
	
	public Map<String, Object> getUserPromptMap() {
		return this.userPromptMap;
	}
	
}
