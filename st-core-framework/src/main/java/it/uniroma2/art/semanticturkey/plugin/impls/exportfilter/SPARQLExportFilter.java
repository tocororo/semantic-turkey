package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter;

import it.uniroma2.art.semanticturkey.plugin.extpts.ExportFilter;
import it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf.SPARQLExportFilterConfiguration;

/**
 * An {@link ExportFilter} that executes a SPARQL Update.
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class SPARQLExportFilter extends AbstractSPARQLExportFilter {

	private String sparqlUpdate;
	private boolean sliced;

	/**
	 * Constructs a filter utilizing the supplied configuration object
	 * 
	 * @param config
	 */
	public SPARQLExportFilter(SPARQLExportFilterConfiguration config) {
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
