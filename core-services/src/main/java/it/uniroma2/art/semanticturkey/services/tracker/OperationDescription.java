package it.uniroma2.art.semanticturkey.services.tracker;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import it.uniroma2.art.semanticturkey.mvc.IntrospectableController;
import it.uniroma2.art.semanticturkey.properties.STPropertiesSerializer;
import it.uniroma2.art.semanticturkey.services.annotations.DisplayName;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Metadata about ST service operations.
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class OperationDescription {

    private final List<Parameter> parameters;
    private final String name;
    private String groupId;
    private String artifactId;
    private String serviceClass;
    private String operation;
    private Entry<RequestMappingInfo, HandlerMethod> springEntry;
    private IRI operationIRI;
    private String displayName;
    private Type returnType;

    protected OperationDescription(String groupId, String artifactId, String serviceClass, String operation,
                                   Entry<RequestMappingInfo, HandlerMethod> springEntry, String displayName, Type returnType) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.serviceClass = serviceClass;
        this.operation = operation;
        this.springEntry = springEntry;

        this.name = operation;
        this.operationIRI = SimpleValueFactory.getInstance()
                .createIRI("http://semanticturkey.uniroma2.it/services/" + groupId + "/" + artifactId + "/"
                        + serviceClass + "/" + operation);
        this.parameters =
                Arrays.stream(springEntry.getValue().getMethod().getParameters()).filter(p -> p.isAnnotationPresent(RequestParam.class)).map(Parameter::fromJavaParameter)
                .collect(Collectors.toList());
        this.displayName = displayName;
        this.returnType = returnType;
    }

    public static OperationDescription create(ApplicationContext applicationContext, String groupId,
                                              String artifactId, String serviceClass, String operation,
                                              Entry<RequestMappingInfo, HandlerMethod> springEntry) {
        Optional<Method> method = Arrays
                .stream(AopProxyUtils.ultimateTargetClass(((IntrospectableController) applicationContext
                        .getBean((String) springEntry.getValue().getBean())).getService())
                        .getMethods())
                .filter(m -> m.getName().equals(operation)).findAny();
        String displayName = method.map(serviceMethod -> {

                    DisplayName displayNameAnnot = AnnotationUtils.findAnnotation(serviceMethod,
                            DisplayName.class);

                    if (displayNameAnnot != null) {
                        return displayNameAnnot.value();
                    }

                    return null;
                }).orElse(null);

        Type returnType = method.map(Method::getGenericReturnType).map(Type::new).orElse(null);

        return new OperationDescription(groupId, artifactId, serviceClass, operation, springEntry,
                displayName, returnType);
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

    public String getName() {
        return name;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public Type getReturnType() {
        return returnType;
    }

    public static class Parameter {
        private String name;
        private Type type;
        private boolean required;

        public Parameter(String name, Type type, boolean required) {
            this.name = name;
            this.type = type;
            this.required = required;
        }

        public static Parameter fromJavaParameter(java.lang.reflect.Parameter parameter) {
            Type type = new Type(parameter.getParameterizedType());
            Optional<RequestParam> requestParamAnnot = Optional.ofNullable(parameter.getAnnotation(RequestParam.class));
            String name = requestParamAnnot.map(RequestParam::value).orElse(null);
            boolean required = requestParamAnnot.map(RequestParam::required).orElse(false);
            return new Parameter(name, type, required);
        }

        public Type getType() {
            return type;
        }

        public boolean isRequired() {
            return required;
        }

        public String getName() {
            return name;
        }
    }

    public static class Type {
        private java.lang.reflect.Type javaType;

        public Type(java.lang.reflect.Type javaType) {
            this.javaType = javaType;
        }

        public String getName() {
            return javaType.getTypeName();
        }

        public List<Type> getTypeArguments() {
            if (TypeUtils.isArrayType(javaType)) {
                return Lists.newArrayList(new Type(TypeUtils.getArrayComponentType(javaType)));
            } else if (javaType instanceof ParameterizedType) {
                return Stream.of(javaType).filter(ParameterizedType.class::isInstance)
                        .flatMap(t -> Arrays.stream(((ParameterizedType) t)
                                .getActualTypeArguments())).map(Type::new).collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        }

        @JsonIgnore
        public java.lang.reflect.Type getJavaType() {
            return javaType;
        }
    }
}
