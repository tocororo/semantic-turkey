package it.uniroma2.art.semanticturkey.customform;

/**
 * Mapping between a class/property and a {@link FormCollection}
 * @author Tiziano
 *
 */
public class FormCollectionMapping {
	
	private String resourceURI; //prop or class. This cannot be an IRI since when CustomFormsConfig parses a mapping
		//it cannot create the IRI object from the URI of the prop/class
	private FormCollection formCollection;
	private boolean replace;

	public FormCollectionMapping(String resourceURI, FormCollection formCollection, boolean replace) {
		this.resourceURI = resourceURI;
		this.formCollection = formCollection;
		this.replace = replace;
	}

	public String getResource() {
		return resourceURI;
	}

	public void setResource(String resourceURI) {
		this.resourceURI = resourceURI;
	}

	public FormCollection getFormCollection() {
		return formCollection;
	}

	public void setFormCollection(FormCollection formCollection) {
		this.formCollection = formCollection;
	}

	public boolean getReplace() {
		return replace;
	}

	public void setReplace(boolean replace) {
		this.replace = replace;
	}
}
