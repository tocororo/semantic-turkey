package it.uniroma2.art.semanticturkey.plugin.impls.rendering;

import static java.util.stream.Collectors.joining;

import java.util.Set;

import org.eclipse.rdf4j.query.parser.sparql.SPARQLUtil;

import com.google.common.collect.Sets;

import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.SKOSRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.rendering.BaseRenderingEngine;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;

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
		return String
				.format("{{?object <http://www.w3.org/2004/02/skos/core#prefLabel> ?%1$s_object_label .} union {?resource <http://www.w3.org/2004/02/skos/core#prefLabel> ?%1$s_subject_label .} union {}}",
						varPrefix);
	}

	@Override
	protected Set<ARTURIResource> getPlainURIs() {
		return Sets.newHashSet(SKOS.Res.PREFLABEL);
	}

	@Override
	public GraphPattern getGraphPattern() {
		StringBuilder gp = new StringBuilder();
		gp.append("?resource skos:prefLabel ?labelInternal .                                             \n");
		if (!takeAll) {
			gp.append(String.format(" FILTER(LANG(?labelInternal) IN (%s))", languages.stream()
					.map(lang -> "\"" + SPARQLUtil.encodeString(lang) + "\"").collect(joining(", "))));
		}
		gp.append(
				"BIND(CONCAT(STR(?labelInternal), \" (\", LANG(?labelInternal), \")\") AS ?labelInternal2)       \n");
		return GraphPatternBuilder.create().prefix("skos", org.eclipse.rdf4j.model.vocabulary.SKOS.NAMESPACE)
				.projection(ProjectionElementBuilder.groupConcat("labelInternal2", "label"))
				.pattern(gp.toString()).graphPattern();
	}

}
