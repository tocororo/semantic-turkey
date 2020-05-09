package it.uniroma2.art.semanticturkey.event.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import it.uniroma2.art.semanticturkey.event.Event;

/**
 * Annotation that marks a method as a candidate transactional event listener. The annotated method shall have
 * one parameter of type {@link Event} (or subtype thereof).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface TransactionalEventListener {
	enum Phase {
		beforeCommit, afterCommit, afterRollback
	};

	Phase phase();
}
