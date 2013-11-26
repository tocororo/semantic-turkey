package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.navigation.ARTNodeIterator;
import it.uniroma2.art.owlart.utilities.RDFIterators;
import it.uniroma2.art.semanticturkey.constraints.Existing;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;

import java.util.Collection;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@GenerateSTServiceController
@Validated
@Component
public class Resource extends STServiceAdapter {

	@GenerateSTServiceController
	public Response getPropertyValues(@Existing ARTResource subject, ARTURIResource predicate) {
		OWLModel model = getOWLModel();
		ARTResource[] graphs;
		try {
			graphs = getUserNamedGraphs();
			ARTNodeIterator it = model.listValuesOfSubjPredPair(subject, predicate, true, graphs);

			Collection<ARTNode> explicitValues = RDFIterators.getCollectionFromIterator(model
					.listValuesOfSubjPredPair(subject, predicate, false, getWorkingGraph()));

			XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("getPropertyValues", RepliesStatus.ok);

			Collection<STRDFNode> values = STRDFNodeFactory.createEmptyNodeCollection();
			while (it.streamOpen()) {
				ARTNode next = it.getNext();
				boolean explicit;
				if (explicitValues.contains(next))
					explicit = true;
				else
					explicit = false;
				values.add(STRDFNodeFactory.createSTRDFNode(model, next, true, explicit, true));
			}
			it.close();
			RDFXMLHelp.addRDFNodes(response, values);
			return response;

		} catch (ModelAccessException e) {
			return logAndSendException(e);
		}
	}
	
	public ARTResource[] getUserNamedGraphs() {
		ARTResource[] graphs = {NodeFilters.ANY};
		return graphs;
	}
	
	public ARTResource getWorkingGraph() {
		return NodeFilters.MAINGRAPH;
	}
	
	// public Response getPropertyValues(String resourceName, String propertyName) {
	// OWLModel model = getOWLModel();
	// ARTResource[] graphs;
	// try {
	// graphs = getUserNamedGraphs();
	// ARTResource resource = retrieveExistingResource(model, resourceName, graphs);
	// ARTURIResource property = model.createURIResource(propertyName);
	// ARTNodeIterator it = model.listValuesOfSubjPredPair(resource, property, true, graphs);
	//
	// Collection<ARTNode> explicitValues = RDFIterators.getCollectionFromIterator(model
	// .listValuesOfSubjPredPair(resource, property, false, getWorkingGraph()));
	//
	// XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
	//
	// Collection<STRDFNode> values = STRDFNodeFactory.createEmptyNodeCollection();
	// while (it.streamOpen()) {
	// ARTNode next = it.getNext();
	// boolean explicit;
	// if (explicitValues.contains(next))
	// explicit = true;
	// else
	// explicit = false;
	// values.add(STRDFNodeFactory.createSTRDFNode(model, next, true, explicit, true));
	// }
	// it.close();
	// RDFXMLHelp.addRDFNodes(response, values);
	// return response;
	//
	// } catch (ModelAccessException e) {
	// return logAndSendException(e);
	// } catch (NonExistingRDFResourceException e) {
	// return logAndSendException(e);
	// }
	// }

}
