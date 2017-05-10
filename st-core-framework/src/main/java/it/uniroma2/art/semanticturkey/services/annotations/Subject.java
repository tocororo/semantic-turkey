package it.uniroma2.art.semanticturkey.services.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a service operation parameter to indicate that it contains the subject.
 * 
 * @author <a href="mailto:manuel.fiorelli@gmail.com">Manuel Fiorelli</a>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
public @interface Subject {

}
