package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.plugin.extpts.ExportFilter;
import it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf.XLabelDereificationExportFilterConfiguration;

public class XLabelDereificationExportFilter implements ExportFilter {

	private boolean preserveReifiedLabels;

	public XLabelDereificationExportFilter(XLabelDereificationExportFilterConfiguration config) {
		preserveReifiedLabels = config.preserveReifiedLabels;
	}

	@Override
	public void filter(RepositoryConnection sourceRepositoryConnection,
			RepositoryConnection workingRepositoryConnection, IRI[] graphs) throws RDF4JException {
		IRI[] expandedGraphs = FilterUtils.expandGraphs(workingRepositoryConnection, graphs);
		
		// TODO: implement this method!!!
	}

}
