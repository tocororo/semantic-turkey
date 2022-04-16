package it.uniroma2.art.semanticturkey.event.support;

import it.uniroma2.art.semanticturkey.event.Event;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An {@link ApplicationListener} that listens for {@link EventListenerTest} objects and propagates them to
 * other {@link ApplicationEventPublisher}s.
 * 
 * @author Manuel
 *
 */
public class CrossContextPropagationEventListener implements ApplicationListener<Event> {

	@Autowired
	private BundleContext bundleContext;

	private ServiceTracker serviceTracker;

	private static Set<Event> alreadyPropagated = ConcurrentHashMap.newKeySet();

	public CrossContextPropagationEventListener() {
	}

	@PostConstruct
	public void init() throws InvalidSyntaxException {
		long selfBundleId = bundleContext.getBundle().getBundleId();
		serviceTracker = new ServiceTracker(bundleContext,
				bundleContext.createFilter("(&(" + Constants.OBJECTCLASS +  "=" + ApplicationEventPublisher.class.getName() +")(!(service.bundleid=" + selfBundleId +")))"),
				null);
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
		boolean notAlreadyPropagated = alreadyPropagated.add(event);

		if (notAlreadyPropagated) {
			try {
				Object[] eventPublishers = serviceTracker.getServices();

				for (Object pub : eventPublishers) {
					((ApplicationEventPublisher) pub).publishEvent(event);
				}
			} finally {
				alreadyPropagated.remove(event);
			}
		}
	}
}
