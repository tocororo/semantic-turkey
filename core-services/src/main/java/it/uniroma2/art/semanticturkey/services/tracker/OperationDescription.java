package it.uniroma2.art.semanticturkey.services.tracker;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.uniroma2.art.semanticturkey.mvc.IntrospectableController;
import it.uniroma2.art.semanticturkey.services.annotations.DisplayName;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Metadata about ST service operations.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class OperationDescription {

	public static class Parameter {
		private Type type;
		private boolean required;

		public Parameter(Type type, boolean required) {
			this.type = type;
			this.required = required;
		}

		public Type getType() {
			return type;
		}

		public boolean isRequired() {
			return required;
		}

		public static Parameter fromJavaParameter(java.lang.reflect.Parameter parameter) {
			Type type = Type.fromJavaType(parameter.getParameterizedType());
			boolean required = Optional.ofNullable(parameter.getAnnotation(RequestParam.class)).map(RequestParam::required).orElse(false);
			return new Parameter(type, required);
		}
	}

	public static class Type {
		private String name;
		private List<Type> typeArguments;

		public Type(String name, List<Type> typeArguments) {
			this.name = name;
			this.typeArguments = typeArguments;
		}
		public String getName() {
			return name;
		}

		public List<Type> getTypeArguments() {
			return typeArguments;
		}

		public static Type fromJavaType(java.lang.reflect.Type javaType) {
			String typeName = javaType.getTypeName();
			List<Type> typeArguments;
			if (javaType instanceof ParameterizedType) {
				typeArguments = Arrays.stream(((ParameterizedType) javaType).getActualTypeArguments()).map(Type::fromJavaType).collect(Collectors.toList());
			} else {
				typeArguments = new ArrayList<>();
			}
			return new Type(javaType.getTypeName(), typeArguments);
		}
	}

	private String groupId;
	private String artifactId;
	private String serviceClass;
	private String operation;
	private Entry<RequestMappingInfo, HandlerMethod> springEntry;
	private IRI operationIRI;
	private final List<Parameter> parameters;
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
		this.parameters = Arrays.stream(springEntry.getValue().getMethod().getParameters()).map(Parameter::fromJavaParameter).collect(Collectors.toList());
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

	@JsonIgnore
	public Entry<RequestMappingInfo, HandlerMethod> getSpringEntry() {
		return springEntry;
	}

	public IRI getOperationIRI() {
		return operationIRI;
	}

	public Optional<String> getDisplayName() {
		return Optional.ofNullable(displayName);
	}

	public List<Parameter> getParameters() {
		return parameters;
	}
}
