package it.uniroma2.art.semanticturkey.versioning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.model.Resource;

public class VersioningMetadata {
	private List<Resource> createdResources = new ArrayList<>();
	private List<Resource> modifiedResources = new ArrayList<>();
	
	public void addCreatedResource(Resource resource) {
		createdResources.add(resource);
	}

	public void addModifiedResource(Resource resource) {
		modifiedResources.add(resource);
	}

	public List<Resource> getCreatedResources() {
		return Collections.unmodifiableList(createdResources);
	}

	public List<Resource> getModifiedResources() {
		return Collections.unmodifiableList(modifiedResources);
	}

}
