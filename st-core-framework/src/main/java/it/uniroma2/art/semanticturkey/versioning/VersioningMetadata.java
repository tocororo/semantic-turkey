package it.uniroma2.art.semanticturkey.versioning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.rdf4j.model.Resource;

import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;

public class VersioningMetadata {
	private Map<Resource, RDFResourceRolesEnum> createdResources = new HashMap<>();
	private Map<Resource, RDFResourceRolesEnum> modifiedResources = new HashMap<>();

	public void addCreatedResource(Resource resource) {
		addCreatedResource(resource, RDFResourceRolesEnum.undetermined);
	}

	public void addCreatedResource(Resource resource, RDFResourceRolesEnum role) {
		createdResources.put(resource, role);
	}

	public void addModifiedResource(Resource resource) {
		addModifiedResource(resource, RDFResourceRolesEnum.undetermined);
	}

	public void addModifiedResource(Resource resource, RDFResourceRolesEnum role) {
		modifiedResources.put(resource, role);
	}

	public List<ImmutablePair<Resource, RDFResourceRolesEnum>> getCreatedResources() {
		return createdResources.entrySet().stream().map(e -> new ImmutablePair<>(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
	}

	public List<ImmutablePair<Resource, RDFResourceRolesEnum>> getModifiedResources() {
		return modifiedResources.entrySet().stream().map(e -> new ImmutablePair<>(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
	}

}
