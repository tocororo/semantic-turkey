package it.uniroma2.art.semanticturkey.search;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import it.uniroma2.art.semanticturkey.project.STRepositoryInfo.SearchStrategies;
import it.uniroma2.art.semanticturkey.project.STRepositoryInfoUtils;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;

/**
 * An AOP Alliance {@link MethodInterceptor} implementation that manages search-relevant resources after the
 * execution of Semantic Turkey service (mutation) operations.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SearchUpdateInterceptor implements MethodInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(SearchUpdateInterceptor.class);

	@Autowired
	private STServiceContext stServiceContext;

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

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
				}

				@Override
				public void afterCompletion(int status) {
				}

				@Override
				public void afterCommit() {
					Repository repository = STServiceContextUtils.getRepostory(stServiceContext);
					RepositoryConnection connection = RDF4JRepositoryUtils.getConnection(repository, false);
					SearchStrategies searchStrategy = STRepositoryInfoUtils.getSearchStrategy(
							stServiceContext.getProject().getRepositoryManager().getSTRepositoryInfo(
									STServiceContextUtils.getRepostoryId(stServiceContext)));
					try {
						connection.begin();
						try {
							ValidationUtilities.executeWithoutValidation(
									ValidationUtilities.isValidationEnabled(stServiceContext), connection,
									conn -> {
										SearchStrategyUtils.instantiateSearchStrategy(searchStrategy)
												.update(connection);
									});
							connection.commit();
						} catch (Exception e) {
							logger.debug("Exception while updating search indexes", e);
							connection.rollback();
						}
					} catch (Exception e) {
						logger.debug("Exception occurred when updating search-related resources", e);
					}
				}
			});
		}

		return invocation.proceed();
	}

}