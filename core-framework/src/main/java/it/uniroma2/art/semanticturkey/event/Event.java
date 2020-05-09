package it.uniroma2.art.semanticturkey.event;

import org.springframework.context.ApplicationEvent;

/**
 * Base class for ST events. These are a kind of {@link ApplicationEvent} that are propagated across the
 * application contexts associated with different ST bundles.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class Event extends ApplicationEvent {

	private static final long serialVersionUID = 5481428090727232185L;

	public Event(Object source) {
		super(source);
	}

}
