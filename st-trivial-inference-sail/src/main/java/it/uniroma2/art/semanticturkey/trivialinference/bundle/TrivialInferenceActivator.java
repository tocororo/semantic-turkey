package it.uniroma2.art.semanticturkey.trivialinference.bundle;

import org.eclipse.rdf4j.sail.config.SailRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import it.uniroma2.art.semanticturkey.trivialinference.sail.config.TrivialInferencerFactory;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class TrivialInferenceActivator implements BundleActivator {

	private TrivialInferencerFactory trivialInferencerFactory;

	@Override
	public void start(BundleContext context) throws Exception {
		trivialInferencerFactory = new TrivialInferencerFactory();
		SailRegistry.getInstance().add(trivialInferencerFactory);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (trivialInferencerFactory != null) {
			SailRegistry.getInstance().remove(trivialInferencerFactory);
		}
	}

}
