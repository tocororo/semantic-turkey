package it.uniroma2.art.semanticturkey.services.annotations;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates in the context of a service operation that the annotated parameter holds an RDF resource, which
 * is modified by the operation.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@Documented
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Modified {

}
