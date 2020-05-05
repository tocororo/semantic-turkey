package it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf;

import it.uniroma2.art.semanticturkey.plugin.impls.rendering.RDFSRenderingEngine;
import it.uniroma2.art.semanticturkey.rendering.AbstractLabelBasedRenderingEngineConfiguration;

/**
 * Configuration class for {@link RDFSRenderingEngine}.
 *
 */
public class RDFSRenderingEngineConfiguration extends AbstractLabelBasedRenderingEngineConfiguration {

	@Override
	public String getShortName() {
		return "RDFS Rendering";
	}


}
