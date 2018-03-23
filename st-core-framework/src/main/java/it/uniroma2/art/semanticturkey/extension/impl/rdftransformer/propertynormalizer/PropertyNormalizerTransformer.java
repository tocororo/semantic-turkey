package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.propertynormalizer;

import static java.util.stream.Collectors.joining;

import org.eclipse.rdf4j.queryrender.RenderUtils;

import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.sparql.AbstractSPARQLRDFTransformer;

/**
 * An {@link RDFTransformer} that replaces a set of properties with a normalized one.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PropertyNormalizerTransformer extends AbstractSPARQLRDFTransformer implements RDFTransformer {

	private PropertyNormalizerTransformerConfiguration config;

	public PropertyNormalizerTransformer(PropertyNormalizerTransformerConfiguration config) {
		this.config = config;
	}

	@Override
	protected String getSPARQLUpdate() {
		return
		// @formatter:off
			" DELETE {                 \n" +
			"   GRAPH ?g {             \n" +
			"     ?s ?p ?o .           \n" +
			"   }                      \n" +
			" }                        \n" +
			" INSERT {                 \n" +
			"   GRAPH ?g {             \n" +
			"     ?s " + RenderUtils.toSPARQL(config.normalizingProperty) + " ?o .          \n" +
			"   }                      \n" +
			" }                        \n" +
			" WHERE {                  \n" +
			"   VALUES(?p){            \n" +
			config.propertiesBeingNormalized.stream().map(p -> "     (" + RenderUtils.toSPARQL(p) + ")\n")
			.collect(joining()) +
			"   }                      \n" +
			"   GRAPH ?g {             \n" +
			"     ?s ?p ?o             \n" +
			"   }                      \n" +
			" }                        ";
			// @formatter:on
	}

	@Override
	protected boolean isSliced() {
		return false;
	}

}
