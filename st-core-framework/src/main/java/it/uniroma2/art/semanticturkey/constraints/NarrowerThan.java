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

import it.uniroma2.art.semanticturkey.validators.NarrowerThanValidator;

/**
 * Requires that an RDF resource is a narrower of a given SKOS concept
 * 
 * @author Tiziano Lorenzetti
 * @author Manuel Fiorelli
 */
@Documented
@Constraint(validatedBy = NarrowerThanValidator.class)
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface NarrowerThan {
	String message() default "Error: resource " + MsgInterpolationVariables.invalidParamValuePlaceHolder
			+ " is not a narrower concept of the provided 'broaderConceptIRI' parameter";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	String broaderConceptIRI();

}