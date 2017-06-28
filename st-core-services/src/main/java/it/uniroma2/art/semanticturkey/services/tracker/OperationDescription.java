package it.uniroma2.art.semanticturkey.services.tracker;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import it.uniroma2.art.semanticturkey.mvc.IntrospectableController;
import it.uniroma2.art.semanticturkey.services.annotations.DisplayName;

/**
 * Metadata about ST service operations.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class OperationDescription {
	private String groupId;
	private String artifactId;
	private String serviceClass;
	private String operation;
	private Entry<RequestMappingInfo, HandlerMethod> springEntry;
	private IRI operationIRI;
	private String displayName;

	protected OperationDescription(String groupId, String artifactId, String serviceClass, String operation,
			Entry<RequestMappingInfo, HandlerMethod> springEntry, String displayName) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.serviceClass = serviceClass;
		this.operation = operation;
		this.springEntry = springEntry;

		this.operationIRI = SimpleValueFactory.getInstance()
				.createIRI("http://semanticturkey.uniroma2.it/services/" + groupId + "/" + artifactId + "/"
						+ serviceClass + "/" + operation);

		this.displayName = displayName;
	}

	public static OperationDescription create(ApplicationContext applicationContext, String groupId,
			String artifactId, String serviceClass, String operation,
			Entry<RequestMappingInfo, HandlerMethod> springEntry) {
		String displayName = Arrays
				.stream(((IntrospectableController) applicationContext
						.getBean((String) springEntry.getValue().getBean())).getService().getClass()
								.getMethods())
				.filter(m -> m.getName().equals(operation)).findAny().map(serviceMethod -> {

					DisplayName displayNameAnnot = AnnotationUtils.findAnnotation(serviceMethod,
							DisplayName.class);

					if (displayNameAnnot != null) {
						return displayNameAnnot.value();
					}

					return null;
				}).orElse(null);

		return new OperationDescription(groupId, artifactId, serviceClass, operation, springEntry,
				displayName);
	}

	public Entry<RequestMappingInfo, HandlerMethod> getSpringEntry() {
		return springEntry;
	}

	public IRI getOperationIRI() {
		return operationIRI;
	}

	public Optional<String> getDisplayName() {
		return Optional.ofNullable(displayName);
	}
}
