package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.exceptions.UnsupportedRDFFormatException;
import it.uniroma2.art.owlart.io.RDFFormat;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.BaseRDFTripleModel;
import it.uniroma2.art.owlart.models.ModelFactory;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.SKOSModel;
import it.uniroma2.art.owlart.models.SKOSXLModel;
import it.uniroma2.art.owlart.models.TransactionBasedModel;
import it.uniroma2.art.owlart.models.conf.ModelConfiguration;
import it.uniroma2.art.owlart.models.impl.SKOSModelImpl;
import it.uniroma2.art.owlart.navigation.ARTNodeIterator;
import it.uniroma2.art.owlart.utilities.DataRefactoring;
import it.uniroma2.art.owlart.utilities.RDFIterators;
import it.uniroma2.art.owlart.utilities.transform.ReifiedSKOSDefinitionsConverter;
import it.uniroma2.art.owlart.utilities.transform.ReifiedSKOSDefinitionsFlattener;
import it.uniroma2.art.owlart.utilities.transform.SKOS2SKOSXLConverter;
import it.uniroma2.art.owlart.utilities.transform.SKOSXL2SKOSConverter;
import it.uniroma2.art.semanticturkey.constraints.Existing;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.NonExistingRDFResourceException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectIncompatibleException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.AutoRendering;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.io.File;
import java.io.IOException;
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
		public static String renameResourceRequest = "renameResource";
		public static String replaceBaseURIRequest = "replaceBaseURI";
		public static String convertLabelsToSKOSXLRequest = "convertLabelsToSKOSXL";
		public static String exportWithSKOSLabelsRequest = "exportWithSKOSLabels";
		public static String reifySKOSDefinitionsRequest = "reifySKOSDefinitions";
		public static String exportWithFlatSKOSDefinitionsRequest = "exportWithFlatSKOSDefinitions";
		public static String exportWithTransformationsRequest = "exportWithTransformations";
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
	//in this method oldResource is String (instead ARTURIResource) to avoid checks performed by StringToARTUriResource converter 
	public Response renameResource(String oldResource, ARTResource newResource) throws 
			ModelAccessException, DuplicatedResourceException, ModelUpdateException, NonExistingRDFResourceException {
		RDFModel ontModel = getOWLModel();
		ARTURIResource oldURIResource = retrieveExistingURIResource(ontModel, oldResource);
			if (ontModel.existsResource(newResource)){
				throw new DuplicatedResourceException("could not rename resource: " + 
						oldURIResource.getNominalValue() + " to: " + newResource.getNominalValue()+
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
			ontModel.renameResource(oldURIResource, newResource.getNominalValue());
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

		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(Req.renameResourceRequest,
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element element = XMLHelp.newElement(dataElement, "UpdateResource");
		element.setAttribute("name", oldURIResource.getNominalValue());
		element.setAttribute("newname", newResource.getNominalValue());
		return response;
	}
	
	@GenerateSTServiceController
	public Response replaceBaseURI(@Optional String sourceBaseURI, String targetBaseURI, 
			@Optional String graphArrayString) 
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
		} else {
			graphs = new ARTResource[0];
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
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(Req.renameResourceRequest,
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element element = XMLHelp.newElement(dataElement, "changeResourceName");
		element.setAttribute("sourceBaseURI", sourceBaseURI);
		element.setAttribute("targetBaseURI", targetBaseURI);
		element.setAttribute("graphs", graphArrayString);
		return response;
	}
	
	/**
	 * Converts the SKOS labels in SKOSXL labels. In the underlying model the SKOS labels are deleted and
	 * replaced by the SKOS-XL produced. 
	 * This service should be invoked only from SKOSXL projects. It's supposed that should be some client-side
	 * check to avoid exceptions.
	 * @param copyAlsoSKOSLabels Specifies whether in the model the skos labels will be preserved or not. 
	 * @return
	 * @throws ModelAccessException
	 * @throws ModelUpdateException
	 * @throws ProjectIncompatibleException 
	 */
	@GenerateSTServiceController
	public Response convertLabelsToSKOSXL(@Optional(defaultValue="false") boolean copyAlsoSKOSLabels)
			throws ModelAccessException, ModelUpdateException, ProjectIncompatibleException {
		OWLModel owlModel = getOWLModel();
		if (owlModel instanceof SKOSXLModel){
			SKOSXLModel xlModel = (SKOSXLModel) owlModel;
			SKOS2SKOSXLConverter.convert(xlModel, xlModel, true, copyAlsoSKOSLabels);
			XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(Req.convertLabelsToSKOSXLRequest,
					RepliesStatus.ok);
			return response;
		} else {
			throw new ProjectIncompatibleException("Unable to perform the conversion on a non-SKOSXL model");
		}
	}
	
	/**
	 * Exports the underlying project model after converting the SKOSXL labels in SKOS labels. This service
	 * should be invoked only from SKOSXL projects. It's supposed that should be some client-side check to
	 * avoid exceptions.
	 * @param exportPackage
	 * @param copyAlsoSKOSXLabels Specifies whether in the exported model the skosxl labels will be 
	 * preserved or not. 
	 * @return
	 * @throws ModelAccessException
	 * @throws ModelUpdateException
	 * @throws ProjectIncompatibleException 
	 * @throws ProjectInconsistentException 
	 * @throws UnavailableResourceException 
	 * @throws UnsupportedRDFFormatException 
	 * @throws IOException 
	 */
	@GenerateSTServiceController
	public Response exportWithSKOSLabels(String exportPackage, boolean copyAlsoSKOSXLabels)
			throws ModelAccessException, ModelUpdateException, ProjectIncompatibleException, 
			UnavailableResourceException, ProjectInconsistentException, IOException {
		OWLModel owlModel = getOWLModel();
		if (owlModel instanceof SKOSXLModel){//source model must be skosxl since it should contain skosxl labels	
			SKOSXLModel sourceXLModel = (SKOSXLModel) owlModel;
			ModelFactory<ModelConfiguration> ontFact = PluginManager.getOntManagerImpl(getProject().getOntologyManagerImplID()).createModelFactory();
			BaseRDFTripleModel ligthWeigth = ontFact.createLightweightRDFModel();
			SKOSModel tempTargetModel = new SKOSModelImpl(ligthWeigth);
			SKOSXL2SKOSConverter.convert(sourceXLModel, tempTargetModel, copyAlsoSKOSXLabels);
			try {
				tempTargetModel.writeRDF(new File(exportPackage), RDFFormat.RDFXML, NodeFilters.MAINGRAPH);
			} catch (UnsupportedRDFFormatException e) {
				e.printStackTrace();
			}
			tempTargetModel.close();
			XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(Req.exportWithSKOSLabelsRequest,
					RepliesStatus.ok);
			return response;
		} else {
			throw new ProjectIncompatibleException("Unable to perform the conversion on a non-SKOSXL model");
		}
	}
	
	/**
	 * Reifies flat <code>skos:definition</code>. This service should be invoked only from SKOS or 
	 * SKOSXL projects. It's supposed that should be some client-side checks to avoid exceptions.
	 * @param copyAlsoPlainDefinitions Specifies whether in the model the flat notes will be preserved or not. 
	 * @return
	 * @throws ModelAccessException
	 * @throws ModelUpdateException
	 * @throws ProjectIncompatibleException 
	 */
	@GenerateSTServiceController
	public Response reifySKOSDefinitions(@Optional(defaultValue="false") boolean copyAlsoPlainDefinitions)
			throws ModelAccessException, ModelUpdateException, ProjectIncompatibleException {
		OWLModel owlModel = getOWLModel();
		if (owlModel instanceof SKOSModel){//source model must be at least skos since it should contain skos definitions
			SKOSModel model = (SKOSModel) owlModel;
			ReifiedSKOSDefinitionsConverter.convert(model, model, true, true, copyAlsoPlainDefinitions);
			XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(Req.reifySKOSDefinitionsRequest,
					RepliesStatus.ok);
			return response;
		} else {
			throw new ProjectIncompatibleException("Unable to perform the conversion on a non-SKOS model");
		}
	}
	
	/**
	 * Exports the underlying project model after flattering the <code>skos:definition</code>. This service 
	 * should be invoked only from SKOS or SKOSXL projects. It's supposed that should be some client-side
	 * check to avoid exceptions.
	 * @param exportPackage
	 * @param copyAlsoReifiedDefinition Specifies whether in the exported model the reified notes
	 * will be preserved or not.
	 * @return
	 * @throws ModelAccessException
	 * @throws ModelUpdateException
	 * @throws ProjectIncompatibleException 
	 * @throws ProjectInconsistentException 
	 * @throws UnavailableResourceException 
	 * @throws UnsupportedRDFFormatException 
	 * @throws IOException 
	 */
	@GenerateSTServiceController
	public Response exportWithFlatSKOSDefinitions(String exportPackage, @Optional(defaultValue="false") boolean copyAlsoReifiedDefinition) 
			throws ModelAccessException, ModelUpdateException, ProjectIncompatibleException, 
			UnavailableResourceException, ProjectInconsistentException, IOException, UnsupportedRDFFormatException {
		OWLModel owlModel = getOWLModel();
		if (owlModel instanceof SKOSModel){//source model must be at least skos since it should contain skos definitions
			SKOSModel sourceModel = (SKOSModel) owlModel;
			ModelFactory<ModelConfiguration> ontFact = PluginManager.getOntManagerImpl(getProject().getOntologyManagerImplID()).createModelFactory();
			BaseRDFTripleModel ligthWeigth = ontFact.createLightweightRDFModel();
			SKOSModel tempTargetModel = new SKOSModelImpl(ligthWeigth);
			ReifiedSKOSDefinitionsFlattener.convert(sourceModel, tempTargetModel, copyAlsoReifiedDefinition);
			tempTargetModel.writeRDF(new File(exportPackage), RDFFormat.RDFXML, NodeFilters.MAINGRAPH);
			tempTargetModel.close();
			XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(Req.exportWithFlatSKOSDefinitionsRequest,
					RepliesStatus.ok);
			return response;
		} else {
			throw new ProjectIncompatibleException("Unable to perform the conversion on a non-SKOS model");
		}
	}
	
	/**
	 * Exports the underlying project model after flattering the <code>skos:definition</code> and
	 * converting SKOSXL labels to SKOS. This service should be invoked only from SKOS or SKOSXL
	 * projects. It's supposed that should be some client-side check to avoid exceptions.
	 * 
	 * @param exportPackage
	 * @param copyAlsoSKOSXLabels Specifies whether in the exported model the skosxl labels will be 
	 * preserved or not. 
	 * @param copyAlsoReifiedDefinition Specifies whether in the exported model the reified notes 
	 * will be preserved or not. 
	 * @return
	 * @throws UnavailableResourceException
	 * @throws ProjectInconsistentException
	 * @throws ProjectIncompatibleException
	 * @throws ModelAccessException
	 * @throws ModelUpdateException
	 * @throws IOException
	 * @throws UnsupportedRDFFormatException
	 */
	@GenerateSTServiceController
	public Response exportWithTransformations(String exportPackage, @Optional(defaultValue="false") boolean copyAlsoSKOSXLabels,
			@Optional(defaultValue="false") boolean copyAlsoReifiedDefinition) 
			throws UnavailableResourceException, ProjectInconsistentException, ProjectIncompatibleException,
			ModelAccessException, ModelUpdateException, IOException, UnsupportedRDFFormatException {
		OWLModel owlModel = getOWLModel();
		if (owlModel instanceof SKOSXLModel){//source model must be skosxl since it should contain skosxl labels
			SKOSXLModel sourceModel = (SKOSXLModel) owlModel;
			ModelFactory<ModelConfiguration> ontFact = PluginManager.getOntManagerImpl(getProject().getOntologyManagerImplID()).createModelFactory();
			BaseRDFTripleModel ligthWeigth = ontFact.createLightweightRDFModel();
			SKOSModel tempTargetModel = new SKOSModelImpl(ligthWeigth);
			SKOSXL2SKOSConverter.convert(sourceModel, tempTargetModel, copyAlsoSKOSXLabels);
			ReifiedSKOSDefinitionsFlattener.convert(tempTargetModel, tempTargetModel, copyAlsoReifiedDefinition);
			tempTargetModel.writeRDF(new File(exportPackage), RDFFormat.RDFXML, NodeFilters.MAINGRAPH);
			tempTargetModel.close();
			XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(Req.exportWithTransformationsRequest ,
					RepliesStatus.ok);
			return response;
		} else {
			throw new ProjectIncompatibleException("Unable to perform the conversion on a non-SKOSXL model");
		}
	}
}
