package it.uniroma2.art.semanticturkey.services.core;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;

import com.google.common.collect.Iterators;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.filter.ConceptsInSchemePredicate;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.SKOSModel;
import it.uniroma2.art.owlart.models.SKOSXLModel;
import it.uniroma2.art.owlart.navigation.ARTURIResourceIterator;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.owlart.query.TupleBindingsIterator;
import it.uniroma2.art.owlart.query.TupleQuery;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.semanticturkey.constraints.Existing;
import it.uniroma2.art.semanticturkey.exceptions.HTTPParameterUnspecifiedException;
import it.uniroma2.art.semanticturkey.exceptions.NonExistingRDFResourceException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.plugin.extpts.ServiceAdapter;
import it.uniroma2.art.semanticturkey.services.STServiceAdapterOLD;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

@GenerateSTServiceController
@Validated
@Component
public class TestPerformance  extends STServiceAdapterOLD {

	protected static Logger logger = LoggerFactory.getLogger(ServiceAdapter.class);
	
	
	@GenerateSTServiceController
	public Response getTest0(@Existing ARTURIResource concept) {
		OWLModel owlModel = getOWLModel();
		SKOSXLModel skosxlModel = null;
		
		if(owlModel instanceof SKOSModel){
			skosxlModel = (SKOSXLModel) owlModel;
		} else{
			throw new ClassCastException("The model should be a skos model");
		}
		

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		//long start = System.currentTimeMillis();
		try {
			ARTResource[] graphs = getUserNamedGraphs();
			// String query = "";
			// skosxlModel.createTupleQuery(query);

			long start = System.currentTimeMillis();
			
			// narrower concepts' list using OWLART APIs
			ARTURIResourceIterator unfilteredIt = skosxlModel.listNarrowerConcepts(concept, false, true,
					graphs);

			Element dataElement = response.getDataElement();
			Element extCollection = XMLHelp.newElement(dataElement, "collection");
			
			while (unfilteredIt.hasNext()) {
				ARTURIResource narrowerConcept = unfilteredIt.next();
				Element narrowerConceptElement = XMLHelp.newElement(extCollection, "narrowerConcept");
				narrowerConceptElement.setAttribute("uri", narrowerConcept.getURI());
			}
			
			long end = System.currentTimeMillis();
			String exectTime = String.valueOf(end - start);
			extCollection.setAttribute("execTime", exectTime);
			
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} 
		return response;
	}
	
	@GenerateSTServiceController
	public Response getTest1(@Existing ARTURIResource scheme, @Existing ARTURIResource concept) {
		OWLModel owlModel = getOWLModel();
		SKOSXLModel skosxlModel = null;
		
		if(owlModel instanceof SKOSModel){
			skosxlModel = (SKOSXLModel) owlModel;
		} else{
			throw new ClassCastException("The model should be a skos model");
		}
		

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		//long start = System.currentTimeMillis();
		try {
			ARTResource[] graphs = getUserNamedGraphs();
			// String query = "";
			// skosxlModel.createTupleQuery(query);

			long start = System.currentTimeMillis();
			
			// narrower concepts' list using OWLART APIs
			ARTURIResourceIterator unfilteredIt = skosxlModel.listNarrowerConcepts(concept, false, true,
					graphs);
			Iterator<ARTURIResource> it;
			it = Iterators.filter(unfilteredIt,
					ConceptsInSchemePredicate.getFilter(skosxlModel, scheme, graphs));

			Element dataElement = response.getDataElement();
			Element extCollection = XMLHelp.newElement(dataElement, "collection");
			
			while (it.hasNext()) {
				ARTURIResource narrowerConcept = it.next();
				Element narrowerConceptElement = XMLHelp.newElement(extCollection, "narrowerConcept");
				narrowerConceptElement.setAttribute("uri", narrowerConcept.getURI());
			}
			
			long end = System.currentTimeMillis();
			String exectTime = String.valueOf(end - start);
			extCollection.setAttribute("execTime", exectTime);
			
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} 
		return response;
	}
	
	@GenerateSTServiceController
	public Response getTest2(@Existing ARTURIResource scheme, @Existing ARTURIResource concept) {
		OWLModel owlModel = getOWLModel();
		SKOSXLModel skosxlModel = null;
		
		if(owlModel instanceof SKOSModel){
			skosxlModel = (SKOSXLModel) owlModel;
		} else{
			throw new ClassCastException("The model should be a skos model");
		}

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		//long start = System.currentTimeMillis();
		try {
			ARTResource[] graphs = getUserNamedGraphs();
			// String query = "";
			// skosxlModel.createTupleQuery(query);

			long start = System.currentTimeMillis();
			
			// narrower concepts' list using SPARQL
			String query = "SELECT ?narrowerConcept"+
			"\nWHERE {" +
			"\n{" +
			"\n?narrowerConcept <"+SKOS.BROADER+"> <"+concept.getURI()+"> ."+
			"\n}" +
			"\nUNION" +
			"\n{" +
			"\n<"+concept.getURI()+"> <"+SKOS.NARROWER+"> ?narrowerConcept ."+
			"\n}" +
			"\n?narrowerConcept <" +SKOS.INSCHEME+"> <"+scheme.getURI()+"> ."+
			"\n}";
			
			Element dataElement = response.getDataElement();
			Element extCollection = XMLHelp.newElement(dataElement, "collection");
			
			TupleQuery tupleQuery = skosxlModel.createTupleQuery(query);
			TupleBindingsIterator tupleIter = tupleQuery.evaluate(false);
			while(tupleIter.hasNext()){
				TupleBindings tupleBindings = tupleIter.getNext();
				ARTURIResource narrowerConcept = 
						tupleBindings.getBinding("narrowerConcept").getBoundValue().asURIResource();
				Element narrowerConceptElement = XMLHelp.newElement(extCollection, "narrowerConcept");
				narrowerConceptElement.setAttribute("uri", narrowerConcept.getURI());
			}
			tupleIter.close();
			
			long end = System.currentTimeMillis();
			String exectTime = String.valueOf(end - start);
			extCollection.setAttribute("time", exectTime);
			
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (UnsupportedQueryLanguageException e) {
			return logAndSendException(e);
		} catch (MalformedQueryException e) {
			return logAndSendException(e);
		} catch (QueryEvaluationException e) {
			return logAndSendException(e);
		}
		return response;
	}
	
	@GenerateSTServiceController
	public Response getTest3(@Existing ARTURIResource scheme, @Existing ARTURIResource concept) {
		OWLModel owlModel = getOWLModel();
		SKOSXLModel skosxlModel = null;
		
		if(owlModel instanceof SKOSModel){
			skosxlModel = (SKOSXLModel) owlModel;
		} else{
			throw new ClassCastException("The model should be a skos model");
		}

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		//long start = System.currentTimeMillis();
		try {
			ARTResource[] graphs = getUserNamedGraphs();
			// String query = "";
			// skosxlModel.createTupleQuery(query);

			
			long start = System.currentTimeMillis();
			
			// narrower concepts' list using SPARQL
			String query = "SELECT ?narrowerConcept"+
			"\nWHERE {" +
			"\n?narrowerConcept (<"+SKOS.BROADER+">|^<"+SKOS.NARROWER+">) <"+concept.getURI()+"> ."+
			"\n?narrowerConcept <" +SKOS.INSCHEME+"> <"+scheme.getURI()+"> ."+
			"\n}";
			
			Element dataElement = response.getDataElement();
			Element extCollection = XMLHelp.newElement(dataElement, "collection");
			
			TupleQuery tupleQuery = skosxlModel.createTupleQuery(query);
			TupleBindingsIterator tupleIter = tupleQuery.evaluate(false);
			while(tupleIter.hasNext()){
				TupleBindings tupleBindings = tupleIter.getNext();
				ARTURIResource narrowerConcept = 
						tupleBindings.getBinding("narrowerConcept").getBoundValue().asURIResource();
				Element narrowerConceptElement = XMLHelp.newElement(extCollection, "narrowerConcept");
				narrowerConceptElement.setAttribute("uri", narrowerConcept.getURI());
			}
			tupleIter.close();
			
			long end = System.currentTimeMillis();
			String exectTime = String.valueOf(end - start);
			extCollection.setAttribute("time", exectTime);
			
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (UnsupportedQueryLanguageException e) {
			return logAndSendException(e);
		} catch (MalformedQueryException e) {
			return logAndSendException(e);
		} catch (QueryEvaluationException e) {
			return logAndSendException(e);
		}
		return response;
	}
	
}
