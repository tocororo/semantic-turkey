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
import it.uniroma2.art.semanticturkey.services.events.ResourceModified;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * Invokes an <em>update custom form</em> on a modified resource.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class UpdateCustomFormInvoker {

	private static final Logger logger = LoggerFactory.getLogger(UpdateCustomFormInvoker.class);

	@TransactionalEventListener(phase=Phase.beforeCommit)
	public void afterCommit(ResourceModified event) {
		Project project = event.getProject();
		String creationDateProp = project.getProperty(Project.CREATION_DATE_PROP);
		String modificationDateProp = project.getProperty(Project.MODIFICATION_DATE_PROP);

		if (creationDateProp == null && modificationDateProp == null)
			return;

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
