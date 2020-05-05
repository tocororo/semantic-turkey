package it.uniroma2.art.semanticturkey.plugin.impls.rendering;

import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.SKOSXLRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.rendering.BaseRenderingEngine;

/**
 * An implementation of {@link RenderingEngine} dealing with <code>skosxl:prefLabel</a>s.
 * 
 */
public class SKOSXLRenderingEngine extends BaseRenderingEngine implements RenderingEngine {

	public SKOSXLRenderingEngine(SKOSXLRenderingEngineConfiguration config) {
		super(config);
	}

	@Override
	public void getGraphPatternInternal(StringBuilder gp) {
		gp.append("\n?resource <http://www.w3.org/2008/05/skos-xl#prefLabel> [<http://www.w3.org/2008/05/skos-xl#literalForm> ?labelInternal ] .\n");
	}

}
