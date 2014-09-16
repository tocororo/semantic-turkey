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
import it.uniroma2.art.owlart.query.Query;
import it.uniroma2.art.owlart.query.QueryLanguage;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.owlart.query.TupleBindingsIterator;
import it.uniroma2.art.owlart.query.TupleQuery;
import it.uniroma2.art.owlart.vocabulary.RDF;
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
		String q = "SELECT ?concept ?scheme WHERE{"
				+ " FILTER NOT EXISTS {?concept <" + SKOS.TOPCONCEPTOF + "> ?scheme}"
				+ " FILTER NOT EXISTS {?scheme <" + SKOS.HASTOPCONCEPT + "> ?concept }"
				+ " {"
				+ " ?concept a <" + SKOS.CONCEPT + "> ."
				+ " ?concept <" + SKOS.INSCHEME + "> ?scheme ."
				+ " FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1  . }"
				+ " } UNION {"
				+ " ?concept a <" + SKOS.CONCEPT + "> ."
				+ " ?concept <" + SKOS.INSCHEME + "> ?scheme ."
				+ " ?concept <" + SKOS.BROADER + "> ?broaderConcept1 ."
				+ " FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.INSCHEME + "> ?scheme  . }"
				+ " } {"
				+ " ?concept a <" + SKOS.CONCEPT + "> ."
				+ " ?concept <" + SKOS.INSCHEME + "> ?scheme ."
				+ " FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }"
				+ " } UNION {"
				+ " ?concept a <" + SKOS.CONCEPT + "> ."
				+ " ?concept <" + SKOS.INSCHEME + "> ?scheme ."
				+ " ?broaderConcept2 <" + SKOS.NARROWER + "> ?concept ."
				+ " FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.INSCHEME + "> ?scheme . }"
				+ " } }";
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
	 * Detects cyclic hierarchical relations. Returns a list of records topCyclicConcept-cyclicConcept where 
	 * both elements are part of the cycle, and topCyclicConcept is the concept that has a broader outside the
	 * cycle (in addition to the broader inside) and so, probably is the cause of the problem
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
		String q = "SELECT DISTINCT ?topCyclicConcept ?cyclicConcept WHERE { "
				+ "?topCyclicConcept (<" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + ">)+ ?cyclicConcept . "
				+ "?cyclicConcept (<" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + ">)+ ?topCyclicConcept . "
				+ "?topCyclicConcept (<" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + ">) ?broader . "
				+ "FILTER NOT EXISTS { "
				+ "?broader (<" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + ">)+ ?topCyclicConcept } }";
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String topCyclicConcept = tb.getBinding("topCyclicConcept").getBoundValue().getNominalValue();
			String cyclicConcept = tb.getBinding("cyclicConcept").getBoundValue().getNominalValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("topCyclicConcept", topCyclicConcept);
			recordElem.setAttribute("cyclicConcept", cyclicConcept);
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
		String q = "SELECT ?conceptScheme WHERE { "
				+ "?conceptScheme a <" + SKOS.CONCEPTSCHEME + "> . "
				+ "FILTER NOT EXISTS { { "
				+ "?conceptScheme <" + SKOS.HASTOPCONCEPT + "> ?topConcept . "
				+ "} UNION { "
				+ "?topConcept <" + SKOS.TOPCONCEPTOF + "> ?conceptScheme . } } }";
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
		String q = "SELECT ?concept WHERE { "
				+ "?concept a <" + SKOS.CONCEPT + "> . "
				+ "FILTER NOT EXISTS { "
				+ "?concept <" + SKOS.INSCHEME + "> ?scheme . } }";
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
	//TODO: controllo giusto? e se un concetto è top concept in uno scheme, ma ha broader in altri scheme?
	public Response listTopConceptsWithBroader() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listTopConceptsWithBroader",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept WHERE { "
				+ "?concept <" + SKOS.TOPCONCEPTOF + "> | ^<" + SKOS.HASTOPCONCEPT + "> ?scheme . "
				+ "?concept <" + SKOS.BROADER + "> | ^<" + SKOS.NARROWER + "> ?broader . }";
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
		String q = "SELECT ?concept1 ?concept2 ?label ?lang WHERE { "
				+ "?concept1 a <" + SKOS.CONCEPT + "> . "
				+ "?concept2 a <" + SKOS.CONCEPT + "> . "
				+ "?concept1 <" + SKOS.PREFLABEL + "> ?skoslabel . "
				+ "?concept2 <" + SKOS.PREFLABEL + "> ?skoslabel . "
				+ "bind(lang(?skoslabel) as ?lang) "
				+ "bind(str(?skoslabel) as ?label) "
				+ "FILTER (str(?concept1) < str(?concept2)) }";
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
		String q = "SELECT ?concept1 ?concept2 ?label ?lang WHERE { "
				+ "?concept1 a <" + SKOS.CONCEPT + "> . "
				+ "?concept2 a <" + SKOS.CONCEPT + "> . "
				+ "?concept1 <" + SKOSXL.PREFLABEL + "> ?xlabel1 . "
				+ "?concept2 <" + SKOSXL.PREFLABEL + "> ?xlabel2 . "
				+ "?xlabel1 <" + SKOSXL.LITERALFORM + "> ?lit1 . "
				+ "?xlabel2 <" + SKOSXL.LITERALFORM + "> ?lit2 . "
				+ "FILTER (?lit1 = ?lit2) "
				+ "FILTER (str(?concept1) < str(?concept2)) "
				+ "bind(lang(?lit1) as ?lang) "
				+ "bind(str(?lit1) as ?label) }";
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
		String q = "SELECT ?concept ?lang WHERE { "
				+ "?concept a <" + SKOS.CONCEPT + "> . "
				+ "?concept <" + SKOS.ALTLABEL + "> ?alt . "
				+ "bind (lang(?alt) as ?lang) . "
				+ "FILTER NOT EXISTS { "
				+ "?concept <" + SKOS.PREFLABEL + "> ?pref . "
				+ "FILTER (lang(?pref) = ?lang) } }";
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
		String q = "SELECT ?concept ?lang WHERE { "
				+ "?concept a <" + SKOS.CONCEPT + "> . "
				+ "?concept <" + SKOSXL.ALTLABEL + "> ?alt . "
				+ "?alt <" + SKOSXL.LITERALFORM + "> ?literalForm . "
				+ "bind (lang(?literalForm) as ?lang) . "
				+ "FILTER NOT EXISTS { "
				+ "?concept <" + SKOSXL.PREFLABEL + "> ?pref . "
				+ "?pref <" + SKOSXL.LITERALFORM + "> ?literalForm . "
				+ "FILTER (lang(?literalForm) = ?lang) } }";
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
	//TODO: unico servizio o separare? (1 per concept e 1 per scheme)
	public Response listConceptsWithNoLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException,
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithNoLabel",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept WHERE { "
				+ "?concept a <" + SKOS.CONCEPT + "> . "
				+ "FILTER NOT EXISTS { { "
				+ "?concept <" + SKOS.PREFLABEL + "> ?prefLabel . "
				+ "} UNION { "
				+ "?concept <" + SKOSXL.PREFLABEL + "> ?prefLabel . "
				+ "} UNION { "
				+ "?concept <" + RDFS.LABEL + "> ?prefLabel . } } }";
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
	//TODO: unico servizio o separare? (1 per concept e 1 per scheme)
	public Response listConceptSchemesWithNoLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException,
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptSchemesWithNoLabel",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?scheme WHERE { "
				+ "?scheme a <" + SKOS.CONCEPTSCHEME + "> . "
				+ "FILTER NOT EXISTS { { "
				+ "?scheme <" + SKOS.PREFLABEL + "> ?prefLabel . "
				+ "} UNION { "
				+ "?scheme <" + SKOSXL.PREFLABEL + "> ?prefLabel . "
				+ "} UNION { "
				+ "?scheme <" + RDFS.LABEL + "> ?prefLabel . } } }";
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
		String q = "SELECT DISTINCT ?concept ?lang WHERE { "
				+ "?concept a <" + SKOS.CONCEPT + "> . "
				+ "?concept <" + SKOS.PREFLABEL + "> ?label1. "
				+ "?concept <" + SKOS.PREFLABEL + "> ?label2. "
				+ "FILTER ( ?label1 != ?label2 && lang(?label1) = lang(?label2) ) "
				+ "bind(lang(?label1) as ?lang) }";
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
		String q = "SELECT DISTINCT ?concept ?lang WHERE { "
				+ "?concept a <" + SKOS.CONCEPT + "> . "
				+ "?concept <" + SKOSXL.PREFLABEL + "> ?label1 . "
				+ "?concept <" + SKOSXL.PREFLABEL + "> ?label2 . "
				+ "?label1 <" + SKOSXL.LITERALFORM + "> ?lit1 . "
				+ "?label2 <" + SKOSXL.LITERALFORM + "> ?lit2 . "
				+ "bind(lang(?lit1) as ?lang) "
				+ "FILTER ( ?label1 != ?label2 && lang(?lit1) = lang(?lit2) ) }";
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
	//MEMO: labelPred serve per tener traccia del tipo di label in caso di modifica da parte del client
	public Response listConceptsWithNoLanguageTagSKOSLabel() throws QueryEvaluationException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listConceptsWithNoLanguageTagSKOSLabel",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		String q = "SELECT ?concept ?labelPred ?label WHERE { "
				+ "?concept a <" + SKOS.CONCEPT + "> . "
				+ "?labelPred <" + RDFS.SUBPROPERTYOF + "> <" + RDFS.LABEL + "> . "
				+ "?concept ?labelPred ?label . "
				+ "bind(lang(?label) as ?lang) "
				+ "FILTER (?lang = '') }";
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
		String q = "SELECT ?concept ?labelPred ?literal WHERE { "
				+ "?concept a <" + SKOS.CONCEPT + "> . "
				+ "?xlabel a <" + SKOSXL.LABEL + "> . "
				+ "?concept ?labelPred ?xlabel . "
				+ "?xlabel <" + SKOSXL.LITERALFORM + "> ?literal . "
				+ "bind(lang(?literal) as ?lang) "
				+ "FILTER (?lang = '') }";
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			String labelPred = tb.getBinding("labelPred").getBoundValue().getNominalValue();
			String literal = tb.getBinding("literal").getBoundValue().getNominalValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("concept", concept);
			recordElem.setAttribute("labelPred", labelPred);
			recordElem.setAttribute("literal", literal);
		}
		return response;
	}
	
	/**
	 * Returns a list of records concept-labelPred1-labelPred2-label-lang. A record like that means that
	 * the concept ?concept has the same label ?label in language ?lang for the predicates ?labelPred1 and
	 * ?labelPred2 (e.g. http://baseuri.org#c_1, skos:prefLabel, skos:altLabel, concept, en) 
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
		String q = "SELECT ?concept ?labelPred1 ?labelPred2 ?label ?lang WHERE { "
				+ "?labelPred1 <" + RDFS.SUBPROPERTYOF + "> <" + RDFS.LABEL + "> . "
				+ "?labelPred2 <" + RDFS.SUBPROPERTYOF + "> <" + RDFS.LABEL + "> . "
				+ "?concept ?labelPred1 ?label1 . "
				+ "?concept ?labelPred2 ?label1 . "
				+ "FILTER ( (?labelPred1 = <" + SKOS.PREFLABEL + "> && ?labelPred2 = <" + SKOS.ALTLABEL + ">) || "
				+ "(?labelPred1 = <" + SKOS.PREFLABEL + "> && ?labelPred2 = <" + SKOS.HIDDENLABEL + ">) || "
				+ "(?labelPred1 = <" + SKOS.ALTLABEL + "> && ?labelPred2 = <" + SKOS.HIDDENLABEL + ">) ) "
				+ "bind(str(?label1) as ?label) . "
				+ "bind(lang(?label1) as ?lang) . }";
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			String label = tb.getBinding("label").getBoundValue().getNominalValue();
			String lang = tb.getBinding("lang").getBoundValue().getNominalValue();
			String labelPred1 = tb.getBinding("labelPred1").getBoundValue().getNominalValue();
			String labelPred2 = tb.getBinding("labelPred2").getBoundValue().getNominalValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("concept", concept);
			recordElem.setAttribute("labelPred1", labelPred1);
			recordElem.setAttribute("labelPred2", labelPred2);
			recordElem.setAttribute("label", label);
			recordElem.setAttribute("lang", lang);
		}
		return response;
	}
	
	/**
	 * Returns a list of records concept-labelPred1-labelPred2-label-lang. A record like that means that
	 * the concept ?concept has the same skosxl label ?label in language ?lang for the predicates ?labelPred1 and
	 * ?labelPred2 (e.g. http://baseuri.org#c_1, skosxl:prefLabel, skosxl:altLabel, concept, en) 
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
		String q = "SELECT ?concept ?labelPred1 ?labelPred2 ?label ?lang WHERE { { "
				+ "bind (<" + SKOSXL.PREFLABEL + "> as ?labelPred1) "
				+ "bind (<" + SKOSXL.ALTLABEL + "> as ?labelPred2) "
				+ "} UNION { "
				+ "bind (<" + SKOSXL.PREFLABEL + "> as ?labelPred1) "
				+ "bind (<" + SKOSXL.HIDDENLABEL + "> as ?labelPred2) "
				+ "} UNION { "
				+ "bind (<" + SKOSXL.ALTLABEL + "> as ?labelPred1) "
				+ "bind (<" + SKOSXL.HIDDENLABEL + "> as ?labelPred2) } "
				+ "?concept ?labelPred1 ?xlabel1 . "
				+ "?concept ?labelPred2 ?xlabel2 . "
				+ "?xlabel1 <" + SKOSXL.LITERALFORM + "> ?label1 . "
				+ "?xlabel2 <" + SKOSXL.LITERALFORM + "> ?label2 . "
				+ "FILTER ( ?label1 = ?label2 ) "
				+ "bind(str(?label1) as ?label) . "
				+ "bind(lang(?label1) as ?lang) . }";
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		TupleBindingsIterator itTupleBinding = query.evaluate(false);
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			String label = tb.getBinding("label").getBoundValue().getNominalValue();
			String lang = tb.getBinding("lang").getBoundValue().getNominalValue();
			String labelPred1 = tb.getBinding("labelPred1").getBoundValue().getNominalValue();
			String labelPred2 = tb.getBinding("labelPred2").getBoundValue().getNominalValue();
			Element recordElem = XMLHelp.newElement(dataElement, "record");
			recordElem.setAttribute("concept", concept);
			recordElem.setAttribute("labelPred1", labelPred1);
			recordElem.setAttribute("labelPred2", labelPred2);
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
		String q = "SELECT ?concept ?labelPred ?label ?lang WHERE { "
				+ "?labelPred <" + RDFS.SUBPROPERTYOF + "> <" + RDFS.LABEL + "> . "
				+ "?concept ?labelPred ?skoslabel . "
				+ "bind(str(?skoslabel) as ?label) "
				+ "FILTER (regex (?label, '^ +') || regex (?label, ' +$') || regex(?label, ' {2,}?')) "
				+ "bind(lang(?skoslabel) as ?lang) }";
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
		String q = "SELECT ?concept ?labelPred ?label ?lang WHERE { "
				+ "?xlabel a <" + SKOSXL.LABEL + "> . "
				+ "?concept ?labelPred ?xlabel . "
				+ "?xlabel <" + SKOSXL.LITERALFORM + "> ?litForm . "
				+ "bind(str(?litForm) as ?label) "
				+ "FILTER (regex (?label, '^ +') || regex (?label, ' +$') || regex(?label, ' {2,}?')) "
				+ "bind(lang(?litForm) as ?lang) }";
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
