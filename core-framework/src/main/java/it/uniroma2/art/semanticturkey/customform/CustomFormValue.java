package it.uniroma2.art.semanticturkey.customform;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.rdf4j.model.Value;


public class CustomFormValue {

	private String customFormId;
	private Map<String, Object> userPromptMap;
	private Map<String, Object> stdFormMap; //currently, just for stdForm/resource
	
	public CustomFormValue(@JsonProperty("customFormId") String customFormId, 
			@JsonProperty("userPromptMap") Map<String, Object> userPromptMap,
			@JsonProperty("stdFormMap") Map<String, Object> stdFormMap) {
		this.customFormId = customFormId;
		this.userPromptMap = userPromptMap;
		this.stdFormMap = stdFormMap;
	}
	
	public String getCustomFormId() {
		return this.customFormId;
	}
	
	public Map<String, Object> getUserPromptMap() {
		return this.userPromptMap;
	}

	public Map<String, Object> getStdFormMap() {
		return stdFormMap;
	}
}
