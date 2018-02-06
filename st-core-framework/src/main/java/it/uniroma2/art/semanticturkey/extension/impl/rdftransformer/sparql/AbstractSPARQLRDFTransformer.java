package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.sparql;

import java.util.Arrays;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.FilterUtils;

/**
 * An abstract {@link RDFTransformer} that is based on the execution of a SPARQL Update supplied by a (concrete)
 * subclass.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class AbstractSPARQLRDFTransformer implements RDFTransformer {

	@Override
	public void filter(RepositoryConnection sourceRepositoryConnection,
			RepositoryConnection workingRepositoryConnection, IRI[] graphs) throws RDF4JException {
		
		IRI[] expandedGraphs = FilterUtils.expandGraphs(workingRepositoryConnection, graphs);
		
		if (expandedGraphs.length == 0) return;
		
		Update update = workingRepositoryConnection.prepareUpdate(getSPARQLUpdate());
		if (isSliced()) {
			for (IRI g : expandedGraphs) {
				SimpleDataset dataset = new SimpleDataset();
				Arrays.stream(expandedGraphs).forEach(g2 -> {
					dataset.addNamedGraph(g2);
				});
				dataset.addDefaultGraph(g);
				dataset.addDefaultRemoveGraph(g);
				dataset.setDefaultInsertGraph(g);

				update.setDataset(dataset);
				update.execute();
			}
		} else {
			SimpleDataset dataset = new SimpleDataset();
			Arrays.stream(expandedGraphs).forEach(g -> {
				dataset.addNamedGraph(g);
				dataset.addDefaultGraph(g);
			});

			update.setDataset(dataset);
			update.execute();
		}
	}

	protected abstract String getSPARQLUpdate();

	protected abstract boolean isSliced();
}
