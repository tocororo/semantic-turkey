package it.uniroma2.art.semanticturkey.versioning;

import java.util.Date;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.data.role.RoleRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.event.annotation.TransactionalEventListener;
import it.uniroma2.art.semanticturkey.event.annotation.TransactionalEventListener.Phase;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.events.ResourceCreated;
import it.uniroma2.art.semanticturkey.services.events.ResourceDeleted;
import it.uniroma2.art.semanticturkey.services.events.ResourceModified;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * Managed metadata about a resource across its life cycle.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ResourceMetadataManager {

	private static final Logger logger = LoggerFactory.getLogger(ResourceMetadataManager.class);

	/**
	 * Manages metadata about a resource just created. This listener is executed before the current
	 * transaction is committed, so that it can add metadata within the same transaction that created a
	 * resource.
	 * 
	 * @param event
	 */
	@TransactionalEventListener(phase = Phase.beforeCommit)
	public void onCreation(ResourceCreated event) {
		System.out.println("Created: " + event.getResource());
		Project project = event.getProject();
		String creationDateProp = project.getProperty(Project.CREATION_DATE_PROP);

		Repository repository = event.getRepository();
		RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repository, false);

		ValueFactory vf = conn.getValueFactory();

		Literal currentTime = vf.createLiteral(new Date());

		// Resource workingGraph =
		Resource workingGraph = event.getWGraph();

		Resource r = event.getResource();

		// Usually, we expect either one created resource or one modified resource, so
		// there should be no need of complex buffering techniques

		if (creationDateProp != null) {
			IRI creationDatePropIRI = vf.createIRI(creationDateProp);
			if (determineNecessityOfMetadata(conn, r, event.getRole(), project)) {
				conn.remove(r, creationDatePropIRI, null, workingGraph);
				conn.add(r, creationDatePropIRI, currentTime, workingGraph);
			}
		}
	}

	/**
	 * Manages metadata about a resource that was updated. This listener is executed before the current
	 * transaction is committed, so that it can add metadata within the same transaction that updated a
	 * resource.
	 * 
	 * @param event
	 */
	@TransactionalEventListener(phase = Phase.beforeCommit)
	public void onUpdate(ResourceModified event) {
		System.out.println("Updated: " + event.getResource());

		Project project = event.getProject();
		String modificationDateProp = project.getProperty(Project.MODIFICATION_DATE_PROP);

		Repository repository = event.getRepository();
		RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repository, false);

		ValueFactory vf = conn.getValueFactory();

		Literal currentTime = vf.createLiteral(new Date());

		// Resource workingGraph =
		Resource workingGraph = event.getWGraph();

		Resource r = event.getResource();

		// Usually, we expect either one created resource or one modified resource, so
		// there should be no need of complex buffering techniques

		if (modificationDateProp != null) {
			IRI modificationDatePropIRI = vf.createIRI(modificationDateProp);
			if (determineNecessityOfMetadata(conn, r, event.getRole(), project)) {
				conn.remove(r, modificationDatePropIRI, null, workingGraph);
				conn.add(r, modificationDatePropIRI, currentTime, workingGraph);
			}
		}
	}

	/**
	 * Manages metadata about a resource that was created. This listener is executed before the current
	 * transaction is committed, so that it can manipulate metadata within the same transaction that delete a
	 * resource. Deleting a resource implies that every statement about it is deleted from the repository;
	 * therefore, it is not possible to add metadata, such as the deletion date/time, because the subject
	 * resource no longer exists after the successful completion of the transaction. However, it might be
	 * necessary to explicitly delete metadata, in particular, when there are 2nd-level resources.
	 * 
	 * @param event
	 */
	@TransactionalEventListener(phase = Phase.beforeCommit)
	public void onDeletions(ResourceDeleted event) {
		System.out.println("Deleted: " + event.getResource());
	}

	private boolean determineNecessityOfMetadata(RepositoryConnection conn, Resource resource,
			RDFResourceRole role, Project project) {

		logger.debug("Given role: {}", role);

		if (role == RDFResourceRole.undetermined) {
			role = RDFResourceRole.valueOf(RoleRecognitionOrchestrator.computeRole(resource, conn).name());
		}

		logger.debug("After computation role: {}", role);

		for (RDFResourceRole updatableRole : project.getUpdateForRoles()) {
			if (RDFResourceRole.subsumes(updatableRole, role, true)) {
				logger.debug("Role {} is subsumed by role {}", role, updatableRole);
				return true;
			}
		}

		logger.debug("Do not update");

		return false;
	}

}
