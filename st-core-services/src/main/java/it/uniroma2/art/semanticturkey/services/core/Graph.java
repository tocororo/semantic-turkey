package it.uniroma2.art.semanticturkey.services.core;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

@STService
public class Graph extends STServiceAdapter {
	
	private static Logger logger = LoggerFactory.getLogger(Graph.class);
	
	/**
	 * Returns a set of triples <code>property</code>-<code>domain</code><code>range</code> for each properties
	 * defined in the main graph.
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf', 'R')")
	public JsonNode getGraphModel() {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode modelArrayNode = jsonFactory.arrayNode();
		
		RepositoryConnection conn = getManagedConnection();
		String query = 
				"SELECT DISTINCT ?p ?d ?r ?isDatatype WHERE { "
				+ "	?propType " + NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* " + NTriplesUtil.toNTriplesString(RDF.PROPERTY)
				+ " GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {"
				+ "		?p a ?propType . " 
				+ "		BIND(IF(EXISTS { ?p a " + NTriplesUtil.toNTriplesString(OWL.DATATYPEPROPERTY) + " }, true, false ) as ?isDatatype) "
				+ "		OPTIONAL { ?p " + NTriplesUtil.toNTriplesString(RDFS.DOMAIN) + " ?d }"
				+ "		OPTIONAL { ?p " + NTriplesUtil.toNTriplesString(RDFS.RANGE) + " ?r }"
				+ "	}"
				+ "}";
		logger.debug("query: " + query);
		TupleQuery tupleQuery = conn.prepareTupleQuery(query);
		TupleQueryResult results = tupleQuery.evaluate();
		while (results.hasNext()) {
			BindingSet bs = results.next();
			String prop = bs.getBinding("p").getValue().stringValue();
			Binding d = bs.getBinding("d");
			Binding r = bs.getBinding("r");
			boolean isDatatype = Boolean.parseBoolean(bs.getBinding("isDatatype").getValue().stringValue());
			
			ObjectNode rangeDomainNode = jsonFactory.objectNode();
			rangeDomainNode.set("property", jsonFactory.textNode(prop));
			if (d != null) {
				rangeDomainNode.set("domain", jsonFactory.textNode(d.getValue().stringValue()));
			} else { //if there is no domain, set owl:Thing by default
				rangeDomainNode.set("domain", jsonFactory.textNode(OWL.THING.stringValue()));
			}
			if (r != null) {
				rangeDomainNode.set("range", jsonFactory.textNode(r.getValue().stringValue()));
			} else { //if there is no range, set the default to rdfs:Literal for the DatatypeProperty, owl:Thing otherwise
				if (isDatatype) {
					rangeDomainNode.set("range", jsonFactory.textNode(RDFS.LITERAL.stringValue()));
				} else {
					rangeDomainNode.set("range", jsonFactory.textNode(OWL.THING.stringValue()));
				}
			}
			modelArrayNode.add(rangeDomainNode);
		}
		return modelArrayNode;
	}

}
