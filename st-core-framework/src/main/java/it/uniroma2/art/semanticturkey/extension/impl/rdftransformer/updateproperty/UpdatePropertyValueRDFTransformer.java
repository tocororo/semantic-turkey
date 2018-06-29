package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.updateproperty;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.FilterUtils;

/**
 * An {@link RDFTransformer} that adds to a <code>resource</code> a given <code>value</code> for a certain
 * <code>property</code>. Before adding the new value, the filter does the following: if an
 * <code>oldValue</code> is specified, then the triple <code>&lt;resource, property,
 * oldValue&gt;</code> is deleted, otherwise every triple matching the pattern <code>&lt;resource, property,
 * *&gt;</code> is deleted.
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class UpdatePropertyValueRDFTransformer implements RDFTransformer {

	private Resource resource;
	private IRI property;
	private Value value;
	@Nullable
	private Value oldValue;

	public UpdatePropertyValueRDFTransformer(UpdatePropertyValueRDFTransformerConfiguration config) {
		this.resource = config.resource;
		this.property = config.property;
		this.value = config.value;
		this.oldValue = config.oldValue;

	}

	@Override
	public void transform(RepositoryConnection sourceRepositoryConnection,
			RepositoryConnection workingRepositoryConnection, IRI[] graphs) throws RDF4JException {
		IRI[] expandedGraphs = FilterUtils.expandGraphs(workingRepositoryConnection, graphs);

		if (expandedGraphs.length == 0)
			return;

		workingRepositoryConnection.remove(resource, property, oldValue, expandedGraphs);
		workingRepositoryConnection.add(resource, property, value, expandedGraphs);
	}

}
