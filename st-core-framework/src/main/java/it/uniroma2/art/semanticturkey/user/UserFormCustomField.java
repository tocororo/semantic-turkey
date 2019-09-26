package it.uniroma2.art.semanticturkey.user;

import org.eclipse.rdf4j.model.IRI;

public class UserFormCustomField {
	
	private IRI iri;
	private int position;
	private String label;
	
	public UserFormCustomField(IRI iri, int position, String label) {
		this.iri = iri;
		this.position = position;
		this.label = label;
	}

	public IRI getIri() {
		return iri;
	}

	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	
	

}
