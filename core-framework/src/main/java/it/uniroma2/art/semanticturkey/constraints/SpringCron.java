package it.uniroma2.art.semanticturkey.constraints;

import it.uniroma2.art.semanticturkey.validators.DatatypeValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * Requires that a String is a Spring cron expression
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@Documented
@Constraint(validatedBy = DatatypeValidator.class)
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SpringCron {
	String message() default "{it.uniroma2.art.semanticturkey.constraints.SpringCron.message}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}