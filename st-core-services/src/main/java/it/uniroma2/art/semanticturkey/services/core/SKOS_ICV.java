package it.uniroma2.art.semanticturkey.services.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.owlart.query.TupleBindingsIterator;
import it.uniroma2.art.owlart.query.TupleQuery;
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.owlart.vocabulary.SKOSXL;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

/**
 * Service that retrieve a list of dangling concepts. A dangling concept is a concept that has no broader
 * concept and is not a top concept of a scheme
 * @author Tiziano Lorenzetti
 *
 */

@GenerateSTServiceController
@Validated
@Component
public class SKOS_ICV extends STServiceAdapter {
	
	protected static Logger logger = LoggerFactory.getLogger(SKOS_ICV.class);
	
	//-----ICV ON CONCEPTS STRUCTURE-----
	
	/**
	 * Returns a list of records <concept-scheme>, where concept is a dangling skos:Concept, and scheme is the
	 * skos:ConceptScheme where concept is dangling 
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	public Response listDanglingConcepts() throws UnsupportedQueryLanguageException, ModelAccessException,
			MalformedQueryException, QueryEvaluationException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listDanglingConcepts",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept ?scheme WHERE{\n"
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
				+ "} }";
		logger.info("query [listDanglingConcepts]:\n" + q);
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
	 * Detects cyclic hierarchical relations. Returns a list of records top, n1, n2 where 
	 * top is likely the cause of the cycle, n1 and n2 are vertex that belong to the cycle
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	public Response listCyclicConcepts() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listCyclicConcepts",
				RepliesStatus.ok);
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
	public Response listConceptSchemesWithNoTopConcept() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptSchemeWithNoTopConcept",
				RepliesStatus.ok);
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
	public Response listConceptsWithNoScheme() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithNoScheme",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "FILTER NOT EXISTS {\n" 
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme . } }";
		logger.info("query [listConceptsWithNoScheme]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			XMLHelp.newElement(dataElement, "concept", concept);
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
	public Response listTopConceptsWithBroader() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listTopConceptsWithBroader",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT DISTINCT ?concept ?scheme WHERE {\n"
				+ "?concept <" + SKOS.TOPCONCEPTOF + "> | ^<" + SKOS.HASTOPCONCEPT + "> ?scheme .\n"
				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader .\n"
				+ "FILTER (EXISTS { ?broader <" + SKOS.INSCHEME + "> ?scheme . } ) }";
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
	public Response listHierarchicallyRedundantConcepts() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listHierarchicallyRedundantConcepts",
				RepliesStatus.ok);
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
	
	//-----ICV ON CONCEPTS LABELS-----
	
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
	public Response listConceptsWithSameSKOSPrefLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithSameSKOSPrefLabel",
				RepliesStatus.ok);
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
	public Response listConceptsWithSameSKOSXLPrefLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithSameSKOSXLPrefLabel",
				RepliesStatus.ok);
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
	public Response listConceptsWithOnlySKOSAltLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithOnlySKOSAltLabel",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept ?lang WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOS.ALTLABEL + "> ?alt .\n"
				+ "bind (lang(?alt) as ?lang) .\n"
				+ "FILTER NOT EXISTS {\n"
				+ "?concept <" + SKOS.PREFLABEL + "> ?pref .\n"
				+ "FILTER (lang(?pref) = ?lang) } }";
		logger.info("query [listConceptsWithOnlySKOSAltLabel]:\n" + q);
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
	 * Returns a list of records concept-lang, of concept that have a skosxl:altLabel for a lang but not a skosxl:prefLabel
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	public Response listConceptsWithOnlySKOSXLAltLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithOnlySKOSXLAltLabel",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept ?lang WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?concept <" + SKOSXL.ALTLABEL + "> ?alt .\n"
				+ "?alt <" + SKOSXL.LITERALFORM + "> ?literalFormAlt .\n"
				+ "bind (lang(?literalFormAlt) as ?lang) .\n"
				+ "FILTER NOT EXISTS {\n"
				+ "?concept <" + SKOSXL.PREFLABEL + "> ?pref .\n"
				+ "?pref <" + SKOSXL.LITERALFORM + "> ?literalFormPref .\n"
				+ "FILTER (lang(?literalFormPref) = ?lang) } }";
		logger.info("query [listConceptsWithOnlySKOSXLAltLabel]:\n" + q);
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
	 * Returns a list of concepts that have no a main label (rdfs:label, skos:prefLabel or skosxl:prefLabel) in any language
	 * @return
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws ModelAccessException 
	 * @throws UnsupportedQueryLanguageException 
	 */
	@GenerateSTServiceController
	public Response listConceptsWithNoLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException,
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithNoLabel",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "FILTER NOT EXISTS { {\n"
				+ "?concept <" + SKOS.PREFLABEL + "> ?prefLabel .\n"
				+ "} UNION {\n"
				+ "?concept <" + SKOSXL.PREFLABEL + "> ?prefLabel .\n"
				+ "} UNION {\n"
				+ "?concept <" + RDFS.LABEL + "> ?prefLabel . } } }";
		logger.info("query [listConceptsWithNoLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			XMLHelp.newElement(dataElement, "concept", concept);
		}
		return response;
	}
	
	/**
	 * Returns a list of conceptScheme that have no a main label (rdfs:label, skos:prefLabel or skosxl:prefLabel) in any language
	 * @return
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws ModelAccessException 
	 * @throws UnsupportedQueryLanguageException 
	 */
	@GenerateSTServiceController
	public Response listConceptSchemesWithNoLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException,
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptSchemesWithNoLabel",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?scheme WHERE {\n"
				+ "?scheme a <" + SKOS.CONCEPTSCHEME + "> .\n"
				+ "FILTER NOT EXISTS { {\n"
				+ "?scheme <" + SKOS.PREFLABEL + "> ?prefLabel .\n"
				+ "} UNION {\n"
				+ "?scheme <" + SKOSXL.PREFLABEL + "> ?prefLabel .\n"
				+ "} UNION {\n"
				+ "?scheme <" + RDFS.LABEL + "> ?prefLabel . } } }";
		logger.info("query [listConceptSchemesWithNoLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String scheme = tb.getBinding("scheme").getBoundValue().getNominalValue();
			XMLHelp.newElement(dataElement, "scheme", scheme);
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
	public Response listConceptsWithMultipleSKOSPrefLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithMultipleSKOSPrefLabel",
				RepliesStatus.ok);
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
	public Response listConceptsWithMultipleSKOSXLPrefLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithMultipleSKOSXLPrefLabel",
				RepliesStatus.ok);
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
	 * Returns a list of records concept-labelPred-label of that concept that have a skos label without languageTag
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	public Response listConceptsWithNoLanguageTagSKOSLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithNoLanguageTagSKOSLabel",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept ?labelPred ?label WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "{ bind(<" + SKOS.PREFLABEL + "> as ?labelPred)}\n"
				+ "UNION\n"
				+ "{bind(<" + SKOS.ALTLABEL + "> as ?labelPred)}\n"
				+ "?concept ?labelPred ?label .\n"
				+ "bind(lang(?label) as ?lang)\n"
				+ "FILTER (?lang = '') }";
		logger.info("query [listConceptsWithNoLanguageTagSKOSLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			String labelPred = tb.getBinding("labelPred").getBoundValue().getNominalValue();
			String label = tb.getBinding("label").getBoundValue().getNominalValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("concept", concept);
			recordElem.setAttribute("labelPred", labelPred);
			recordElem.setAttribute("label", label);
		}
		return response;
	}
	
	/**
	 * Returns a list of records concept-labelPred-xlabel-literal of that concept that have a skosxl:Label 
	 * without languageTag
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	/*MEMO: labelPred serve per tener traccia del tipo di label in caso di modifica da parte del client, idem
	* per xlabel che serve a tener traccia del bnode della label.
	* TODO: decidere se in fase di correzione eliminare il bnode e ricrearne uno nuovo o se eliminare solo
	* il legame literalForm - literal e ricreare solo quello
	*/
	@GenerateSTServiceController
	public Response listConceptsWithNoLanguageTagSKOSXLLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithNoLanguageTagSKOSXLLabel",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept ?labelPred ?label WHERE {\n"
				+ "?concept a <" + SKOS.CONCEPT + "> .\n"
				+ "?xlabel a <" + SKOSXL.LABEL + "> .\n"
				+ "?concept ?labelPred ?xlabel .\n"
				+ "?xlabel <" + SKOSXL.LITERALFORM + "> ?label .\n"
				+ "bind(lang(?label) as ?lang)\n"
				+ "FILTER (?lang = '') }";
		logger.info("query [listConceptsWithNoLanguageTagSKOSXLLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			String labelPred = tb.getBinding("labelPred").getBoundValue().getNominalValue();
			String label = tb.getBinding("label").getBoundValue().getNominalValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("concept", concept);
			recordElem.setAttribute("labelPred", labelPred);
			recordElem.setAttribute("label", label);
		}
		return response;
	}
	
	/**
	 * Returns a list of records concept-label-lang. A record like that means that the concept ?concept has 
	 * the same skos:prefLabel and skos:altLabel ?label in language ?lang
	 * @return
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 */
	@GenerateSTServiceController
	public Response listConceptsWithOverlappedSKOSLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithOverlappedSKOSLabel",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept ?label ?lang WHERE {\n"
				+ "?concept <" + SKOS.PREFLABEL + "> ?label .\n"
				+ "?concept <" + SKOS.ALTLABEL + "> ?label .\n"
				+ "bind(lang(?label) as ?lang) . }";
		logger.info("query [listConceptsWithOverlappedSKOSLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			String label = tb.getBinding("label").getBoundValue().getNominalValue();
			String lang = tb.getBinding("lang").getBoundValue().getNominalValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("concept", concept);
			recordElem.setAttribute("label", label);
			recordElem.setAttribute("lang", lang);
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
	public Response listConceptsWithOverlappedSKOSXLLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithOverlappedSKOSXLLabel",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept ?label ?lang WHERE {\n"
				+ "?concept <" + SKOSXL.PREFLABEL + "> ?xlabel1 .\n"
				+ "?concept <" + SKOSXL.ALTLABEL + "> ?xlabel2 .\n"
				+ "?xlabel1 <" + SKOSXL.LITERALFORM + "> ?label .\n"
				+ "?xlabel2 <" + SKOSXL.LITERALFORM + "> ?label .\n"
				+ "bind(lang(?label) as ?lang) . }";
		logger.info("query [listConceptsWithOverlappedSKOSXLLabel]:\n" + q);
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			String label = tb.getBinding("label").getBoundValue().getNominalValue();
			String lang = tb.getBinding("lang").getBoundValue().getNominalValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("concept", concept);
			recordElem.setAttribute("label", label);
			recordElem.setAttribute("lang", lang);
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
	public Response listConceptsWithExtraWhitespaceInSKOSLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithExtraWhitespaceInSKOSLabel",
				RepliesStatus.ok);
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
	public Response listConceptsWithExtraWhitespaceInSKOSXLLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithExtraWhitespaceInSKOSXLLabel",
				RepliesStatus.ok);
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

}
