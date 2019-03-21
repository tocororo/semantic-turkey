package it.uniroma2.art.semanticturkey.services.core;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
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
				"		FILTER (!isBlank(?p))															\n" +
				"		BIND(IF(EXISTS { ?p a owl:DatatypeProperty }, true, false ) as ?isDatatype)		\n" +
				"		OPTIONAL { 																		\n" +
				"			?p rdfs:domain ?d .															\n" +
				"			FILTER (!isBlank(?d))														\n" +
				"		}																				\n" +
				"		OPTIONAL {																		\n" +
				"			?p rdfs:range ?r .															\n" +
				"			FILTER (!isBlank(?r))														\n" +
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
		
		query = 
				// @formatter:off
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>										\n" +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>											\n" +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#>													\n" +                                      
				"SELECT DISTINCT ?s ?p ?o WHERE {																\n" +
				"	?metaclass rdfs:subClassOf* rdfs:Class .													\n" +
				"	 GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {							\n" +
				"		?s a ?metaclass .																		\n" +
				"		?o a ?metaclass .																		\n" +
				"		?s ?p ?o .																				\n" +
				"		FILTER (!isBlank(?s))																	\n" +
				"		FILTER (!isBlank(?o))																	\n" +
				"		FILTER (?p IN(rdfs:subClassOf,owl:complementOf,owl:disjointWith,owl:equivalentClass))	\n" +
				"	}																							\n" +
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
	@PreAuthorize("@auth.isAuthorized('rdf', 'R')")
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
				"				FILTER (!isBlank(?r))													\n" +
				"			}																			\n" +
				"		} UNION {																		\n" +
				"			?p rdfs:range ?res .														\n" +
				"			BIND(?res as ?r)															\n" +
				" 			OPTIONAL {																	\n" + 
				"				?p rdfs:domain ?d .														\n" +
				"				FILTER (!isBlank(?d))													\n" +
				"			}																			\n" +
				"		}																				\n" +
				"		FILTER (!isBlank(?p))															\n" +
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
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>										\n" +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>											\n" +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#>													\n" +                                      
				"SELECT DISTINCT ?s ?p ?o WHERE {																\n" +
				"	?metaclass rdfs:subClassOf* rdfs:Class .													\n" +
				"	 GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {							\n" +
				"		?s a ?metaclass .																		\n" +
				"		?o a ?metaclass .																		\n" +
				"		{																						\n" + 
				"			?res ?p ?o .																		\n" +
				"			FILTER (!isBlank(?o))																\n" +
				"			BIND(?res as ?s)																	\n" + 
				"		} UNION {																				\n" + 
				"			?s ?p ?res .																		\n" +
				"			FILTER (!isBlank(?s))																\n" +
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
			graphModelNode.set("source", jsonFactory.textNode(subj));
			graphModelNode.set("link", jsonFactory.textNode(prop));
			graphModelNode.set("target", jsonFactory.textNode(obj));
			graphModelArrayNode.add(graphModelNode);
		}
	}

}
