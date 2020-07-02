package it.uniroma2.art.semanticturkey.services.events;

import it.uniroma2.art.semanticturkey.user.STUser;
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
public class ResourceCreated extends ResourceEvent {

	private static final long serialVersionUID = 5598632870917194880L;

	public ResourceCreated(Resource resource, RDFResourceRole role, Resource wgraph, Repository repository,
			Project project, STUser author) {
		super(resource, role, wgraph, repository, project, author);
	}

}
