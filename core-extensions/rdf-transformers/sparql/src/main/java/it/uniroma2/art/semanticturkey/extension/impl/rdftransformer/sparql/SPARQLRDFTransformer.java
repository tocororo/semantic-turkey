package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.sparql;

import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;

/**
 * An {@link RDFTransformer} that executes a SPARQL Update.
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class SPARQLRDFTransformer extends AbstractSPARQLRDFTransformer {

	private String sparqlUpdate;
	private boolean sliced;

	/**
	 * Constructs a filter utilizing the supplied configuration object
	 * 
	 * @param config
	 */
	public SPARQLRDFTransformer(SPARQLRDFTransformerConfiguration config) {
		this.sparqlUpdate = config.filter;
		this.sliced = config.sliced;
	}

	@Override
	protected String getSPARQLUpdate() {
		return sparqlUpdate;
	}

	@Override
	protected boolean isSliced() {
		return sliced;
	}

}
