package it.uniroma2.art.semanticturkey.trivialinference.sail.config;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.trivialinference.sail.TrivialInferencer;


/**
 * Factory for {@link TrivialInferencerTest}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class TrivialInferencerFactory implements SailFactory {
	private static final Logger logger = LoggerFactory.getLogger(TrivialInferencerFactory.class);

	public static final String SAIL_TYPE = "http://semanticturkey.uniroma2.it/sail/trivialinferencer";

	@Override
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		return new TrivialInferencerConfig(null);
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}

		return new TrivialInferencer((TrivialInferencerConfig)config);
	}

}
