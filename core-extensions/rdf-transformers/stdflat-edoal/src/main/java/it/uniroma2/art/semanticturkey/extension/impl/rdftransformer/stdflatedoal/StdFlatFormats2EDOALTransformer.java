package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.stdflatedoal;

import it.uniroma2.art.semanticturkey.alignment.Cell;
import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.FilterUtils;
import it.uniroma2.art.semanticturkey.vocabulary.Alignment;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
public class StdFlatFormats2EDOALTransformer implements RDFTransformer {

	@Nullable
	private Set<IRI> mappingProperties;

	private Entity1Position entity1_position;

	private HashMap<IRI, String> possiblePropMap;

	public enum Entity1Position {subject, object}

	public StdFlatFormats2EDOALTransformer(StdFlatFormats2EDOALTransformerConfiguration config) {
		this.entity1_position = config.entity1_position;

		this.mappingProperties = config.mappingProperties;

		// initialize the possibleProp Set
		// (list taken from core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\alignment\\AlignmentUtils.java)
		possiblePropMap = new HashMap<>();

		//possiblePropMap.put(RDF.TYPE, "InstanceOf");
		possiblePropMap.put(RDFS.SUBCLASSOF, "<");
		possiblePropMap.put(RDFS.SUBPROPERTYOF, "<");

		possiblePropMap.put(OWL.DIFFERENTFROM, "%");
		possiblePropMap.put(OWL.DISJOINTWITH, "%");
		possiblePropMap.put(OWL.EQUIVALENTCLASS, "=");
		possiblePropMap.put(OWL.EQUIVALENTPROPERTY, "=");
		possiblePropMap.put(OWL.PROPERTYDISJOINTWITH, "%");
		possiblePropMap.put(OWL.SAMEAS, "=");

		possiblePropMap.put(SKOS.BROAD_MATCH, "<");
		possiblePropMap.put(SKOS.CLOSE_MATCH, "=");
		possiblePropMap.put(SKOS.EXACT_MATCH, "=");
		possiblePropMap.put(SKOS.NARROW_MATCH, ">");
	}

	@Override
	public void transform(RepositoryConnection sourceRepositoryConnection,
			RepositoryConnection workingRepositoryConnection, IRI[] graphs) throws RDF4JException {
		IRI[] expandedGraphs = FilterUtils.expandGraphs(workingRepositoryConnection, graphs);

		if (expandedGraphs.length == 0) {
			return;
		}

		List<IRI> propToUse = new ArrayList<>();

		// prepare the list of property that will be searched via the SPARQL query
		if(mappingProperties != null && !mappingProperties.isEmpty()) {
			// at least one property has been passed in mappingProperties, so check if at least one of these
			// property can be used (is in possiblePropMap)
			for (IRI prop : mappingProperties) {
				String relation = getRelation(prop);
				if (relation!=null) {
					propToUse.add(prop);
				}
			}
		}
		if (propToUse.isEmpty()) {
			// no property was passed or the ones that were passed were not usable, so use the ones from possiblePropMap
			propToUse.addAll(possiblePropMap.keySet());
		}

		// do the query using the graphs from expandedGraphs
		String query = prepareSelectQuery(expandedGraphs, propToUse);

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


		// execute the query
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();

		// process the query results
		// this map will contain each graph to the list of Cell
		Map<Resource, List<Cell>> graphToCelLListMap = new HashMap<>();
		while (tupleQueryResult.hasNext()) {
			BindingSet bindingSet = tupleQueryResult.next();

			IRI subj = (IRI) bindingSet.getValue("subj");
			IRI pred = (IRI) bindingSet.getValue("pred");
			IRI obj = (IRI) bindingSet.getValue("obj");
			Resource graph = (Resource) bindingSet.getValue("g");

			// consider only those subj and obj having a different namespace
			String namespaceSubj = subj.getNamespace();
			String namespaceObj = obj.getNamespace();
			if (namespaceSubj.equals(namespaceObj)) {
				// same namespace, so skip it
				continue;
			}
			String relation = getRelation(pred);
			Cell cell;
			if(entity1_position.equals(Entity1Position.subject)) {
				cell = new Cell(subj, obj, Float.parseFloat("1.0"), relation);
			} else {
				cell = new Cell(obj, subj, Float.parseFloat("1.0"), relation);
			}
			cell.setMappingProperty(pred);
			if (!graphToCelLListMap.containsKey(graph)) {
				graphToCelLListMap.put(graph, new ArrayList<>());
			}
			graphToCelLListMap.get(graph).add(cell);
		}

		// add the cell list to the workingRepositoryConnection

		// check if the sourceRepositoryConnection already has a bnode being an instance of
		// <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#Alignment>, is so, use such resource (normally a BNode)
		// otherwise create a new bnode
		Map<IRI, Resource> graphToAlignResourceMap = new HashMap<>();
		for(IRI graph : expandedGraphs) {
			//@formatter:off
			query = "SELECT ?align " +
					"\nWHERE {" +
					"\nGRAPH <"+graph.stringValue()+"> {" +
					"?align a <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#Alignment> ." +
					"\n}" +
					"\n}";
			//@formatter:on


			tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
			tupleQuery.setIncludeInferred(false);
			tupleQueryResult = tupleQuery.evaluate();
			if(tupleQueryResult.hasNext()) {
				Resource alignRes = (Resource) tupleQueryResult.next().getValue("align");
				graphToAlignResourceMap.put(graph, alignRes);
			} else {
				// the graph has no instance of <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#Alignment>
				// so create a bnode
				graphToAlignResourceMap.put(graph, SimpleValueFactory.getInstance().createBNode());
			}
		}

		// for each graph, add the cells and for the align (clean the data in such graph before)
		for (IRI graph : expandedGraphs) {
			workingRepositoryConnection.clear(graph);
			for (Cell cell : graphToCelLListMap.get(graph)) {
				addCellToRepo(workingRepositoryConnection, graph, cell, graphToAlignResourceMap.get(graph));
			}
		}
	}

	private String getRelation(IRI prop) {
		// return the relation or null if the passed property is not present in the map
		return possiblePropMap.get(prop);
	}

	private String prepareSelectQuery(IRI[] expandedGraphs, List<IRI> propToUse) {

		//@formatter:off
		String query = "SELECT DISTINCT ?g ?subj ?pred ?obj " +
				"\nWHERE {" +
				// add the graphs for the variable ?g
				"\nVALUES ?g {";
		for(IRI graph : expandedGraphs){
			query += " <"+graph.stringValue()+"> ";
		}
		query += "}" +
				"\nGRAPH ?g {" +
				"\nVALUES ?pred {";
		// add the property to use/search
		for (IRI prop : propToUse) {
			query+= " <"+prop.stringValue()+"> ";
		}
		query += "}" +
				"\n?subj ?pred ?obj ." +
				"\nFILTER ISIRI(?subj)" +
				"\nFILTER ISIRI(?obj)" +
				"\n}" +
				"\n}";
		//@formatter:on

		return query;
	}

	private void addCellToRepo(RepositoryConnection repoConnection, IRI graph, Cell cell, Resource alignRes) {
		//@formatter:off
		String query = "INSERT {" +
				"\n GRAPH <"+graph.stringValue()+"> {" +
				"\n?alignment a <" + Alignment.ALIGNMENT.stringValue() + "> ." +
				"\n_:cell a <" + Alignment.CELL.stringValue() + "> ." +
				"\n?alignment <" + Alignment.MAP.stringValue() + "> _:cell ." +
				"\n_:cell <" + Alignment.ENTITY1.stringValue() + "> <" + cell.getEntity1().stringValue() + "> ." +
				"\n_:cell <" + Alignment.ENTITY2.stringValue() + "> <" + cell.getEntity2().stringValue() + "> ." +
				"\n_:cell <" + Alignment.MEASURE.stringValue() + "> '" + cell.getMeasure() + "'^^<" + XSD.FLOAT.stringValue() + "> ." +
				"\n_:cell <" + Alignment.RELATION.stringValue() + "> '" + cell.getRelation() + "' .\n" +
				"\n_:cell <" + Alignment.MAPPING_PROPERTY.stringValue() + "> <" + cell.getMappingProperty().stringValue() + "> ." +
				"\n}"+
				"\n}" +
				"\nWHERE {" +
				"\nBIND ( ?alignRes AS ?alignment)" + // just to have a variable in the INSERT
				"\n}";
		//@formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("alignRes", alignRes);


		update.setIncludeInferred(false);
		update.execute();
	}

}
