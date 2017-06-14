package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.toMap;

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;


import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.TurtleHelp;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;


@GenerateSTServiceController
@Validated
@Component
public class ICV extends STServiceAdapter {
	
	protected static Logger logger = LoggerFactory.getLogger(ICV.class);
	
	//-----ICV ON CONCEPTS STRUCTURE-----
	
	/**
	 * Returns a list of records <concept>, where concept is a dangling skos:Concept in the given
	 * skos:ConceptScheme
	 * @param scheme scheme where the concepts are dangling
	 * @param limit limit of the record to return
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Response listDanglingConcepts(IRI scheme, @Optional (defaultValue="0") Integer limit)  {
		RepositoryConnection conn = getManagedConnection();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element collectionElem = XMLHelp.newElement(dataElement, "collection");
		//nested query
		String q = "SELECT ?concept ?count WHERE { \n"
				//this counts records
				+ "{ SELECT (COUNT (*) AS ?count) WHERE{\n"
				+ "BIND(<" + scheme.stringValue() + "> as ?scheme)"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.TOP_CONCEPT_OF + "> ?scheme}\n"
				+ "FILTER NOT EXISTS {?scheme <" + SKOS.HAS_TOP_CONCEPT + "> ?concept }\n"
				+ "{?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1  . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> ?broaderConcept1 .\n"
				+ "FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.IN_SCHEME + "> ?scheme  . }\n"
				+ "} {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "?broaderConcept2 <" + SKOS.NARROWER + "> ?concept .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.IN_SCHEME + "> ?scheme . }\n"
				+ "} } }"
				//this retrieves data
				+ "{ SELECT ?concept ?scheme WHERE{\n"
				+ "BIND(<" + scheme.stringValue() + "> as ?scheme)"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.TOP_CONCEPT_OF + "> ?scheme}\n"
				+ "FILTER NOT EXISTS {?scheme <" + SKOS.HAS_TOP_CONCEPT + "> ?concept }\n"
				+ "{?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1  . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> ?broaderConcept1 .\n"
				+ "FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.IN_SCHEME + "> ?scheme  . }\n"
				+ "} {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "?broaderConcept2 <" + SKOS.NARROWER + "> ?concept .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.IN_SCHEME + "> ?scheme . }\n"
				+ "} } ORDER BY ?concept";
		if (limit > 0)
			q = q + " LIMIT " + limit; //for client-side performance issue
		q = q + "} }";
		logger.info("query [listDanglingConcepts]:\n" + q);
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		collectionElem.setAttribute("count", "0");//default (if query return result, in the next while it's override
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String count = tb.getBinding("count").getValue().stringValue();
			collectionElem.setAttribute("count", count);
			String concept = tb.getBinding("concept").getValue().stringValue();
			Element recordElem = XMLHelp.newElement(collectionElem, "record");
			recordElem.setAttribute("concept", concept);
		}
		return response;
	}
	
	/**
	 * Detects cyclic hierarchical relations. Returns a list of records top, n1, n2 where 
	 * top is likely the cause of the cycle, n1 and n2 are vertex that belong to the cycle
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Response listCyclicConcepts()  {
		RepositoryConnection conn = getManagedConnection();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT DISTINCT ?top ?n1 ?n2 WHERE{\n"
				+ "{?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?n1 .\n"
				+ "?n1 (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">) ?n2 .\n"
				+ "?n2 (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?top .\n"
				+ "}UNION{\n"
				+ "?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">) ?n1 .\n"
				+ "?n1 (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">) ?top .\n"
				+ "bind(?top as ?n2)\n"
				+ "} {\n" 
				+ "?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?cyclicConcept .\n"
				+ "?cyclicConcept (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?top .\n"
				+ "?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">) ?broader .\n"
				+ "FILTER NOT EXISTS {\n"
				+ "?broader (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?top }\n"
				+ "} UNION {\n"
				+ "?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?cyclicConcept .\n"
				+ "?cyclicConcept (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?top .\n"
				+ "?top (<" + SKOS.TOP_CONCEPT_OF + "> | ^ <" + SKOS.HAS_TOP_CONCEPT + ">)+ ?scheme .} }";
		logger.info("query [listCyclicConcepts]:\n" + q);
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String topCyclicConcept = tb.getBinding("top").getValue().stringValue();
			String node1 = tb.getBinding("n1").getValue().stringValue();
			String node2 = tb.getBinding("n2").getValue().stringValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("topCyclicConcept", topCyclicConcept);
			recordElem.setAttribute("node1", node1);
			recordElem.setAttribute("node2", node2);
		}
		return response;
	}
	
	/**
	 * Returns a list of skos:ConceptScheme that have no top concept
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(conceptScheme)', 'R')")
	public Response listConceptSchemesWithNoTopConcept() {
		RepositoryConnection conn = getManagedConnection();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?conceptScheme WHERE {\n"
				+ "?conceptScheme a <" + SKOS.CONCEPT_SCHEME + "> .\n"
				+ "FILTER NOT EXISTS { {\n"
				+ "?conceptScheme <" + SKOS.HAS_TOP_CONCEPT + "> ?topConcept .\n"
				+ "} UNION {\n"
				+ "?topConcept <" + SKOS.TOP_CONCEPT_OF + "> ?conceptScheme . } } }";
		logger.info("query [listConceptSchemesWithNoTopConcept]:\n" + q);
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String conceptScheme = tb.getBinding("conceptScheme").getValue().stringValue();
			XMLHelp.newElement(dataElement, "conceptScheme", conceptScheme);
		}
		return response;
	}
	
	/**
	 * Returns a list of skos:Concept that don't belong to any scheme 
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Response listConceptsWithNoScheme(@Optional (defaultValue="0") Integer limit){
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element collectionElem = XMLHelp.newElement(dataElement, "collection");
		RepositoryConnection conn = getManagedConnection();
		//nested query
		String q = "SELECT ?concept ?count WHERE { \n"
				//this counts records
				+ "{ SELECT (COUNT (?concept) AS ?count) WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "FILTER NOT EXISTS { ?concept <" + SKOS.IN_SCHEME + "> ?scheme . }"
				+ "\n}\n}"
				//this retrieves data
				+ "{ SELECT ?concept WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "FILTER NOT EXISTS { ?concept <" + SKOS.IN_SCHEME + "> ?scheme . } }";
		if (limit > 0)
			q = q + "\nLIMIT " + limit; //for client-side performance issue
		q = q + "\n}\n}";
		logger.info("query [listConceptsWithNoScheme]:\n" + q);
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		collectionElem.setAttribute("count", "0");//default (if query return result, in the next while it's override
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String count = tb.getBinding("count").getValue().stringValue();
			collectionElem.setAttribute("count", count);
			String concept = tb.getBinding("concept").getValue().stringValue();
			XMLHelp.newElement(collectionElem, "concept", concept);
		}
		return response;
	}
	
	/**
	 * Returns a list of skos:Concept that are topConcept but have a broader 
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Response listTopConceptsWithBroader(){
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT DISTINCT ?concept ?scheme WHERE {\n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?broader <" + SKOS.IN_SCHEME + "> ?scheme . }";
		logger.info("query [listTopConceptsWithBroader]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String concept = tb.getBinding("concept").getValue().stringValue();
			String scheme = tb.getBinding("scheme").getValue().stringValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("concept", concept);
			recordElem.setAttribute("scheme", scheme);
		}
		return response;
	}
	
	/**
	 * Returns a list of skos:Concept that have redundant hierarchical relations
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Response listHierarchicallyRedundantConcepts(){
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT DISTINCT ?narrower ?broader WHERE{\n"
				+ "?narrower <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?narrower (<" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + ">)+ ?middle .\n"
				+ "?middle <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "FILTER(?narrower != ?middle)\n}";
		logger.info("query [listHierarchicallyRedundantConcepts]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String narrower = tb.getBinding("narrower").getValue().stringValue();
			String broader = tb.getBinding("broader").getValue().stringValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("broader", broader);
			recordElem.setAttribute("narrower", narrower);
		}
		return response;
	}
	
	//-----ICV ON LABELS-----
	
	/**
	 * Returns a list of records concept1-concept2-label-lang, of concepts that have the same skos:prefLabel
	 * in the same language
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Response listConceptsWithSameSKOSPrefLabel() {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept1 ?concept2 ?label ?lang WHERE {\n"
				+ "?concept1 a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept2 a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept1 <" + SKOS.PREF_LABEL + "> ?label .\n"
				+ "?concept2 <" + SKOS.PREF_LABEL + "> ?label .\n"
				+ "bind(lang(?label) as ?lang)\n"
				+ "FILTER (str(?concept1) < str(?concept2)) }";
		logger.info("query [listConceptsWithSameSKOSPrefLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String concept1 = tb.getBinding("concept1").getValue().stringValue();
			String concept2 = tb.getBinding("concept2").getValue().stringValue();
			String label = tb.getBinding("label").getValue().stringValue();
			String lang = tb.getBinding("lang").getValue().stringValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("concept1", concept1);
			recordElem.setAttribute("concept2", concept2);
			recordElem.setAttribute("label", label);
			recordElem.setAttribute("lang", lang);
		}
		return response;
	}
	
	/**
	 * Returns a list of records concept1-concept2-label-lang, of concepts that have the same skosxl:prefLabel
	 * in the same language
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Response listConceptsWithSameSKOSXLPrefLabel() {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept1 ?concept2 ?label1 ?lang WHERE {\n"
				+ "?concept1 a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept2 a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept1 <" + SKOSXL.PREF_LABEL + "> ?xlabel1 .\n"
				+ "?concept2 <" + SKOSXL.PREF_LABEL + "> ?xlabel2 .\n"
				+ "?xlabel1 <" + SKOSXL.LITERAL_FORM + "> ?label1 .\n"
				+ "?xlabel2 <" + SKOSXL.LITERAL_FORM + "> ?label2 .\n"
				+ "FILTER (?label1 = ?label2)\n"
				+ "FILTER (str(?concept1) < str(?concept2))\n"
				+ "bind(lang(?label1) as ?lang) }";
		logger.info("query [listConceptsWithSameSKOSXLPrefLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String concept1 = tb.getBinding("concept1").getValue().stringValue();
			String concept2 = tb.getBinding("concept2").getValue().stringValue();
			String label = tb.getBinding("label1").getValue().stringValue();
			String lang = tb.getBinding("lang").getValue().stringValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("concept1", concept1);
			recordElem.setAttribute("concept2", concept2);
			recordElem.setAttribute("label", label);
			recordElem.setAttribute("lang", lang);
		}
		return response;
	}
	
	/**
	 * Returns a list of records resource-lang, of concept that have a skos:altLabel for a lang but not a skos:prefLabel
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Response listResourcesWithOnlySKOSAltLabel() {
		String q = "SELECT DISTINCT ?resource ?lang ?type WHERE {\n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . } \n"
				+ "UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPT_SCHEME + "> . } \n"
				+ "?resource a ?type . \n"
				+ "?resource <" + SKOS.ALT_LABEL + "> ?alt .\n"
				+ "bind (lang(?alt) as ?lang) .\n"
				+ "FILTER NOT EXISTS {\n"
				+ "?resource <" + SKOS.PREF_LABEL + "> ?pref .\n"
				+ "FILTER (lang(?pref) = ?lang) } }";
		logger.info("query [listResourcesWithOnlySKOSAltLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			IRI resource = (IRI) tb.getValue("resource");
			String type = tb.getBinding("type").getValue().stringValue();
			RDFResourceRole role = RDFResourceRole.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRole.concept;
			} else if (type.equals(SKOS.CONCEPT_SCHEME)) {
				role = RDFResourceRole.conceptScheme;
			}
			String lang = tb.getBinding("lang").getValue().stringValue();
			
			Element recordElem = XMLHelp.newElement(dataElem, "record");
			
			addResourceToElement(recordElem, resource, role, resource.stringValue());
			
			XMLHelp.newElement(recordElem, "lang", lang);
		}
		return response;
	}
	
	/**
	 * Returns a list of records resource-lang, of concept or scheme that have a skos:altLabel
	 * for a lang but not a skos:prefLabel
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Response listResourcesWithOnlySKOSXLAltLabel() {
		String q = "SELECT DISTINCT ?resource ?lang ?type WHERE { \n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . } \n"
				+ "UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPT_SCHEME + "> . } \n"
				+ "?resource a ?type . \n"
				+ "?resource <" + SKOSXL.ALT_LABEL + "> ?alt . \n"
				+ "?alt <" + SKOSXL.LITERAL_FORM + "> ?literalFormAlt . \n"
				+ "bind (lang(?literalFormAlt) as ?lang) . \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?resource <" + SKOSXL.PREF_LABEL + "> ?pref . \n"
				+ "?pref <" + SKOSXL.LITERAL_FORM + "> ?literalFormPref . \n"
				+ "FILTER (lang(?literalFormPref) = ?lang) } }";
		logger.info("query [listResourcesWithOnlySKOSAltLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			IRI resource = (IRI) tb.getValue("resource");
			String type = tb.getBinding("type").getValue().stringValue();
			RDFResourceRole role = RDFResourceRole.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRole.concept;
			} else if (type.equals(SKOS.CONCEPT_SCHEME)) {
				role = RDFResourceRole.conceptScheme;
			}
			String lang = tb.getBinding("lang").getValue().stringValue();
			
			Element recordElem = XMLHelp.newElement(dataElem, "record");
			
			addResourceToElement(recordElem, resource, role, resource.stringValue());
			
			XMLHelp.newElement(recordElem, "lang", lang);
		}
		return response;
	}
	
	/**
	 * Returns a list of concepts or schemes that have no skos:prefLabel
	 * @return
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws ModelAccessException 
	 * @throws UnsupportedQueryLanguageException 
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Response listResourcesWithNoSKOSPrefLabel() {
		String q = "SELECT ?resource ?type WHERE {\n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPT_SCHEME + "> . }\n"
				+ "?resource a ?type . \n"
				+ "FILTER NOT EXISTS {\n"
				+ "?resource <" + SKOS.PREF_LABEL + "> ?prefLabel .\n"
				+ "} }";
		logger.info("query [listResourcesWithNoSKOSPrefLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element collectionElement = XMLHelp.newElement(dataElement, "collection");
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			IRI resource = (IRI) tb.getValue("resource");
			String type = tb.getBinding("type").getValue().stringValue();
			RDFResourceRole role = RDFResourceRole.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRole.concept;
			} else if (type.equals(SKOS.CONCEPT_SCHEME)) {
				role = RDFResourceRole.conceptScheme;
			}
			addResourceToElement(collectionElement, resource, role, resource.stringValue());
		}
		return response;
	}
	
	/**
	 * Returns a list of concepts or schemes that have no skosxl:prefLabel
	 * @return
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws ModelAccessException 
	 * @throws UnsupportedQueryLanguageException 
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Response listResourcesWithNoSKOSXLPrefLabel()  {
		String q = "SELECT ?resource ?type WHERE {\n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPT_SCHEME + "> . }\n"
				+ "?resource a ?type . \n"
				+ "FILTER NOT EXISTS {\n"
				+ "?resource <" + SKOSXL.PREF_LABEL + "> ?prefLabel .\n"
				+ "} }";
		logger.info("query [listResourcesWithNoSKOSXLPrefLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element collectionElement = XMLHelp.newElement(dataElement, "collection");
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			IRI resource = (IRI) tb.getValue("resource");
			String type = tb.getBinding("type").getValue().stringValue();
			RDFResourceRole role = RDFResourceRole.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRole.concept;
			} else if (type.equals(SKOS.CONCEPT_SCHEME)) {
				role = RDFResourceRole.conceptScheme;
			}
			addResourceToElement(collectionElement, resource, role, resource.stringValue());
		}
		return response;
	}
	
	/**
	 * Returns a list of pairs concept-lang of that concept that have more skos:prefLabel in a same language
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Response listConceptsWithMultipleSKOSPrefLabel() {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT DISTINCT ?concept ?lang WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.PREF_LABEL + "> ?label1.\n"
				+ "?concept <" + SKOS.PREF_LABEL + "> ?label2.\n"
				+ "FILTER ( ?label1 != ?label2 && lang(?label1) = lang(?label2) )\n"
				+ "bind(lang(?label1) as ?lang) }";
		logger.info("query [listConceptsWithMultipleSKOSPrefLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String concept = tb.getBinding("concept").getValue().stringValue();
			String lang = tb.getBinding("lang").getValue().stringValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("concept", concept);
			recordElem.setAttribute("lang", lang);
		}
		return response;
	}
	
	/**
	 * Returns a list of records concept-lang of that concept that have more skosxl:prefLabel in a same language
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Response listConceptsWithMultipleSKOSXLPrefLabel() {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT DISTINCT ?concept ?lang WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOSXL.PREF_LABEL + "> ?label1 .\n"
				+ "?concept <" + SKOSXL.PREF_LABEL + "> ?label2 .\n"
				+ "?label1 <" + SKOSXL.LITERAL_FORM + "> ?lit1 .\n"
				+ "?label2 <" + SKOSXL.LITERAL_FORM + "> ?lit2 .\n"
				+ "bind(lang(?lit1) as ?lang)\n"
				+ "FILTER ( ?label1 != ?label2 && lang(?lit1) = lang(?lit2) ) }";
		logger.info("query [listConceptsWithMultipleSKOSXLPrefLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String concept = tb.getBinding("concept").getValue().stringValue();
			String lang = tb.getBinding("lang").getValue().stringValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("concept", concept);
			recordElem.setAttribute("lang", lang);
		}
		return response;
	}
	
	/**
	 * Returns a list of records resource-labelPred-label of concepts or scheme that have 
	 * a skos label without languageTag
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Response listResourcesWithNoLanguageTagSKOSLabel() {
		String q = "SELECT ?resource ?labelPred ?label ?type WHERE {\n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . }\n"
				+ "UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPT_SCHEME + "> . }\n"
				+ "?resource a ?type \n"
				+ "{ bind(<" + SKOS.PREF_LABEL + "> as ?labelPred)}\n"
				+ "UNION\n"
				+ "{bind(<" + SKOS.ALT_LABEL + "> as ?labelPred)}\n"
				+ "?resource ?labelPred ?label .\n"
				+ "FILTER (lang(?label) = '') }";
		logger.info("query [listResourcesWithNoLanguageTagSKOSLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			IRI resource = (IRI) tb.getValue("resource");
			String type = tb.getBinding("type").getValue().stringValue();
			RDFResourceRole role = RDFResourceRole.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRole.concept;
			} else if (type.equals(SKOS.CONCEPT_SCHEME)) {
				role = RDFResourceRole.conceptScheme;
			}
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			
			Element resourceElem = XMLHelp.newElement(recordElem, "resource");
			addResourceToElement(resourceElem, resource, role, resource.stringValue());
			
			Element predicateElem = XMLHelp.newElement(recordElem, "predicate");
			IRI labelPred = (IRI) tb.getValue("labelPred");
			addResourceToElement(predicateElem, labelPred, RDFResourceRole.annotationProperty, 
					TurtleHelp.toQname(labelPred, ns2PrefixMapping(conn)));
			
			Element objectElem = XMLHelp.newElement(recordElem, "object");
			Literal label = (Literal) tb.getValue("label");
			addLiteralToElement(objectElem, label);
		}
		return response;
	}
	
	/**
	 * Returns a list of records resource-labelPred-xlabel-literal of concepts or schemes that
	 * have a skosxl:Label without languageTag
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Response listResourcesWithNoLanguageTagSKOSXLLabel() {
		String q = "SELECT ?resource ?labelPred ?xlabel ?literalForm ?type WHERE {\n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . }\n"
				+ "UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPT_SCHEME + "> . }\n"
				+ "?resource a ?type . \n"
				+ "?xlabel a <" + SKOSXL.LABEL + "> .\n"
				+ "?resource ?labelPred ?xlabel .\n"
				+ "?xlabel <" + SKOSXL.LITERAL_FORM + "> ?literalForm .\n"
				+ "FILTER (lang(?literalForm)= '') }";
		logger.info("query [listConceptsWithNoLanguageTagSKOSXLLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			IRI resource = (IRI) tb.getValue("resource");
			String type = tb.getBinding("type").getValue().stringValue();
			RDFResourceRole role = RDFResourceRole.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRole.concept;
			} else if (type.equals(SKOS.CONCEPT_SCHEME)) {
				role = RDFResourceRole.conceptScheme;
			}
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			
			Element resourceElem = XMLHelp.newElement(recordElem, "resource");
			addResourceToElement(resourceElem, resource, role, resource.stringValue());
			
			Element predicateElem = XMLHelp.newElement(recordElem, "predicate");
			IRI labelPred = (IRI) tb.getValue("labelPred");
			addResourceToElement(predicateElem, labelPred, RDFResourceRole.objectProperty,
					TurtleHelp.toQname(labelPred, ns2PrefixMapping(conn)) );
			
			Element objectElem = XMLHelp.newElement(recordElem, "object");
			Resource label = (Resource) tb.getValue("xlabel");
			Literal literalForm = (Literal) tb.getValue("literalForm");
			addResourceToElement(objectElem, label, RDFResourceRole.xLabel,  literalForm.getLabel());
		}
		return response;
		
	}
	
	/**
	 * Returns a list of records resource-label-lang. A record like that means that the concept ?concept has 
	 * the same skos:prefLabel and skos:altLabel ?label in language ?lang
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Response listResourcesWithOverlappedSKOSLabel()  {
		String q = "SELECT ?resource ?label ?lang ?type WHERE {\n"
				+ "?resource a ?type .\n"
				+ "?resource <" + SKOS.PREF_LABEL + "> ?label .\n"
				+ "?resource <" + SKOS.ALT_LABEL + "> ?label .\n"
				+ "bind(lang(?label) as ?lang) . }";
		logger.info("query [listResourcesWithOverlappedSKOSLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			IRI resource = (IRI) tb.getValue("resource");
			String label = tb.getValue("label").stringValue();
			String lang = tb.getValue("lang").stringValue();
			String type = tb.getValue("type").stringValue();
			RDFResourceRole role = RDFResourceRole.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRole.concept;
			} else if (type.equals(SKOS.CONCEPT_SCHEME)) {
				role = RDFResourceRole.conceptScheme;
			}
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			addResourceToElement(recordElem,resource, role, resource.stringValue());
			
			addLiteralToElement(recordElem, conn.getValueFactory().createLiteral(label, lang));
		}
		return response;
	}
	
	/**
	 * Returns a list of records concept-label-lang. A record like that means that the concept ?concept has 
	 * the same skosxl:prefLabel and skosxl:altLabel ?label in language ?lang
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Response listResourcesWithOverlappedSKOSXLLabel()  {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?resource ?type ?prefLabel ?altLabel ?literalForm ?lang WHERE {\n"
				+ "?resource a ?type .\n"
				+ "?resource <" + SKOSXL.PREF_LABEL + "> ?prefLabel .\n"
				+ "?resource <" + SKOSXL.ALT_LABEL + "> ?altLabel .\n"
				+ "?prefLabel <" + SKOSXL.LITERAL_FORM + "> ?literalForm .\n"
				+ "?altLabel <" + SKOSXL.LITERAL_FORM + "> ?literalForm .\n"
				+ "bind(lang(?literalForm) as ?lang) . }";
		logger.info("query [listResourcesWithOverlappedSKOSXLLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			IRI resource = (IRI) tb.getValue("resource");
			IRI type = (IRI) tb.getValue("type");
			Resource prefLabel = (Resource) tb.getValue("prefLabel");
			Resource altLabel = (Resource) tb.getValue("altLabel");
			String literalForm = tb.getValue("literalForm").stringValue();
			String lang = tb.getValue("lang").stringValue();
			
			RDFResourceRole role = RDFResourceRole.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRole.concept;
			} else if (type.equals(SKOS.CONCEPT_SCHEME)) {
				role = RDFResourceRole.conceptScheme;
			} else if (type.equals(SKOS.COLLECTION)) {
				role = RDFResourceRole.skosCollection;
			} else if (type.equals(SKOS.ORDERED_COLLECTION)) {
				role = RDFResourceRole.skosOrderedCollection;
			}
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			addResourceToElement(recordElem, resource, role, resource.stringValue());

			
			Element prefLabelElem = XMLHelp.newElement(recordElem, "prefLabel");
			Element resPrefLabelElem = addResourceToElement(prefLabelElem, prefLabel, RDFResourceRole.xLabel, literalForm);
			resPrefLabelElem.setAttribute("lang", lang);
			
			Element altLabelElem = XMLHelp.newElement(recordElem, "altLabel");
			Element resAltLabelElem = addResourceToElement(altLabelElem, altLabel, RDFResourceRole.xLabel, literalForm);
			resAltLabelElem.setAttribute("lang", lang);
		}
		return response;
	}
	
	/**
	 * Returns a list of records concept-labelPred-label-lang. A record like that means that
	 * that the concept ?concept has the skos label ?label in language ?lang for the predicates ?labelPred that
	 * contains some extra whitespace (at the begin, at the end or multiple whitespace between two words)
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Response listConceptsWithExtraWhitespaceInSKOSLabel() {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept ?labelPred ?label ?lang WHERE {\n"
				+ "{ bind(<" + SKOS.PREF_LABEL + "> as ?labelPred)}\n"
				+ "UNION\n"
				+ "{bind(<" + SKOS.ALT_LABEL + "> as ?labelPred)}\n"
				+ "?concept ?labelPred ?skoslabel .\n"
				+ "bind(str(?skoslabel) as ?label)\n"
				+ "FILTER (regex (?label, '^ +') || regex (?label, ' +$') || regex(?label, ' {2,}?'))\n"
				+ "bind(lang(?skoslabel) as ?lang) }";
		logger.info("query [listConceptsWithExtraWhitespaceInSKOSLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String concept = tb.getBinding("concept").getValue().stringValue();
			String labelPred = tb.getBinding("labelPred").getValue().stringValue();
			String label = tb.getBinding("label").getValue().stringValue();
			String lang = tb.getBinding("lang").getValue().stringValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("concept", concept);
			recordElem.setAttribute("labelPred", labelPred);
			recordElem.setAttribute("label", label);
			recordElem.setAttribute("lang", lang);
		}
		return response;
	}
	
	/**
	 * Returns a list of records concept-labelPred-label-lang. A record like that means that
	 * that the concept ?concept has the skosxl label ?label in language ?lang for the predicates ?labelPred that
	 * contains some extra whitespace (at the begin, at the end or multiple whitespace between two words)
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Response listConceptsWithExtraWhitespaceInSKOSXLLabel() {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept ?labelPred ?label ?lang WHERE {\n"
				+ "{ bind(<" + SKOSXL.PREF_LABEL + "> as ?labelPred)}\n"
				+ "UNION\n"
				+ "{bind(<" + SKOSXL.ALT_LABEL + "> as ?labelPred)}\n"
				+ "?concept ?labelPred ?xlabel .\n"
				+ "?xlabel <" + SKOSXL.LITERAL_FORM + "> ?litForm .\n"
				+ "bind(str(?litForm) as ?label)\n"
				+ "FILTER (regex (?label, '^ +') || regex (?label, ' +$') || regex(?label, ' {2,}?'))\n"
				+ "bind(lang(?litForm) as ?lang) }";
		logger.info("query [listConceptsWithExtraWhitespaceInSKOSXLLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String concept = tb.getBinding("concept").getValue().stringValue();
			String labelPred = tb.getBinding("labelPred").getValue().stringValue();
			String label = tb.getBinding("label").getValue().stringValue();
			String lang = tb.getBinding("lang").getValue().stringValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("concept", concept);
			recordElem.setAttribute("labelPred", labelPred);
			recordElem.setAttribute("label", label);
			recordElem.setAttribute("lang", lang);
		}
		return response;
	}
	
	/**
	 * Returns a list of dangling skosxl:Label, namely the skosxl:Label not linked with any concept
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(xLabel)', 'R')")
	public Response listDanglingXLabels() {
		String q = "SELECT ?xlabel WHERE {\n"
				+ "?xlabel a <" + SKOSXL.LABEL + "> .\n"
				+ "FILTER NOT EXISTS {\n" 
				+ "?concept <" + SKOSXL.PREF_LABEL + "> | <" + SKOSXL.ALT_LABEL + "> | <" + SKOSXL.HIDDEN_LABEL + "> ?xlabel.\n"
				+ "} }";
		logger.info("query [listDanglingXLabels]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element collectionElement = XMLHelp.newElement(dataElement, "collection");
		while (tupleQueryResult.hasNext()) {
			BindingSet tb = tupleQueryResult.next();
			Resource xlabelNode = (Resource) tb.getValue("xlabel");
			addResourceToElement(collectionElement, xlabelNode, RDFResourceRole.xLabel, xlabelNode.stringValue());
		}
		return response;
	}
	
	//-----GENERICS-----
	
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Response listResourcesURIWithSpace(@Optional (defaultValue="0") Integer limit)  {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element collectionElem = XMLHelp.newElement(dataElement, "collection");
		RepositoryConnection conn = getManagedConnection();
		//nested query
		String q = "SELECT ?resource ?count WHERE { \n"+
				//this counts records
				"{ SELECT (COUNT(DISTINCT ?resource) AS ?count) WHERE { \n"+
				"{?resource ?p1 ?o1} \n"+
				"UNION \n"+
				"{?s1 ?p2 ?resource} \n"+
				"UNION \n"+
				"{?s2 ?resource ?o2} \n"+
				"bind(str(?resource) as ?uri) \n"+
				"FILTER (regex(?uri, ' +?')) \n"+ //uri has 1+ space
				"FILTER (isURI(?resource)) \n} \n}"+
				//this retrieves data
				"{ SELECT DISTINCT ?resource WHERE { \n"+
				"{?resource ?p1 ?o1} \n"+
				"UNION \n"+
				"{?s1 ?p2 ?resource} \n"+
				"UNION \n"+
				"{?s2 ?resource ?o2} \n"+
				"bind(str(?resource) as ?uri) \n"+
				"FILTER (regex(?uri, ' +?')) \n"+ //uri has 1+ space
				"FILTER (isURI(?resource)) \n}";
		if (limit > 0)
				q = q + "\nLIMIT " + limit; //for client-side performance issue
		q = q + "\n} \n}";
		logger.info("query [listResourcesURIWithSpace]:\n" + q);
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		collectionElem.setAttribute("count", "0");//default (if query return result, in the next while it's override
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String count = tb.getBinding("count").getValue().stringValue();
			collectionElem.setAttribute("count", count);
			String resource = tb.getBinding("resource").getValue().stringValue();
			XMLHelp.newElement(collectionElem, "resource", resource);
		}
		return response;
	}
	
	//########### QUICK FIXES #################
	
	/**
	 * Quick fix for dangling concepts. Set all dangling concepts as topConceptOf the given scheme
	 * @param scheme
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'C')")
	public Response setAllDanglingAsTopConcept(IRI scheme) {
		String q = "INSERT {\n"
				+ "GRAPH <" + getWorkingGraph().stringValue() + ">\n"
				+ "{ ?concept <" + SKOS.TOP_CONCEPT_OF + "> <" + scheme.stringValue() + "> }\n"
				+ "} WHERE {\n"
				+ "BIND(<" + scheme.stringValue() + "> as ?scheme)\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.TOP_CONCEPT_OF + "> ?scheme}\n"
				+ "FILTER NOT EXISTS {?scheme <" + SKOS.HAS_TOP_CONCEPT + "> ?concept }\n"
				+ "{ ?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1 . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> ?broaderConcept1 .\n"
				+ "FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.IN_SCHEME + "> ?scheme  . }\n"
				+ "} {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "?broaderConcept2 <" + SKOS.NARROWER + "> ?concept .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.IN_SCHEME + "> ?scheme . }\n"
				+ "} }";
		logger.info("query [setAllDanglingAsTopConcept]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Quick fix for dangling concepts. Set the given broader for all dangling concepts in the given scheme 
	 * @param scheme
	 * @param broader
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'C')")
	public Response setBroaderForAllDangling(IRI scheme, IRI broader) {
		String q = "INSERT {\n"
				+ "GRAPH <" + getWorkingGraph().stringValue() + ">\n"
				+ "{ ?concept <" + SKOS.BROADER + "> <" + broader.stringValue() + "> }\n"
				+ "} WHERE {\n"
				+ "BIND(<" + scheme.stringValue() + "> as ?scheme)\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.TOP_CONCEPT_OF + "> ?scheme}\n"
				+ "FILTER NOT EXISTS {?scheme <" + SKOS.HAS_TOP_CONCEPT + "> ?concept }\n"
				+ "{ ?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1 . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> ?broaderConcept1 .\n"
				+ "FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.IN_SCHEME + "> ?scheme  . }\n"
				+ "} {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "?broaderConcept2 <" + SKOS.NARROWER + "> ?concept .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.IN_SCHEME + "> ?scheme . }\n"
				+ "} }";
		logger.info("query [setBroaderForAllDangling]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Quick fix for dangling concepts. Removes all dangling concepts from the given scheme
	 * @param scheme
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept, schemes)', 'D')")
	public Response removeAllDanglingFromScheme(IRI scheme) {
		String q = "DELETE { ?concept <" + SKOS.IN_SCHEME + "> <" + scheme.stringValue() + "> }\n"
				+ "WHERE {\n"
				+ "BIND(<" + scheme.stringValue() + "> as ?scheme)\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.TOP_CONCEPT_OF + "> ?scheme}\n"
				+ "FILTER NOT EXISTS {?scheme <" + SKOS.HAS_TOP_CONCEPT + "> ?concept }\n"
				+ "{ ?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1 . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> ?broaderConcept1 .\n"
				+ "FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.IN_SCHEME + "> ?scheme  . }\n"
				+ "} {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "?broaderConcept2 <" + SKOS.NARROWER + "> ?concept .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.IN_SCHEME + "> ?scheme . }\n"
				+ "}\n}";
		logger.info("query [removeAllDanglingFromScheme]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Quick fix for dangling concepts. Delete all the dangling concepts of the given scheme
	 * @param scheme
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'D')")
	public Response deleteAllDanglingConcepts(IRI scheme) {
		String q = "DELETE { "
				+ "?concept ?p1 ?o .\n"
				+ "?s ?p2 ?concept \n"
				+ "} WHERE {\n"
				+ "BIND(<" + scheme.stringValue() + "> as ?scheme)\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.TOP_CONCEPT_OF + "> ?scheme}\n"
				+ "FILTER NOT EXISTS {?scheme <" + SKOS.HAS_TOP_CONCEPT + "> ?concept }\n"
				+ "OPTIONAL { ?concept ?p1 ?o . }\n"
				+ "OPTIONAL { ?s ?p2 ?concept . }\n"
				+ "{ ?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1 . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> ?broaderConcept1 .\n"
				+ "FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.IN_SCHEME + "> ?scheme  . }\n"
				+ "} {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.IN_SCHEME + "> ?scheme .\n"
				+ "?broaderConcept2 <" + SKOS.NARROWER + "> ?concept .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.IN_SCHEME + "> ?scheme . }\n"
				+ "}\n}";
		logger.info("query [deleteAllDanglingConcepts]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Quick fix for concepts in no scheme. Add all concepts without scheme to the given scheme
	 * @param scheme
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept, scheme)', 'C')")
	public Response addAllConceptsToScheme(IRI scheme) {
		String q = "INSERT {\n"
				+ "GRAPH <" + getWorkingGraph().stringValue() + ">\n"
				+ "{ ?concept <" + SKOS.IN_SCHEME + "> <" + scheme.stringValue() + "> }\n"
				+ "} WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "FILTER NOT EXISTS { ?concept <" + SKOS.IN_SCHEME + "> ?scheme . } }";
		logger.info("query [addAllConceptsToScheme]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Fix for topConcept with broader. Remove all the broader relation in the given scheme of the given concept.
	 * @param concept
	 * @param scheme
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'D')")
	public Response removeBroadersToConcept(IRI concept, IRI scheme) {
		String q = "DELETE {\n"
				+ "?concept <" + SKOS.BROADER + "> ?broader .\n"
				+ "?broader <" + SKOS.NARROWER + "> ?concept .\n"
				+ "} WHERE {\n"
				+ "BIND (<" + concept.stringValue() + "> as ?concept) \n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?broader <" + SKOS.IN_SCHEME + "> ?scheme . }";
		logger.info("query [removeBroadersToConcept]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Quick fix for topConcept with broader. Remove all the broader (or narrower) relation in the 
	 * of top concepts with broader (in the same scheme).
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'D')")
	public Response removeBroadersToAllConcepts() {
		String q = "DELETE {\n"
				+ "?concept <" + SKOS.BROADER + "> ?broader .\n"
				+ "?broader <" + SKOS.NARROWER + "> ?concept .\n"
				+ "} WHERE {\n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?broader <" + SKOS.IN_SCHEME + "> ?scheme . }";
		logger.info("query [removeBroadersToAllConcepts]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Quick fix for topConcept with broader. Remove as topConceptOf all the topConcept with broader.
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'D')")
	public Response removeAllAsTopConceptsWithBroader() {
		String q = "DELETE {\n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> ?scheme .\n"
				+ "?scheme <" + SKOS.HAS_TOP_CONCEPT + "> ?concept .\n"
				+ "} WHERE {\n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + "> ?scheme .\n"
   				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
   				+ "?broader <" + SKOS.IN_SCHEME + "> ?scheme . }";
		logger.info("query [removeAllAsTopConceptsWithBroader]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Quick fix for hierarchical redundancy. Remove narrower/broader redundant relations.
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'D')")
	public Response removeAllHierarchicalRedundancy() {
		String q = "DELETE {\n"
				+ "?narrower <" + SKOS.BROADER + "> ?broader .\n"
				+ "?broader <" + SKOS.NARROWER + "> ?narrower .\n"
				+ "} WHERE {\n"
				+ "?narrower <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?narrower (<" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + ">)+ ?middle .\n"
				+ "?middle <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "FILTER(?narrower != ?middle) }";
		logger.info("query [removeAllHierarchicalRedundancy]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Quick fix for dangling xLabel. Deletes all triples that involve the dangling xLabel(s)
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(xLabel)', 'D')")
	public Response deleteAllDanglingXLabel() {
		String q = "DELETE {\n"
				+ "?s ?p1 ?xlabel .\n"
				+ "?xlabel ?p2 ?o .\n"
				+ "} WHERE {\n"
				+ "?xlabel a <" + SKOSXL.LABEL + "> .\n"
				+ "OPTIONAL { ?s ?p1 ?xlabel . }\n"
				+ "OPTIONAL { ?xlabel ?p2 ?o . }\n"
				+ "FILTER NOT EXISTS {\n"
				+ "?concept <" + SKOSXL.PREF_LABEL + "> | <" + SKOSXL.ALT_LABEL + "> | <" + SKOSXL.HIDDEN_LABEL + "> ?xlabel.\n"
				+ "} }";
		logger.info("query [deleteAllDanglingXLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Fix for dangling xLabel. Links the dangling xLabel to the given concept through the given predicate 
	 * @param concept
	 * @param xlabelPred
	 * @param xlabel
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(resource, lexicalization)', 'C')")
	public Response setDanglingXLabel(IRI concept, IRI xlabelPred, Resource xlabel) {
		String q = "";
		if (xlabelPred.equals(SKOSXL.PREF_LABEL)) {
			q = "DELETE {\n"
					+ "<" + concept.stringValue() + "> <" + SKOSXL.PREF_LABEL + "> ?oldPrefLabel\n"
					+ "} INSERT {\n"
					+ "GRAPH <" + getWorkingGraph() + "> {\n"
					+ "<" + concept.stringValue() + "> <" + SKOSXL.ALT_LABEL + "> ?oldPrefLabel .\n"
					+ "<" + concept.stringValue() + "> <" + SKOSXL.PREF_LABEL + "> <" + xlabel.stringValue() + "> . }\n"
					+ "} WHERE {\nOPTIONAL {\n"
					+ "<" + concept.stringValue() + "> <" + SKOSXL.PREF_LABEL + "> ?oldPrefLabel \n"
					+ "} }";
		} else { //altLabel or hiddenLabel
			q = "INSERT DATA {\n"
					+ "GRAPH <" + getWorkingGraph() + "> {\n"
					+ "<" + concept.stringValue() + "> <" + xlabelPred.stringValue() + "> <" + xlabel.stringValue() + "> \n"
					+ "}\n}";
		}
		logger.info("query [setDanglingXLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
		return createReplyResponse(RepliesStatus.ok);
	}

	
	private Element addResourceToElement(Element parent, Resource resource, RDFResourceRole role, String show){
		Element nodeElement;
		if(resource instanceof IRI){
			nodeElement = XMLHelp.newElement(parent, "uri");
		} else { // (node.isBlank())
			nodeElement = XMLHelp.newElement(parent, "bnode");
		}
		nodeElement.setTextContent(resource.stringValue());
		if (role != null)
			nodeElement.setAttribute("role", role.toString());
		if (show != null)
			nodeElement.setAttribute("show", show);
		//explicit is set to true
		nodeElement.setAttribute("explicit", Boolean.toString(true));
		
		//OLD
		//serializeMap(nodeElement, node);
		
		return nodeElement;
	}
	
	private Element addLiteralToElement(Element parent, Literal literal){
		Element nodeElement;
		if(literal.getLanguage().isPresent()){
			nodeElement = XMLHelp.newElement(parent, "plainLiteral");
			nodeElement.setAttribute("lang", literal.getLanguage().get());
		} else if(literal.getDatatype()==null){
			nodeElement = XMLHelp.newElement(parent, "plainLiteral");
		} else{
			nodeElement = XMLHelp.newElement(parent, "typedLiteral");
			nodeElement.setAttribute("type", literal.getDatatype().stringValue());
		}
		nodeElement.setTextContent(literal.stringValue());
		//explicit is set to true
		nodeElement.setAttribute("explicit", Boolean.toString(true));
		
		//OLD
		//serializeMap(nodeElement, node);

		return nodeElement;
	}
	
	private static Map<String, String> ns2PrefixMapping(RepositoryConnection conn){
		return QueryResults.stream(conn.getNamespaces()).collect(
				toMap(Namespace::getName, Namespace::getPrefix, (v1, v2) -> v1 != null ? v1 : v2));
	}
	
}
