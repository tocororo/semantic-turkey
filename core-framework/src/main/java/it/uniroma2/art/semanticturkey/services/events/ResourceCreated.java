package it.uniroma2.art.semanticturkey.services.events;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.event.Event;
import it.uniroma2.art.semanticturkey.event.TransactionalEventListener;
import it.uniroma2.art.semanticturkey.project.Project;

/**
 * A {@link Event} raised when a resource has been created. Usually, raised within a transaction affecting a
 * {@link Repository}, this event can be handled through a {@link TransactionalEventListener}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ResourceCreated extends Event {

	private static final long serialVersionUID = 5598632870917194880L;

	private RDFResourceRole role;
	private Resource wgraph;
	private Repository repository;
	private Project project;

	public ResourceCreated(Resource resource, RDFResourceRole role, Resource wgraph, Repository repository,
			Project project) {
		super(resource);
		this.role = role;
		this.wgraph = wgraph;
		this.repository = repository;
		this.project = project;
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

}
