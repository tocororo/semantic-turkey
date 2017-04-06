package it.uniroma2.art.semanticturkey.customform;

import java.util.HashMap;
import java.util.Map;

public class StandardForm {
	
	private static final String URI_FS_NAME = "stdUri";
	private static final String LABEL_FS_NAME = "stdLabel";
	private static final String LANG_FS_NAME = "stdLang";
	
	private String uri;
	private String label; //this could be a URI (xLabel) or a literal (rdfs or skos label)
	private String lang;
	
	public StandardForm(String uri, String label, String lang) {
		this.uri = uri;
		this.label = label;
		this.lang = lang;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}
	
	public Map<String, Object> asMap() {
		Map<String, Object> formMap = new HashMap<String, Object>();
		if (uri != null) {
			formMap.put(URI_FS_NAME, uri);
		}
		if (label != null) {
			formMap.put(LABEL_FS_NAME, label);
		}
		if (lang != null) {
			formMap.put(LANG_FS_NAME, lang);
		}
		return formMap;
	}
	
}

