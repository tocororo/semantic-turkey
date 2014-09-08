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
import it.uniroma2.art.owlart.vocabulary.SKOS;
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
	
	@GenerateSTServiceController
	public Response listDanglingConcepts() throws UnsupportedQueryLanguageException, ModelAccessException,
			MalformedQueryException, QueryEvaluationException {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("listDanglingConcepts",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		
		TupleBindingsIterator itTupleBinding = doQueryForDanglingConcepts();
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String concept = tb.getBinding("concept").getBoundValue().getNominalValue();
			String scheme = tb.getBinding("scheme").getBoundValue().getNominalValue();
			Element pairElem = XMLHelp.newElement(dataElement, "pair");
			pairElem.setAttribute("concept", concept);
			pairElem.setAttribute("scheme", scheme);
		}
		
		return response;
	}
	
	//TODO: useful or not? if not delete it
	public boolean isDanglingConcept(String concept) throws UnsupportedQueryLanguageException,
			ModelAccessException, MalformedQueryException, QueryEvaluationException{
		boolean dangling = false;
		TupleBindingsIterator itTupleBinding = doQueryForDanglingConcepts();
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String c = tb.getBinding("concept").getBoundValue().getNominalValue();
			if (c.equals(concept)){
				dangling = true;
				break;
			}
		}
		return dangling;
	}

	//TODO: useful or not? if not delete it
	public boolean isDanglingConcept(String concept, String scheme) throws UnsupportedQueryLanguageException,
			ModelAccessException, MalformedQueryException, QueryEvaluationException{
		boolean dangling = false;
		TupleBindingsIterator itTupleBinding = doQueryForDanglingConcepts();
		while (itTupleBinding.hasNext()){
			TupleBindings tb = itTupleBinding.next();
			String c = tb.getBinding("concept").getBoundValue().getNominalValue();
			if (c.equals(concept)){
				String s = tb.getBinding("scheme").getBoundValue().getNominalValue();
				if (s.equals(scheme)){
					dangling = true;
					break;
				}
			}
		}
		return dangling;
	}
	
	private TupleBindingsIterator doQueryForDanglingConcepts() throws UnsupportedQueryLanguageException,
			ModelAccessException, MalformedQueryException, QueryEvaluationException{
		String q = "SELECT ?concept ?scheme WHERE{"
				+ " FILTER NOT EXISTS {?concept <" + SKOS.TOPCONCEPTOF + "> ?scheme}"
				+ " FILTER NOT EXISTS {?scheme <" + SKOS.HASTOPCONCEPT + "> ?concept }"
				+ " {"
				+ " ?concept <" + RDF.TYPE + "> <" + SKOS.CONCEPT + "> ."
				+ " ?concept <" + SKOS.INSCHEME + "> ?scheme ."
				+ " FILTER NOT EXISTS {?concept <" + SKOS.BROADER + "> ?broaderConcept1  . }"
				+ " } UNION {"
				+ " ?concept <" + RDF.TYPE + "> <" + SKOS.CONCEPT + "> ."
				+ " ?concept <" + SKOS.INSCHEME + "> ?scheme ."
				+ " ?concept <" + SKOS.BROADER + "> ?broaderConcept1 ."
				+ " FILTER NOT EXISTS {?broaderConcept1 <" + SKOS.INSCHEME + "> ?scheme  . }"
				+ " } {"
				+ " ?concept <" + RDF.TYPE + "> <" + SKOS.CONCEPT + "> ."
				+ " ?concept <" + SKOS.INSCHEME + "> ?scheme ."
				+ " FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.NARROWER + "> ?concept . }"
				+ " } UNION {"
				+ " ?concept <" + RDF.TYPE + "> <" + SKOS.CONCEPT + "> ."
				+ " ?concept <" + SKOS.INSCHEME + "> ?scheme ."
				+ " ?broaderConcept2 <" + SKOS.NARROWER + "> ?concept ."
				+ " FILTER NOT EXISTS {?broaderConcept2 <" + SKOS.INSCHEME + "> ?scheme . }"
				+ " } }";
		OWLModel model = getOWLModel();
		TupleQuery query = model.createTupleQuery(q);
		return query.evaluate(false);
	}

}
