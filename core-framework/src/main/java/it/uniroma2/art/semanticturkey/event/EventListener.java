package it.uniroma2.art.semanticturkey.event;

import org.springframework.context.ApplicationListener;

/**
 * Interfaces for listeners of ST {@link EventListenerTest}. While not strictly necessary, this interface has been
 * introduced to weaken the coupling of event consumers from the underlying Spring infrastructure.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public interface EventListener<T extends Event> extends ApplicationListener<T> {

}
