package it.uniroma2.art.semanticturkey.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an {@link STProperties} with the allowed values.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.FIELD)
public @interface Enumeration {
	String[] value();
	boolean open() default false;
}
