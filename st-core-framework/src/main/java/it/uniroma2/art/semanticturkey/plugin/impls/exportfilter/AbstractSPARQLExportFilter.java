package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.plugin.extpts.ExportFilter;

/**
 * An abstract {@link ExportFilter} that is based on the execution of a SPARQL Update supplied by a (concrete)
 * subclass. The working graph is automatically bound to the variable <code>?workingGraph</code>.
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class AbstractSPARQLExportFilter implements ExportFilter {

	@Override
	public void filter(RepositoryConnection sourceRepositoryConnection,
			RepositoryConnection workingRepositoryConnection, Resource workingGraph) throws RDF4JException {
		Update update = workingRepositoryConnection.prepareUpdate(getSPARQLUpdate());
		update.setBinding("workingGraph", workingGraph);
		update.execute();
	}

	protected abstract String getSPARQLUpdate();
}
