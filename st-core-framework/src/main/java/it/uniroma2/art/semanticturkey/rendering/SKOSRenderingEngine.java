package it.uniroma2.art.semanticturkey.rendering;

import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;

import java.util.Set;

import com.google.common.collect.Sets;

/**
 * An implementation of {@link RenderingEngine} dealing with <code>skos:prefLabel</a>s.
 * 
 */
public class SKOSRenderingEngine extends BaseRenderingEngine implements RenderingEngine {

	@Override
	public String getGraphPatternForDescribe(ResourcePosition resourcePosition,
			ARTResource resourceToBeRendered, String varPrefix) {
		return String
				.format("{{?object <http://www.w3.org/2004/02/skos/core#prefLabel> ?%1$s_object_label .} union {?resource <http://www.w3.org/2004/02/skos/core#prefLabel> ?%1$s_subject_label .} union {}}",
						varPrefix);
	}

	@Override
	protected Set<ARTURIResource> getPlainURIs() {
		return Sets.newHashSet(SKOS.Res.PREFLABEL);
	}

}
