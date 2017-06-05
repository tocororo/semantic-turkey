package it.uniroma2.art.semanticturkey.services.annotations;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;

/**
 * Indicates in the context of a service operation that the annotated parameter holds an RDF resource, which
 * is created by the operation.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@Documented
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Created {
	/**
	 * The role of the resource. If the role is not known a-priori, leave it to the default value
	 * {@link RDFResourceRolesEnum#undetermined}
	 * 
	 * @return
	 */
	RDFResourceRolesEnum role() default RDFResourceRolesEnum.undetermined;
}
