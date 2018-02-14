package it.uniroma2.art.semanticturkey.extension.impl.rendering.rdfs;

import it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngine;
import it.uniroma2.art.semanticturkey.extension.impl.rendering.BaseRenderingEngine;

/**
 * An implementation of {@link RenderingEngine} dealing with <code>rdfs:label</a>s.
 * 
 */
public class RDFSRenderingEngine extends BaseRenderingEngine implements RenderingEngine {

	@Override
	protected void getGraphPatternInternal(StringBuilder gp) {
		gp.append("?resource <http://www.w3.org/2000/01/rdf-schema#label> ?labelInternal .\n");
	}

}
