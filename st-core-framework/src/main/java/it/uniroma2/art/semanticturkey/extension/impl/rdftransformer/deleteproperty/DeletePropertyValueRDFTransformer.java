package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.deleteproperty;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.FilterUtils;
/**
 * An {@link RDFTransformer} that removes some property values. If no <code>value</code> is provided, then it
 * will remove all triples with <code>resource</code> and <code>property</code> as subject an predicate,
 * respectively.
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class DeletePropertyValueRDFTransformer implements RDFTransformer {

	private Resource resource;
	private IRI property;
	private Value value;

	public DeletePropertyValueRDFTransformer(DeletePropertyValueRDFTransformerConfiguration config) {
		ValueFactory vf = SimpleValueFactory.getInstance();

		this.resource = NTriplesUtil.parseResource(config.resource, vf);
		this.property = NTriplesUtil.parseURI(config.property, vf);
		this.value = config.value != null && !config.value.isEmpty()
				? NTriplesUtil.parseValue(config.value, vf) : null;
	}

	@Override
	public void filter(RepositoryConnection sourceRepositoryConnection,
			RepositoryConnection workingRepositoryConnection, IRI[] graphs) throws RDF4JException {
		IRI[] expandedGraphs = FilterUtils.expandGraphs(workingRepositoryConnection, graphs);

		if (expandedGraphs.length == 0)
			return;

		workingRepositoryConnection.remove(resource, property, value, expandedGraphs);
	}

}
