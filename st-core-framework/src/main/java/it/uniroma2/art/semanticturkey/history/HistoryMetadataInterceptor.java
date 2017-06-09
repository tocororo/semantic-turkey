package it.uniroma2.art.semanticturkey.history;

import java.lang.reflect.Method;
import java.util.Optional;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import it.uniroma2.art.semanticturkey.aop.MethodInvocationUtilities;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Subject;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.vocabulary.STCHANGELOG;

/**
 * An AOP Alliance {@link MethodInterceptor} implementation that manages history-relevant metadata related to
 * Semantic Turkey service operations.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class HistoryMetadataInterceptor implements MethodInterceptor {

	@Autowired
	private STServiceContext stServiceContext;

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		IRI userIRI = UsersManager.getLoggedUser().getIRI();
		String extensionPathComponent = stServiceContext.getExtensionPathComponent();
		Class<?> serviceClass = invocation.getThis().getClass();
		Method serviceOperation = invocation.getMethod();

		IRI operationIRI = SimpleValueFactory.getInstance()
				.createIRI("http://semanticturkey.uniroma2.it/services/" + extensionPathComponent + "/"
						+ serviceClass.getSimpleName() + "/" + serviceOperation.getName());

		Optional<Resource> subjectResource = MethodInvocationUtilities
				.getValueOfFirstAnnotatedParameter(invocation, Subject.class, Resource.class);

		OperationMetadata operationMetadata = new OperationMetadata();
		operationMetadata.setUserIRI(userIRI, STCHANGELOG.PERFORMER); // TODO: make it sensitive to validation
		operationMetadata.setOperationIRI(operationIRI);
		subjectResource.ifPresent(operationMetadata::setSubject);

		HistoryMetadataSupport.setOperationMetadata(operationMetadata);

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

					if (!stServiceContext.getProject().isHistoryEnabled()
							&& !stServiceContext.getProject().isValidationEnabled())
						return;

					Repository repository = STServiceContextUtils.getRepostory(stServiceContext);
					RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repository, false);

					Model rdfOperationMetadata = operationMetadata.toRDF();
					conn.add(rdfOperationMetadata, CHANGETRACKER.COMMIT_METADATA);
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
			HistoryMetadataSupport.removeOperationMetadata();
		}

		return rv;
	}
}