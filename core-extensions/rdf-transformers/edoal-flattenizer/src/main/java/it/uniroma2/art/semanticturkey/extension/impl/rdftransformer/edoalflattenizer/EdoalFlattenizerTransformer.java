package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.edoalflattenizer;

import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.FilterUtils;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

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
public class EdoalFlattenizerTransformer implements RDFTransformer {

	@Nullable
	private Set<IRI> mappingProperties;

	public EdoalFlattenizerTransformer(EdoalFlattenizerTransformerConfiguration config) {
		this.mappingProperties = config.mappingProperties;

	}

	@Override
	public void transform(RepositoryConnection sourceRepositoryConnection,
			RepositoryConnection workingRepositoryConnection, IRI[] graphs) throws RDF4JException {
		IRI[] expandedGraphs = FilterUtils.expandGraphs(workingRepositoryConnection, graphs);

		if (expandedGraphs.length == 0)
			return;

		/* working repo contains a copy of source repo, I need to start from an empty repo and copy there
		only the mappings, so clear the working repo */
		workingRepositoryConnection.clear();

		String query = "prefix align: <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#>\n" +
				"select distinct ?entity1 ?entity2 ?mappingProperty {\n" +
				"    ?x a align:Cell .\n" +
				"    ?x align:entity1 ?entity1 .\n" +
				"    ?x align:entity2 ?entity2 .\n" +
				"    ?x align:mappingProperty ?mappingProperty .\n" +
				"}";

		TupleQuery tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();

		while (tupleQueryResult.hasNext()) {
			BindingSet bindingSet = tupleQueryResult.next();
			Resource entity1 = (Resource) bindingSet.getBinding("entity1").getValue();
			Resource entity2 = (Resource) bindingSet.getBinding("entity2").getValue();
			IRI mappingProperty = (IRI) bindingSet.getBinding("mappingProperty").getValue();
			workingRepositoryConnection.add(entity1, mappingProperty, entity2, expandedGraphs);
		}

	}

}
