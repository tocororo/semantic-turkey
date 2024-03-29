package it.uniroma2.art.semanticturkey.generation.annotation.processor.internal;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;

import it.uniroma2.art.semanticturkey.services.annotations.Produces;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

public class VelocitySupportTools {

	public static final String OPTIONAL_QUALIFIED_NAME = "it.uniroma2.art.semanticturkey.services.annotations.Optional";
	public static final String JSONSERIALIZED_QUALIFIED_NAME = "it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized";
	public static final String SKIPTERMVALIDATION_QUALIFIED_NAME = "it.uniroma2.art.semanticturkey.services.annotations.SkipTermValidation";
	public static final String HTTPSERVLETRESPONSE_QUALIFIED_NAME = "javax.servlet.http.HttpServletResponse";
	public static final String HTTPSERVLETREQUEST_QUALIFIED_NAME = "javax.servlet.http.HttpServletRequest";
	public static final String DEFAULT_VALUE = "defaultValue";
	private Types typeUtils;
	private Elements elementUtils;

	public VelocitySupportTools(Types typeUtils, Elements elementUtils) {
		this.typeUtils = typeUtils;
		this.elementUtils = elementUtils;
	}

	public boolean isMappedToVoid(ExecutableElement executableElement) {
		return executableElement.getParameters().stream().anyMatch(parameter -> {
			TypeMirror parameterType = parameter.asType();
			Element parameterTypeAsElement = typeUtils.asElement(parameterType);
			if (parameterTypeAsElement instanceof QualifiedNameable) {
				String qname = ((QualifiedNameable) parameterTypeAsElement).getQualifiedName().toString();

				if (qname.equals(HTTPSERVLETRESPONSE_QUALIFIED_NAME)) {
					return true;
				}
			}
			return false;
		});
	}

	public boolean isMappedParameter(VariableElement parameter) {

		TypeMirror parameterType = parameter.asType();
		Element parameterTypeAsElement = typeUtils.asElement(parameterType);
		if (parameterTypeAsElement instanceof QualifiedNameable) {
			String qname = ((QualifiedNameable) parameterTypeAsElement).getQualifiedName().toString();

			if (qname.equals(HTTPSERVLETRESPONSE_QUALIFIED_NAME)) {
				return false;
			}
			if (qname.equals(HTTPSERVLETREQUEST_QUALIFIED_NAME)) {
				return false;
			}
		}
		return true;
	}

	public boolean isOptionalParameter(VariableElement element) {

		for (AnnotationMirror am : element.getAnnotationMirrors()) {
			if (((QualifiedNameable) am.getAnnotationType().asElement()).getQualifiedName().toString()
					.equals(OPTIONAL_QUALIFIED_NAME)) {
				return true;
			}
		}

		return false;
	}

	public boolean isSkipTermValidationParameter(VariableElement element) {

		for (AnnotationMirror am : element.getAnnotationMirrors()) {
			if (((QualifiedNameable) am.getAnnotationType().asElement()).getQualifiedName().toString()
					.equals(SKIPTERMVALIDATION_QUALIFIED_NAME)) {
				return true;
			}
		}

		return false;
	}

	public boolean isJsonSerializedParameter(VariableElement element) {

		for (AnnotationMirror am : element.getAnnotationMirrors()) {
			if (((QualifiedNameable) am.getAnnotationType().asElement()).getQualifiedName().toString()
					.equals(JSONSERIALIZED_QUALIFIED_NAME)) {
				return true;
			}
		}

		return false;
	}

	public boolean hasDefaultValue(VariableElement element) {

		for (AnnotationMirror am : element.getAnnotationMirrors()) {
			if (((QualifiedNameable) am.getAnnotationType().asElement()).getQualifiedName().toString()
					.equals(OPTIONAL_QUALIFIED_NAME)) {
				for (Entry<? extends ExecutableElement, ? extends AnnotationValue> elem : am
						.getElementValues().entrySet()) {
					if (elem.getKey().getSimpleName().toString().equals(DEFAULT_VALUE)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public String getDefaultLiteralValue(VariableElement element) {

		for (AnnotationMirror am : element.getAnnotationMirrors()) {
			if (((QualifiedNameable) am.getAnnotationType().asElement()).getQualifiedName().toString()
					.equals(OPTIONAL_QUALIFIED_NAME)) {
				for (Entry<? extends ExecutableElement, ? extends AnnotationValue> elem : am
						.getElementValues().entrySet()) {
					if (elem.getKey().getSimpleName().toString().equals(DEFAULT_VALUE)) {
						return elem.getValue().toString();
					}
				}
			}
		}

		return null;
	}

	public boolean isVoidMethod(ExecutableElement executableElement) {
		return (executableElement.getReturnType().getKind() == TypeKind.VOID);
	}

	public String toStringLiteral(String string) {
		return "\"" + string.replaceAll("\"", "\\\"") + "\"";
	}

	public String getRequestMethodAsSource(ExecutableElement executableElement) {
		STServiceOperation ann2 = executableElement.getAnnotation(STServiceOperation.class);
		if (ann2 != null) {
			RequestMethod[] requestMethods = ann2.methods();
			if (requestMethods.length == 0) {
				String requestMethodName = ann2.method().toString();
				if (requestMethodName.equals("GET")) {
					return "RequestMethod.GET";
				} else if (requestMethodName.equals("POST")) {
					return "RequestMethod.POST";
				} else if (requestMethodName.equals("HEAD")) {
					return "RequestMethod.HEAD";
				} else {
					throw new IllegalArgumentException(
							"Unrecognized request method \"" + requestMethodName + "\" on " + executableElement);
				}
			} else {
				return Arrays.stream(requestMethods)
						.map(reqMethod -> {
							String reqMethodName = reqMethod.toString();
							if (reqMethodName.equals("POST")) {
								return "RequestMethod.POST";
							} else if (reqMethodName.equals("HEAD")) {
								return "RequestMethod.HEAD";
							} else {
								return "RequestMethod.GET";
							}
						})
						.collect(Collectors.joining(","));
			}
		}

		throw new IllegalArgumentException("Missing operation-related annotation on " + executableElement);
	}

	public String getProducedMimeType(ExecutableElement executableElement) {
		Produces ann = executableElement.getAnnotation(Produces.class);
		if (ann != null) {
			return Arrays.stream(ann.value()).map(el -> "\"" + el + "\"").collect(Collectors.joining(","));
		} else {
			return "\"application/json;charset=UTF-8\"";
		}
	}

	public String printType(TypeMirror typeMirror) {
		TypeVisitor<String, Void> typeAnnotationStripingVisitor = new SimpleTypeVisitor8<String, Void>() {
			@Override
			public String visitDeclared(DeclaredType t, Void p) {
				String typeName = ((QualifiedNameable) t.asElement()).getQualifiedName().toString();

				if (t.getTypeArguments().isEmpty())
					return typeName;

				return typeName + "<" + t.getTypeArguments().stream().map(arg -> arg.accept(this, p))
						.collect(Collectors.joining(", ")) + ">";
			}

			@Override
			public String visitPrimitive(PrimitiveType t, Void p) {
				return t.toString();
			}

			@Override
			public String visitArray(ArrayType t, Void p) {
				return t.getComponentType().accept(this, p) + "[]";
			}

			@Override
			protected String defaultAction(TypeMirror e, Void p) {
				throw new IllegalArgumentException("Unable to process type mirror: " + e);
			}
		};
		return typeMirror.accept(typeAnnotationStripingVisitor, null);

	}
}