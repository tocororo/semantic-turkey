package it.uniroma2.art.semanticturkey.versioning;

import java.util.Date;
import java.util.Optional;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import it.uniroma2.art.semanticturkey.aop.MethodInvocationUtilities;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.data.role.RoleRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Created;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * An AOP Alliance {@link MethodInterceptor} implementation that manages versioning-relevant metadata related
 * to Semantic Turkey service operations.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class VersioningMetadataInterceptor implements MethodInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(VersioningMetadataInterceptor.class);
	
	@Autowired
	private STServiceContext stServiceContext;

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		Optional<ImmutablePair<Resource, Created>> createdResource = MethodInvocationUtilities
				.getFirstAnnotatedArgument(invocation, Created.class, Resource.class);
		Optional<ImmutablePair<Resource, Modified>> modifiedResource = MethodInvocationUtilities
				.getFirstAnnotatedArgument(invocation, Modified.class, Resource.class);

		VersioningMetadata versioningMetadata = VersioningMetadataSupport.currentVersioningMetadata();

		createdResource
				.ifPresent(p -> versioningMetadata.addCreatedResource(p.getLeft(), p.getRight().role()));
		modifiedResource
				.ifPresent(p -> versioningMetadata.addModifiedResource(p.getLeft(), p.getRight().role()));

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

				@Override
				public void suspend() {
				}

				@Override
				public void resume() {
				}

				@Override
				public void flush() {
				}

				@Override
				public void beforeCompletion() {
				}

				@Override
				public void beforeCommit(boolean readOnly) {
					if (readOnly)
						return;

					Project project = stServiceContext.getProject();
					String creationDateProp = project.getProperty(Project.CREATION_DATE_PROP);
					String modificationDateProp = project.getProperty(Project.MODIFICATION_DATE_PROP);

					if (creationDateProp == null && modificationDateProp == null)
						return;

					Repository repository = STServiceContextUtils.getRepostory(stServiceContext);
					RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repository, false);

					ValueFactory vf = conn.getValueFactory();

					Literal currentTime = vf.createLiteral(new Date());

					//Resource workingGraph = RDF4JMigrationUtils.convert2rdf4j(stServiceContext.getWGraph());
					Resource workingGraph = stServiceContext.getWGraph();

					
					// Usually, we expect either one created resource or one modified resource, so
					// there should be no need of complex buffering techiniques

					if (creationDateProp != null) {
						IRI creationDatePropIRI = vf.createIRI(creationDateProp);
						for (ImmutablePair<Resource, RDFResourceRole> r : versioningMetadata
								.getCreatedResources()) {
							if (determineNecessityOfMetadata(conn, r)) {
								conn.add(r.getLeft(), creationDatePropIRI, currentTime, workingGraph);
							}
						}
					}

					if (modificationDateProp != null) {
						IRI modificationDatePropIRI = vf.createIRI(modificationDateProp);
						for (ImmutablePair<Resource, RDFResourceRole> r : versioningMetadata
								.getModifiedResources()) {
							if (determineNecessityOfMetadata(conn, r)) {
								conn.remove(r.getLeft(), modificationDatePropIRI, null, workingGraph);
								conn.add(r.getLeft(), modificationDatePropIRI, currentTime, workingGraph);
							}
						}
					}

				}

				@Override
				public void afterCompletion(int status) {
				}

				@Override
				public void afterCommit() {
				}
			});
		}

		Object rv;
		try {
			rv = invocation.proceed();
		} finally {
			VersioningMetadataSupport.removeVersioningMetadata();
		}

		return rv;
	}

	private boolean determineNecessityOfMetadata(RepositoryConnection conn,
			ImmutablePair<Resource, RDFResourceRole> r) {

		RDFResourceRole role = r.getRight();

		logger.debug("Given role: {}", role);

		if (role == RDFResourceRole.undetermined) {
			role = RDFResourceRole.valueOf(RoleRecognitionOrchestrator.computeRole(r.getLeft(), conn).name());
		}

		logger.debug("After computation role: {}", role);

		for (RDFResourceRole updatableRole : stServiceContext.getProject().getUpdateForRoles()) {
			if (RDFResourceRole.subsumes(updatableRole, role, true)) {
				logger.debug("Role {} is subsumed by role {}", role, updatableRole);
				return true;
			}
		}

		logger.debug("Do not update");

		return false;
	}

}