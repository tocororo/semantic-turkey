package it.uniroma2.art.semanticturkey.services.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * this annotation is used to inform the {@link STServiceProcessor} to generate a controller for the service
 * (class) annotated with it
 * 
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE })
@Component // meta-annotation
@Validated // meta-annotation
@Documented
public @interface STService {
	/**
	 * Indicates the HTTP Method supported by exposed method in the controller.
	 */
	RequestMethod method() default RequestMethod.GET;
}