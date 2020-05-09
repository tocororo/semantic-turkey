package it.uniroma2.art.semanticturkey.event.support;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;

import it.uniroma2.art.semanticturkey.event.Event;

/**
 * An {@link ApplicationListener} that listens for {@link EventListenerTest} objects and propagates them to other
 * {@link ApplicationEventPublisher}s.
 * 
 * @author Manuel
 *
 */
public class CrossContextPropagationEventListener implements ApplicationListener<Event> {

	@Autowired
	private BundleContext bundleContext;

	private ServiceTracker serviceTracker;

	private ThreadLocal<Event> currentEventHolder;

	public CrossContextPropagationEventListener() {
		currentEventHolder = ThreadLocal.withInitial(() -> null);
	}

	@PostConstruct
	public void init() {
		serviceTracker = new ServiceTracker(bundleContext, ApplicationEventPublisher.class.getName(), null);
		serviceTracker.open();
	}

	@PreDestroy
	public void destroy() {
		if (serviceTracker != null) {
			serviceTracker.close();
		}
	}

	@Override
	public void onApplicationEvent(Event event) {
		Event currentEvent = currentEventHolder.get();

		if (event == currentEvent)
			return; // avoid infinite loop

		currentEventHolder.set(event);
		try {
			Object[] eventPublishers = serviceTracker.getServices();

			for (Object pub : eventPublishers) {
				((ApplicationEventPublisher) pub).publishEvent(event);
			}
		} finally {
			currentEventHolder.set(currentEvent); // currentEvent should be null initially
		}
	}
}
