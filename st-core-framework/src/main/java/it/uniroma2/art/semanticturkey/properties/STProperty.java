package it.uniroma2.art.semanticturkey.properties;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * fields decorated with this annotation are marked as properties
 * 
 * @author Armando Stellato
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface STProperty {
	String description();
}
