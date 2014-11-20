package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.TransactionBasedModel;
import it.uniroma2.art.owlart.navigation.ARTNodeIterator;
import it.uniroma2.art.owlart.utilities.DataRefactoring;
import it.uniroma2.art.owlart.utilities.RDFIterators;
import it.uniroma2.art.semanticturkey.constraints.Existing;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.AutoRendering;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;

@GenerateSTServiceController
@Validated
@Component
public class Refactor extends STServiceAdapter {

	protected static Logger logger = LoggerFactory.getLogger(Refactor.class);
	
	public static class Req {
		public static String renameRequest = "rename";
		public static String replaceBaseURIRequest = "replaceBaseUri";
	}
	
	// Temporarily disabled, since we still have not automatic handling of domain objects
	//@GenerateSTServiceController
	@AutoRendering
	public Collection<STRDFNode> getPropertyValues(@Existing ARTResource subject, ARTURIResource predicate)
			throws ModelAccessException {
		OWLModel model = getOWLModel();
		ARTResource[] graphs;
		graphs = getUserNamedGraphs();
		ARTNodeIterator it = model.listValuesOfSubjPredPair(subject, predicate, true, graphs);

		Collection<ARTNode> explicitValues = RDFIterators.getCollectionFromIterator(model
				.listValuesOfSubjPredPair(subject, predicate, false, getWorkingGraph()));

		Collection<STRDFNode> values = STRDFNodeFactory.createEmptyNodeCollection();
		while (it.streamOpen()) {
			ARTNode next = it.getNext();
			boolean explicit;
			if (explicitValues.contains(next))
				explicit = true;
			else
				explicit = false;
			values.add(STRDFNodeFactory.createSTRDFNode(model, next, true, explicit, false)); // disables
																								// rendering
		}
		it.close();

		return values;
	}
	
	@GenerateSTServiceController
	public void removePropertyValue(@Existing ARTResource subject, @Existing ARTURIResource predicate, ARTNode object) throws ModelUpdateException {
		getOWLModel().deleteTriple(subject, predicate, object, getUserNamedGraphs());
	}
	
	@GenerateSTServiceController
	public Response changeResourceName(@Existing ARTResource oldResource, ARTResource newResource) throws 
			ModelAccessException, DuplicatedResourceException, ModelUpdateException {
		RDFModel ontModel = getOWLModel();
			if (ontModel.existsResource(newResource)){
				throw new DuplicatedResourceException("could not rename resource: " + 
						oldResource.getNominalValue() + " to: " + newResource.getNominalValue()+
						" because a resource with this name already exists in the ontology");
			}

		if (ontModel instanceof TransactionBasedModel)
			try {
				((TransactionBasedModel) ontModel).setAutoCommit(false);
			} catch (ModelUpdateException e1) {
				throw new ModelUpdateException("sorry, unable to commit changes to the data, try to " +
						"close the project and open it again");
			}

		try {
			ontModel.renameResource(oldResource.asURIResource(), newResource.getNominalValue());
		} catch (ModelUpdateException e1) {
			throw new ModelUpdateException(e1);
		}

		if (ontModel instanceof TransactionBasedModel) {
			try {
				((TransactionBasedModel) ontModel).setAutoCommit(true);
			} catch (ModelUpdateException e) {
				throw new ModelUpdateException("sorry, unable to commit changes to the data, try to " +
						"close the project and open it again");
			}
		}

		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(Req.renameRequest,
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element element = XMLHelp.newElement(dataElement, "UpdateResource");
		element.setAttribute("name", oldResource.getNominalValue());
		element.setAttribute("newname", newResource.getNominalValue());
		return response;
	}
	
	@GenerateSTServiceController
	public Response replaceBaseURI(String sourceBaseURI, String targetBaseURI, String graphArrayString) 
			throws ModelAccessException, ModelUpdateException {
		RDFModel ontModel = getOWLModel();
		
		ARTResource []graphs = null;
		if(graphArrayString!=null && graphArrayString.length()>0){
			String[] graphArray = graphArrayString.split("\\|_\\|");
			graphs = new ARTResource[graphArray.length];
			for(int i=0; i<graphArray.length; ++i){
				if(graphArray[i].equals(NodeFilters.MAINGRAPH.getNominalValue())){
					graphs[i] = NodeFilters.MAINGRAPH;
				} else{
					graphs[i] = ontModel.createURIResource(graphArray[i]);
				}
			}
		}
		try {
			if(sourceBaseURI!=null && sourceBaseURI.length()>0){
				DataRefactoring.replaceBaseuri(ontModel, sourceBaseURI, targetBaseURI, graphs);
			} else{
				sourceBaseURI = ontModel.getBaseURI();
				DataRefactoring.replaceBaseuri(ontModel, targetBaseURI, graphs);
			}
		} catch (ModelAccessException e){
			throw new ModelAccessException("sorry, unable to replace the baseuri, try to close the project " +
					"and open it again");
		} catch (ModelUpdateException e) {
			throw new ModelUpdateException("sorry, unable to replace the baseuri, try to close the project " +
					"and open it again");
		}
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(Req.renameRequest,
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element element = XMLHelp.newElement(dataElement, "changeResourceName");
		element.setAttribute("sourceBaseURI", sourceBaseURI);
		element.setAttribute("targetBaseURI", targetBaseURI);
		element.setAttribute("graphs", graphArrayString);
		return response;
	}

}
