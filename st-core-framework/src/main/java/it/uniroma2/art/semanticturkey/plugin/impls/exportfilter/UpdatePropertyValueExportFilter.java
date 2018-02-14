package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.FilterUtils;
import it.uniroma2.art.semanticturkey.plugin.extpts.ExportFilter;
import it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf.UpdatePropertyValueExportFilterConfiguration;

/**
 * An {@link ExportFilter} that adds to a <code>resource</code> a given <code>value</code> for a certain
 * <code>property</code>. Before adding the new value, the filter does the following: if an
 * <code>oldValue</code> is specified, then the triple <code>&lt;resource, property,
 * oldValue&gt;</code> is deleted, otherwise every triple matching the pattern <code>&lt;resource, property,
 * *&gt;</code> is deleted.
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class UpdatePropertyValueExportFilter implements ExportFilter {

	private Resource resource;
	private IRI property;
	private Value value;
	private Value oldValue;

	public UpdatePropertyValueExportFilter(UpdatePropertyValueExportFilterConfiguration config) {
		ValueFactory vf = SimpleValueFactory.getInstance();

		this.resource = NTriplesUtil.parseResource(config.resource, vf);
		this.property = NTriplesUtil.parseURI(config.property, vf);
		this.value = NTriplesUtil.parseValue(config.value, vf);
		this.oldValue = config.oldValue != null && !config.oldValue.isEmpty()
				? NTriplesUtil.parseValue(config.oldValue, vf) : null;

	}

	@Override
	public void filter(RepositoryConnection sourceRepositoryConnection,
			RepositoryConnection workingRepositoryConnection, IRI[] graphs) throws RDF4JException {
		IRI[] expandedGraphs = FilterUtils.expandGraphs(workingRepositoryConnection, graphs);
		
		if (expandedGraphs.length == 0) return;
		
		workingRepositoryConnection.remove(resource,  property, oldValue, expandedGraphs);
		workingRepositoryConnection.add(resource,  property, value, expandedGraphs);
	}

}
