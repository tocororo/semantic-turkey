package it.uniroma2.art.semanticturkey.plugin.impls.rendering;

import java.util.Set;

import com.google.common.collect.Sets;

import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.SKOSRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.rendering.BaseRenderingEngine;

/**
 * An implementation of {@link RenderingEngine} dealing with <code>skos:prefLabel</a>s.
 * 
 */
public class SKOSRenderingEngine extends BaseRenderingEngine implements RenderingEngine {

	public SKOSRenderingEngine(SKOSRenderingEngineConfiguration config) {
		super(config);
	}

	@Override
	public String getGraphPatternForDescribe(ResourcePosition resourcePosition,
			ARTResource resourceToBeRendered, String varPrefix) {
		return String.format(
				"{{?object <http://www.w3.org/2004/02/skos/core#prefLabel> ?%1$s_object_label .} union {?object <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest>*/<http://www.w3.org/1999/02/22-rdf-syntax-ns#first> ?%1$s_indirectObject . ?%1$s_indirectObject <http://www.w3.org/2004/02/skos/core#prefLabel> ?%1$s_indirectObject_label .} union {?resource <http://www.w3.org/2004/02/skos/core#prefLabel> ?%1$s_subject_label .} union {}}",
				varPrefix);
	}

	@Override
	protected Set<ARTURIResource> getPlainURIs() {
		return Sets.newHashSet(SKOS.Res.PREFLABEL);
	}

	@Override
	protected void getGraphPatternInternal(StringBuilder gp) {
		gp.append("?resource <http://www.w3.org/2004/02/skos/core#prefLabel> ?labelInternal .\n");
	}

}
