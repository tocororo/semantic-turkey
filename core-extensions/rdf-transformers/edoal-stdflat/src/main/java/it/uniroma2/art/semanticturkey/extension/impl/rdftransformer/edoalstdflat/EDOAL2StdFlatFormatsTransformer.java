package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.edoalstdflat;

import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.FilterUtils;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An {@link RDFTransformer} that adds to a <code>resource</code> a given <code>value</code> for a certain
 * <code>property</code>. Before adding the new value, the filter does the following: if an
 * <code>oldValue</code> is specified, then the triple <code>&lt;resource, property,
 * oldValue&gt;</code> is deleted, otherwise every triple matching the pattern <code>&lt;resource, property,
 * *&gt;</code> is deleted.
 *
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 *
 */
public class EDOAL2StdFlatFormatsTransformer implements RDFTransformer {

	@Nullable
	private Set<IRI> mappingProperties;

	private HashSet<IRI> possiblePropSet;

	public EDOAL2StdFlatFormatsTransformer(EDOAL2StdFlatFormatsTransformerConfiguration config) {
		this.mappingProperties = config.mappingProperties;

		// initialize the possibleProp Set
		// (list taken from core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\alignment\\AlignmentUtils.java)
		possiblePropSet = new HashSet<>();

		possiblePropSet.add(RDF.TYPE);
		possiblePropSet.add(RDFS.SUBCLASSOF);
		possiblePropSet.add(RDFS.SUBPROPERTYOF);

		possiblePropSet.add(OWL.DIFFERENTFROM);
		possiblePropSet.add(OWL.DISJOINTWITH);
		possiblePropSet.add(OWL.EQUIVALENTCLASS);
		possiblePropSet.add(OWL.EQUIVALENTPROPERTY);
		possiblePropSet.add(OWL.PROPERTYDISJOINTWITH);
		possiblePropSet.add(OWL.SAMEAS);

		possiblePropSet.add(SKOS.BROAD_MATCH);
		possiblePropSet.add(SKOS.CLOSE_MATCH);
		possiblePropSet.add(SKOS.EXACT_MATCH);
		possiblePropSet.add(SKOS.NARROW_MATCH);
	}

	@Override
	public void transform(RepositoryConnection sourceRepositoryConnection,
			RepositoryConnection workingRepositoryConnection, IRI[] graphs) throws RDF4JException {
		IRI[] expandedGraphs = FilterUtils.expandGraphs(workingRepositoryConnection, graphs);

		if (expandedGraphs.length == 0) {
			return;
		}

		// create a temporary Repository
		SailRepository tempRepository = new SailRepository(new MemoryStore());
		try (RepositoryConnection tempConnection = tempRepository.getConnection()) {

			// do the query using the graphs in expandedGraphs
			String query = prepareQuery(expandedGraphs);
			TupleQuery tupleQuery = workingRepositoryConnection.prepareTupleQuery(query);
			tupleQuery.setIncludeInferred(false);

			/*
			//add the named graphs (used in the GRAPH sections of the query: WHERE)
			SimpleDataset dataset = new SimpleDataset();
			for(IRI iri : expandedGraphs){
				dataset.addNamedGraph(iri);
			}
			tupleQuery.setDataset(dataset);
			*/

			TupleQueryResult tupleQueryResult = tupleQuery.evaluate();

			while (tupleQueryResult.hasNext()) {
				BindingSet bindingSet = tupleQueryResult.next();
				IRI entity1 = (IRI) bindingSet.getValue("entity1");
				IRI entity2 = (IRI) bindingSet.getValue("entity2");
				IRI mappingProperty = (IRI) bindingSet.getValue("mappingProperty");
				Resource graph = (Resource) bindingSet.getValue("g");
				// add the triple to the tempConnection using the extracted graph
				tempConnection.add(entity1, mappingProperty, entity2, graph);
			}


			// clear the workingRepositoryConnection for the graphs in expandedGraphs
			for(IRI graph : expandedGraphs) {
				workingRepositoryConnection.clear(graph);
			}

			// add all the triples from tempConnection to workingRepositoryConnection
			RepositoryResult<Statement> repositoryResult = tempConnection.getStatements(null, null, null);
			while (repositoryResult.hasNext()) {
				Statement statement = repositoryResult.next();
				Resource graph = statement.getContext();
				workingRepositoryConnection.add(statement, graph);
			}
		}

	}

	private boolean isPossibleProp(IRI prop) {
		if(possiblePropSet.contains(prop)) {
			return true;
		}
		return false;
	}


	private String prepareQuery(IRI[] expandedGraphs) {

		//@formatter:off
		String query = "prefix align: <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#>" +
				"\nSELECT DISTINCT ?g ?entity1 ?entity2 ?mappingProperty " +
				"\nWHERE {" +
				// add the graphs for the variable ?g
				"\nVALUES ?g {";
		for(IRI graph : expandedGraphs){
			query += " <"+graph.stringValue()+"> ";
		}
		query += "}" +
				"\nGRAPH ?g {" +
				"\n?x a align:Cell ." +
				"\n?x align:entity1 ?entity1 ." +
				"\n FILTER ISIRI(?entity1)" +
				"\n?x align:entity2 ?entity2 ." +
				"\nFILTER ISIRI(?entity2)" +
				"\n?x align:mappingProperty ?mappingProperty .";
		//@formatter:on
		if (mappingProperties != null && !mappingProperties.isEmpty()) {
			List<IRI> propToUse = new ArrayList<>();
			for (IRI prop : mappingProperties) {
				if (isPossibleProp(prop)) {
					propToUse.add(prop);
				}
			}
			if (!propToUse.isEmpty()) {
				query += "FILTER (";
				boolean first = true;
				for (IRI prop : propToUse) {
					if (!first) {
						query += "|| ";
					}
					query += "?mappingProperty = <" + prop.stringValue() + ">";
					first = false;
				}

				query += ")\n";
			}
		}
		query += "\n}"+
				"\n}";
		//@formatter:on
		return query;
	}
}
