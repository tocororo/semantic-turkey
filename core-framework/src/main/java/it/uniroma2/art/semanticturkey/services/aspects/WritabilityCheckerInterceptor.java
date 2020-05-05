package it.uniroma2.art.semanticturkey.services.aspects;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;

/**
 * An AOP Alliance {@link MethodInterceptor} implementation that enforces a check on the writability of the
 * current repository for a method annotated with {@link Write}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class WritabilityCheckerInterceptor implements MethodInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(WritabilityCheckerInterceptor.class);

	@Autowired
	private STServiceContext stServiceContext;

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		String version = stServiceContext.getVersion();

		if (version != null) {
			throw new IllegalArgumentException(
					"Can't execute a @Write-annotated operation on a version dump");
		}

		Repository repository = STServiceContextUtils.getRepostory(stServiceContext);

		if (repository == null) {
			throw new IllegalArgumentException(
					"Contextual repository for an @Write-annotated operation not specified");
		}

		if (!repository.isWritable()) {
			throw new IllegalArgumentException(
					"Contextual repository for an @Write-annotated operation not writable");
		}

		return invocation.proceed();
	}
}