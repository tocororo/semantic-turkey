package it.uniroma2.art.semanticturkey.extension.impl.rendering.skosxl;

import it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngine;
import it.uniroma2.art.semanticturkey.extension.impl.rendering.BaseRenderingEngine;

/**
 * An implementation of {@link RenderingEngine} dealing with <code>skosxl:prefLabel</a>s.
 * 
 */
public class SKOSXLRenderingEngine extends BaseRenderingEngine implements RenderingEngine {

	@Override
	protected void getGraphPatternInternal(StringBuilder gp) {
		gp.append(
				"?resource <http://www.w3.org/2008/05/skos-xl#prefLabel> [<http://www.w3.org/2008/05/skos-xl#literalForm> ?labelInternal ] .\n");
	}

}
