package it.uniroma2.art.semanticturkey.extension.impl.rendering.skos;

import it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngine;
import it.uniroma2.art.semanticturkey.extension.impl.rendering.BaseRenderingEngine;

/**
 * An implementation of {@link RenderingEngine} dealing with <code>skos:prefLabel</a>s.
 * 
 */
public class SKOSRenderingEngine extends BaseRenderingEngine implements RenderingEngine {

	@Override
	protected void getGraphPatternInternal(StringBuilder gp) {
		gp.append("?resource <http://www.w3.org/2004/02/skos/core#prefLabel> ?labelInternal .\n");
	}


}
