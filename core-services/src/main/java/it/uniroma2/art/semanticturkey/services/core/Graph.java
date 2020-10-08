package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.stream.Collectors;

@STService
public class Graph extends STServiceAdapter {
	
	private static final Logger logger = LoggerFactory.getLogger(Graph.class);
	
	/**
	 * Returns a set of triples <code>property</code>-<code>domain</code><code>range</code> for each properties
	 * defined in the main graph.
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(code)', 'R')")
	public JsonNode getGraphModel() {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode graphModelArrayNode = jsonFactory.arrayNode();

		RepositoryConnection conn = getManagedConnection();

		/*
		 * Property range-domain
		 */
		
		String query =
				// @formatter:off
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>								\n" +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>									\n" +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#>											\n" +                                      
				"SELECT DISTINCT ?p ?d ?r ?isDatatype WHERE {											\n" +
				"	?propType rdfs:subClassOf* rdf:Property												\n" +
				"	GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {					\n" +
				"		?p a ?propType .																\n" +
				"		FILTER (isIRI(?p))																\n" +
				"		BIND(IF(EXISTS { ?p a owl:DatatypeProperty }, true, false ) as ?isDatatype)		\n" +
				"		OPTIONAL { 																		\n" +
				"			?p rdfs:domain ?d .															\n" +
				"			FILTER (isIRI(?d))															\n" +
				"		}																				\n" +
				"		OPTIONAL {																		\n" +
				"			?p rdfs:range ?r .															\n" +
				"			FILTER (isIRI(?r))															\n" +
				"		}																				\n" +
				"	}																					\n" +
				"}";
				// @formatter:on
		logger.debug("query: " + query);
		
		TupleQuery tupleQuery = conn.prepareTupleQuery(query);
		evaluateDomainRangeQuery(tupleQuery, graphModelArrayNode);
		
		/*
		 * class axioms
		 */

		List<String> graphs = QueryResults.stream(conn.getContextIDs())
				.filter(g -> !g.equals(getWorkingGraph()))
				.map(NTriplesUtil::toNTriplesString)
				.collect(Collectors.toList());

		query =
				// @formatter:off
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>											\n" +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#>													\n" +
				"SELECT DISTINCT ?s ?p ?o WHERE {																\n" +
				"	?metaclass rdfs:subClassOf* rdfs:Class .													\n" +
				"	GRAPH ?g1 {																					\n" +
				"		?s a ?metaclass .																		\n" +
				"	}																							\n" +
				"	GRAPH ?g2 {																					\n" +
				"		?o a ?metaclass .																		\n" +
				"	}																							\n" +
				"	?s ?p ?o .																					\n" +
				"	FILTER (isIRI(?s))																			\n" +
				"	FILTER (isIRI(?o))																			\n" +
				"	FILTER (?p IN(rdfs:subClassOf,owl:complementOf,owl:disjointWith,owl:equivalentClass))		\n" +
				//eclude triples where both subject and object are not in the working graph
				"	FILTER (																					\n" +
				"		?g1 NOT IN(" + String.join(",", graphs) + ") ||											\n" +
				"		?g2 NOT IN(" + String.join(",", graphs) + ")											\n" +
				"	)																							\n" +
				"}";
				// @formatter:on
		logger.debug("query: " + query);
		
		tupleQuery = conn.prepareTupleQuery(query);
		evaluateClassAxiomQuery(tupleQuery, graphModelArrayNode);
		
		return graphModelArrayNode;
	}
	
	
	
	
	/**
	 * Returns a set of triples <code>property</code>-<code>domain</code><code>range</code> for each properties
	 * defined in the main graph.
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(code)', 'R')")
	public JsonNode expandGraphModelNode(IRI resource) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

		ArrayNode graphModelArrayNode = jsonFactory.arrayNode();
		
		/*
		 * Property range-domain
		 */
		
		RepositoryConnection conn = getManagedConnection();
		String query = 
				// @formatter:off
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>								\n" +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>									\n" +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#>											\n" +                                      
				"SELECT DISTINCT ?p ?d ?r ?isDatatype WHERE {											\n" +
				"	?propType rdfs:subClassOf* rdf:Property												\n" +
				"	GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {					\n" +
				"		?p a ?propType .																\n" +
				"		BIND(IF(EXISTS { ?p a owl:DatatypeProperty }, true, false ) as ?isDatatype)		\n" +
				"		{ 																				\n" +
				"			?p rdfs:domain ?res .														\n" +
				"			BIND(?res as ?d)															\n" +
				" 			OPTIONAL {																	\n" + 
				"				?p rdfs:range ?r .														\n" +
				"				FILTER (isIRI(?r))														\n" +
				"			}																			\n" +
				"		} UNION {																		\n" +
				"			?p rdfs:range ?res .														\n" +
				"			BIND(?res as ?r)															\n" +
				" 			OPTIONAL {																	\n" + 
				"				?p rdfs:domain ?d .														\n" +
				"				FILTER (isIRI(?d))														\n" +
				"			}																			\n" +
				"		}																				\n" +
				"		FILTER (isIRI(?p))																\n" +
				"	}																					\n" +
				"}";
				// @formatter:on
		logger.debug("query: " + query);
		
		TupleQuery tupleQuery = conn.prepareTupleQuery(query);
		tupleQuery.setBinding("res", resource);
		evaluateDomainRangeQuery(tupleQuery, graphModelArrayNode);
		
		/*
		 * class axioms
		 */
		
		query = 
				// @formatter:off
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>											\n" +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#>													\n" +                                      
				"SELECT DISTINCT ?s ?p ?o WHERE {																\n" +
				"	?metaclass rdfs:subClassOf* rdfs:Class .													\n" +
				"	 GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {							\n" +
				"		?s a ?metaclass .																		\n" +
				"		?o a ?metaclass .																		\n" +
				"		{																						\n" + 
				"			?res ?p ?o .																		\n" +
				"			FILTER (isIRI(?o))																	\n" +
				"			BIND(?res as ?s)																	\n" + 
				"		} UNION {																				\n" + 
				"			?s ?p ?res .																		\n" +
				"			FILTER (isIRI(?s))																	\n" +
				"			BIND(?res as ?o)																	\n" + 
				"		}																						\n" +
				"		FILTER (?p IN(rdfs:subClassOf,owl:complementOf,owl:disjointWith,owl:equivalentClass))	\n" +
				"	}																							\n" +
				"}";
				// @formatter:on
		logger.debug("query: " + query);
		
		tupleQuery = conn.prepareTupleQuery(query);
		tupleQuery.setBinding("res", resource);
		evaluateClassAxiomQuery(tupleQuery, graphModelArrayNode);
		
		return graphModelArrayNode;
	}
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(code)', 'R')")
	public JsonNode expandSubResources(IRI resource, RDFResourceRole role) {
		
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode graphModelArrayNode = jsonFactory.arrayNode();
		
		if (role.isProperty() || role == RDFResourceRole.skosCollection || role == RDFResourceRole.cls || role == RDFResourceRole.concept) {
		
			RepositoryConnection conn = getManagedConnection();
			String query = 
					// @formatter:off
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>									\n" +
					"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>									\n" +
					"SELECT DISTINCT ?s ?p ?o WHERE {														\n";
			if (role == RDFResourceRole.concept) {
				query +=
					"	?metaclass rdfs:subClassOf* skos:Concept .										\n" +
					"	{ 																				\n" +
					"		?p rdfs:subPropertyOf* skos:narrower .										\n" +
					"		?o a ?metaclass .															\n" +
					"		?res ?p ?o .																\n" +
					"		BIND(?res as ?s)															\n" +
					"	} UNION {																		\n" +
					"		?p rdfs:subPropertyOf* skos:broader .										\n" +
					"		?s a ?metaclass .															\n" +
					"		?s ?p ?res .																\n" +
					"		BIND(?res as ?o)															\n" +
					"	}																				\n";
			} else if (role == RDFResourceRole.cls) {
				query +=
					"	?p rdfs:subPropertyOf* rdfs:subClassOf .										\n" +
					"	?s ?p ?res .																	\n" +
					"	BIND(?res as ?o)																\n";
			} else if (role.isProperty()) {
				query +=
					"	?p rdfs:subPropertyOf* rdfs:subPropertyOf .										\n" +
					"	?s ?p ?res .																	\n" +
					"	BIND(?res as ?o)																\n";
			} else if (role == RDFResourceRole.skosCollection) {
				//orderedCollection not included: the relation parent child is not direct
				query +=
					"	FILTER NOT EXISTS {?res skos:memberList [] }									\n" +
					"	?res skos:member ?o .															\n" +
					"	BIND(skos:member as ?p)															\n" +
					"	BIND(?res as ?s)																\n";
			}
			query +=
					"}";
					// @formatter:on
			logger.debug("query: " + query);
			
			TupleQuery tupleQuery = conn.prepareTupleQuery(query);
			tupleQuery.setBinding("res", resource);
			evaluateSubjPredObjQuery(tupleQuery, graphModelArrayNode);
		}
		return graphModelArrayNode;
	}
	
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(code)', 'R')")
	public JsonNode expandSuperResources(IRI resource, RDFResourceRole role) {
		
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode graphModelArrayNode = jsonFactory.arrayNode();
		
		if (role.isProperty() || role == RDFResourceRole.skosCollection|| role == RDFResourceRole.cls || role == RDFResourceRole.concept) {
		
			RepositoryConnection conn = getManagedConnection();
			String query = 
					// @formatter:off
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>									\n" +
					"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>									\n" +
					"SELECT DISTINCT ?s ?p ?o WHERE {														\n";
			if (role == RDFResourceRole.concept) {
				query +=
					"	?metaclass rdfs:subClassOf* skos:Concept .										\n" +
					"	{ 																				\n" +
					"		?p rdfs:subPropertyOf* skos:narrower .										\n" +
					"		?s a ?metaclass .															\n" +
					"		?s ?p ?res .																\n" +
					"		BIND(?res as ?o)															\n" +
					"	} UNION {																		\n" +
					"		?p rdfs:subPropertyOf* skos:broader .										\n" +
					"		?o a ?metaclass .															\n" +
					"		?res ?p ?o .																\n" +
					"		BIND(?res as ?s)															\n" +
					"	}																				\n";
			} else if (role == RDFResourceRole.cls) {
				query +=
					"	?p rdfs:subPropertyOf* rdfs:subClassOf .										\n" +
					"	?res ?p ?o .																	\n" +
					"	BIND(?res as ?s)																\n";
			} else if (role.isProperty()) {
				query +=
					"	?p rdfs:subPropertyOf* rdfs:subPropertyOf .										\n" +
					"	?res ?p ?o .																	\n" +
					"	BIND(?res as ?s)																\n";
			} else if (role == RDFResourceRole.skosCollection) {
				//orderedCollection not included: the relation parent child is not direct
				query +=
					"	FILTER NOT EXISTS {?s skos:memberList [] }										\n" +
					"	?s skos:member ?res .															\n" +
					"	BIND(skos:member as ?p)															\n" +
					"	BIND(?res as ?o)																\n";
			}
			query +=
					"}";
					// @formatter:on
			logger.debug("query: " + query);
			
			TupleQuery tupleQuery = conn.prepareTupleQuery(query);
			tupleQuery.setBinding("res", resource);
			evaluateSubjPredObjQuery(tupleQuery, graphModelArrayNode);
		}
		return graphModelArrayNode;
	}
	

	private void evaluateDomainRangeQuery(TupleQuery tupleQuery, ArrayNode graphModelArrayNode) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		TupleQueryResult results = tupleQuery.evaluate();
		while (results.hasNext()) {
			BindingSet bs = results.next();
			String prop = bs.getBinding("p").getValue().stringValue();
			Binding d = bs.getBinding("d");
			Binding r = bs.getBinding("r");
			boolean isDatatype = Boolean.parseBoolean(bs.getBinding("isDatatype").getValue().stringValue());
			
			String domain;
			String range;
			
			if (d != null) {
				domain = d.getValue().stringValue();
			} else { //if there is no domain, set owl:Thing by default
				domain = OWL.THING.stringValue();
			}
			if (r != null) {
				range = r.getValue().stringValue();
			} else { //if there is no range, set the default to rdfs:Literal for the DatatypeProperty, owl:Thing otherwise
				if (isDatatype) {
					range = RDFS.LITERAL.stringValue();
				} else {
					range = OWL.THING.stringValue();
				}
			}

			ObjectNode graphModelNode = jsonFactory.objectNode();
			graphModelNode.set("classAxiom", jsonFactory.booleanNode(false));
			graphModelNode.set("rangeDatatype", jsonFactory.booleanNode(isDatatype));
			graphModelNode.set("link", jsonFactory.textNode(prop));
			graphModelNode.set("source", jsonFactory.textNode(domain));
			graphModelNode.set("target", jsonFactory.textNode(range));
			graphModelArrayNode.add(graphModelNode);
		}
	}
	
	private void evaluateClassAxiomQuery(TupleQuery tupleQuery, ArrayNode graphModelArrayNode) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		TupleQueryResult results = tupleQuery.evaluate();
		while (results.hasNext()) {
			BindingSet bs = results.next();
			String subj = bs.getBinding("s").getValue().stringValue();
			String prop = bs.getBinding("p").getValue().stringValue();
			String obj = bs.getBinding("o").getValue().stringValue();
			
			ObjectNode graphModelNode = jsonFactory.objectNode();
			graphModelNode.set("classAxiom", jsonFactory.booleanNode(true));
			graphModelNode.set("rangeDatatype", jsonFactory.booleanNode(false));
			graphModelNode.set("source", jsonFactory.textNode(subj));
			graphModelNode.set("link", jsonFactory.textNode(prop));
			graphModelNode.set("target", jsonFactory.textNode(obj));
			graphModelArrayNode.add(graphModelNode);
		}
	}
	
	private void evaluateSubjPredObjQuery(TupleQuery tupleQuery, ArrayNode graphModelArrayNode) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		TupleQueryResult results = tupleQuery.evaluate();
		while (results.hasNext()) {
			BindingSet bs = results.next();
			String subj = bs.getBinding("s").getValue().stringValue();
			String prop = bs.getBinding("p").getValue().stringValue();
			String obj = bs.getBinding("o").getValue().stringValue();
			
			ObjectNode graphModelNode = jsonFactory.objectNode();
			graphModelNode.set("source", jsonFactory.textNode(subj));
			graphModelNode.set("link", jsonFactory.textNode(prop));
			graphModelNode.set("target", jsonFactory.textNode(obj));
			graphModelArrayNode.add(graphModelNode);
		}
	}

}
