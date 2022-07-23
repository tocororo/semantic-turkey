package it.uniroma2.art.semanticturkey.constraints;

import it.uniroma2.art.semanticturkey.extension.Extension;
import it.uniroma2.art.semanticturkey.validators.HasExtensionPointValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * Requires that a {@link it.uniroma2.art.semanticturkey.config.Configuration} is about a certain extension point.
 *
 * @author Manuel Fiorelli
 */
@Documented
@Constraint(validatedBy = HasExtensionPointValidator.class)
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface HasExtensionPoint {
	String message() default "{it.uniroma2.art.semanticturkey.constraints.HasExtensionPoint.message}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	Class<? extends Extension> value();
}