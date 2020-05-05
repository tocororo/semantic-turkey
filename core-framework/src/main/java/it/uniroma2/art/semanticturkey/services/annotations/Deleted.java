package it.uniroma2.art.semanticturkey.services.annotations;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;

/**
 * Indicates in the context of a service operation that the annotated parameter holds an RDF resource, which
 * is deleted by the operation.
 * 
 */
@Documented
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Deleted {
	/**
	 * The role of the resource. If the role is not known a-priori, leave it to the default value
	 * {@link RDFResourceRole#undetermined}
	 * 
	 * @return
	 */
	RDFResourceRole role() default RDFResourceRole.undetermined;
}
