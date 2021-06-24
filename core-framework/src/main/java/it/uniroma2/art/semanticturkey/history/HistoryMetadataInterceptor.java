package it.uniroma2.art.semanticturkey.history;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.primitives.Primitives;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.mvc.ResolvedRequestHolder;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.OmitHistoryMetadata;
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
	private LocalVariableTableParameterNameDiscoverer parameterNameDiscoverer;

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		IRI userIRI = UsersManager.getLoggedUser().getIRI();
		String extensionPathComponent = stServiceContext.getExtensionPathComponent();
		Class<?> serviceClass = invocation.getThis().getClass();
		Method serviceOperation = invocation.getMethod();
		Object[] serviceArguments = invocation.getArguments();

		OperationMetadata operationMetadata = new OperationMetadata();
		HistoryMetadataSupport.setOperationMetadata(operationMetadata);

		if (AnnotationUtils.findAnnotation(serviceOperation, OmitHistoryMetadata.class) == null) {

			IRI operationIRI = SimpleValueFactory.getInstance()
					.createIRI("http://semanticturkey.uniroma2.it/services/" + extensionPathComponent + "/"
							+ serviceClass.getSimpleName() + "/" + serviceOperation.getName());

			parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
			String[] parameterNames = parameterNameDiscoverer.getParameterNames(invocation.getMethod());
			String[] parameterValues = new String[parameterNames.length];

			HttpServletRequest request = ResolvedRequestHolder.get();

			for (int i = 0; i < parameterNames.length; i++) {
				String paramenterName = parameterNames[i];
				String providedValue = request.getParameter(paramenterName);
				if (providedValue == null) {
					Object actualValue = serviceArguments[i];

					if (actualValue == null)
						continue;

					Class<?> parameterType = serviceOperation.getParameterTypes()[i];

					if (MultipartFile.class.isAssignableFrom(parameterType)) {
						parameterValues[i] = ((MultipartFile) actualValue).getOriginalFilename();
					} else {
						Annotation[] paramAnnotations = invocation.getMethod().getParameterAnnotations()[i];
						String defaultValue = Arrays.stream(paramAnnotations)
								.filter(ann -> ann instanceof it.uniroma2.art.semanticturkey.services.annotations.Optional)
								.map(it.uniroma2.art.semanticturkey.services.annotations.Optional.class::cast)
								.map(ann -> ann.defaultValue()).findAny().orElse(null);

						if (defaultValue != null) {
							parameterValues[i] = defaultValue;
						} else {
							if (parameterType.isPrimitive() || Primitives.isWrapperType(parameterType)) {
								parameterValues[i] = actualValue.toString();
							}
						}
					}
				} else {
					parameterValues[i] = providedValue;
				}
			}
			operationMetadata.setUserIRI(userIRI, STCHANGELOG.PERFORMER);
			operationMetadata.setOperation(operationIRI, parameterNames, parameterValues);

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
								&& !stServiceContext.getProject().isValidationEnabled() && !stServiceContext
						.getProject().isUndoEnabled())
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