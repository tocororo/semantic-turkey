package it.uniroma2.art.semanticturkey.services.annotations.logging;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import it.uniroma2.art.semanticturkey.services.annotations.Write;

/**
 * This annotation can be used to mark a {@link Write} annotated service method to indicate that it will
 * create a label/concept. If logging is enabled, this information will be stored in the validation graph and,
 * if the proposed label is rejected, it will be added to a blacklist.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@Documented
@Retention(RUNTIME)
@Target(ElementType.METHOD)
public @interface TermCreation {
	enum Facets {
		UNSPECIFIED, PREF_LABEL, ALT_LABEL, HIDDEN_LABEL, NEW_CONCEPT
	}

	String label();

	Facets facet() default Facets.UNSPECIFIED;

	String concept() default "";
}
