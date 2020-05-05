package it.uniroma2.art.semanticturkey.trivialinference.sail.config;

import org.eclipse.rdf4j.sail.config.AbstractDelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailImplConfig;

/**
 * A configuration class for the {@link TrivialInferencerTest} sail.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class TrivialInferencerConfig extends AbstractDelegatingSailImplConfig {

	public TrivialInferencerConfig() {
		this(null);
	}

	public TrivialInferencerConfig(SailImplConfig delegate) {
		super(TrivialInferencerFactory.SAIL_TYPE, delegate);
	}

}
