package it.uniroma2.art.semanticturkey.generation.annotation.processor.internal;

import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;

import java.util.Map.Entry;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

public class VelocitySupportTools {

	public static final String OPTIONAL_QUALIFIED_NAME = "it.uniroma2.art.semanticturkey.services.annotations.Optional";
	public static final String DEFAULT_VALUE = "defaultValue";

	public boolean isOptionalParameter(VariableElement element) {

		for (AnnotationMirror am : element.getAnnotationMirrors()) {
			if (((QualifiedNameable) am.getAnnotationType().asElement()).getQualifiedName().toString()
					.equals(OPTIONAL_QUALIFIED_NAME)) {
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
		GenerateSTServiceController ann = executableElement.getAnnotation(GenerateSTServiceController.class);
		String requestMethodName = ann.method().toString();
		if (requestMethodName.equals("GET")) {
			return "RequestMethod.GET";
		} else if (requestMethodName.equals("POST")){
			return "RequestMethod.POST";
		} else {
			throw new IllegalArgumentException("Unrecognized request method \"" + requestMethodName + "\" on " + executableElement);
		}
	}
}