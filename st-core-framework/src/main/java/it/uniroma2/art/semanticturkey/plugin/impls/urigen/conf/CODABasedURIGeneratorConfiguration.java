package it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.CODABasedURIGeneratorFactory;

/**
 * Abstract superclass of all configuration classes for {@link CODABasedURIGeneratorFactory}.
 */
public abstract class CODABasedURIGeneratorConfiguration extends AbstractPluginConfiguration {

	/**
	 * Contract URL for random ID generation.
	 */
	public static final String CODA_RANDOM_ID_GENERATOR_CONTRACT = "http://art.uniroma2.it/coda/contracts/randIdGen";

}
