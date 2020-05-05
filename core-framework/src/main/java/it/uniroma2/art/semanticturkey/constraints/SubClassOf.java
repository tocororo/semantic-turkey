package it.uniroma2.art.semanticturkey.constraints;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import it.uniroma2.art.semanticturkey.validators.SubClassOfValidator;

/**
 * Requires that an RDF resource is subClassOf a given class
 * 
 * @author Tiziano Lorenzetti
 */
@Documented
@Constraint(validatedBy = SubClassOfValidator.class)
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface SubClassOf {
	String message() default "Error: resource " + MsgInterpolationVariables.invalidParamValuePlaceHolder
			+ " is not subClass of the provided 'superClassIRI' parameter";
	
	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	String superClassIRI();
	
}