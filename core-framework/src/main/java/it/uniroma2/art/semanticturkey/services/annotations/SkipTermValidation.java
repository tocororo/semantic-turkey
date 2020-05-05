package it.uniroma2.art.semanticturkey.services.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that a service parameter whose type is a kind of RDF4J Value should not be validated.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@Documented
@Retention(RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface SkipTermValidation {
}
