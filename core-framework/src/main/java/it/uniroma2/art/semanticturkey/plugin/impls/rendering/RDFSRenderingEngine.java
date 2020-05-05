package it.uniroma2.art.semanticturkey.plugin.impls.rendering;

import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.RDFSRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.rendering.BaseRenderingEngine;

/**
 * An implementation of {@link RenderingEngine} dealing with <code>rdfs:label</a>s.
 * 
 */
public class RDFSRenderingEngine extends BaseRenderingEngine implements RenderingEngine {

	public RDFSRenderingEngine(RDFSRenderingEngineConfiguration config) {
		super(config);
	}

	@Override
	public void getGraphPatternInternal(StringBuilder gp) {
		gp.append("\n?resource <http://www.w3.org/2000/01/rdf-schema#label> ?labelInternal .\n");
	}

}
