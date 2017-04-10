package it.uniroma2.art.semanticturkey.customform;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

public class StandardForm {
	
	private static final String RESOURCE_URI_FS_NAME = "resource";
	private static final String LABEL_FS_NAME = "label";
	private static final String XLABEL_URI_FS_NAME = "xLabel";
	private static final String LANG_FS_NAME = "labelLang";
//	private static final String SCHEMES_FS_NAME = "schemes";
	
	private IRI resource;
	private Literal label; //rdfs or skos label or literal form of skosxl label
	private IRI xLabel;
	private String lang; //langTag of label
//	private String schemes;
	
	public StandardForm(IRI resource, Literal label, String lang) {
		this.resource = resource;
		this.label = label;
		this.lang = lang;
	}
	
	public StandardForm(IRI resource, IRI xLabel, Literal label, String lang) {
		this.resource = resource;
		this.xLabel = xLabel;
		this.label = label;
		this.lang = lang;
	}

	public IRI getResource() {
		return resource;
	}

	public void setResource(IRI resource) {
		this.resource = resource;
	}

	public Literal getLabel() {
		return label;
	}

	public void setLabel(Literal label) {
		this.label = label;
	}
	
	public IRI getXLabel() {
		return xLabel;
	}
	
	public void setXLabel(IRI xLabel) {
		this.xLabel = xLabel;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}
	
	public Map<String, Object> asMap() {
		Map<String, Object> formMap = new HashMap<String, Object>();
		if (resource != null) {
			formMap.put(RESOURCE_URI_FS_NAME, resource.stringValue());
		}
		if (label != null) {
			formMap.put(LABEL_FS_NAME, label.getLabel());
		}
		if (xLabel != null) {
			formMap.put(XLABEL_URI_FS_NAME, xLabel.stringValue());
		}
		if (lang != null) {
			formMap.put(LANG_FS_NAME, lang);
		}
		return formMap;
	}
	
}

