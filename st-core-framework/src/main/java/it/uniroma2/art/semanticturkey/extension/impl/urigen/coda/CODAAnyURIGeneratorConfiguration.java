package it.uniroma2.art.semanticturkey.extension.impl.urigen.coda;

import it.uniroma2.art.semanticturkey.plugin.impls.urigen.CODAURIGeneratorFactory;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link CODAURIGeneratorFactory} that supports the use of any CODA converter
 * compliant with the <code>coda:randIdGen</code>.
 *
 */
public class CODAAnyURIGeneratorConfiguration extends CODAURIGeneratorConfiguration {

	@Override
	public String getShortName() {
		return "CODA-based any converter URI generator";
	}

	@STProperty(description="The class name for the selection of the desired CODA converter")
	public String converter = "http://art.uniroma2.it/coda/converters/templateBasedRandIdGen";

}
