package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks test methods that do not want history. This annotation should be used in conjunction with
 * subclasses of {@link AbstractChangeTrackerTest}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DoesNotWantHistory {

}
