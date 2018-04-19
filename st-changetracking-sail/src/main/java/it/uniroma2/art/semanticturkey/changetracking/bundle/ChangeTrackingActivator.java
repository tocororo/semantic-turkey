package it.uniroma2.art.semanticturkey.changetracking.bundle;

import org.eclipse.rdf4j.sail.config.SailRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerFactory;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTrackingActivator implements BundleActivator {

	private ChangeTrackerFactory changeTrackerFactory;

	@Override
	public void start(BundleContext context) throws Exception {
		changeTrackerFactory = new ChangeTrackerFactory();
		SailRegistry.getInstance().add(changeTrackerFactory);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (changeTrackerFactory != null) {
			SailRegistry.getInstance().remove(changeTrackerFactory);
		}
	}

}
