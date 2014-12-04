package it.uniroma2.art.semanticturkey.data.id;

import it.uniroma2.art.owlart.model.ARTURIResource;

public class ARTURIResAndRandomString {

	String randomValue;
	ARTURIResource artURIResource;
	
	public ARTURIResAndRandomString() { }
	
	public ARTURIResAndRandomString(String randomValue, ARTURIResource artUriResource) {
		super();
		this.randomValue = randomValue;
		this.artURIResource = artUriResource;
	}
	
	public void setRandomValue(String randomValue) {
		this.randomValue = randomValue;
	}

	public void setArtURIResource(ARTURIResource artURIResource) {
		this.artURIResource = artURIResource;
	}

	public String getRandomValue() {
		return randomValue;
	}

	public ARTURIResource getArtURIResource() {
		return artURIResource;
	}
	
}
