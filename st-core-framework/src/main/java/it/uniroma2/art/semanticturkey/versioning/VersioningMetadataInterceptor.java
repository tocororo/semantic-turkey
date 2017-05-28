package it.uniroma2.art.semanticturkey.versioning;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.util.MethodInvocationUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ReflectionUtils;

import it.uniroma2.art.semanticturkey.aop.MethodInvocationUtilities;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Created;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.annotations.Subject;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryConnectionHolder;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.RDF4JMigrationUtils;
import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;
import it.uniroma2.art.semanticturkey.vocabulary.STCHANGELOG;

/**
 * An AOP Alliance {@link MethodInterceptor} implementation that manages versioning-relevant metadata related
 * to Semantic Turkey service operations.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class VersioningMetadataInterceptor implements MethodInterceptor {

	@Autowired
	private STServiceContext stServiceContext;

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		Optional<Resource> createdResource = MethodInvocationUtilities
				.getValueOfFirstAnnotatedParameter(invocation, Created.class, Resource.class);
		Optional<Resource> modifiedResource = MethodInvocationUtilities
				.getValueOfFirstAnnotatedParameter(invocation, Modified.class, Resource.class);

		VersioningMetadata versioningMetadata = VersioningMetadataSupport.currentVersioningMetadata();

		createdResource.ifPresent(versioningMetadata::addCreatedResource);
		modifiedResource.ifPresent(versioningMetadata::addModifiedResource);

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

					Project<?> project = stServiceContext.getProject();
					String creationDateProp = project.getProperty(Project.CREATION_DATE_PROP);
					String modificationDateProp = project.getProperty(Project.MODIFICATION_DATE_PROP);

					if (creationDateProp == null && modificationDateProp == null)
						return;

					Repository repository = STServiceContextUtils.getRepostory(stServiceContext);
					RDF4JRepositoryConnectionHolder connHolder = (RDF4JRepositoryConnectionHolder) TransactionSynchronizationManager
							.getResource(repository);

					if (connHolder != null && connHolder.hasConnection()) {

						RepositoryConnection conn = connHolder.getConnection();

						ValueFactory vf = conn.getValueFactory();

						Literal currentTime = vf.createLiteral(new Date());

						Resource workingGraph = RDF4JMigrationUtils
								.convert2rdf4j(stServiceContext.getWGraph());

						// Usually, we expect either one created resource or one modified resource, so
						// there should be no need of complex buffering techiniques

						if (creationDateProp != null) {
							IRI creationDatePropIRI = vf.createIRI(creationDateProp);
							for (Resource r : versioningMetadata.getCreatedResources()) {
								conn.add(r, creationDatePropIRI, currentTime, workingGraph);
							}
						}

						if (modificationDateProp != null) {
							IRI modificationDatePropIRI = vf.createIRI(modificationDateProp);
							for (Resource r : versioningMetadata.getModifiedResources()) {
								conn.remove(r, modificationDatePropIRI, null, workingGraph);
								conn.add(r, modificationDatePropIRI, currentTime, workingGraph);
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
}