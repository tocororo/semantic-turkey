package it.uniroma2.art.semanticturkey.config.invokablereporter;

import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * A storage for invokable reporters based on invocations of custom services.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * 
 */
public class InvokableReporterStore implements PUScopedConfigurableComponent<InvokableReporter> {

	@Override
	public String getId() {
		return InvokableReporterStore.class.getName();
	}

}
