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

import it.uniroma2.art.semanticturkey.validators.LanguageTaggedStringValidator;
import it.uniroma2.art.semanticturkey.validators.LocallyDefinedValidator;

/**
 * Requires that an RDF literal represents a language tagged string.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@Documented
@Constraint(validatedBy = LanguageTaggedStringValidator.class)
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface LanguageTaggedString {
	String message() default "Error: literal " + MsgInterpolationVariables.invalidParamValuePlaceHolder
			+ " does not represent a language tagged string";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}