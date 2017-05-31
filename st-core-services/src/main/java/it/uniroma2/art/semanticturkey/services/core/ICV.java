package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.owlart.query.TupleBindingsIterator;
import it.uniroma2.art.owlart.query.TupleQuery;
import it.uniroma2.art.owlart.query.Update;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.owlart.vocabulary.SKOSXL;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFLiteral;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFURI;
import it.uniroma2.art.semanticturkey.services.STServiceAdapterOLD;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;


@GenerateSTServiceController
@Validated
@Component
public class ICV extends STServiceAdapterOLD {
	
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
	public Response listDanglingConcepts(ARTURIResource scheme, @Optional (defaultValue="0") Integer limit) throws UnsupportedQueryLanguageException,
			ModelAccessException, MalformedQueryException, QueryEvaluationException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element collectionElem = XMLHelp.newElement(dataElement, "collection");
		OWLModel model = getOWLModel();
		//nested query
		String q = "SELECT ?concept ?count WHERE { \n"
				//this counts records
				+ "{ SELECT (COUNT (*) AS ?count) WHERE{\n"
				+ "BIND(<" + scheme.getURI() + "> as ?scheme)"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.TOPCONCEPTOF + "> ?scheme}\n"
				+ "FILTER NOT EXISTS {?scheme <" + SKOS.HASTOPCONCEPT + "> ?concept }\n"
				+ "{?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1  . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> ?broaderConcept1 .\n"
				+ "FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.INSCHEME + "> ?scheme  . }\n"
				+ "} {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "?broaderConcept2 <" + SKOS.NARROWER + "> ?concept .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.INSCHEME + "> ?scheme . }\n"
				+ "} } }"
				//this retrieves data
				+ "{ SELECT ?concept ?scheme WHERE{\n"
				+ "BIND(<" + scheme.getURI() + "> as ?scheme)"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.TOPCONCEPTOF + "> ?scheme}\n"
				+ "FILTER NOT EXISTS {?scheme <" + SKOS.HASTOPCONCEPT + "> ?concept }\n"
				+ "{?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1  . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> ?broaderConcept1 .\n"
				+ "FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.INSCHEME + "> ?scheme  . }\n"
				+ "} {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "?broaderConcept2 <" + SKOS.NARROWER + "> ?concept .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.INSCHEME + "> ?scheme . }\n"
				+ "} } ORDER BY ?concept";
		if (limit > 0)
			q = q + " LIMIT " + limit; //for client-side performance issue
		q = q + "} }";
		logger.info("query [listDanglingConcepts]:\n" + q);
		
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		collectionElem.setAttribute("count", "0");//default (if query return result, in the next while it's override
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String count = tb.getBinding("count").getBoundValue().getNominalValue();
			collectionElem.setAttribute("count", count);
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
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
	public Response listCyclicConcepts() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
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
				+ "?top (<" + SKOS.TOPCONCEPTOF + "> | ^ <" + SKOS.HASTOPCONCEPT + ">)+ ?scheme .} }";
		logger.info("query [listCyclicConcepts]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String topCyclicConcept = tb.getBinding("top").getBoundValue().getNominalValue();
			String node1 = tb.getBinding("n1").getBoundValue().getNominalValue();
			String node2 = tb.getBinding("n2").getBoundValue().getNominalValue();
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
	public Response listConceptSchemesWithNoTopConcept() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?conceptScheme WHERE {\n"
				+ "?conceptScheme a <" + SKOS.CONCEPTSCHEME + "> .\n"
				+ "FILTER NOT EXISTS { {\n"
				+ "?conceptScheme <" + SKOS.HASTOPCONCEPT + "> ?topConcept .\n"
				+ "} UNION {\n"
				+ "?topConcept <" + SKOS.TOPCONCEPTOF + "> ?conceptScheme . } } }";
		logger.info("query [listConceptSchemesWithNoTopConcept]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String conceptScheme = tb.getBinding("conceptScheme").getBoundValue().getNominalValue();
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
	public Response listConceptsWithNoScheme(@Optional (defaultValue="0") Integer limit) throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element collectionElem = XMLHelp.newElement(dataElement, "collection");
		OWLModel model = getOWLModel();
		//nested query
		String q = "SELECT ?concept ?count WHERE { \n"
				//this counts records
				+ "{ SELECT (COUNT (?concept) AS ?count) WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "FILTER NOT EXISTS { ?concept <" + SKOS.INSCHEME + "> ?scheme . }"
				+ "\n}\n}"
				//this retrieves data
				+ "{ SELECT ?concept WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "FILTER NOT EXISTS { ?concept <" + SKOS.INSCHEME + "> ?scheme . } }";
		if (limit > 0)
			q = q + "\nLIMIT " + limit; //for client-side performance issue
		q = q + "\n}\n}";
		logger.info("query [listConceptsWithNoScheme]:\n" + q);
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		collectionElem.setAttribute("count", "0");//default (if query return result, in the next while it's override
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String count = tb.getBinding("count").getBoundValue().getNominalValue();
			collectionElem.setAttribute("count", count);
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
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
	public Response listTopConceptsWithBroader() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT DISTINCT ?concept ?scheme WHERE {\n"
				+ "?concept <" + SKOS.TOPCONCEPTOF + "> | ^<" + SKOS.HASTOPCONCEPT + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?broader <" + SKOS.INSCHEME + "> ?scheme . }";
		logger.info("query [listTopConceptsWithBroader]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			String scheme = tb.getBinding("scheme").getBoundValue().getNominalValue();
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
	public Response listHierarchicallyRedundantConcepts() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT DISTINCT ?narrower ?broader WHERE{\n"
				+ "?narrower <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?narrower (<" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + ">)+ ?middle .\n"
				+ "?middle <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "FILTER(?narrower != ?middle)\n}";
		logger.info("query [listHierarchicallyRedundantConcepts]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String narrower = tb.getBinding("narrower").getBoundValue().getNominalValue();
			String broader = tb.getBinding("broader").getBoundValue().getNominalValue();
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
	public Response listConceptsWithSameSKOSPrefLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept1 ?concept2 ?label ?lang WHERE {\n"
				+ "?concept1 a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept2 a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept1 <" + SKOS.PREFLABEL + "> ?label .\n"
				+ "?concept2 <" + SKOS.PREFLABEL + "> ?label .\n"
				+ "bind(lang(?label) as ?lang)\n"
				+ "FILTER (str(?concept1) < str(?concept2)) }";
		logger.info("query [listConceptsWithSameSKOSPrefLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept1 = tb.getBinding("concept1").getBoundValue().getNominalValue();
			String concept2 = tb.getBinding("concept2").getBoundValue().getNominalValue();
			String label = tb.getBinding("label").getBoundValue().getNominalValue();
			String lang = tb.getBinding("lang").getBoundValue().getNominalValue();
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
	public Response listConceptsWithSameSKOSXLPrefLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept1 ?concept2 ?label1 ?lang WHERE {\n"
				+ "?concept1 a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept2 a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept1 <" + SKOSXL.PREFLABEL + "> ?xlabel1 .\n"
				+ "?concept2 <" + SKOSXL.PREFLABEL + "> ?xlabel2 .\n"
				+ "?xlabel1 <" + SKOSXL.LITERALFORM + "> ?label1 .\n"
				+ "?xlabel2 <" + SKOSXL.LITERALFORM + "> ?label2 .\n"
				+ "FILTER (?label1 = ?label2)\n"
				+ "FILTER (str(?concept1) < str(?concept2))\n"
				+ "bind(lang(?label1) as ?lang) }";
		logger.info("query [listConceptsWithSameSKOSXLPrefLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept1 = tb.getBinding("concept1").getBoundValue().getNominalValue();
			String concept2 = tb.getBinding("concept2").getBoundValue().getNominalValue();
			String label = tb.getBinding("label1").getBoundValue().getNominalValue();
			String lang = tb.getBinding("lang").getBoundValue().getNominalValue();
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
	public Response listResourcesWithOnlySKOSAltLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		String q = "SELECT DISTINCT ?resource ?lang ?type WHERE {\n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . } \n"
				+ "UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPTSCHEME + "> . } \n"
				+ "?resource a ?type . \n"
				+ "?resource <" + SKOS.ALTLABEL + "> ?alt .\n"
				+ "bind (lang(?alt) as ?lang) .\n"
				+ "FILTER NOT EXISTS {\n"
				+ "?resource <" + SKOS.PREFLABEL + "> ?pref .\n"
				+ "FILTER (lang(?pref) = ?lang) } }";
		logger.info("query [listResourcesWithOnlySKOSAltLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			ARTURIResource resource = tb.getBinding("resource").getBoundValue().asURIResource();
			String type = tb.getBinding("type").getBoundValue().getNominalValue();
			RDFResourceRolesEnum role = RDFResourceRolesEnum.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRolesEnum.concept;
			} else if (type.equals(SKOS.CONCEPTSCHEME)) {
				role = RDFResourceRolesEnum.conceptScheme;
			}
			String lang = tb.getBinding("lang").getBoundValue().getNominalValue();
			
			Element recordElem = XMLHelp.newElement(dataElem, "record");
			STRDFURI uriElem = STRDFNodeFactory.createSTRDFURI(resource, role, true, resource.getURI());
			RDFXMLHelp.addRDFNode(recordElem, uriElem);
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
	public Response listResourcesWithOnlySKOSXLAltLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		String q = "SELECT DISTINCT ?resource ?lang ?type WHERE { \n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . } \n"
				+ "UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPTSCHEME + "> . } \n"
				+ "?resource a ?type . \n"
				+ "?resource <" + SKOSXL.ALTLABEL + "> ?alt . \n"
				+ "?alt <" + SKOSXL.LITERALFORM + "> ?literalFormAlt . \n"
				+ "bind (lang(?literalFormAlt) as ?lang) . \n"
				+ "FILTER NOT EXISTS { \n"
				+ "?resource <" + SKOSXL.PREFLABEL + "> ?pref . \n"
				+ "?pref <" + SKOSXL.LITERALFORM + "> ?literalFormPref . \n"
				+ "FILTER (lang(?literalFormPref) = ?lang) } }";
		logger.info("query [listResourcesWithOnlySKOSAltLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			ARTURIResource resource = tb.getBinding("resource").getBoundValue().asURIResource();
			String type = tb.getBinding("type").getBoundValue().getNominalValue();
			RDFResourceRolesEnum role = RDFResourceRolesEnum.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRolesEnum.concept;
			} else if (type.equals(SKOS.CONCEPTSCHEME)) {
				role = RDFResourceRolesEnum.conceptScheme;
			}
			String lang = tb.getBinding("lang").getBoundValue().getNominalValue();
			
			Element recordElem = XMLHelp.newElement(dataElem, "record");
			STRDFURI uriElem = STRDFNodeFactory.createSTRDFURI(resource, role, true, resource.getURI());
			RDFXMLHelp.addRDFNode(recordElem, uriElem);
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
	public Response listResourcesWithNoSKOSPrefLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException,
			ModelAccessException, MalformedQueryException {
		String q = "SELECT ?resource ?type WHERE {\n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPTSCHEME + "> . }\n"
				+ "?resource a ?type . \n"
				+ "FILTER NOT EXISTS {\n"
				+ "?resource <" + SKOS.PREFLABEL + "> ?prefLabel .\n"
				+ "} }";
		logger.info("query [listResourcesWithNoSKOSPrefLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		Collection<STRDFURI> result = STRDFNodeFactory.createEmptyURICollection();
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			ARTURIResource resource = tb.getBinding("resource").getBoundValue().asURIResource();
			String type = tb.getBinding("type").getBoundValue().getNominalValue();
			RDFResourceRolesEnum role = RDFResourceRolesEnum.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRolesEnum.concept;
			} else if (type.equals(SKOS.CONCEPTSCHEME)) {
				role = RDFResourceRolesEnum.conceptScheme;
			}
			result.add(STRDFNodeFactory.createSTRDFURI(resource, role, true, resource.getURI()));
		}
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		RDFXMLHelp.addRDFNodes(response, result);
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
	public Response listResourcesWithNoSKOSXLPrefLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException,
			ModelAccessException, MalformedQueryException {
		String q = "SELECT ?resource ?type WHERE {\n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . }\n"
				+ " UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPTSCHEME + "> . }\n"
				+ "?resource a ?type . \n"
				+ "FILTER NOT EXISTS {\n"
				+ "?resource <" + SKOSXL.PREFLABEL + "> ?prefLabel .\n"
				+ "} }";
		logger.info("query [listResourcesWithNoSKOSXLPrefLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		Collection<STRDFURI> result = STRDFNodeFactory.createEmptyURICollection();
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			ARTURIResource resource = tb.getBinding("resource").getBoundValue().asURIResource();
			String type = tb.getBinding("type").getBoundValue().getNominalValue();
			RDFResourceRolesEnum role = RDFResourceRolesEnum.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRolesEnum.concept;
			} else if (type.equals(SKOS.CONCEPTSCHEME)) {
				role = RDFResourceRolesEnum.conceptScheme;
			}
			result.add(STRDFNodeFactory.createSTRDFURI(resource, role, true, resource.getURI()));
		}
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		RDFXMLHelp.addRDFNodes(response, result);
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
	public Response listConceptsWithMultipleSKOSPrefLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT DISTINCT ?concept ?lang WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.PREFLABEL + "> ?label1.\n"
				+ "?concept <" + SKOS.PREFLABEL + "> ?label2.\n"
				+ "FILTER ( ?label1 != ?label2 && lang(?label1) = lang(?label2) )\n"
				+ "bind(lang(?label1) as ?lang) }";
		logger.info("query [listConceptsWithMultipleSKOSPrefLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			String lang = tb.getBinding("lang").getBoundValue().getNominalValue();
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
	public Response listConceptsWithMultipleSKOSXLPrefLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT DISTINCT ?concept ?lang WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOSXL.PREFLABEL + "> ?label1 .\n"
				+ "?concept <" + SKOSXL.PREFLABEL + "> ?label2 .\n"
				+ "?label1 <" + SKOSXL.LITERALFORM + "> ?lit1 .\n"
				+ "?label2 <" + SKOSXL.LITERALFORM + "> ?lit2 .\n"
				+ "bind(lang(?lit1) as ?lang)\n"
				+ "FILTER ( ?label1 != ?label2 && lang(?lit1) = lang(?lit2) ) }";
		logger.info("query [listConceptsWithMultipleSKOSXLPrefLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			String lang = tb.getBinding("lang").getBoundValue().getNominalValue();
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
	public Response listResourcesWithNoLanguageTagSKOSLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		String q = "SELECT ?resource ?labelPred ?label ?type WHERE {\n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . }\n"
				+ "UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPTSCHEME + "> . }\n"
				+ "?resource a ?type \n"
				+ "{ bind(<" + SKOS.PREFLABEL + "> as ?labelPred)}\n"
				+ "UNION\n"
				+ "{bind(<" + SKOS.ALTLABEL + "> as ?labelPred)}\n"
				+ "?resource ?labelPred ?label .\n"
				+ "FILTER (lang(?label) = '') }";
		logger.info("query [listResourcesWithNoLanguageTagSKOSLabel]:\n" + q);
		OWLModel model = getOWLModel();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			ARTURIResource resource = tb.getBinding("resource").getBoundValue().asURIResource();
			String type = tb.getBinding("type").getBoundValue().getNominalValue();
			RDFResourceRolesEnum role = RDFResourceRolesEnum.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRolesEnum.concept;
			} else if (type.equals(SKOS.CONCEPTSCHEME)) {
				role = RDFResourceRolesEnum.conceptScheme;
			}
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			
			Element resourceElem = XMLHelp.newElement(recordElem, "resource");
			STRDFURI stResource = STRDFNodeFactory.createSTRDFURI(resource, role, true, resource.getURI());
			RDFXMLHelp.addRDFNode(resourceElem, stResource);
			
			Element predicateElem = XMLHelp.newElement(recordElem, "predicate");
			ARTURIResource labelPred = tb.getBinding("labelPred").getBoundValue().asURIResource();
			STRDFURI stPredicate = STRDFNodeFactory.createSTRDFURI(labelPred, RDFResourceRolesEnum.annotationProperty, true, model.getQName(labelPred.getURI()));
			RDFXMLHelp.addRDFNode(predicateElem, stPredicate);
			
			Element objectElem = XMLHelp.newElement(recordElem, "object");
			ARTLiteral label = tb.getBinding("label").getBoundValue().asLiteral();
			STRDFLiteral stObject = STRDFNodeFactory.createSTRDFLiteral(label, true, label.getLabel());
			RDFXMLHelp.addRDFNode(objectElem, stObject);
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
	public Response listResourcesWithNoLanguageTagSKOSXLLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		String q = "SELECT ?resource ?labelPred ?xlabel ?literalForm ?type WHERE {\n"
				+ "{ ?resource a <" + SKOS.CONCEPT + "> . }\n"
				+ "UNION \n"
				+ "{ ?resource a <" + SKOS.CONCEPTSCHEME + "> . }\n"
				+ "?resource a ?type . \n"
				+ "?xlabel a <" + SKOSXL.LABEL + "> .\n"
				+ "?resource ?labelPred ?xlabel .\n"
				+ "?xlabel <" + SKOSXL.LITERALFORM + "> ?literalForm .\n"
				+ "FILTER (lang(?literalForm)= '') }";
		logger.info("query [listConceptsWithNoLanguageTagSKOSXLLabel]:\n" + q);
		OWLModel model = getOWLModel();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			ARTURIResource resource = tb.getBinding("resource").getBoundValue().asURIResource();
			String type = tb.getBinding("type").getBoundValue().getNominalValue();
			RDFResourceRolesEnum role = RDFResourceRolesEnum.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRolesEnum.concept;
			} else if (type.equals(SKOS.CONCEPTSCHEME)) {
				role = RDFResourceRolesEnum.conceptScheme;
			}
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			
			Element resourceElem = XMLHelp.newElement(recordElem, "resource");
			STRDFURI stResource = STRDFNodeFactory.createSTRDFURI(resource, role, true, resource.getURI());
			RDFXMLHelp.addRDFNode(resourceElem, stResource);
			
			Element predicateElem = XMLHelp.newElement(recordElem, "predicate");
			ARTURIResource labelPred = tb.getBinding("labelPred").getBoundValue().asURIResource();
			STRDFURI stPredicate = STRDFNodeFactory.createSTRDFURI(labelPred, RDFResourceRolesEnum.objectProperty, true, model.getQName(labelPred.getURI()));
			RDFXMLHelp.addRDFNode(predicateElem, stPredicate);
			
			Element objectElem = XMLHelp.newElement(recordElem, "object");
			ARTResource label = tb.getBinding("xlabel").getBoundValue().asResource();
			ARTLiteral literalForm = tb.getBinding("literalForm").getBoundValue().asLiteral();
			STRDFResource stObject = STRDFNodeFactory.createSTRDFResource(label, RDFResourceRolesEnum.xLabel, true, literalForm.getLabel());
			RDFXMLHelp.addRDFNode(objectElem, stObject);
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
	public Response listResourcesWithOverlappedSKOSLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		String q = "SELECT ?resource ?label ?lang ?type WHERE {\n"
				+ "?resource a ?type .\n"
				+ "?resource <" + SKOS.PREFLABEL + "> ?label .\n"
				+ "?resource <" + SKOS.ALTLABEL + "> ?label .\n"
				+ "bind(lang(?label) as ?lang) . }";
		logger.info("query [listResourcesWithOverlappedSKOSLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			ARTURIResource resource = tb.getBinding("resource").getBoundValue().asURIResource();
			String label = tb.getBinding("label").getBoundValue().getNominalValue();
			String lang = tb.getBinding("lang").getBoundValue().getNominalValue();
			String type = tb.getBinding("type").getBoundValue().getNominalValue();
			RDFResourceRolesEnum role = RDFResourceRolesEnum.concept;
			if (type.equals(SKOS.CONCEPT)) {
				role = RDFResourceRolesEnum.concept;
			} else if (type.equals(SKOS.CONCEPTSCHEME)) {
				role = RDFResourceRolesEnum.conceptScheme;
			}
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			STRDFURI uriElem = STRDFNodeFactory.createSTRDFURI(resource, role, true, resource.getURI());
			RDFXMLHelp.addRDFNode(recordElem, uriElem);
			STRDFLiteral labelElem = STRDFNodeFactory.createSTRDFLiteral(model.createLiteral(label, lang), true);
			RDFXMLHelp.addRDFNode(recordElem, labelElem);
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
	public Response listResourcesWithOverlappedSKOSXLLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?resource ?type ?prefLabel ?altLabel ?literalForm ?lang WHERE {\n"
				+ "?resource a ?type .\n"
				+ "?resource <" + SKOSXL.PREFLABEL + "> ?prefLabel .\n"
				+ "?resource <" + SKOSXL.ALTLABEL + "> ?altLabel .\n"
				+ "?prefLabel <" + SKOSXL.LITERALFORM + "> ?literalForm .\n"
				+ "?altLabel <" + SKOSXL.LITERALFORM + "> ?literalForm .\n"
				+ "bind(lang(?literalForm) as ?lang) . }";
		logger.info("query [listResourcesWithOverlappedSKOSXLLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			ARTURIResource resource = tb.getBinding("resource").getBoundValue().asURIResource();
			ARTURIResource type = tb.getBinding("type").getBoundValue().asURIResource();
			ARTResource prefLabel = tb.getBinding("prefLabel").getBoundValue().asResource();
			ARTResource altLabel = tb.getBinding("altLabel").getBoundValue().asResource();
			String literalForm = tb.getBinding("literalForm").getBoundValue().getNominalValue();
			String lang = tb.getBinding("lang").getBoundValue().getNominalValue();
			
			RDFResourceRolesEnum role = RDFResourceRolesEnum.concept;
			if (type.equals(SKOS.Res.CONCEPT)) {
				role = RDFResourceRolesEnum.concept;
			} else if (type.equals(SKOS.Res.CONCEPTSCHEME)) {
				role = RDFResourceRolesEnum.conceptScheme;
			} else if (type.equals(SKOS.Res.COLLECTION)) {
				role = RDFResourceRolesEnum.skosCollection;
			} else if (type.equals(SKOS.Res.ORDEREDCOLLECTION)) {
				role = RDFResourceRolesEnum.skosOrderedCollection;
			}
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			STRDFURI resUriElem = STRDFNodeFactory.createSTRDFURI(resource, role, true, resource.getURI());
			RDFXMLHelp.addRDFNode(recordElem, resUriElem);
			
			STRDFResource resPrefLabel = STRDFNodeFactory.createSTRDFResource(prefLabel, RDFResourceRolesEnum.xLabel, true, literalForm);
			Element prefLabelElem = XMLHelp.newElement(recordElem, "prefLabel");
			Element resPrefLabelElem = RDFXMLHelp.addRDFNode(prefLabelElem, resPrefLabel);
			resPrefLabelElem.setAttribute("lang", lang);
			
			STRDFResource resAltLabel = STRDFNodeFactory.createSTRDFResource(altLabel, RDFResourceRolesEnum.xLabel, true, literalForm);
			Element altLabelElem = XMLHelp.newElement(recordElem, "altLabel");
			Element resAltLabelElem = RDFXMLHelp.addRDFNode(altLabelElem, resAltLabel);
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
	public Response listConceptsWithExtraWhitespaceInSKOSLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept ?labelPred ?label ?lang WHERE {\n"
				+ "{ bind(<" + SKOS.PREFLABEL + "> as ?labelPred)}\n"
				+ "UNION\n"
				+ "{bind(<" + SKOS.ALTLABEL + "> as ?labelPred)}\n"
				+ "?concept ?labelPred ?skoslabel .\n"
				+ "bind(str(?skoslabel) as ?label)\n"
				+ "FILTER (regex (?label, '^ +') || regex (?label, ' +$') || regex(?label, ' {2,}?'))\n"
				+ "bind(lang(?skoslabel) as ?lang) }";
		logger.info("query [listConceptsWithExtraWhitespaceInSKOSLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			String labelPred = tb.getBinding("labelPred").getBoundValue().getNominalValue();
			String label = tb.getBinding("label").getBoundValue().getNominalValue();
			String lang = tb.getBinding("lang").getBoundValue().getNominalValue();
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
	public Response listConceptsWithExtraWhitespaceInSKOSXLLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept ?labelPred ?label ?lang WHERE {\n"
				+ "{ bind(<" + SKOSXL.PREFLABEL + "> as ?labelPred)}\n"
				+ "UNION\n"
				+ "{bind(<" + SKOSXL.ALTLABEL + "> as ?labelPred)}\n"
				+ "?concept ?labelPred ?xlabel .\n"
				+ "?xlabel <" + SKOSXL.LITERALFORM + "> ?litForm .\n"
				+ "bind(str(?litForm) as ?label)\n"
				+ "FILTER (regex (?label, '^ +') || regex (?label, ' +$') || regex(?label, ' {2,}?'))\n"
				+ "bind(lang(?litForm) as ?lang) }";
		logger.info("query [listConceptsWithExtraWhitespaceInSKOSXLLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			String labelPred = tb.getBinding("labelPred").getBoundValue().getNominalValue();
			String label = tb.getBinding("label").getBoundValue().getNominalValue();
			String lang = tb.getBinding("lang").getBoundValue().getNominalValue();
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
	public Response listDanglingXLabels() throws UnsupportedQueryLanguageException, ModelAccessException,
			MalformedQueryException, QueryEvaluationException {
		String q = "SELECT ?xlabel WHERE {\n"
				+ "?xlabel a <" + SKOSXL.LABEL + "> .\n"
				+ "FILTER NOT EXISTS {\n" 
				+ "?concept <" + SKOSXL.PREFLABEL + "> | <" + SKOSXL.ALTLABEL + "> | <" + SKOSXL.HIDDENLABEL + "> ?xlabel.\n"
				+ "} }";
		logger.info("query [listDanglingXLabels]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		Collection<STRDFResource> result = STRDFNodeFactory.createEmptyResourceCollection();
		while (itTupleBinding.hasNext()) {
			TupleBindings tb = itTupleBinding.next();
			ARTNode xlabelNode = tb.getBinding("xlabel").getBoundValue();
			result.add(STRDFNodeFactory.createSTRDFResource(
					xlabelNode.asResource(), RDFResourceRolesEnum.xLabel, true, xlabelNode.getNominalValue()));
		}
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		RDFXMLHelp.addRDFNodes(response, result);
		return response;
	}
	
	//-----GENERICS-----
	
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Response listResourcesURIWithSpace(@Optional (defaultValue="0") Integer limit) throws QueryEvaluationException,
		UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element collectionElem = XMLHelp.newElement(dataElement, "collection");
		OWLModel model = getOWLModel();
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
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		collectionElem.setAttribute("count", "0");//default (if query return result, in the next while it's override
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String count = tb.getBinding("count").getBoundValue().getNominalValue();
			collectionElem.setAttribute("count", count);
			String resource = tb.getBinding("resource").getBoundValue().getNominalValue();
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
	public Response setAllDanglingAsTopConcept(ARTURIResource scheme) 
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException, QueryEvaluationException {
		String q = "INSERT {\n"
				+ "GRAPH <" + getWorkingGraph().getNominalValue() + ">\n"
				+ "{ ?concept <" + SKOS.TOPCONCEPTOF + "> <" + scheme.getURI() + "> }\n"
				+ "} WHERE {\n"
				+ "BIND(<" + scheme.getURI() + "> as ?scheme)\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.TOPCONCEPTOF + "> ?scheme}\n"
				+ "FILTER NOT EXISTS {?scheme <" + SKOS.HASTOPCONCEPT + "> ?concept }\n"
				+ "{ ?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1 . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> ?broaderConcept1 .\n"
				+ "FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.INSCHEME + "> ?scheme  . }\n"
				+ "} {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "?broaderConcept2 <" + SKOS.NARROWER + "> ?concept .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.INSCHEME + "> ?scheme . }\n"
				+ "} }";
		logger.info("query [setAllDanglingAsTopConcept]:\n" + q);
		OWLModel model = getOWLModel();
		Update update = model.createUpdateQuery(q);
		update.evaluate(false);
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
	public Response setBroaderForAllDangling(ARTURIResource scheme, ARTURIResource broader) 
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException, QueryEvaluationException {
		String q = "INSERT {\n"
				+ "GRAPH <" + getWorkingGraph().getNominalValue() + ">\n"
				+ "{ ?concept <" + SKOS.BROADER + "> <" + broader.getURI() + "> }\n"
				+ "} WHERE {\n"
				+ "BIND(<" + scheme.getURI() + "> as ?scheme)\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.TOPCONCEPTOF + "> ?scheme}\n"
				+ "FILTER NOT EXISTS {?scheme <" + SKOS.HASTOPCONCEPT + "> ?concept }\n"
				+ "{ ?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1 . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> ?broaderConcept1 .\n"
				+ "FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.INSCHEME + "> ?scheme  . }\n"
				+ "} {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "?broaderConcept2 <" + SKOS.NARROWER + "> ?concept .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.INSCHEME + "> ?scheme . }\n"
				+ "} }";
		logger.info("query [setBroaderForAllDangling]:\n" + q);
		OWLModel model = getOWLModel();
		Update update = model.createUpdateQuery(q);
		update.evaluate(false);
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
	public Response removeAllDanglingFromScheme(ARTURIResource scheme) 
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException, QueryEvaluationException {
		String q = "DELETE { ?concept <" + SKOS.INSCHEME + "> <" + scheme.getURI() + "> }\n"
				+ "WHERE {\n"
				+ "BIND(<" + scheme.getURI() + "> as ?scheme)\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.TOPCONCEPTOF + "> ?scheme}\n"
				+ "FILTER NOT EXISTS {?scheme <" + SKOS.HASTOPCONCEPT + "> ?concept }\n"
				+ "{ ?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1 . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> ?broaderConcept1 .\n"
				+ "FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.INSCHEME + "> ?scheme  . }\n"
				+ "} {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "?broaderConcept2 <" + SKOS.NARROWER + "> ?concept .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.INSCHEME + "> ?scheme . }\n"
				+ "}\n}";
		logger.info("query [removeAllDanglingFromScheme]:\n" + q);
		OWLModel model = getOWLModel();
		Update update = model.createUpdateQuery(q);
		update.evaluate(false);
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
	public Response deleteAllDanglingConcepts(ARTURIResource scheme) 
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException, QueryEvaluationException {
		String q = "DELETE { "
				+ "?concept ?p1 ?o .\n"
				+ "?s ?p2 ?concept \n"
				+ "} WHERE {\n"
				+ "BIND(<" + scheme.getURI() + "> as ?scheme)\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.TOPCONCEPTOF + "> ?scheme}\n"
				+ "FILTER NOT EXISTS {?scheme <" + SKOS.HASTOPCONCEPT + "> ?concept }\n"
				+ "OPTIONAL { ?concept ?p1 ?o . }\n"
				+ "OPTIONAL { ?s ?p2 ?concept . }\n"
				+ "{ ?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1 . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> ?broaderConcept1 .\n"
				+ "FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.INSCHEME + "> ?scheme  . }\n"
				+ "} {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }\n"
				+ "} UNION {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme .\n"
				+ "?broaderConcept2 <" + SKOS.NARROWER + "> ?concept .\n"
				+ "FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.INSCHEME + "> ?scheme . }\n"
				+ "}\n}";
		logger.info("query [deleteAllDanglingConcepts]:\n" + q);
		OWLModel model = getOWLModel();
		Update update = model.createUpdateQuery(q);
		update.evaluate(false);
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
	public Response addAllConceptsToScheme(ARTURIResource scheme) 
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException, QueryEvaluationException {
		String q = "INSERT {\n"
				+ "GRAPH <" + getWorkingGraph().getNominalValue() + ">\n"
				+ "{ ?concept <" + SKOS.INSCHEME + "> <" + scheme.getURI() + "> }\n"
				+ "} WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "FILTER NOT EXISTS { ?concept <" + SKOS.INSCHEME + "> ?scheme . } }";
		logger.info("query [addAllConceptsToScheme]:\n" + q);
		OWLModel model = getOWLModel();
		Update update = model.createUpdateQuery(q);
		update.evaluate(false);
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
	public Response removeBroadersToConcept(ARTURIResource concept, ARTURIResource scheme)
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException, QueryEvaluationException {
		String q = "DELETE {\n"
				+ "?concept <" + SKOS.BROADER + "> ?broader .\n"
				+ "?broader <" + SKOS.NARROWER + "> ?concept .\n"
				+ "} WHERE {\n"
				+ "BIND (<" + concept.getURI() + "> as ?concept) \n"
				+ "?concept <" + SKOS.TOPCONCEPTOF + "> | ^<" + SKOS.HASTOPCONCEPT + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?broader <" + SKOS.INSCHEME + "> ?scheme . }";
		logger.info("query [removeBroadersToConcept]:\n" + q);
		OWLModel model = getOWLModel();
		Update update = model.createUpdateQuery(q);
		update.evaluate(false);
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
	public Response removeBroadersToAllConcepts()  
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException, QueryEvaluationException {
		String q = "DELETE {\n"
				+ "?concept <" + SKOS.BROADER + "> ?broader .\n"
				+ "?broader <" + SKOS.NARROWER + "> ?concept .\n"
				+ "} WHERE {\n"
				+ "?concept <" + SKOS.TOPCONCEPTOF + "> | ^<" + SKOS.HASTOPCONCEPT + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?broader <" + SKOS.INSCHEME + "> ?scheme . }";
		logger.info("query [removeBroadersToAllConcepts]:\n" + q);
		OWLModel model = getOWLModel();
		Update update = model.createUpdateQuery(q);
		update.evaluate(false);
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
	public Response removeAllAsTopConceptsWithBroader() 
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException, QueryEvaluationException {
		String q = "DELETE {\n"
				+ "?concept <" + SKOS.TOPCONCEPTOF + "> ?scheme .\n"
				+ "?scheme <" + SKOS.HASTOPCONCEPT + "> ?concept .\n"
				+ "} WHERE {\n"
				+ "?concept <" + SKOS.TOPCONCEPTOF + "> | ^<" + SKOS.HASTOPCONCEPT + "> ?scheme .\n"
   				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
   				+ "?broader <" + SKOS.INSCHEME + "> ?scheme . }";
		logger.info("query [removeAllAsTopConceptsWithBroader]:\n" + q);
		OWLModel model = getOWLModel();
		Update update = model.createUpdateQuery(q);
		update.evaluate(false);
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
	public Response removeAllHierarchicalRedundancy() 
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException, QueryEvaluationException {
		String q = "DELETE {\n"
				+ "?narrower <" + SKOS.BROADER + "> ?broader .\n"
				+ "?broader <" + SKOS.NARROWER + "> ?narrower .\n"
				+ "} WHERE {\n"
				+ "?narrower <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "?narrower (<" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + ">)+ ?middle .\n"
				+ "?middle <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "FILTER(?narrower != ?middle) }";
		logger.info("query [removeAllHierarchicalRedundancy]:\n" + q);
		OWLModel model = getOWLModel();
		Update update = model.createUpdateQuery(q);
		update.evaluate(false);
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
	public Response deleteAllDanglingXLabel() 
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException, QueryEvaluationException {
		String q = "DELETE {\n"
				+ "?s ?p1 ?xlabel .\n"
				+ "?xlabel ?p2 ?o .\n"
				+ "} WHERE {\n"
				+ "?xlabel a <" + SKOSXL.LABEL + "> .\n"
				+ "OPTIONAL { ?s ?p1 ?xlabel . }\n"
				+ "OPTIONAL { ?xlabel ?p2 ?o . }\n"
				+ "FILTER NOT EXISTS {\n"
				+ "?concept <" + SKOSXL.PREFLABEL + "> | <" + SKOSXL.ALTLABEL + "> | <" + SKOSXL.HIDDENLABEL + "> ?xlabel.\n"
				+ "} }";
		logger.info("query [deleteAllDanglingXLabel]:\n" + q);
		OWLModel model = getOWLModel();
		Update update = model.createUpdateQuery(q);
		update.evaluate(false);
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
	public Response setDanglingXLabel(ARTURIResource concept, ARTURIResource xlabelPred, ARTResource xlabel)
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException, QueryEvaluationException {
		String q = "";
		if (xlabelPred.equals(SKOSXL.Res.PREFLABEL)) {
			q = "DELETE {\n"
					+ "<" + concept.getURI() + "> <" + SKOSXL.PREFLABEL + "> ?oldPrefLabel\n"
					+ "} INSERT {\n"
					+ "GRAPH <" + getWorkingGraph() + "> {\n"
					+ "<" + concept.getURI() + "> <" + SKOSXL.ALTLABEL + "> ?oldPrefLabel .\n"
					+ "<" + concept.getURI() + "> <" + SKOSXL.PREFLABEL + "> <" + xlabel.getNominalValue() + "> . }\n"
					+ "} WHERE {\nOPTIONAL {\n"
					+ "<" + concept.getURI() + "> <" + SKOSXL.PREFLABEL + "> ?oldPrefLabel \n"
					+ "} }";
		} else { //altLabel or hiddenLabel
			q = "INSERT DATA {\n"
					+ "GRAPH <" + getWorkingGraph() + "> {\n"
					+ "<" + concept.getURI() + "> <" + xlabelPred.getURI() + "> <" + xlabel.getNominalValue() + "> \n"
					+ "}\n}";
		}
		logger.info("query [setDanglingXLabel]:\n" + q);
		OWLModel model = getOWLModel();
		Update update = model.createUpdateQuery(q);
		update.evaluate(false);
		return createReplyResponse(RepliesStatus.ok);
	}

}
