package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.w3c.dom.Element;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.exceptions.UnsupportedRDFFormatException;
import it.uniroma2.art.owlart.io.RDFFormat;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
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
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.Update;
import it.uniroma2.art.owlart.utilities.DataRefactoring;
import it.uniroma2.art.owlart.utilities.RDFIterators;
import it.uniroma2.art.owlart.utilities.transform.ReifiedSKOSDefinitionsConverter;
import it.uniroma2.art.owlart.utilities.transform.ReifiedSKOSDefinitionsFlattener;
import it.uniroma2.art.owlart.utilities.transform.SKOS2SKOSXLConverter;
import it.uniroma2.art.owlart.utilities.transform.SKOSXL2SKOSConverter;
import it.uniroma2.art.owlart.vocabulary.VocabUtilities;
import it.uniroma2.art.semanticturkey.constraints.Existing;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.NonExistingRDFResourceException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectIncompatibleException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFURI;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.AutoRendering;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

@GenerateSTServiceController
@Validated
@Component
@Controller // just for exportByFlattening service
public class Refactor extends STServiceAdapter {

	protected static Logger logger = LoggerFactory.getLogger(Refactor.class);

	// Temporarily disabled, since we still have not automatic handling of domain objects
	// @GenerateSTServiceController
	@AutoRendering
	public Collection<STRDFNode> getPropertyValues(@Existing ARTResource subject, ARTURIResource predicate)
			throws ModelAccessException {
		OWLModel model = getOWLModel();
		ARTResource[] graphs;
		graphs = getUserNamedGraphs();
		ARTNodeIterator it = model.listValuesOfSubjPredPair(subject, predicate, true, graphs);

		Collection<ARTNode> explicitValues = RDFIterators.getCollectionFromIterator(
				model.listValuesOfSubjPredPair(subject, predicate, false, getWorkingGraph()));

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
	public void removePropertyValue(@Existing ARTResource subject, @Existing ARTURIResource predicate,
			ARTNode object) throws ModelUpdateException {
		getOWLModel().deleteTriple(subject, predicate, object, getUserNamedGraphs());
	}

	@GenerateSTServiceController
	// in this method oldResource is String (instead ARTURIResource) to avoid checks performed by
	// StringToARTUriResource converter
	public Response renameResource(String oldResource, ARTResource newResource) throws ModelAccessException,
			DuplicatedResourceException, ModelUpdateException, NonExistingRDFResourceException {
		RDFModel ontModel = getOWLModel();
		ARTURIResource oldURIResource = retrieveExistingURIResource(ontModel, oldResource);
		if (ontModel.existsResource(newResource)) {
			throw new DuplicatedResourceException("could not rename resource: "
					+ oldURIResource.getNominalValue() + " to: " + newResource.getNominalValue()
					+ " because a resource with this name already exists in the ontology");
		}

		if (ontModel instanceof TransactionBasedModel)
			try {
				((TransactionBasedModel) ontModel).setAutoCommit(false);
			} catch (ModelUpdateException e1) {
				throw new ModelUpdateException("sorry, unable to commit changes to the data, try to "
						+ "close the project and open it again");
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
				throw new ModelUpdateException("sorry, unable to commit changes to the data, try to "
						+ "close the project and open it again");
			}
		}

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element element = XMLHelp.newElement(dataElement, "UpdateResource");
		element.setAttribute("name", oldURIResource.getNominalValue());
		element.setAttribute("newname", newResource.getNominalValue());
		return response;
	}

	// This method does the EXACT same thing as renameResource , the only difference is the response
	@GenerateSTServiceController
	// in this method oldResource is String (instead ARTURIResource) to avoid checks performed by
	// StringToARTUriResource converter
	public Response changeResourceURI(String oldResource, String newResource) throws ModelAccessException,
			DuplicatedResourceException, ModelUpdateException, NonExistingRDFResourceException {
		RDFModel ontModel = getOWLModel();
		ARTURIResource oldURIResource = retrieveExistingURIResource(ontModel, oldResource);
		ARTURIResource newURIResource = ontModel.createURIResource(newResource);

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element oldElement = XMLHelp.newElement(dataElement, "oldResource");
		STRDFURI oldStrdfuri = STRDFNodeFactory.createSTRDFURI(ontModel, oldURIResource, true, true, true);
		RDFXMLHelp.addRDFNode(oldElement, oldStrdfuri);

		if (ontModel.existsResource(newURIResource)) {
			throw new DuplicatedResourceException("could not rename resource: "
					+ oldURIResource.getNominalValue() + " to: " + newURIResource.getNominalValue()
					+ " because a resource with this name already exists in the ontology");
		}

		if (ontModel instanceof TransactionBasedModel)
			try {
				((TransactionBasedModel) ontModel).setAutoCommit(false);
			} catch (ModelUpdateException e1) {
				throw new ModelUpdateException("sorry, unable to commit changes to the data, try to "
						+ "close the project and open it again");
			}

		try {
			ontModel.renameResource(oldURIResource, newURIResource.getNominalValue());
		} catch (ModelUpdateException e1) {
			throw new ModelUpdateException(e1);
		}

		if (ontModel instanceof TransactionBasedModel) {
			try {
				((TransactionBasedModel) ontModel).setAutoCommit(true);
			} catch (ModelUpdateException e) {
				throw new ModelUpdateException("sorry, unable to commit changes to the data, try to "
						+ "close the project and open it again");
			}
		}
		Element newElement = XMLHelp.newElement(dataElement, "newResource");
		STRDFURI newStrdfuri = STRDFNodeFactory.createSTRDFURI(ontModel, newURIResource, true, true, true);
		RDFXMLHelp.addRDFNode(newElement, newStrdfuri);

		/*
		 * Element element = XMLHelp.newElement(dataElement, "UpdateResource"); element.setAttribute("name",
		 * oldURIResource.getNominalValue()); element.setAttribute("newname",
		 * newURIResource.getNominalValue());
		 */
		return response;
	}

	@GenerateSTServiceController
	public Response replaceBaseURI(@Optional String sourceBaseURI, String targetBaseURI,
			@Optional String graphArrayString) throws ModelAccessException, ModelUpdateException {
		RDFModel ontModel = getOWLModel();

		ARTResource[] graphs = null;
		if (graphArrayString != null && graphArrayString.length() > 0) {
			String[] graphArray = graphArrayString.split("\\|_\\|");
			graphs = new ARTResource[graphArray.length];
			for (int i = 0; i < graphArray.length; ++i) {
				if (graphArray[i].equals(NodeFilters.MAINGRAPH.getNominalValue())) {
					graphs[i] = NodeFilters.MAINGRAPH;
				} else {
					graphs[i] = ontModel.createURIResource(graphArray[i]);
				}
			}
		} else {
			graphs = new ARTResource[0];
		}
		try {
			if (sourceBaseURI != null && sourceBaseURI.length() > 0) {
				DataRefactoring.replaceBaseuri(ontModel, sourceBaseURI, targetBaseURI, graphs);
			} else {
				sourceBaseURI = ontModel.getBaseURI();
				DataRefactoring.replaceBaseuri(ontModel, targetBaseURI, graphs);
			}
		} catch (ModelAccessException e) {
			throw new ModelAccessException(
					"sorry, unable to replace the baseuri, try to close the project " + "and open it again");
		} catch (ModelUpdateException e) {
			throw new ModelUpdateException(
					"sorry, unable to replace the baseuri, try to close the project " + "and open it again");
		}
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element element = XMLHelp.newElement(dataElement, "changeResourceName");
		element.setAttribute("sourceBaseURI", sourceBaseURI);
		element.setAttribute("targetBaseURI", targetBaseURI);
		element.setAttribute("graphs", graphArrayString);
		return response;
	}

	/**
	 * Converts the SKOS labels in SKOSXL labels. In the underlying model the SKOS labels are deleted and
	 * replaced by the SKOS-XL produced. This service should be invoked only from SKOSXL projects. It's
	 * supposed that should be some client-side check to avoid exceptions.
	 * 
	 * @param copyAlsoSKOSLabels
	 *            Specifies whether in the model the skos labels will be preserved or not.
	 * @return
	 * @throws ModelAccessException
	 * @throws ModelUpdateException
	 * @throws ProjectIncompatibleException
	 */
	@GenerateSTServiceController
	public Response convertLabelsToSKOSXL(@Optional(defaultValue = "false") boolean copyAlsoSKOSLabels)
			throws ModelAccessException, ModelUpdateException, ProjectIncompatibleException {
		OWLModel owlModel = getOWLModel();
		if (owlModel instanceof SKOSXLModel) {
			SKOSXLModel xlModel = (SKOSXLModel) owlModel;
			SKOS2SKOSXLConverter.convert(xlModel, xlModel, true, copyAlsoSKOSLabels);
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
			return response;
		} else {
			throw new ProjectIncompatibleException("Unable to perform the conversion on a non-SKOSXL model");
		}
	}

	/**
	 * Reifies flat <code>skos:definition</code>. This service should be invoked only from SKOS or SKOSXL
	 * projects. It's supposed that should be some client-side checks to avoid exceptions.
	 * 
	 * @param copyAlsoPlainDefinitions
	 *            Specifies whether in the model the flat notes will be preserved or not.
	 * @return
	 * @throws ModelAccessException
	 * @throws ModelUpdateException
	 * @throws ProjectIncompatibleException
	 */
	@GenerateSTServiceController
	public Response reifySKOSDefinitions(@Optional(defaultValue = "false") boolean copyAlsoPlainDefinitions)
			throws ModelAccessException, ModelUpdateException, ProjectIncompatibleException {
		OWLModel owlModel = getOWLModel();
		if (owlModel instanceof SKOSModel) {// source model must be at least skos since it should contain skos
											// definitions
			SKOSModel model = (SKOSModel) owlModel;
			ReifiedSKOSDefinitionsConverter.convert(model, model, true, true, copyAlsoPlainDefinitions);
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
			return response;
		} else {
			throw new ProjectIncompatibleException("Unable to perform the conversion on a non-SKOS model");
		}
	}

	/**
	 * Exports the underlying project model after flattering the <code>skos:definition</code> and converting
	 * SKOSXL labels to SKOS. This service should be invoked only from SKOS or SKOSXL projects. It's supposed
	 * that should be some client-side check to avoid exceptions.
	 * 
	 * @param oRes
	 * @param ext
	 *            desired extension
	 * @param format
	 *            Determines the serialization format and the extension (if ext is not provided) available
	 *            export format: RDF/XML, RDF/XML-ABBREV, N-TRIPLES, N3, TURTLE, TRIG, TRIX, TRIX-EXT, NQUADS
	 * @param toSKOS
	 *            True if skosxl:Label(s) should be converted to SKOS labels
	 * @param keepSKOSXLabels
	 *            True if the exported model should preserves the skosxl labels
	 * @param toFlatDefinitions
	 *            True if the reified definitions should be flattened
	 * @param keepReifiedDefinition
	 *            True if the exported model should preserves the reified notes
	 * @throws UnavailableResourceException
	 * @throws ProjectInconsistentException
	 * @throws ProjectIncompatibleException
	 * @throws ModelAccessException
	 * @throws ModelUpdateException
	 * @throws IOException
	 * @throws UnsupportedRDFFormatException
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Refactor/exportByFlattening", method = org.springframework.web.bind.annotation.RequestMethod.GET)
	public void exportByFlattening(HttpServletResponse oRes,
			@RequestParam(value = "format", required = false) String format,
			@RequestParam(value = "ext", required = false) String ext,
			@RequestParam(value = "toSKOS", required = false, defaultValue = "true") boolean toSKOS,
			@RequestParam(value = "keepSKOSXLabels", required = false, defaultValue = "false") boolean keepSKOSXLabels,
			@RequestParam(value = "toFlatDefinitions", required = false, defaultValue = "true") boolean toFlatDefinitions,
			@RequestParam(value = "keepReifiedDefinition", required = false, defaultValue = "false") boolean keepReifiedDefinition)
					throws UnavailableResourceException, ProjectInconsistentException,
					ProjectIncompatibleException, ModelAccessException, ModelUpdateException, IOException,
					UnsupportedRDFFormatException {

		OWLModel owlModel = getOWLModel();
		if (owlModel instanceof SKOSXLModel) {// source model must be skosxl since it should contain skosxl
												// labels
			SKOSXLModel model = (SKOSXLModel) owlModel;

			File tempServerFile;
			RDFFormat rdfFormat = RDFFormat.parseFormat(format);
			if (rdfFormat == null) { // format not provided or unparsable
				if (ext == null) {
					rdfFormat = RDFFormat.RDFXML; // default format
					ext = RDFFormat.getFormatExtensions(rdfFormat)[0];
				} else { // ext provided -> guess format
					rdfFormat = RDFFormat.guessRDFFormatFromFile(new File("file." + ext));
					if (rdfFormat == null) { // the given ext is not valid -> default format
						rdfFormat = RDFFormat.RDFXML; // default format
						ext = RDFFormat.getFormatExtensions(rdfFormat)[0];
					}
				}
			} else { // valid format provided
				if (ext == null) {
					ext = RDFFormat.getFormatExtensions(rdfFormat)[0];
				} else { // check consistency between required format and ext
					String[] extForFormat = RDFFormat.getFormatExtensions(rdfFormat);
					if (!Arrays.asList(extForFormat).contains(ext)) {// ext isn't compatible with format ->
																		// infer ext
						ext = RDFFormat.getFormatExtensions(rdfFormat)[0];
					}
				}
			}

			tempServerFile = File.createTempFile("save", "." + ext);

			// convert flattering
			ModelFactory<ModelConfiguration> ontFact = PluginManager
					.getOntManagerImpl(getProject().getOntologyManagerImplID()).createModelFactory();
			BaseRDFTripleModel ligthWeigth = ontFact.createLightweightRDFModel();
			SKOSXLModel sourceModel = model;
			SKOSModel tempTargetModel = new SKOSModelImpl(ligthWeigth);
			if (toSKOS) {
				SKOSXL2SKOSConverter.convert(sourceModel, tempTargetModel, keepSKOSXLabels);
				if (toFlatDefinitions)
					ReifiedSKOSDefinitionsFlattener.convert(tempTargetModel, tempTargetModel,
							keepReifiedDefinition);
			} else if (toFlatDefinitions) {
				ReifiedSKOSDefinitionsFlattener.convert(sourceModel, tempTargetModel, keepReifiedDefinition);
			}

			// serialize model on local file
			tempTargetModel.writeRDF(tempServerFile, rdfFormat, NodeFilters.MAINGRAPH);
			tempTargetModel.close();

			// Return file as attachment in response
			oRes.setHeader("Content-Disposition", "attachment; filename=save." + ext);
			oRes.setHeader("Access-Control-Allow-Origin", "*");
			FileInputStream is = new FileInputStream(tempServerFile);
			IOUtils.copy(is, oRes.getOutputStream());
			oRes.setContentType(rdfFormat.getMIMEType());
			oRes.setContentLength((int) tempServerFile.length());
			oRes.flushBuffer();
			is.close();
		} else {
			throw new ProjectIncompatibleException("Unable to perform the conversion on a non-SKOSXL model");
		}
	}

	/**
	 * Moves the content of the default graph to a graph named after the base URI of the current project. This
	 * method clears the default graph and preserves (by default) the information already contained in the
	 * destination graph.
	 * 
	 * @param clearDestinationGraph
	 *            Specifies whether the destination graph is cleared before the insert of triples from the
	 *            default graph
	 * @return
	 * @throws MalformedQueryException
	 * @throws ModelAccessException
	 * @throws UnsupportedQueryLanguageException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	public void migrateDefaultGraphToBaseURIGraph(
			@Optional(defaultValue = "false") boolean clearDestinationGraph)
					throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException,
					QueryEvaluationException {
		String updateSpec;
		if (clearDestinationGraph) {
			updateSpec = "MOVE DEFAULT TO GRAPH %destinationGraph%";
		} else {
			updateSpec = "ADD DEFAULT TO GRAPH %destinationGraph% ; DROP DEFAULT";
		}

		ARTURIResource baseURI = VocabUtilities.nodeFactory.createURIResource(getProject().getBaseURI());
		updateSpec = updateSpec.replace("%destinationGraph%", RDFNodeSerializer.toNT(baseURI));
		
		Update update = getOWLModel().createUpdateQuery(updateSpec);
		update.evaluate(false);
	}
}
