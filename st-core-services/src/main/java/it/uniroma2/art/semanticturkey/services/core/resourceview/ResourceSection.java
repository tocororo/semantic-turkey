package it.uniroma2.art.semanticturkey.services.core.resourceview;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import it.uniroma2.art.semanticturkey.services.AnnotatedValue;

public class ResourceSection implements ResourceViewSection {
	private AnnotatedValue<?> annotatedResource;

	public ResourceSection(AnnotatedValue<?> annotatedResource) {
		this.annotatedResource = annotatedResource;
	}

	@JsonUnwrapped
	public AnnotatedValue<?> getAnnotatedResource() {
		return annotatedResource;
	}
}
