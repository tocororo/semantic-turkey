package it.uniroma2.art.semanticturkey.security.impl;

import org.opensaml.util.resource.ClasspathResource;
import org.opensaml.util.resource.ResourceException;

public class LocalClasspathResource extends ClasspathResource {

	public LocalClasspathResource(String path) throws ResourceException {
		super(path);
	}

}
