package it.uniroma2.art.semanticturkey.services.aspects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.rdf4j.model.Resource;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;

public class ResourceLevelChangeMetadata {
	private Map<Resource, RDFResourceRole> createdResources = new HashMap<>();
	private Map<Resource, RDFResourceRole> modifiedResources = new HashMap<>();
	private Map<Resource, RDFResourceRole> deletedResources = new HashMap<>();

	public void addCreatedResource(Resource resource) {
		addCreatedResource(resource, RDFResourceRole.undetermined);
	}

	public void addCreatedResource(Resource resource, RDFResourceRole role) {
		createdResources.put(resource, role);
	}

	public void addModifiedResource(Resource resource) {
		addModifiedResource(resource, RDFResourceRole.undetermined);
	}

	public void addModifiedResource(Resource resource, RDFResourceRole role) {
		modifiedResources.put(resource, role);
	}

	public void addDeletedResource(Resource resource) {
		deletedResources.put(resource, RDFResourceRole.undetermined);
	}

	public void addDeletedResource(Resource resource, RDFResourceRole role) {
		deletedResources.put(resource, role);
	}

	public List<ImmutablePair<Resource, RDFResourceRole>> getCreatedResources() {
		return createdResources.entrySet().stream().map(e -> new ImmutablePair<>(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
	}

	public List<ImmutablePair<Resource, RDFResourceRole>> getModifiedResources() {
		return modifiedResources.entrySet().stream().map(e -> new ImmutablePair<>(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
	}

	public List<ImmutablePair<Resource, RDFResourceRole>> getDeletedResources() {
		return deletedResources.entrySet().stream().map(e -> new ImmutablePair<>(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
	}

}
