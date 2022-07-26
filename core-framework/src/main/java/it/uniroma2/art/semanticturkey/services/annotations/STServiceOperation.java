package it.uniroma2.art.semanticturkey.services.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * this annotation is used to inform the {@link STServiceProcessor} to map a service operation to a method in
 * the controller generated for the defining service class.
 * 
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
@Target(value = { ElementType.METHOD })
public @interface STServiceOperation {
	/**
	 * Indicates the HTTP Method supported by exposed method in the controller.
	 */
	RequestMethod method() default RequestMethod.GET;

	RequestMethod[] methods() default {};
}