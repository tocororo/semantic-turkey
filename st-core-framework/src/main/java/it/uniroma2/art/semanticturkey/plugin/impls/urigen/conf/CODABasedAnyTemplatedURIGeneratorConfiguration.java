package it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfigurationParameter;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.CODABasedURIGeneratorFactory;

/**
 * Configuration class for {@link CODABasedURIGeneratorFactory} that supports the use of any CODA converter
 * compliant with the <code>coda:randIdGen</code>.
 *
 */
public class CODABasedAnyTemplatedURIGeneratorConfiguration extends CODABasedURIGeneratorConfiguration {

	@Override
	public String getShortName() {
		return "CODA-based templated URI generator";
	}

	@PluginConfigurationParameter(description="The class name for the selection of the desired CODA converter")
	public String converter = "http://art.uniroma2.it/coda/converters/templateBasedRandIdGen";

}
