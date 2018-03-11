package it.uniroma2.art.semanticturkey.ontology;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation telling {@link STEnviroment} that a test method requires validation.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface RequiresValidation {

}
