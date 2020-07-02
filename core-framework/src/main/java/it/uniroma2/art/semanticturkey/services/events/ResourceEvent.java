package it.uniroma2.art.semanticturkey.services.events;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.event.Event;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.user.STUser;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;

public abstract class ResourceEvent extends Event {

	private final RDFResourceRole role;
	private final Resource wgraph;
	private final Repository repository;
	private final Project project;
	private final STUser author;

	public ResourceEvent(Resource resource, RDFResourceRole role, Resource wgraph, Repository repository, Project project, STUser author) {
		super(resource);
		this.role = role;
		this.wgraph = wgraph;
		this.repository = repository;
		this.project = project;
		this.author = author;
}

	public Resource getResource() {
		return (Resource) getSource();
	}

	public Resource getWGraph() {
		return wgraph;
	}

	public RDFResourceRole getRole() {
		return role;
	}

	public Repository getRepository() {
		return repository;
	}

	public Project getProject() {
		return project;
	}

	public STUser getAuthor() {
		return author;
	}

}
