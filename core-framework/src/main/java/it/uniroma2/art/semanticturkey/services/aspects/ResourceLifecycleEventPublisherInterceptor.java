package it.uniroma2.art.semanticturkey.services.aspects;

import java.util.Optional;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import it.uniroma2.art.semanticturkey.aop.MethodInvocationUtilities;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.data.role.RoleRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Created;
import it.uniroma2.art.semanticturkey.services.annotations.Deleted;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.events.ResourceCreated;
import it.uniroma2.art.semanticturkey.services.events.ResourceDeleted;
import it.uniroma2.art.semanticturkey.services.events.ResourceModified;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * An AOP Alliance {@link MethodInterceptor} implementation that manages events about resouce lifecycle
 * related to Semantic Turkey service operations.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ResourceLifecycleEventPublisherInterceptor implements MethodInterceptor {

	private static final Logger logger = LoggerFactory
			.getLogger(ResourceLifecycleEventPublisherInterceptor.class);

	@Autowired
	private STServiceContext stServiceContext;

	@Autowired
	private ApplicationContext applicationContext;

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		Optional<ImmutablePair<Resource, Created>> createdResource = MethodInvocationUtilities
				.getFirstAnnotatedArgument(invocation, Created.class, Resource.class, true);
		Optional<ImmutablePair<Resource, Modified>> modifiedResource = MethodInvocationUtilities
				.getFirstAnnotatedArgument(invocation, Modified.class, Resource.class, true);
		Optional<ImmutablePair<Resource, Deleted>> deletedResource = MethodInvocationUtilities
				.getFirstAnnotatedArgument(invocation, Deleted.class, Resource.class, true);

		ResourceLevelChangeMetadata versioningMetadata = ResourceLevelChangeMetadataSupport
				.currentVersioningMetadata();

		createdResource
				.ifPresent(p -> versioningMetadata.addCreatedResource(p.getLeft(), p.getRight().role()));
		modifiedResource
				.ifPresent(p -> versioningMetadata.addModifiedResource(p.getLeft(), p.getRight().role()));
		deletedResource
				.ifPresent(p -> versioningMetadata.addDeletedResource(p.getLeft(), p.getRight().role()));

		Project project = stServiceContext.getProject();
		STUser user = UsersManager.getLoggedUser();

		Repository repository = STServiceContextUtils.getRepostory(stServiceContext);
		RepositoryConnection repConn = RDF4JRepositoryUtils.getConnection(repository);
		try {
			Object rv;

			try {

				// deletions are published before the actually occur
				for (Pair<Resource, RDFResourceRole> pair : versioningMetadata.getDeletedResources()) {
					Pair<Resource, RDFResourceRole> enhancedPair = enhanceResourceChangeInfo(repConn, pair);
					applicationContext
							.publishEvent(new ResourceDeleted(enhancedPair.getLeft(), enhancedPair.getRight(),
									stServiceContext.getWGraph(), repository, project, user));
				}

				rv = invocation.proceed();

				for (Pair<Resource, RDFResourceRole> pair : versioningMetadata.getCreatedResources()) {
					Pair<Resource, RDFResourceRole> enhancedPair = enhanceResourceChangeInfo(repConn, pair);
					applicationContext
							.publishEvent(new ResourceCreated(enhancedPair.getLeft(), enhancedPair.getRight(),
									stServiceContext.getWGraph(), repository, project, user));
				}

				for (Pair<Resource, RDFResourceRole> pair : versioningMetadata.getModifiedResources()) {
					Pair<Resource, RDFResourceRole> enhancedPair = enhanceResourceChangeInfo(repConn, pair);
					applicationContext.publishEvent(
							new ResourceModified(enhancedPair.getLeft(), enhancedPair.getRight(),
									stServiceContext.getWGraph(), repository, project, user));
				}

			} finally {
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
						public void beforeCommit(boolean readOnly) {

						}

						@Override
						public void beforeCompletion() {

						}

						@Override
						public void afterCommit() {

						}

						@Override
						public void afterCompletion(int status) {
							ResourceLevelChangeMetadataSupport.removeVersioningMetadata();
						}
					});
				} else {
					ResourceLevelChangeMetadataSupport.removeVersioningMetadata();
				}
			}

			return rv;
		} finally {
			RDF4JRepositoryUtils.releaseConnection(repConn, repository);
		}
	}

	protected Pair<Resource, RDFResourceRole> enhanceResourceChangeInfo(RepositoryConnection repConn,
			Pair<Resource, RDFResourceRole> pair) {
		Resource resource = pair.getLeft();
		RDFResourceRole resourceRole = pair.getRight();

		Pair<Resource, RDFResourceRole> enhancedPair;
		if (resourceRole == RDFResourceRole.undetermined) {
			RDFResourceRole computedResourceRole = RoleRecognitionOrchestrator.computeRole(resource, repConn);
			enhancedPair = ImmutablePair.of(resource, computedResourceRole);
		} else {
			enhancedPair = pair;
		}
		return enhancedPair;
	}

}