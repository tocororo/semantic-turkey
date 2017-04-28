package it.uniroma2.art.semanticturkey.properties;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * this annotation marks a property as required (i.e. it must have a value)
 * 
 * @author Armando Stellato
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Required {
}
