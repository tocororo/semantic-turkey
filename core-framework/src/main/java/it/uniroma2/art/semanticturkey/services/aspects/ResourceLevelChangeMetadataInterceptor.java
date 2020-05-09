package it.uniroma2.art.semanticturkey.services.aspects;

import java.util.Optional;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import it.uniroma2.art.semanticturkey.aop.MethodInvocationUtilities;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Created;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.events.ResourceModified;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;

/**
 * An AOP Alliance {@link MethodInterceptor} implementation that manages resource-level change metadata
 * related to Semantic Turkey service operations.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ResourceLevelChangeMetadataInterceptor implements MethodInterceptor {

	private static final Logger logger = LoggerFactory
			.getLogger(ResourceLevelChangeMetadataInterceptor.class);

	@Autowired
	private STServiceContext stServiceContext;

	@Autowired
	private ApplicationContext applicationContext;

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		Optional<ImmutablePair<Resource, Created>> createdResource = MethodInvocationUtilities
				.getFirstAnnotatedArgument(invocation, Created.class, Resource.class);
		Optional<ImmutablePair<Resource, Modified>> modifiedResource = MethodInvocationUtilities
				.getFirstAnnotatedArgument(invocation, Modified.class, Resource.class);

		ResourceLevelChangeMetadata versioningMetadata = ResourceLevelChangeMetadataSupport
				.currentVersioningMetadata();

		createdResource
				.ifPresent(p -> versioningMetadata.addCreatedResource(p.getLeft(), p.getRight().role()));
		modifiedResource
				.ifPresent(p -> versioningMetadata.addModifiedResource(p.getLeft(), p.getRight().role()));

		Project project = stServiceContext.getProject();

		Repository repository = STServiceContextUtils.getRepostory(stServiceContext);

		for (ImmutablePair<Resource, RDFResourceRole> pair : versioningMetadata.getModifiedResources()) {
			applicationContext.publishEvent(new ResourceModified(pair.getLeft(), pair.getRight(),
					stServiceContext.getWGraph(), repository, project));
		}

		Object rv;
		try {
			rv = invocation.proceed();
		} finally {
			ResourceLevelChangeMetadataSupport.removeVersioningMetadata();
		}

		return rv;
	}

}