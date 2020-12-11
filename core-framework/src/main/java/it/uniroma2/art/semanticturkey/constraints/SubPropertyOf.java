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

import it.uniroma2.art.semanticturkey.validators.SubPropertyOfValidator;

/**
 * Requires that a property is subPropertyOf a given property
 * 
 * @author Tiziano Lorenzetti
 */
@Documented
@Constraint(validatedBy = SubPropertyOfValidator.class)
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface SubPropertyOf {
	String message() default "{it.uniroma2.art.semanticturkey.constraints.SubPropertyOf.message}";
	
	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	String superPropertyIRI();
	
}