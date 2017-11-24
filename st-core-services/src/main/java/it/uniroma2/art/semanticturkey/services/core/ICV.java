package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.RemoteResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourceLocator;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedLexicalizationModelException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.vocabulary.OWL2Fragment;


@STService
public class ICV extends STServiceAdapter {
	
	@Autowired
	private ResourceLocator resourceLocator;
	
	private ThreadLocal<Map<Project, RepositoryConnection>> projectConnectionHolder = ThreadLocal
			.withInitial(HashMap::new);
	
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
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
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
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
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
				+ " ?resource " +NTriplesUtil.toNTriplesString(SKOS.EXACT_MATCH) +" ?concept2 .\n"
				
				+ "{?resource "+NTriplesUtil.toNTriplesString(SKOS.BROAD_MATCH) +" ?concept2 . }\n"
				+ "UNION \n"
				+ "{?resource "+NTriplesUtil.toNTriplesString(SKOS.RELATED_MATCH) +" ?concept2 . }\n"
				+ "UNION \n"
				+ "{?concept2 "+NTriplesUtil.toNTriplesString(SKOS.BROAD_MATCH) +" ?resource . }\n"
				+ "UNION \n"
				+ "{?concept2 "+NTriplesUtil.toNTriplesString(SKOS.RELATED_MATCH) +" ?resource . }\n";
				
		
		query+="}\n"
				+ "GROUP BY ?resource ";
		
		logger.debug("query [listConceptsExactMatchDisjoint]:\n" + query);
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
				+ "?resource " +NTriplesUtil.toNTriplesString(SKOS.RELATED) +" ?concept2 .\n"
				
				+ "{?resource "+NTriplesUtil.toNTriplesString(SKOS.BROADER_TRANSITIVE) +" ?concept2 . }\n"
				+ "UNION \n"
				+ "{?concept2 "+NTriplesUtil.toNTriplesString(SKOS.BROADER_TRANSITIVE) +" ?resource . }\n";
				
		
		query+="}\n"
				+ "GROUP BY ?resource ";
		
		logger.debug("query [listConceptsRelatedDisjoint]:\n" + query);
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
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithMorePrefLabelSameLang(RDFResourceRole[] rolesArray) 
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
		
		logger.debug("query [listResourcesWithMorePrefLabelSameLang]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Return a list of <resources> that have a SKOS/SKOSXL label without any language tag 
	 * @param rolesArray
	 * @return
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithNoLanguageTagForLabel(RDFResourceRole[] rolesArray) 
			throws UnsupportedLexicalizationModelException  {
		IRI lexModel = getProject().getLexicalizationModel();
		/*if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}*/
		
		String query = "SELECT DISTINCT ?resource ?attr_xlabel ?attr_label \n"
				+ "WHERE {\n";
		
		query+=rolePartForQuery(rolesArray, "?resource");
		
		if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)){
			query += "?resource ("+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+"|"+
						NTriplesUtil.toNTriplesString(SKOS.ALT_LABEL)+") ?attr_xlabel .\n"
					+ "?attr_xlabel "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?attr_label .\n";
		} else if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL)) {
			query += "?resource ("+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+"|"+
						NTriplesUtil.toNTriplesString(SKOS.ALT_LABEL)+") ?attr_label .\n";
		} else {
			query += "?resource "+NTriplesUtil.toNTriplesString(RDFS.LABEL)+" ?attr_label .\n";
		}
		query += "FILTER(lang(?attr_label) = '') \n"
				+"}\n"
				+ "GROUP BY ?resource ?attr_xlabel ?attr_label";
		
		logger.debug("query [listResourcesWithNoLanguageTagForLabel]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Return a list of <resources> with extra whitespace(s) in skos(xl):label(s) annotation properties
	 * @param rolesArray
	 * @return
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithExtraSpacesInLabel(RDFResourceRole[] rolesArray) 
			throws UnsupportedLexicalizationModelException  {
		IRI lexModel = getProject().getLexicalizationModel();
		
		/*if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}*/
		
		String query = "SELECT ?resource ?attr_xlabel ?attr_label \n"
				+ "WHERE {\n";
		
		query+=rolePartForQuery(rolesArray, "?resource");
		
		if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)){
			query += "?resource ("+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+"|"+
						NTriplesUtil.toNTriplesString(SKOS.ALT_LABEL)+") ?attr_xlabel .\n"
					+ "?attr_xlabel "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?attr_label .\n";
		} else if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL) ){
			query += "?resource ("+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+"|"+
						NTriplesUtil.toNTriplesString(SKOS.ALT_LABEL)+") ?attr_label .\n";
		} else {
			query += "?resource "+NTriplesUtil.toNTriplesString(RDFS.LABEL)+" ?attr_label .\n";
		}
		query += "FILTER (regex (?attr_label, '^ +') || regex (?attr_label, ' +$') || regex(?attr_label, '  '))\n"
				+"}\n"
				+ "GROUP BY ?resource ?attr_xlabel ?attr_label";
		
		logger.debug("query [listResourcesWithExtraSpacesInLabel]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * Return a list of <resources> with overlapped lexicalization
	 * @param rolesArray
	 * @return
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesWithOverlappedLabels(RDFResourceRole[] rolesArray) 
			throws UnsupportedLexicalizationModelException  {
		IRI lexModel = getProject().getLexicalizationModel();
		
		/*if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}*/
		String query = "SELECT DISTINCT ?resource ?attr_xlabel ?attr_label \n"
				+ "WHERE {\n";
		
		query+=rolePartForQuery(rolesArray, "?resource");
		query+=rolePartForQuery(rolesArray, "?resource2");
		if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)){
			query += "?resource "+getSkosxlPrefOrAltOrHidden()+" ?attr_xlabel .\n"
					+ "?attr_xlabel "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?attr_label .\n"
					+ "?resource2 "+getSkosxlPrefOrAltOrHidden()+" ?attr_xlabel2 .\n"
					+ "?attr_xlabel2 "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?attr_label2 .\n";
		} else if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL) ){
			query += "?resource "+getSkosPrefOrAltOrHidden()+" ?attr_label .\n"
					+ "?resource2 "+getSkosPrefOrAltOrHidden()+" ?attr_label2 .\n";
		} else {
			query += "?resource "+NTriplesUtil.toNTriplesString(RDFS.LABEL)+" ?attr_label .\n"
					+ "?resource2 "+NTriplesUtil.toNTriplesString(RDFS.LABEL)+" ?attr_label2 .\n";
		}
		
		query += "FILTER(?resource != ?resource2) \n"
				+ "FILTER(?attr_label = ?attr_label2) \n"
				+"}\n"
				+ "GROUP BY ?resource ?attr_xlabel ?attr_label";
		
		logger.debug("query [listResourcesWithOverlappedLabels]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
	/**
	 * Return a list of <resources> not having the property skos:definition for the given languages
	 * @param rolesArray
	 * @param languagesArray
	 * @return
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> listResourcesNoDef(RDFResourceRole[] rolesArray,
			String[] languagesArray) {
		//IRI lexModel = getProject().getLexicalizationModel();
		
		/*if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}*/
		
		String query = "SELECT ?resource (GROUP_CONCAT(DISTINCT ?lang; separator=\",\") AS ?attr_missingLang) \n"
				+ "WHERE {\n";
		
		query+=rolePartForQuery(rolesArray, "?resource");
		
		
		boolean first = true;
		String union = "";
		for(String lang : languagesArray) {
			if(!first) {
				union = "UNION\n";
			}
			first=false;
			query+=union+"{ \n"
					+"?resource a ?fakeType .\n" // otherwise the FILTER NOT EXISTS does not work
					+"BIND('"+lang+"' as ?lang)\n"
					+ "FILTER NOT EXISTS { \n"
					+ "{ ?resource "+NTriplesUtil.toNTriplesString(SKOS.DEFINITION)+ "?definition . } \n"
					+" UNION\n"
					+ "{ ?resource "+NTriplesUtil.toNTriplesString(SKOS.DEFINITION)+ "?definitionRef . "
					+ "?definitionRef "+NTriplesUtil.toNTriplesString(RDF.VALUE)+"?definition . } \n"
					+"FILTER(lang(?definition) = ?lang)\n"
					+ "}\n}";
		}
		query += "}\n"
				+ "GROUP BY ?resource ?attr_xlabel ?attr_label";
		
		logger.debug("query [listResourcesNoDef]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
	/**
	 * Return a list of <concepts> belong to  hierarchical cyclic
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	//public Collection<AnnotatedValue<Resource>> listConceptsHierarchicalCycles() {
	public JsonNode listConceptsHierarchicalCycles() {
		//IRI lexModel = getProject().getLexicalizationModel();
		
		/*if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}*/
		
		String query = "SELECT ?resource ?attr_broader_concept \n"
				+ "WHERE {\n"
				+ "?resource a "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT)+" .\n"
				+ "?resource "+broaderOrInverseNarrower()+" ?attr_broader_concept .\n"
				+ "?attr_broader_concept "+broaderOrInverseNarrower()+"* ?resource .\n"
				+ "} \n"
				+ "GROUP BY ?resource ?attr_broader_concept";
		logger.debug("query [listConceptsHierarchicalCycles]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		//return qb.runQuery();
		
		Collection<AnnotatedValue<Resource>> annotatedValueList = qb.runQuery();
		
		//now do a post process of the results, to find the cycles
		
		//first of all, read all the result and prepare the two map containing all the relevant information
		Map<String, List<String>> conceptToBroadersConceptMap = new HashMap<>();
		Map<String, AnnotatedValue<Resource>> conceptToAnnotatedValue = new HashMap<>();
		for(AnnotatedValue<Resource>annotatedValue : annotatedValueList) {
			String concept = annotatedValue.getStringValue();
			//add the AnnotatedValue to the map
			conceptToAnnotatedValue.put(concept, annotatedValue);
			//add the concept and the broader to the map
			String broaderConcept = annotatedValue.getAttributes().get("broader_concept").stringValue();
			//remove the attribute "broader_concept" from the AnnotatedValue
			annotatedValue.getAttributes().remove("broader_concept");
			if(!conceptToBroadersConceptMap.containsKey(concept)) {
				conceptToBroadersConceptMap.put(concept, new ArrayList<>());
			}
			conceptToBroadersConceptMap.get(concept).add(broaderConcept);
		}
		
		//now iterate over conceptToBroadersConceptMap to extract the cycles (if any)
		List<List<String>> cyclesList = new ArrayList<>();
		for( String concept : conceptToBroadersConceptMap.keySet()) {
				calculateCycle(concept, new ArrayList<String>(), conceptToBroadersConceptMap, 
						cyclesList );
		}
		
		//remove the duplicate cycles
		List<List<String>> cyclesReduxList = removeDuplicateCycles(cyclesList);
		
		//now the duplicates cycles have been removed, so construct the answer
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		
		ArrayNode cycles = jsonFactory.arrayNode();
		for(List<String> conceptList: cyclesReduxList) {
			ArrayNode singleCycle = jsonFactory.arrayNode();
			for(String concept : conceptList) {
				singleCycle.addPOJO(conceptToAnnotatedValue.get(concept));
			}
			cycles.add(singleCycle);
		}
		
		return cycles;
	}
	
	private void calculateCycle(String concept, ArrayList<String> currentCycle,
			Map<String, List<String>> conceptToBroadersConceptMap,
			List<List<String>> cyclesList) {
		//check if the current concept is the first element of the currentCycle, in this case a cycle was 
		// found, so add it to the cyclesToConceptsInCycleList
		if(currentCycle.size()>0 && currentCycle.get(0).equals(concept)) {
			cyclesList.add(currentCycle);
			return;
		}
		
		//check if the current concept is present in the currentCycle,in this case an inner cycle was found,
		// return without doing nothing (this cycle will be found by starting from this concept)
		if(currentCycle.contains(concept)) {
			return;
		}
		
		//add concept to currentCycle
		currentCycle.add(concept);
		
		//the cycle is not completed, so get the broader of the current concept
		List<String> broaderList = conceptToBroadersConceptMap.get(concept);
		for(String broader : broaderList) {
			//clone the currentCycle, then add the concept and call calculateCycle
			calculateCycle(broader, new ArrayList<>(currentCycle), conceptToBroadersConceptMap, 
					cyclesList);
		}
	}

	private List<List<String>> removeDuplicateCycles(List<List<String>> cyclesList) {
		List<List<String>> cyclesReduxList = new ArrayList<>();
		for(int i=0; i<cyclesList.size(); ++i) {
			boolean toBeAdded = true;
			for(int k=i+1; k<cyclesList.size(); ++k) {
				//check if cycles i and k contain the same elements or not
				if(compareCycles(cyclesList.get(i), cyclesList.get(k))) {
					toBeAdded=false;
					break;
				}
			}
			if(toBeAdded) {
				cyclesReduxList.add(cyclesList.get(i));
			}
		}
		return cyclesReduxList;
	}
	
	private boolean compareCycles(List<String> cycle1, List<String> cycle2) {
		if(cycle1.size() != cycle2.size()) {
			//since they have different sizes, they are different
			return false;
		}
		for(int i=0; i<cycle1.size(); ++i) {
			if(!cycle2.contains(cycle1.get(i))) {
				// the i element of cycle1 is not contained in cycle2, so they do not contain the same
				// elements
				return false;
			}
		}
		//the cycles contains the same elements
		return true;
	}
	
	/**
	 * Return a list of <triples> that are redundant from the hierarchical point of view
	 * @return
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'R')")
	//public Collection<AnnotatedValue<Resource>> listConceptsHierarchicalCycles() {
	public JsonNode listConceptsHierarchicalRedundancies(@Optional(defaultValue="true") boolean sameScheme) 
			throws UnsupportedLexicalizationModelException {
		IRI lexModel = getProject().getLexicalizationModel();
		
		if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}
		
		String query = "SELECT ?resource ?attr_concept ?attr_other_concept ?attr_predicate \n"
				+ "WHERE {\n"
				+ "?attr_concept a "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT)+" .\n"
				+ "?attr_concept "+broaderOrInverseNarrower()+" ?broader_concept .\n"
				+ "?broader_concept "+broaderOrInverseNarrower()+"+ ?attr_other_concept .\n"
				+ "FILTER(?broader_concept != ?attr_other_concept)\n"
				+ "?attr_concept "+broaderOrInverseNarrower()+" ?attr_other_concept .\n";
		if(sameScheme) {
			query += "?attr_concept "+NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME)+" ?scheme .\n"
					+"?broader_concept "+NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME)+" ?scheme .\n"
					+"?attr_other_concept "+NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME)+" ?scheme .\n";
		}
		//now check if the used property is BROADER or NARROWER
		query+= "{?attr_concept "+NTriplesUtil.toNTriplesString(SKOS.BROADER)+" ?attr_other_concept .\n"
				+ "BIND( "+NTriplesUtil.toNTriplesString(SKOS.BROADER)+"AS ?attr_predicate)}\n"
				+ "UNION\n"
				+ "{?attr_concept ^"+NTriplesUtil.toNTriplesString(SKOS.NARROWER)+" ?attr_other_concept .\n"
				+ "BIND( "+NTriplesUtil.toNTriplesString(SKOS.NARROWER)+"AS ?attr_predicate)}\n"
		//now bind the three elements (?attr_concept, ?attr_predicate and ?attr_other_concept )
				+ "{?attr_concept a "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT)+" .\n" //added to have some results
				+ "BIND(?attr_concept AS ?resource)}\n"
				+ "UNION\n"
				+ "{?attr_concept a "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT)+" .\n"//added to have some results
				+ "BIND(?attr_predicate AS ?resource)}\n"
				+ "UNION\n"
				+ "{?attr_concept a "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT)+" .\n" //added to have some results
				+ "BIND(?attr_other_concept AS ?resource)}\n"

				+ "} \n"
				+ "GROUP BY ?resource ?attr_concept ?attr_other_concept ?attr_predicate ";
		logger.debug("query [listConceptsHierarchicalRedundancies]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		//return qb.runQuery();
		
		Collection<AnnotatedValue<Resource>> annotatedValueList = qb.runQuery();
		//iterate over the response to construct the structure which will be used for the answer
		Map<String, TripleForAnnotatedValue> tripleForRedundancyMap = new HashMap<>();
		for(AnnotatedValue<Resource> annotatedValue: annotatedValueList) {
			String concept = annotatedValue.getAttributes().get("concept").stringValue();
			annotatedValue.getAttributes().remove("concept");
			String predicate = annotatedValue.getAttributes().get("predicate").stringValue();
			annotatedValue.getAttributes().remove("predicate");
			String other_concept = annotatedValue.getAttributes().get("other_concept").stringValue();
			annotatedValue.getAttributes().remove("other_concept");
			String key = concept+predicate+other_concept; 
			if(!tripleForRedundancyMap.containsKey(key)) {
				tripleForRedundancyMap.put(key, new TripleForAnnotatedValue());
			}
			TripleForAnnotatedValue tripleForRedundancy = tripleForRedundancyMap.get(key);
			//check the AnnotatedValue to which of its "elements"refer to
			String value = annotatedValue.getValue().stringValue();
			boolean invert=false;
			if(predicate.equals(NTriplesUtil.toNTriplesString(SKOS.NARROWER))) {
				invert=true;
			}
			if(value.equals(concept)) {
				if(invert) {
					tripleForRedundancy.setObject(annotatedValue);
				} else {
					tripleForRedundancy.setSubject(annotatedValue);
				}
			} else if(value.equals(predicate)) {
				tripleForRedundancy.setPredicate(annotatedValue);
			} else { //value.equals(other_concept)
				if(invert) {
					tripleForRedundancy.setSubject(annotatedValue);
				} else {
					tripleForRedundancy.setObject(annotatedValue);
				}
			}
		}
		
		//now construct the response
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode redundancies = jsonFactory.arrayNode();
		for(TripleForAnnotatedValue tripleForRedundancy : tripleForRedundancyMap.values()) {
			ObjectNode singleRedundancy = jsonFactory.objectNode();
			singleRedundancy.putPOJO("subject", tripleForRedundancy.getSubject());
			singleRedundancy.putPOJO("predicate", tripleForRedundancy.getPredicate());
			singleRedundancy.putPOJO("object", tripleForRedundancy.getObject());
			redundancies.add(singleRedundancy);
		}
		return redundancies;
	}
	
	private class TripleForAnnotatedValue {
		AnnotatedValue<Resource> subject;
		AnnotatedValue<Resource> predicate;
		AnnotatedValue<Resource> object;
		
		public AnnotatedValue<Resource> getSubject() {
			return subject;
		}
		public void setSubject(AnnotatedValue<Resource> subject) {
			this.subject = subject;
		}
		public AnnotatedValue<Resource> getPredicate() {
			return predicate;
		}
		public void setPredicate(AnnotatedValue<Resource> predicate) {
			this.predicate = predicate;
		}
		public AnnotatedValue<Resource> getObject() {
			return object;
		}
		public void setObject(AnnotatedValue<Resource> object) {
			this.object = object;
		}
	}
	
	/**
	 * Return a list of namespaces of alignments concepts with the number of alignments per namespace
	 * @param rolesArray
	 * @return
	 * @throws ProjectAccessException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public JsonNode listAlignedNamespaces(RDFResourceRole[] rolesArray) throws ProjectAccessException {
		//IRI lexModel = getProject().getLexicalizationModel();
		
		/*if(!(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) || 
				lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL))) {
			String msg = "The only Lexicalization Model supported by this service are SKOS and SKOSXL";
			throw new UnsupportedLexicalizationModelException(msg);
		}*/
		boolean first = true;
		String query = "SELECT ?namespace (count(?namespace) as ?count) \n"
				+ "WHERE {\n";
		
		boolean alreadyAddedMappingRel = false;
		for(RDFResourceRole role : rolesArray) {
			if(!first) {
				query += "UNION\n";
				first = false;
			}
			if(role.equals(RDFResourceRole.concept) || role.equals(RDFResourceRole.conceptScheme) ||
					role.equals(RDFResourceRole.skosCollection)) {
				if(!alreadyAddedMappingRel) {
					query +=
						// ?propMapping rdfs:subPropertyOf skos:mappingRelation
						"{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
							NTriplesUtil.toNTriplesString(SKOS.MAPPING_RELATION)+" . } \n";
				}
				alreadyAddedMappingRel=true;
			} else if(role.equals(RDFResourceRole.cls)) {
				query += 
						// ?propMapping rdfs:subPropertyOf owl:equivalentClass
						"{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.EQUIVALENTCLASS)+" . } \n"
						+ " UNION \n"
						// ?propMapping rdfs:subPropertyOf owl:disjointWith
						+ "{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.DISJOINTWITH)+" . } \n"
						+ " UNION \n"
						// ?propMapping rdfs:subPropertyOf rdfs:subClassOf
						+ "{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+" . } \n";
			} else if(role.equals(RDFResourceRole.property)) {
				query +=
						// ?propMapping rdfs:subPropertyOf owl:equivalentProperty
						"{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.EQUIVALENTPROPERTY)+" . } \n"
						//+ " UNION \n"
						//NTriplesUtil.toNTriplesString(OWL.PROPERTYDISJOINTWITH)+" . } \n"
						// ?propMapping rdfs:subPropertyOf owl:equivalentProperty
						+ " UNION \n"
						// ?propMapping rdfs:subPropertyOf rdfs.subPropertyOfy
						+ "{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+" . } \n";
			} else if(role.equals(RDFResourceRole.individual)) {
				query +=
						// ?propMapping rdfs:subPropertyOf owl:sameAs
						"{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.SAMEAS)+" . } \n"
						+ " UNION \n"
						// ?propMapping rdfs:subPropertyOf owl:differentFrom
						+ "{?propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.DIFFERENTFROM)+" . } \n";
			}
		}
		
		query += "?resource ?propMapping ?resource2 .\n"
				+ "BIND(REPLACE(str(?resource2), '[^(#|/)]+$', \"\") AS ?namespace)\n"
				+ "}\n"
				+ "GROUP BY ?namespace";
		
		
		logger.debug("query [listAlignedNamespaces]:\n" + query);
		TupleQuery tupleQuery = getManagedConnection().prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		
		//now iterate over the result to create the response
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode responce = jsonFactory.arrayNode();
		SimpleValueFactory simpleValueFactory = SimpleValueFactory.getInstance();
		while(tupleQueryResult.hasNext()) {
			BindingSet bindingSet = tupleQueryResult.next();
			if(bindingSet.hasBinding("namespace")){
				//add the namespace and the count
				String namespace = bindingSet.getBinding("namespace").getValue().stringValue();
				String count = bindingSet.getBinding("count").getValue().stringValue();
				ObjectNode objectNode = jsonFactory.objectNode();
				objectNode.put("namespace", namespace);
				objectNode.put("count", count);
				//get all the assoicated location for the given namaspace
				ArrayNode locationsNode = jsonFactory.arrayNode();
				List<ResourcePosition> resourcePositionList = 
						resourceLocator.listResourceLocations(getProject(), getRepository(), simpleValueFactory.createIRI(namespace));
				//iterate over the list of location to construct the response
				for(ResourcePosition resourcePosition : resourcePositionList) {
					ObjectNode locationNode = jsonFactory.objectNode();
					if(resourcePosition instanceof LocalResourcePosition) {
						locationNode.put("type", "local");
						LocalResourcePosition localResourcePosition = (LocalResourcePosition) resourcePosition;
						locationNode.put("name",localResourcePosition.getProject().getName());
					} else if (resourcePosition instanceof RemoteResourcePosition) {
						locationNode.put("type", "remote");
						RemoteResourcePosition remoteResourcePosition = (RemoteResourcePosition) resourcePosition;
						DatasetMetadata datasetMetadata = remoteResourcePosition.getDatasetMetadata();
						locationNode.put("title", datasetMetadata.getTitle());
						locationNode.put("sparqlEndpoint", datasetMetadata.getSparqlEndpoint());
						locationNode.put("deferenceable", datasetMetadata.isDereferenceable());
					}
					locationsNode.add(locationNode);
				}
				objectNode.set("locations", locationsNode);
				responce.add(objectNode);
			}
		}
		
		return responce;
	}
	
	/**
	 * Return a list of <triples> of broken alignments among concepts
	 * @return
	 * @throws ProjectAccessException 
	 * @throws IOException 
	 * @throws UnsupportedLexicalizationModelException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public JsonNode listBrokenAlignments(RDFResourceRole[] rolesArray,
			String[] namespaces, boolean checkLocalRes, boolean checkRemoteRes) throws ProjectAccessException, 
				IOException {
		
		//check that at least one of localConcepts and remoteConcepts
		if(!checkLocalRes && !checkRemoteRes) {
			String text = "At least of of the two paramters, localConcepts and remoteConcepts should be"
					+ "true";
			throw new IllegalArgumentException(text);
		}
		
		
		//use the input namespaces to get the list of resources which need to be checked 
		// (to see which propety to use, check the rolesArray)
		boolean first = true;
		String query = "SELECT ?resource ?attr_subj ?attr_propMapping ?attr_obj ?attr_namespace \n"
				+ "WHERE {\n";
		
		
		for(RDFResourceRole role : rolesArray) {
			if(!first) {
				query += "UNION\n";
				first = false;
			}
			if(role.equals(RDFResourceRole.concept) || role.equals(RDFResourceRole.conceptScheme) ||
					role.equals(RDFResourceRole.skosCollection)) {
				query +=
						// ?attr_propMapping rdfs:subPropertyOf skos:mappingRelation
						"{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
							NTriplesUtil.toNTriplesString(SKOS.MAPPING_RELATION)+" . } \n";
				
			} else if(role.equals(RDFResourceRole.cls)) {
				query += 
						// ?attr_propMapping rdfs:subPropertyOf owl:equivalentClass
						"{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.EQUIVALENTCLASS)+" . } \n"
						+ " UNION \n"
						// ?attr_propMapping rdfs:subPropertyOf owl:disjointWith
						+ "{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.DISJOINTWITH)+" . } \n"
						+ " UNION \n"
						// ?attr_propMapping rdfs:subPropertyOf rdfs:subClassOf
						+ "{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+" . } \n";
			} else if(role.equals(RDFResourceRole.property)) {
				query +=
						// ?attr_propMapping rdfs:subPropertyOf owl:equivalentProperty
						"{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.EQUIVALENTPROPERTY)+" . } \n"
						//+ " UNION \n"
						//NTriplesUtil.toNTriplesString(OWL.PROPERTYDISJOINTWITH)+" . } \n"
						// ?attr_propMapping rdfs:subPropertyOf owl:equivalentProperty
						+ " UNION \n"
						// ?attr_propMapping rdfs:subPropertyOf rdfs.subPropertyOfy
						+ "{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+" . } \n";
			} else if(role.equals(RDFResourceRole.individual)) {
				query +=
						// ?attr_propMapping rdfs:subPropertyOf owl:sameAs
						"{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.SAMEAS)+" . } \n"
						+ " UNION \n"
						// ?attr_propMapping rdfs:subPropertyOf owl:differentFrom
						+ "{?attr_propMapping "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+
						NTriplesUtil.toNTriplesString(OWL.DIFFERENTFROM)+" . } \n";
			}
		}
		
		query += "?attr_subj ?attr_propMapping ?attr_obj .\n"
				+ "BIND(REPLACE(str(?attr_obj), '[^(#|/)]+$', \"\") AS ?attr_namespace)\n";
		query += "FILTER(";
		first = true;
		for(String namespace : namespaces) {
			if(!first) {
				query += " || ";
			}
			first = false;
			query += "?attr_namespace = \""+namespace+"\"";
		}
		query += ")\n"
		
		//put ?attr_subj ?attr_propMapping ?obj in ?resource (so the will be in the annotated value)
				+ "{?attr_subj ?attr_propMapping ?attr_obj .\n" //added to have some results
				+ "BIND(?attr_subj AS ?resource)}\n"
				+ "UNION\n"
				+ "{?attr_subj ?attr_propMapping ?attr_obj .\n"//added to have some results
				+ "BIND(?attr_propMapping AS ?resource)}\n"
				+ "UNION\n"
				+ "{?attr_subj ?attr_propMapping ?attr_obj .\n" //added to have some results
				+ "BIND(?attr_obj AS ?resource)}\n"		
				
				+ "}\n"
				+ "GROUP BY ?resource ?attr_subj ?attr_propMapping ?attr_obj ?attr_namespace";
		
		logger.debug("query [listBrokenAlignments1]:\n" + query);
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		
		Collection<AnnotatedValue<Resource>> annotatedValueList = qb.runQuery();
		
		//iterate over the result from the SPARQL query to contruct a temp map containing the
		// TripleForAnnotatedValue (used as key for the map the subj_pred_obj)
		Map <String, TripleForAnnotatedValue> tripleForAnnotatedValueMap = new HashMap<>();
		for(AnnotatedValue<Resource> annotatedValue : annotatedValueList) {
			String subj = annotatedValue.getAttributes().get("subj").stringValue();
			annotatedValue.getAttributes().remove("subj");
			String propMapping = annotatedValue.getAttributes().get("propMapping").stringValue();
			annotatedValue.getAttributes().remove("propMapping");
			String obj = annotatedValue.getAttributes().get("obj").stringValue();
			annotatedValue.getAttributes().remove("obj");
			String key = subj+propMapping+obj; 
			if(!tripleForAnnotatedValueMap.containsKey(key)) {
				tripleForAnnotatedValueMap.put(key, new TripleForAnnotatedValue());
			}
			TripleForAnnotatedValue tripleForAnnotatedValue = tripleForAnnotatedValueMap.get(key);
			//check the AnnotatedValue to which of its "elements"refer to
			String value = annotatedValue.getValue().stringValue();
			if(value.equals(subj)) {
				tripleForAnnotatedValue.setSubject(annotatedValue);
			} else if(value.equals(propMapping)) {
				tripleForAnnotatedValue.setPredicate(annotatedValue);
			} else { //value.equals(obj)
				tripleForAnnotatedValue.setObject(annotatedValue);
			}
		}
		
		//use the just created tripleForAnnotatedValueMap to construct a map linking the namespace of the
		// objct of the list of triple of annotatedValue (all the Triple having having that namespace 
		// in their object)
		Map<String, List<TripleForAnnotatedValue>> namespaceToTripleMap = new HashMap<>();
		for(TripleForAnnotatedValue tripleForAnnotatedValue : tripleForAnnotatedValueMap.values()) {
			String namespace = tripleForAnnotatedValue.getObject().getAttributes().get("namespace").stringValue();
			if(!namespaceToTripleMap.containsKey(namespace)) {
				namespaceToTripleMap.put(namespace, new ArrayList<>());
			}
			tripleForAnnotatedValue.getSubject().getAttributes().remove("namespace");
			tripleForAnnotatedValue.getPredicate().getAttributes().remove("namespace");
			tripleForAnnotatedValue.getObject().getAttributes().remove("namespace");
			//add only the the triples having an IRI as object
			if(tripleForAnnotatedValue.getObject().getValue() instanceof IRI) {
				namespaceToTripleMap.get(namespace).add(tripleForAnnotatedValue);
			}
		}
		
		//now iterate over the map, consider just one resource per given namespace, since all resources
		// with the same namespace belong to the same location (TODO check this statement)
		Map<String, ResourcePosition> namespaceToPositionMap = new HashMap<>();
		for(String namespace : namespaceToTripleMap.keySet()) {
			IRI firstResForNamespace = (IRI) namespaceToTripleMap.get(namespace).get(0).getObject().getValue();
			ResourcePosition resourcePosition = 
					resourceLocator.locateResource(getProject(), getRepository(), firstResForNamespace);
			namespaceToPositionMap.put(namespace, resourcePosition);
		}
		
		//prepare the empty response, which will be fill everytime a broken alignment is found
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode response = jsonFactory.arrayNode();
		
		//now iterate over the map of namespace-resources and namespace-location to check if each 
		// resource is present in the associated location (check the two boolean values)
		for(String namespace : namespaceToTripleMap.keySet()) {
			ResourcePosition resourcePosition = namespaceToPositionMap.get(namespace);
			boolean doHttpRequest = false;
			RepositoryConnection connectionToOtherRepository = null;
			if(checkLocalRes && resourcePosition instanceof LocalResourcePosition) {
				LocalResourcePosition localResourcePosition = (LocalResourcePosition) resourcePosition;
				Project otherLocalProject = localResourcePosition.getProject();
				connectionToOtherRepository = acquireManagedConnectionToProject(getProject(),
						otherLocalProject);
			}
			else if(checkRemoteRes && resourcePosition instanceof RemoteResourcePosition) {
				RemoteResourcePosition remoteResourcePosition = (RemoteResourcePosition) resourcePosition;
				//get the SPARQL endpoint for the remote position
				String sparqlEndPoint = remoteResourcePosition.getDatasetMetadata().getSparqlEndpoint();
				if(sparqlEndPoint != null) {
					Repository sparqlRepository = new SPARQLRepository(sparqlEndPoint);
					sparqlRepository.initialize();
					connectionToOtherRepository = sparqlRepository.getConnection();
				} else {
					doHttpRequest = true;
				}
			} else if(checkRemoteRes &&  resourcePosition.equals(ResourceLocator.UNKNOWN_RESOURCE_POSITION)) {
				doHttpRequest = true;
			} else {
				//this shoyld never happen, decide what to do
			}
			
			//first implementation, do a SPARQL query for each resource
			for(TripleForAnnotatedValue tripleForAnnotatedValue : namespaceToTripleMap.get(namespace)) {
				IRI resource = (IRI) tripleForAnnotatedValue.getObject().getValue();
				if(doHttpRequest) {
					//do an httpRequest to see if the which IRIs are associated to an existing web page
					URL url = new URL(resource.stringValue());
					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					con.setRequestMethod("GET");
					con.setRequestProperty("Content-Type", "application/json");
					con.setConnectTimeout(5000);
					con.setReadTimeout(5000);
					//connect to the remote site
					con.connect();
					
					int code = con.getResponseCode();
					if(code!=200) {
						//if the page cannot be found, then the resource does not exist, so return it
						ObjectNode singleBrokenAlign = jsonFactory.objectNode();
						singleBrokenAlign.putPOJO("subject", tripleForAnnotatedValue.getSubject());
						singleBrokenAlign.putPOJO("predicate", tripleForAnnotatedValue.getPredicate());
						singleBrokenAlign.putPOJO("object", tripleForAnnotatedValue.getObject());
						response.add(singleBrokenAlign);
					}
					//close the connection with the remote site
					con.disconnect();
				} else {
					//do the sparql query
					query = "SELECT ?deprecated ?hasType"
							+" WHERE {\n"
							//check if the resourse is deprecated
							+ "{ "+NTriplesUtil.toNTriplesString(resource)+ " "+
								NTriplesUtil.toNTriplesString(OWL2Fragment.DEPRECATED)+" \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> .\n"
							+ "BIND(true AS ?deprecated )\n"
							+ "}\n"
							+ "UNION\n"
							//check if the 
							+ "{ "+NTriplesUtil.toNTriplesString(resource) + " a ?type .\n"  
							+ "BIND(true AS ?hasType)\n "
							+ "}\n"
							+ "}";
					logger.debug("query [listBrokenAlignments2]:\n" + query);
					boolean hasType = false;
					boolean isDeprecated = false;
					TupleQuery tupleQuery = connectionToOtherRepository.prepareTupleQuery(query);
					tupleQuery.setIncludeInferred(false);
					TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
					//analyze the response of the query to see if the resource has a type and/or is deprecated
					if(tupleQueryResult.hasNext()) {
						//it has at least a type or it is deprecated
						BindingSet bindingSet = tupleQueryResult.next();
						if(bindingSet.hasBinding("hasType")) {
							hasType = true;
						}
						if(bindingSet.hasBinding("deprecated")) {
							isDeprecated = true;
						}
					}
					
					//if the resource has no type or is deprecated, then return it (the triple from which the 
					// resource was taken)
					if(!hasType || isDeprecated) {
						ObjectNode singleBrokenAlign = jsonFactory.objectNode();
						singleBrokenAlign.putPOJO("subject", tripleForAnnotatedValue.getSubject());
						singleBrokenAlign.putPOJO("predicate", tripleForAnnotatedValue.getPredicate());
						if(isDeprecated) {
							tripleForAnnotatedValue.getObject().setAttribute("deprecated", true);
						} else {
							tripleForAnnotatedValue.getObject().setAttribute("deprecated", false);
						}
						singleBrokenAlign.putPOJO("object", tripleForAnnotatedValue.getObject());
						response.add(singleBrokenAlign);
					}
				}
			}
			//close the connection to the other repository (if it not null)
			if(connectionToOtherRepository != null) {
				connectionToOtherRepository.close();
			}
		}
		return response;
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
	
	private String getSkosxlPrefOrAltOrHidden() {
		String or = "("+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+" | " +
				NTriplesUtil.toNTriplesString(SKOSXL.ALT_LABEL)+" | " +
				NTriplesUtil.toNTriplesString(SKOSXL.HIDDEN_LABEL)+" )";
		return or;
	}
	
	private String getSkosPrefOrAltOrHidden() {
		String or = "("+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+" | " +
				NTriplesUtil.toNTriplesString(SKOS.ALT_LABEL)+" | " +
				NTriplesUtil.toNTriplesString(SKOS.HIDDEN_LABEL)+" )";
		return or;
	}
	
	private String broaderOrInverseNarrower() {
		String broaderOrInverceNarrower = "("+NTriplesUtil.toNTriplesString(SKOS.BROADER)+" | ^" +
				NTriplesUtil.toNTriplesString(SKOS.NARROWER)+" )";
		return broaderOrInverceNarrower;
	}
	
	private RepositoryConnection acquireManagedConnectionToProject(ProjectConsumer consumer,
			Project resourceHoldingProject) throws ProjectAccessException {
		if (consumer.equals(resourceHoldingProject)) {
			return getManagedConnection();
		} else {
			AccessResponse accessResponse = ProjectManager.checkAccessibility(consumer,
					resourceHoldingProject, AccessLevel.R, LockLevel.NO);

			if (!accessResponse.isAffirmative()) {
				throw new ProjectAccessException(accessResponse.getMsg());
			}

			return projectConnectionHolder.get().computeIfAbsent(resourceHoldingProject,
					p -> RDF4JRepositoryUtils.wrapReadOnlyConnection(p.getRepository().getConnection()));
		}
	}
}
