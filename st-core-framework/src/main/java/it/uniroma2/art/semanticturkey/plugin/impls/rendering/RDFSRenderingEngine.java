package it.uniroma2.art.semanticturkey.plugin.impls.rendering;

import static java.util.stream.Collectors.joining;

import java.util.Set;

import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLUtil;

import com.google.common.collect.Sets;

import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.RDFSRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.rendering.BaseRenderingEngine;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElement;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;

/**
 * An implementation of {@link RenderingEngine} dealing with <code>rdfs:label</a>s.
 * 
 */
public class RDFSRenderingEngine extends BaseRenderingEngine implements RenderingEngine {

	public RDFSRenderingEngine(RDFSRenderingEngineConfiguration config) {
		super(config);
	}

	@Override
	public String getGraphPatternForDescribe(ResourcePosition resourcePosition,
			ARTResource resourceToBeRendered, String varPrefix) {
		return String.format(
				"{{?object <http://www.w3.org/2000/01/rdf-schema#label> ?%1$s_object_label .} union {?object <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest>*/<http://www.w3.org/1999/02/22-rdf-syntax-ns#first> ?%1$s_indirectObject . ?%1$s_indirectObject <http://www.w3.org/2000/01/rdf-schema#label> ?%1$s_indirectObject_label .} union {?resource <http://www.w3.org/2000/01/rdf-schema#label> ?%1$s_subject_label .} union {}}",
				varPrefix);
	}

	@Override
	protected Set<ARTURIResource> getPlainURIs() {
		return Sets.newHashSet(it.uniroma2.art.owlart.vocabulary.RDFS.Res.LABEL);
	}

	@Override
	public GraphPattern getGraphPattern() {
		StringBuilder gp = new StringBuilder();
		gp.append("?resource rdfs:label ?labelInternal .                                                 \n");
		if (!takeAll) {
			gp.append(String.format(" FILTER(LANG(?labelInternal) IN (%s))", languages.stream()
					.map(lang -> "\"" + SPARQLUtil.encodeString(lang) + "\"").collect(joining(", "))));
		}
		gp.append(
				"BIND(CONCAT(STR(?labelInternal), \" (\", LANG(?labelInternal), \")\") AS ?labelInternal2)       \n");
		return GraphPatternBuilder.create().prefix("rdfs", RDFS.NAMESPACE)
				.projection(ProjectionElementBuilder.groupConcat("labelInternal2", "label"))
				.pattern(gp.toString()).graphPattern();
	}

}
