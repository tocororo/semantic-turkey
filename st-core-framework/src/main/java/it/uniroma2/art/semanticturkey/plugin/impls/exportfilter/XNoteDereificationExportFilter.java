package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.plugin.extpts.ExportFilter;
import it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf.XNoteDereificationExportFilterConfiguration;

public class XNoteDereificationExportFilter implements ExportFilter {

	private boolean preserveReifiedNotes;

	public XNoteDereificationExportFilter(XNoteDereificationExportFilterConfiguration config) {
		preserveReifiedNotes = config.preserveReifiedNotes;
	}

	@Override
	public void filter(RepositoryConnection sourceRepositoryConnection,
			RepositoryConnection workingRepositoryConnection, IRI[] graphs) throws RDF4JException {
		
		IRI[] expandedGraphs = FilterUtils.expandGraphs(workingRepositoryConnection, graphs);
		
		// TODO: implement this method!!!

	}

}
