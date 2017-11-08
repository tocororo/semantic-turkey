package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedLexicalizationModelException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;


@STService
public class ICV extends STServiceAdapter {
	
	protected static Logger logger = LoggerFactory.getLogger(ICV.class);
	
	//-----ICV ON CONCEPTS STRUCTURE-----
	
	/**
	 * Returns a list of records <concept>, where concept is a dangling skos:Concept in the given
	 * skos:ConceptScheme
	 * @param scheme scheme where the concepts are dangling
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Collection<AnnotatedValue<Resource>> listDanglingConcepts(IRI scheme)  {
		String q = "SELECT ?resource WHERE { \n"
				+ "BIND(" + NTriplesUtil.toNTriplesString(scheme) + " as ?scheme) \n"
				+ "?resource a " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT) + ". \n"
				+ "?resource " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?resource " + NTriplesUtil.toNTriplesString(SKOS.TOP_CONCEPT_OF) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.HAS_TOP_CONCEPT) + "  ?scheme \n"
				+ "} \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?resource " + NTriplesUtil.toNTriplesString(SKOS.BROADER) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.NARROWER) + "  ?broader . \n"
				+ "?broader " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "} \n } GROUP BY ?resource";
		logger.debug("query [listDanglingConcepts]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
//	/**
//	 * Detects cyclic hierarchical relations. Returns a list of records top, n1, n2 where 
//	 * top is likely the cause of the cycle, n1 and n2 are vertex that belong to the cycle
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
//	public Response listCyclicConcepts()  {
//		RepositoryConnection conn = getManagedConnection();
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT DISTINCT ?top ?n1 ?n2 WHERE{\n"
//				+ "{?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?n1 .\n"
//				+ "?n1 (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">) ?n2 .\n"
//				+ "?n2 (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?top .\n"
//				+ "}UNION{\n"
//				+ "?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">) ?n1 .\n"
//				+ "?n1 (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">) ?top .\n"
//				+ "bind(?top as ?n2)\n"
//				+ "} {\n" 
//				+ "?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?cyclicConcept .\n"
//				+ "?cyclicConcept (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?top .\n"
//				+ "?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">) ?broader .\n"
//				+ "FILTER NOT EXISTS {\n"
//				+ "?broader (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?top }\n"
//				+ "} UNION {\n"
//				+ "?top (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?cyclicConcept .\n"
//				+ "?cyclicConcept (<" + SKOS.BROADER + "> | ^ <" + SKOS.NARROWER + ">)+ ?top .\n"
//				+ "?top (<" + SKOS.TOP_CONCEPT_OF + "> | ^ <" + SKOS.HAS_TOP_CONCEPT + ">)+ ?scheme .} }";
//		logger.debug("query [listCyclicConcepts]:\n" + q);
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			String topCyclicConcept = tb.getBinding("top").getValue().stringValue();
//			String node1 = tb.getBinding("n1").getValue().stringValue();
//			String node2 = tb.getBinding("n2").getValue().stringValue();
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			recordElem.setAttribute("topCyclicConcept", topCyclicConcept);
//			recordElem.setAttribute("node1", node1);
//			recordElem.setAttribute("node2", node2);
//		}
//		return response;
//	}
	
	/**
	 * Returns a list of skos:ConceptScheme that have no top concept
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(conceptScheme)', 'R')")
	public Collection<AnnotatedValue<Resource>> listConceptSchemesWithNoTopConcept() {
		String q = "SELECT ?resource WHERE {\n"
				+ "?resource a <" + SKOS.CONCEPT_SCHEME + "> .\n"
				+ "FILTER NOT EXISTS { {\n"
				+ "?resource <" + SKOS.HAS_TOP_CONCEPT + "> ?topConcept .\n"
				+ "} UNION {\n"
				+ "?topConcept <" + SKOS.TOP_CONCEPT_OF + "> ?resource . } } }\n"
				+ "GROUP BY ?resource";
		logger.debug("query [listConceptSchemesWithNoTopConcept]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Returns a list of skos:Concept that don't belong to any scheme 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Collection<AnnotatedValue<Resource>> listConceptsWithNoScheme(){
		String q = "SELECT ?resource WHERE { \n"
				+ "?resource a <" + SKOS.CONCEPT + "> .\n"
				+ "FILTER NOT EXISTS { ?resource <" + SKOS.IN_SCHEME + "> ?scheme . } }\n"
				+ "GROUP BY ?resource";
		logger.debug("query [listConceptsWithNoScheme]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Returns a list of skos:Concept that are topConcept but have a broader 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public JsonNode listTopConceptsWithBroader(){
		String q = "SELECT DISTINCT ?concept ?scheme WHERE {\n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?broader <" + SKOS.IN_SCHEME + "> | <" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + "> ?scheme . }";
		logger.debug("query [listTopConceptsWithBroader]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		TupleQuery query = conn.prepareTupleQuery(q);
		query.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = query.evaluate();
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode recordArrayNode = jsonFactory.arrayNode();
		while (tupleQueryResult.hasNext()){
			BindingSet tb = tupleQueryResult.next();
			String concept = tb.getBinding("concept").getValue().stringValue();
			String scheme = tb.getBinding("scheme").getValue().stringValue();
			ObjectNode recordNode = jsonFactory.objectNode();
			recordNode.set("concept", jsonFactory.textNode(concept));
			recordNode.set("scheme", jsonFactory.textNode(scheme));
			recordArrayNode.add(recordNode);
		}
		return recordArrayNode;
	}
	
//	/**
//	 * Returns a list of skos:Concept that have redundant hierarchical relations
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
//	public Response listHierarchicallyRedundantConcepts(){
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT DISTINCT ?narrower ?broader WHERE{\n"
//				+ "?narrower <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
//				+ "?narrower (<" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + ">)+ ?middle .\n"
//				+ "?middle <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
//				+ "FILTER(?narrower != ?middle)\n}";
//		logger.debug("query [listHierarchicallyRedundantConcepts]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			String narrower = tb.getBinding("narrower").getValue().stringValue();
//			String broader = tb.getBinding("broader").getValue().stringValue();
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			recordElem.setAttribute("broader", broader);
//			recordElem.setAttribute("narrower", narrower);
//		}
//		return response;
//	}
//	
//	//-----ICV ON LABELS-----
//	
//	/**
//	 * Returns a list of records concept1-concept2-label-lang, of concepts that have the same skos:prefLabel
//	 * in the same language
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
//	public Response listConceptsWithSameSKOSPrefLabel() {
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT ?concept1 ?concept2 ?label ?lang WHERE {\n"
//				+ "?concept1 a <" + SKOS.CONCEPT + "> .\n"
//				+ "?concept2 a <" + SKOS.CONCEPT + "> .\n"
//				+ "?concept1 <" + SKOS.PREF_LABEL + "> ?label .\n"
//				+ "?concept2 <" + SKOS.PREF_LABEL + "> ?label .\n"
//				+ "bind(lang(?label) as ?lang)\n"
//				+ "FILTER (str(?concept1) < str(?concept2)) }";
//		logger.debug("query [listConceptsWithSameSKOSPrefLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			String concept1 = tb.getBinding("concept1").getValue().stringValue();
//			String concept2 = tb.getBinding("concept2").getValue().stringValue();
//			String label = tb.getBinding("label").getValue().stringValue();
//			String lang = tb.getBinding("lang").getValue().stringValue();
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			recordElem.setAttribute("concept1", concept1);
//			recordElem.setAttribute("concept2", concept2);
//			recordElem.setAttribute("label", label);
//			recordElem.setAttribute("lang", lang);
//		}
//		return response;
//	}
//	
//	/**
//	 * Returns a list of records concept1-concept2-label-lang, of concepts that have the same skosxl:prefLabel
//	 * in the same language
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
//	public Response listConceptsWithSameSKOSXLPrefLabel() {
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT ?concept1 ?concept2 ?label1 ?lang WHERE {\n"
//				+ "?concept1 a <" + SKOS.CONCEPT + "> .\n"
//				+ "?concept2 a <" + SKOS.CONCEPT + "> .\n"
//				+ "?concept1 <" + SKOSXL.PREF_LABEL + "> ?xlabel1 .\n"
//				+ "?concept2 <" + SKOSXL.PREF_LABEL + "> ?xlabel2 .\n"
//				+ "?xlabel1 <" + SKOSXL.LITERAL_FORM + "> ?label1 .\n"
//				+ "?xlabel2 <" + SKOSXL.LITERAL_FORM + "> ?label2 .\n"
//				+ "FILTER (?label1 = ?label2)\n"
//				+ "FILTER (str(?concept1) < str(?concept2))\n"
//				+ "bind(lang(?label1) as ?lang) }";
//		logger.debug("query [listConceptsWithSameSKOSXLPrefLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			String concept1 = tb.getBinding("concept1").getValue().stringValue();
//			String concept2 = tb.getBinding("concept2").getValue().stringValue();
//			String label = tb.getBinding("label1").getValue().stringValue();
//			String lang = tb.getBinding("lang").getValue().stringValue();
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			recordElem.setAttribute("concept1", concept1);
//			recordElem.setAttribute("concept2", concept2);
//			recordElem.setAttribute("label", label);
//			recordElem.setAttribute("lang", lang);
//		}
//		return response;
//	}
//	
	
	/**
	 * Returns a list of concepts or schemes that have no skos:prefLabel
	 * @return
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws ModelAccessException 
	 * @throws UnsupportedQueryLanguageException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithNoSKOSPrefLabel() {
		String q = "SELECT ?resource WHERE {\n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPT_SCHEME + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.COLLECTION + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.ORDERED_COLLECTION + "> . }\n"
				+ "FILTER NOT EXISTS {\n"
				+ "?resource <" + SKOS.PREF_LABEL + "> ?prefLabel .\n"
				+ "} } GROUP BY ?resource";
		logger.debug("query [listResourcesWithNoSKOSPrefLabel]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Returns a list of concepts/schemes/collections that have no skosxl:prefLabel
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithNoSKOSXLPrefLabel()  {
		String q = "SELECT ?resource WHERE {\n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPT_SCHEME + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.COLLECTION + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.ORDERED_COLLECTION + "> . }\n"
				+ "FILTER NOT EXISTS {\n"
				+ "?resource <" + SKOSXL.PREF_LABEL + "> ?prefLabel .\n"
				+ "} } GROUP BY ?resource";
		logger.debug("query [listResourcesWithNoSKOSXLPrefLabel]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
//	/**
//	 * Returns a list of pairs concept-lang of that concept that have more skos:prefLabel in a same language
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
//	public Response listConceptsWithMultipleSKOSPrefLabel() {
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT DISTINCT ?concept ?lang WHERE {\n"
//				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
//				+ "?concept <" + SKOS.PREF_LABEL + "> ?label1.\n"
//				+ "?concept <" + SKOS.PREF_LABEL + "> ?label2.\n"
//				+ "FILTER ( ?label1 != ?label2 && lang(?label1) = lang(?label2) )\n"
//				+ "bind(lang(?label1) as ?lang) }";
//		logger.debug("query [listConceptsWithMultipleSKOSPrefLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			String concept = tb.getBinding("concept").getValue().stringValue();
//			String lang = tb.getBinding("lang").getValue().stringValue();
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			recordElem.setAttribute("concept", concept);
//			recordElem.setAttribute("lang", lang);
//		}
//		return response;
//	}
//	
//	/**
//	 * Returns a list of records concept-lang of that concept that have more skosxl:prefLabel in a same language
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
//	public Response listConceptsWithMultipleSKOSXLPrefLabel() {
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT DISTINCT ?concept ?lang WHERE {\n"
//				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
//				+ "?concept <" + SKOSXL.PREF_LABEL + "> ?label1 .\n"
//				+ "?concept <" + SKOSXL.PREF_LABEL + "> ?label2 .\n"
//				+ "?label1 <" + SKOSXL.LITERAL_FORM + "> ?lit1 .\n"
//				+ "?label2 <" + SKOSXL.LITERAL_FORM + "> ?lit2 .\n"
//				+ "bind(lang(?lit1) as ?lang)\n"
//				+ "FILTER ( ?label1 != ?label2 && lang(?lit1) = lang(?lit2) ) }";
//		logger.debug("query [listConceptsWithMultipleSKOSXLPrefLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			String concept = tb.getBinding("concept").getValue().stringValue();
//			String lang = tb.getBinding("lang").getValue().stringValue();
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			recordElem.setAttribute("concept", concept);
//			recordElem.setAttribute("lang", lang);
//		}
//		return response;
//	}
//	
//	/**
//	 * Returns a list of records resource-labelPred-label of concepts or scheme that have 
//	 * a skos label without languageTag
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
//	public Response listResourcesWithNoLanguageTagSKOSLabel() {
//		String q = "SELECT ?resource ?labelPred ?label ?type WHERE {\n"
//				+ "{ ?resource a <" + SKOS.CONCEPT + "> . }\n"
//				+ "UNION \n"
//				+ "{ ?resource a <" + SKOS.CONCEPT_SCHEME + "> . }\n"
//				+ "?resource a ?type \n"
//				+ "{ bind(<" + SKOS.PREF_LABEL + "> as ?labelPred)}\n"
//				+ "UNION\n"
//				+ "{bind(<" + SKOS.ALT_LABEL + "> as ?labelPred)}\n"
//				+ "?resource ?labelPred ?label .\n"
//				+ "FILTER (lang(?label) = '') }";
//		logger.debug("query [listResourcesWithNoLanguageTagSKOSLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			IRI resource = (IRI) tb.getValue("resource");
//			String type = tb.getBinding("type").getValue().stringValue();
//			RDFResourceRole role = RDFResourceRole.concept;
//			if (type.equals(SKOS.CONCEPT)) {
//				role = RDFResourceRole.concept;
//			} else if (type.equals(SKOS.CONCEPT_SCHEME)) {
//				role = RDFResourceRole.conceptScheme;
//			}
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			
//			Element resourceElem = XMLHelp.newElement(recordElem, "resource");
//			addResourceToElement(resourceElem, resource, role, resource.stringValue());
//			
//			Element predicateElem = XMLHelp.newElement(recordElem, "predicate");
//			IRI labelPred = (IRI) tb.getValue("labelPred");
//			addResourceToElement(predicateElem, labelPred, RDFResourceRole.annotationProperty, 
//					TurtleHelp.toQname(labelPred, ns2PrefixMapping(conn)));
//			
//			Element objectElem = XMLHelp.newElement(recordElem, "object");
//			Literal label = (Literal) tb.getValue("label");
//			addLiteralToElement(objectElem, label);
//		}
//		return response;
//	}
//	
//	/**
//	 * Returns a list of records resource-labelPred-xlabel-literal of concepts or schemes that
//	 * have a skosxl:Label without languageTag
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
//	public Response listResourcesWithNoLanguageTagSKOSXLLabel() {
//		String q = "SELECT ?resource ?labelPred ?xlabel ?literalForm ?type WHERE {\n"
//				+ "{ ?resource a <" + SKOS.CONCEPT + "> . }\n"
//				+ "UNION \n"
//				+ "{ ?resource a <" + SKOS.CONCEPT_SCHEME + "> . }\n"
//				+ "?resource a ?type . \n"
//				+ "?xlabel a <" + SKOSXL.LABEL + "> .\n"
//				+ "?resource ?labelPred ?xlabel .\n"
//				+ "?xlabel <" + SKOSXL.LITERAL_FORM + "> ?literalForm .\n"
//				+ "FILTER (lang(?literalForm)= '') }";
//		logger.debug("query [listConceptsWithNoLanguageTagSKOSXLLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			IRI resource = (IRI) tb.getValue("resource");
//			String type = tb.getBinding("type").getValue().stringValue();
//			RDFResourceRole role = RDFResourceRole.concept;
//			if (type.equals(SKOS.CONCEPT)) {
//				role = RDFResourceRole.concept;
//			} else if (type.equals(SKOS.CONCEPT_SCHEME)) {
//				role = RDFResourceRole.conceptScheme;
//			}
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			
//			Element resourceElem = XMLHelp.newElement(recordElem, "resource");
//			addResourceToElement(resourceElem, resource, role, resource.stringValue());
//			
//			Element predicateElem = XMLHelp.newElement(recordElem, "predicate");
//			IRI labelPred = (IRI) tb.getValue("labelPred");
//			addResourceToElement(predicateElem, labelPred, RDFResourceRole.objectProperty,
//					TurtleHelp.toQname(labelPred, ns2PrefixMapping(conn)) );
//			
//			Element objectElem = XMLHelp.newElement(recordElem, "object");
//			Resource label = (Resource) tb.getValue("xlabel");
//			Literal literalForm = (Literal) tb.getValue("literalForm");
//			addResourceToElement(objectElem, label, RDFResourceRole.xLabel,  literalForm.getLabel());
//		}
//		return response;
//		
//	}
//	
//	/**
//	 * Returns a list of records resource-label-lang. A record like that means that the concept ?concept has 
//	 * the same skos:prefLabel and skos:altLabel ?label in language ?lang
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
//	public Response listResourcesWithOverlappedSKOSLabel()  {
//		String q = "SELECT ?resource ?label ?lang ?type WHERE {\n"
//				+ "?resource a ?type .\n"
//				+ "?resource <" + SKOS.PREF_LABEL + "> ?label .\n"
//				+ "?resource <" + SKOS.ALT_LABEL + "> ?label .\n"
//				+ "bind(lang(?label) as ?lang) . }";
//		logger.debug("query [listResourcesWithOverlappedSKOSLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			IRI resource = (IRI) tb.getValue("resource");
//			String label = tb.getValue("label").stringValue();
//			String lang = tb.getValue("lang").stringValue();
//			String type = tb.getValue("type").stringValue();
//			RDFResourceRole role = RDFResourceRole.concept;
//			if (type.equals(SKOS.CONCEPT)) {
//				role = RDFResourceRole.concept;
//			} else if (type.equals(SKOS.CONCEPT_SCHEME)) {
//				role = RDFResourceRole.conceptScheme;
//			}
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			addResourceToElement(recordElem,resource, role, resource.stringValue());
//			
//			addLiteralToElement(recordElem, conn.getValueFactory().createLiteral(label, lang));
//		}
//		return response;
//	}
//	
//	/**
//	 * Returns a list of records concept-label-lang. A record like that means that the concept ?concept has 
//	 * the same skosxl:prefLabel and skosxl:altLabel ?label in language ?lang
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
//	public Response listResourcesWithOverlappedSKOSXLLabel()  {
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT ?resource ?type ?prefLabel ?altLabel ?literalForm ?lang WHERE {\n"
//				+ "?resource a ?type .\n"
//				+ "?resource <" + SKOSXL.PREF_LABEL + "> ?prefLabel .\n"
//				+ "?resource <" + SKOSXL.ALT_LABEL + "> ?altLabel .\n"
//				+ "?prefLabel <" + SKOSXL.LITERAL_FORM + "> ?literalForm .\n"
//				+ "?altLabel <" + SKOSXL.LITERAL_FORM + "> ?literalForm .\n"
//				+ "bind(lang(?literalForm) as ?lang) . }";
//		logger.debug("query [listResourcesWithOverlappedSKOSXLLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			IRI resource = (IRI) tb.getValue("resource");
//			IRI type = (IRI) tb.getValue("type");
//			Resource prefLabel = (Resource) tb.getValue("prefLabel");
//			Resource altLabel = (Resource) tb.getValue("altLabel");
//			String literalForm = tb.getValue("literalForm").stringValue();
//			String lang = tb.getValue("lang").stringValue();
//			
//			RDFResourceRole role = RDFResourceRole.concept;
//			if (type.equals(SKOS.CONCEPT)) {
//				role = RDFResourceRole.concept;
//			} else if (type.equals(SKOS.CONCEPT_SCHEME)) {
//				role = RDFResourceRole.conceptScheme;
//			} else if (type.equals(SKOS.COLLECTION)) {
//				role = RDFResourceRole.skosCollection;
//			} else if (type.equals(SKOS.ORDERED_COLLECTION)) {
//				role = RDFResourceRole.skosOrderedCollection;
//			}
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			addResourceToElement(recordElem, resource, role, resource.stringValue());
//
//			
//			Element prefLabelElem = XMLHelp.newElement(recordElem, "prefLabel");
//			Element resPrefLabelElem = addResourceToElement(prefLabelElem, prefLabel, RDFResourceRole.xLabel, literalForm);
//			resPrefLabelElem.setAttribute("lang", lang);
//			
//			Element altLabelElem = XMLHelp.newElement(recordElem, "altLabel");
//			Element resAltLabelElem = addResourceToElement(altLabelElem, altLabel, RDFResourceRole.xLabel, literalForm);
//			resAltLabelElem.setAttribute("lang", lang);
//		}
//		return response;
//	}
//	
//	/**
//	 * Returns a list of records concept-labelPred-label-lang. A record like that means that
//	 * that the concept ?concept has the skos label ?label in language ?lang for the predicates ?labelPred that
//	 * contains some extra whitespace (at the begin, at the end or multiple whitespace between two words)
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
//	public Response listConceptsWithExtraWhitespaceInSKOSLabel() {
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT ?concept ?labelPred ?label ?lang WHERE {\n"
//				+ "{ bind(<" + SKOS.PREF_LABEL + "> as ?labelPred)}\n"
//				+ "UNION\n"
//				+ "{bind(<" + SKOS.ALT_LABEL + "> as ?labelPred)}\n"
//				+ "?concept ?labelPred ?skoslabel .\n"
//				+ "bind(str(?skoslabel) as ?label)\n"
//				+ "FILTER (regex (?label, '^ +') || regex (?label, ' +$') || regex(?label, ' {2,}?'))\n"
//				+ "bind(lang(?skoslabel) as ?lang) }";
//		logger.debug("query [listConceptsWithExtraWhitespaceInSKOSLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			String concept = tb.getBinding("concept").getValue().stringValue();
//			String labelPred = tb.getBinding("labelPred").getValue().stringValue();
//			String label = tb.getBinding("label").getValue().stringValue();
//			String lang = tb.getBinding("lang").getValue().stringValue();
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			recordElem.setAttribute("concept", concept);
//			recordElem.setAttribute("labelPred", labelPred);
//			recordElem.setAttribute("label", label);
//			recordElem.setAttribute("lang", lang);
//		}
//		return response;
//	}
//	
//	/**
//	 * Returns a list of records concept-labelPred-label-lang. A record like that means that
//	 * that the concept ?concept has the skosxl label ?label in language ?lang for the predicates ?labelPred that
//	 * contains some extra whitespace (at the begin, at the end or multiple whitespace between two words)
//	 * @return
//	 * @throws QueryEvaluationException
//	 * @throws UnsupportedQueryLanguageException
//	 * @throws ModelAccessException
//	 * @throws MalformedQueryException
//	 */
//	@GenerateSTServiceController
//	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
//	public Response listConceptsWithExtraWhitespaceInSKOSXLLabel() {
//		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
//		Element dataElement = response.getDataElement();
//		String q = "SELECT ?concept ?labelPred ?label ?lang WHERE {\n"
//				+ "{ bind(<" + SKOSXL.PREF_LABEL + "> as ?labelPred)}\n"
//				+ "UNION\n"
//				+ "{bind(<" + SKOSXL.ALT_LABEL + "> as ?labelPred)}\n"
//				+ "?concept ?labelPred ?xlabel .\n"
//				+ "?xlabel <" + SKOSXL.LITERAL_FORM + "> ?litForm .\n"
//				+ "bind(str(?litForm) as ?label)\n"
//				+ "FILTER (regex (?label, '^ +') || regex (?label, ' +$') || regex(?label, ' {2,}?'))\n"
//				+ "bind(lang(?litForm) as ?lang) }";
//		logger.debug("query [listConceptsWithExtraWhitespaceInSKOSXLLabel]:\n" + q);
//		RepositoryConnection conn = getManagedConnection();
//		TupleQuery query = conn.prepareTupleQuery(q);
//		query.setIncludeInferred(false);
//		TupleQueryResult tupleQueryResult = query.evaluate();
//		while (tupleQueryResult.hasNext()){
//			BindingSet tb = tupleQueryResult.next();
//			String concept = tb.getBinding("concept").getValue().stringValue();
//			String labelPred = tb.getBinding("labelPred").getValue().stringValue();
//			String label = tb.getBinding("label").getValue().stringValue();
//			String lang = tb.getBinding("lang").getValue().stringValue();
//			Element recordElem = XMLHelp.newElement(dataElement, "record");
//			recordElem.setAttribute("concept", concept);
//			recordElem.setAttribute("labelPred", labelPred);
//			recordElem.setAttribute("label", label);
//			recordElem.setAttribute("lang", lang);
//		}
//		return response;
//	}
	
	/**
	 * Returns a list of dangling skosxl:Label, namely the skosxl:Label not linked with any concept
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(xLabel)', 'R')")
	public Collection<AnnotatedValue<Resource>> listDanglingXLabels() {
		String q = "SELECT ?resource WHERE {\n"
				+ "?resource a <" + SKOSXL.LABEL + "> .\n"
				+ "FILTER NOT EXISTS {\n" 
				+ "?concept <" + SKOSXL.PREF_LABEL + "> | <" + SKOSXL.ALT_LABEL + "> | <" + SKOSXL.HIDDEN_LABEL + "> ?resource.\n"
				+ "} } GROUP BY ?resource";
		logger.debug("query [listDanglingXLabels]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
	/**
	 * Return a list of <resources> with skos:altLabel(s) (or skosxl:altLabel) but not a corresponding 
	 * skos:prefLabel (or skos:prefLabel) for the same language locale. 
	 * @param rolesArray
	 * @return
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithAltNoPrefLabel(RDFResourceRole[] rolesArray) 
			throws UnsupportedLexicalizationModelException  {
		IRI lexModel = getProject().getLexicalizationModel();
		
		if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}
		String q = "SELECT DISTINCT ?resource (GROUP_CONCAT(DISTINCT ?lang; separator=\",\") AS ?attr_missingLang)\n"
				+ "WHERE {\n";
		
		q += rolePartForQuery(rolesArray, "?resource");
		
		if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
			q+= "?resource " + NTriplesUtil.toNTriplesString(SKOSXL.ALT_LABEL) +" ?altLabel . \n" 
				+ "?altLabel "+ NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM) + " ?altTerm . \n";
		} else {
			q+= "?resource " + NTriplesUtil.toNTriplesString(SKOS.ALT_LABEL) +" ?altTerm . \n"; 
		}
			q+= "bind (lang(?altTerm) as ?lang) .\n"
				+ "FILTER NOT EXISTS { \n";
			
		if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
			q+=  "?resource " + NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL) +" ?prefLabel . \n" 
				+ "?prefLabel "+ NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM) + " ?prefTerm . \n";
		} else {
			q+=  "?resource " + NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL) +" ?prefTerm . \n"; 
		}
		q += "FILTER(lang(?prefTerm) = ?lang)"
				+ "}\n"
				+ "}\n"
				+"GROUP BY ?resource ";
				
				
		logger.debug("query [listConceptNoSkosxlPrefLang]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Return a list of <resources> with no lexicalization (rdfs:label, skos:prefLabel or skosxl:prefLabel)
	 *  in one or more input languages
	 * @param rolesArray
	 * @param languagesArray 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesNoLexicalization(RDFResourceRole[] rolesArray, 
			String[] languagesArray)  {
		
		String query = "SELECT DISTINCT ?resource (GROUP_CONCAT(DISTINCT ?lang; separator=\",\") AS ?attr_missingLang)\n"
				+ "WHERE {\n";
		
		//now look for the roles
		query+=rolePartForQuery(rolesArray, "?resource");
		
		//now add the part that, using the lexicalization model, search for resources not having a language
		IRI lexModel = getProject().getLexicalizationModel();
		boolean first = true;
		String union = "";
		for(String lang : languagesArray) {
			if(!first) {
				union = "UNION\n";
			}
			first=false;
			if(lexModel.equals(Project.RDFS_LEXICALIZATION_MODEL)) {
				query+=union+"{ \n"
						+"?resource a ?fakeType .\n" // otherwise the FILTER NOT EXISTS does not work
						+"BIND('"+lang+"' as ?lang)\n"
						+ "FILTER NOT EXISTS { \n"
						+"?resource "+NTriplesUtil.toNTriplesString(RDFS.LABEL)+" ?label .\n"
						+"FILTER(lang(?label) = ?lang)\n"
						+ "}\n}";
			} else if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL)) {
				query+=union+"{ \n"
						+"?resource a ?fakeType .\n" // otherwise the FILTER NOT EXISTS does not work
						+"BIND('"+lang+"' as ?lang)\n"
						+ "FILTER NOT EXISTS { \n"
						+"?resource "+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+" ?label .\n"
						+"FILTER(lang(?label) = ?lang)\n"
						+ "}\n}";
			} else if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
				query+=union+"{ \n"
						+"?resource a ?fakeType .\n" // otherwise the FILTER NOT EXISTS does not work
						+"BIND('"+lang+"' as ?lang)\n"
						+ "FILTER NOT EXISTS { \n"
						+"?resource "+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+" ?xlabel .\n"
						+"?xlabel "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?label .\n"
						+"FILTER(lang(?label) = ?lang)\n"
						+ "}\n}";
			} 
		}
		
		query+="}\n"
				+ "GROUP BY ?resource ";
				
		logger.debug("query [listResourcesNoLexicalization]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
	/**
	 * Return a list of <concept> mapped to each other using both skos:exactMatch and one of skos:broadMatch 
	 * or skos:relatedMatch mapping properties as the exactMatch relation is disjoint with both broadMatch 
	 * and relatedMatch
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Collection<AnnotatedValue<Resource>> listConceptsExactMatchDisjoint()  {
		String query = "SELECT DISTINCT ?resource \n"
				+ "WHERE {\n"
				+ "?resource a "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT) +" . \n"
				+ "?concept2 a "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT) +" . \n"
				+ " ?resource " +NTriplesUtil.toNTriplesString(SKOS.EXACT_MATCH) +" ?concepts .\n"
				
				+ "{?resource "+NTriplesUtil.toNTriplesString(SKOS.BROAD_MATCH) +" ?concept2 . }\n"
				+ "UNION \n"
				+ "{?resource "+NTriplesUtil.toNTriplesString(SKOS.RELATED_MATCH) +" ?concept2 . }\n"
				+ "UNION \n"
				+ "{?concept2 "+NTriplesUtil.toNTriplesString(SKOS.BROAD_MATCH) +" ?resource . }\n"
				+ "UNION \n"
				+ "{?concept2 "+NTriplesUtil.toNTriplesString(SKOS.RELATED_MATCH) +" ?resource . }\n";
				
		
		query+="}\n"
				+ "GROUP BY ?resource ";
		
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Return a list of <concept> connected to each other with both the skos:related and the 
	 * skos:broaderTransitive as the related relation is disjoint with broaderTransitive
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Collection<AnnotatedValue<Resource>> listConceptsRelatedDisjoint()  {
		String query = "SELECT DISTINCT ?resource \n"
				+ "WHERE {\n"
				+ "?resource a "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT) +" . \n"
				+ "?concept2 a "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT) +" . \n"
				+ "?resource " +NTriplesUtil.toNTriplesString(SKOS.RELATED) +" ?concepts .\n"
				
				+ "{?resource "+NTriplesUtil.toNTriplesString(SKOS.BROADER_TRANSITIVE) +" ?concept2 . }\n"
				+ "UNION \n"
				+ "{?concept2 "+NTriplesUtil.toNTriplesString(SKOS.BROADER_TRANSITIVE) +" ?resource . }\n";
				
		
		query+="}\n"
				+ "GROUP BY ?resource ";
		
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Return a list of <resources> that have more than one skosxl:prefLabel for the same language locale
	 * @param rolesArray
	 * @return
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcessWithMorePrefLabelSameLang(RDFResourceRole[] rolesArray) 
			throws UnsupportedLexicalizationModelException  {
		IRI lexModel = getProject().getLexicalizationModel();
		if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}
		
		String query = "SELECT DISTINCT ?resource (GROUP_CONCAT(DISTINCT ?lang; separator=\",\") AS ?attr_duplicateLang)\n"
				+ "WHERE {\n";
		
		query+=rolePartForQuery(rolesArray, "?resource");
		
		if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)){
			query += "?resource "+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+" ?xlabel1 .\n"
					+ "?resource "+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+" ?xlabel2 .\n"
					+ "FILTER(?xlabel1 != ?xlabel2) \n"
					+ "?xlabel1 "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?label1 .\n"
					+ "?xlabel2 "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?label2 .\n";
		} else {
			query += "?resource "+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+" ?label1 .\n"
					+ "?resource "+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+" ?label2 .\n"
					+ "FILTER(?label1 != ?label2) \n";
		}
		query += "FILTER(lang(?label1) = lang(?label2)) \n"
				+ "BIND(lang(?label1) AS ?lang) \n"
				+"}\n"
				+ "GROUP BY ?resource ";
		
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	//-----GENERICS-----
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesURIWithSpace()  {
		String q = "SELECT ?resource WHERE { \n"+
				"{?resource ?p1 ?o1} \n"+
				"UNION \n"+
				"{?s1 ?p2 ?resource} \n"+
				"UNION \n"+
				"{?s2 ?resource ?o2} \n"+
				"bind(str(?resource) as ?uri) \n"+
				"FILTER (regex(?uri, ' +?')) \n"+ //uri has 1+ space
				"FILTER (isURI(?resource)) \n } GROUP BY ?resource";
		logger.debug("query [listResourcesURIWithSpace]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	//########### QUICK FIXES #################
	
	/**
	 * Quick fix for dangling concepts. Set all dangling concepts as topConceptOf the given scheme
	 * @param scheme
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'C')")
	public void setAllDanglingAsTopConcept(IRI scheme) {
		String q = "INSERT {\n"
				+ "GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + "\n"
				+ "{ ?concept " + NTriplesUtil.toNTriplesString(SKOS.TOP_CONCEPT_OF) + " " + NTriplesUtil.toNTriplesString(scheme) + " }\n"
				+ "} WHERE {\n"
				+ "BIND(" + NTriplesUtil.toNTriplesString(scheme) + " as ?scheme) \n"
				+ "?concept a " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT) + ". \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.TOP_CONCEPT_OF) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.HAS_TOP_CONCEPT) + "  ?scheme \n"
				+ "} \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.BROADER) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.NARROWER) + "  ?broader . \n"
				+ "?broader " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "} \n}";
		logger.debug("query [setAllDanglingAsTopConcept]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for dangling concepts. Set the given broader for all dangling concepts in the given scheme 
	 * @param scheme
	 * @param broader
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'C')")
	public void setBroaderForAllDangling(IRI scheme, IRI broader) {
		String q = "INSERT {\n"
				+ "GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " \n"
				+ "{ ?concept " + NTriplesUtil.toNTriplesString(SKOS.BROADER) + " " + NTriplesUtil.toNTriplesString(broader) + " }\n"
				+ "} WHERE {\n"
				+ "BIND(" + NTriplesUtil.toNTriplesString(scheme) + " as ?scheme) \n"
				+ "?concept a " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT) + ". \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.TOP_CONCEPT_OF) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.HAS_TOP_CONCEPT) + "  ?scheme \n"
				+ "} \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.BROADER) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.NARROWER) + "  ?broader . \n"
				+ "?broader " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "} \n}";
		logger.debug("query [setBroaderForAllDangling]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for dangling concepts. Removes all dangling concepts from the given scheme
	 * @param scheme
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, schemes)', 'D')")
	public void removeAllDanglingFromScheme(IRI scheme) {
		String q = "DELETE { \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " " + NTriplesUtil.toNTriplesString(scheme) + " }\n"
				+ "WHERE {\n"
				+ "BIND(" + NTriplesUtil.toNTriplesString(scheme) + " as ?scheme) \n"
				+ "?concept a " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT) + ". \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.TOP_CONCEPT_OF) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.HAS_TOP_CONCEPT) + "  ?scheme \n"
				+ "} \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?concept " + NTriplesUtil.toNTriplesString(SKOS.BROADER) 
				+ "|^" + NTriplesUtil.toNTriplesString(SKOS.NARROWER) + "  ?broader . \n"
				+ "?broader " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " ?scheme . \n"
				+ "} \n }";
		logger.debug("query [removeAllDanglingFromScheme]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for dangling concepts. Delete all the dangling concepts of the given scheme
	 * @param scheme
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'D')")
	public void deleteAllDanglingConcepts(IRI scheme) {
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
		logger.debug("query [deleteAllDanglingConcepts]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for concepts in no scheme. Add all concepts without scheme to the given scheme
	 * @param scheme
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, scheme)', 'C')")
	public void addAllConceptsToScheme(IRI scheme) {
		String q = "INSERT {\n"
				+ "GRAPH <" + getWorkingGraph().stringValue() + ">\n"
				+ "{ ?concept <" + SKOS.IN_SCHEME + "> <" + scheme.stringValue() + "> }\n"
				+ "} WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "FILTER NOT EXISTS { ?concept <" + SKOS.IN_SCHEME + "> ?scheme . } }";
		logger.debug("query [addAllConceptsToScheme]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Fix for topConcept with broader. Remove all the broader relation in the given scheme of the given concept.
	 * @param concept
	 * @param scheme
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'D')")
	public void removeBroadersToConcept(IRI concept, IRI scheme) {
		String q = "DELETE {\n"
				+ "?concept <" + SKOS.BROADER + "> ?broader .\n"
				+ "?broader <" + SKOS.NARROWER + "> ?concept .\n"
				+ "} WHERE {\n"
				+ "BIND (<" + concept.stringValue() + "> as ?concept) \n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?broader <" + SKOS.IN_SCHEME + "> ?scheme . }";
		logger.debug("query [removeBroadersToConcept]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for topConcept with broader. Remove all the broader (or narrower) relation in the 
	 * of top concepts with broader (in the same scheme).
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'D')")
	public void removeBroadersToAllConcepts() {
		String q = "DELETE {\n"
				+ "?concept <" + SKOS.BROADER + "> ?broader .\n"
				+ "?broader <" + SKOS.NARROWER + "> ?concept .\n"
				+ "} WHERE {\n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?broader <" + SKOS.IN_SCHEME + "> ?scheme . }";
		logger.debug("query [removeBroadersToAllConcepts]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for topConcept with broader. Remove as topConceptOf all the topConcept with broader.
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'D')")
	public void removeAllAsTopConceptsWithBroader() {
		String q = "DELETE {\n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> ?scheme .\n"
				+ "?scheme <" + SKOS.HAS_TOP_CONCEPT + "> ?concept .\n"
				+ "} WHERE {\n"
				+ "?concept <" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + "> ?scheme .\n"
   				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
   				+ "?broader <" + SKOS.IN_SCHEME + "> ?scheme . }";
		logger.debug("query [removeAllAsTopConceptsWithBroader]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for hierarchical redundancy. Remove narrower/broader redundant relations.
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'D')")
	public void removeAllHierarchicalRedundancy() {
		String q = "DELETE {\n"
				+ "?narrower <" + SKOS.BROADER + "> ?broader .\n"
				+ "?broader <" + SKOS.NARROWER + "> ?narrower .\n"
				+ "} WHERE {\n"
				+ "?narrower <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?narrower (<" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + ">)+ ?middle .\n"
				+ "?middle <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "FILTER(?narrower != ?middle) }";
		logger.debug("query [removeAllHierarchicalRedundancy]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Quick fix for dangling xLabel. Deletes all triples that involve the dangling xLabel(s)
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(xLabel)', 'D')")
	public void deleteAllDanglingXLabel() {
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
		logger.debug("query [deleteAllDanglingXLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}
	
	/**
	 * Fix for dangling xLabel. Links the dangling xLabel to the given concept through the given predicate 
	 * @param concept
	 * @param xlabelPred
	 * @param xlabel
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(resource, lexicalization)', 'C')")
	public void setDanglingXLabel(IRI concept, IRI xlabelPred, Resource xlabel) {
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
		logger.debug("query [setDanglingXLabel]:\n" + q);
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(q);
		update.execute();
	}

	
//	private Element addResourceToElement(Element parent, Resource resource, RDFResourceRole role, String show){
//		Element nodeElement;
//		if(resource instanceof IRI){
//			nodeElement = XMLHelp.newElement(parent, "uri");
//		} else { // (node.isBlank())
//			nodeElement = XMLHelp.newElement(parent, "bnode");
//		}
//		nodeElement.setTextContent(resource.stringValue());
//		if (role != null)
//			nodeElement.setAttribute("role", role.toString());
//		if (show != null)
//			nodeElement.setAttribute("show", show);
//		//explicit is set to true
//		nodeElement.setAttribute("explicit", Boolean.toString(true));
//		
//		//OLD
//		//serializeMap(nodeElement, node);
//		
//		return nodeElement;
//	}
//	
//	private Element addLiteralToElement(Element parent, Literal literal){
//		Element nodeElement;
//		if(literal.getLanguage().isPresent()){
//			nodeElement = XMLHelp.newElement(parent, "plainLiteral");
//			nodeElement.setAttribute("lang", literal.getLanguage().get());
//		} else if(literal.getDatatype()==null){
//			nodeElement = XMLHelp.newElement(parent, "plainLiteral");
//		} else{
//			nodeElement = XMLHelp.newElement(parent, "typedLiteral");
//			nodeElement.setAttribute("type", literal.getDatatype().stringValue());
//		}
//		nodeElement.setTextContent(literal.stringValue());
//		//explicit is set to true
//		nodeElement.setAttribute("explicit", Boolean.toString(true));
//		
//		//OLD
//		//serializeMap(nodeElement, node);
//
//		return nodeElement;
//	}
//	
//	private static Map<String, String> ns2PrefixMapping(RepositoryConnection conn){
//		return QueryResults.stream(conn.getNamespaces()).collect(
//				toMap(Namespace::getName, Namespace::getPrefix, (v1, v2) -> v1 != null ? v1 : v2));
//	}
//	
	
	private String rolePartForQuery(RDFResourceRole[] rolesArray, String var) {
		String query = "";
		String union="";
		boolean first=true;
		query += "{SELECT "+var+" \n"
				+ "WHERE {\n";
		for(RDFResourceRole role : rolesArray) {
			if(!first) {
				union = "UNION\n";
			}
			
			if(role.equals(RDFResourceRole.concept)) {
				query+=union+"{ "+var+" a <"+SKOS.CONCEPT.stringValue()+"> . } \n";
				first=false;
			} else if(role.equals(RDFResourceRole.cls)) {
				query+=union+"{ "+var+" a ?type .  \n"
						+ "\nFILTER(?type = <"+OWL.CLASS.stringValue()+"> || "
								+ "?type = <"+RDFS.CLASS.stringValue()+"> ) } \n";
				first = false;
			} else if(role.equals(RDFResourceRole.property)) {
				query+=union+"{ "+var+" a ?type .  \n"
						+ "\nFILTER(?type = <"+RDF.PROPERTY.stringValue()+"> || "
						+ "?type = <"+OWL.OBJECTPROPERTY.stringValue()+"> || "
						+ "?type = <"+OWL.DATATYPEPROPERTY.stringValue()+"> || "
						+ "?type = <"+OWL.ANNOTATIONPROPERTY.stringValue()+"> || " 
						+ "?type = <"+OWL.ONTOLOGYPROPERTY.stringValue()+"> )"+
						"\n}";
				first = false;
			} else if(role.equals(RDFResourceRole.conceptScheme)) {
				query+=union+"{ "+var+" a <"+SKOS.CONCEPT_SCHEME.stringValue()+"> . } \n";
				first=false;
			} else if(role.equals(RDFResourceRole.conceptScheme)) {
				query+=union+"{ "+var+" a ?type .  \n"
						+ "\nFILTER(?type = <"+SKOS.COLLECTION.stringValue()+"> || "
						+ "?type = <"+SKOS.ORDERED_COLLECTION.stringValue()+"> ) }\n";
				first = false;
			} else if (role.equals(RDFResourceRole.individual)) {
				query+=union+"{ "+var+" a ?type .  \n"
						+"?type a ?classType . \n"
						+ "\nFILTER(?classType = <"+OWL.CLASS.stringValue()+"> || "
								+ "?classType = <"+RDFS.CLASS.stringValue()+"> ) } \n";
				first = false;
			}
		}
		
		query +="}\n}\n";
		return query;
	}
}
