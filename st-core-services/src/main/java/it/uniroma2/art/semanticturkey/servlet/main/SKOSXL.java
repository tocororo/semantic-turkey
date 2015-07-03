package it.uniroma2.art.semanticturkey.servlet.main;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.SKOSXLModel;
import it.uniroma2.art.owlart.navigation.ARTResourceIterator;
import it.uniroma2.art.owlart.navigation.ARTStatementIterator;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.HTTPParameterUnspecifiedException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.MalformedURIException;
import it.uniroma2.art.semanticturkey.exceptions.NonExistingRDFResourceException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

@Component
public class SKOSXL extends SKOS {

	protected static Logger logger = LoggerFactory.getLogger(SKOSXL.class);

	protected static enum XLabelCreationMode {
		bnode, uri
	};

	// REQUESTS
	public static class Req {
		// GET REQUESTS
		// public static final String getTopConceptsRequest = "getTopConcepts";
		// public static final String getBroaderConceptsRequest = "getBroaderConcepts";
		// public static final String getNarrowerConceptsRequest = "getNarrowerConcepts";
		// public static final String getAllSchemesListRequest = "getAllSchemesList";
		public static final String getLabelsRequest = "getLabels";
		public static final String getPrefLabelRequest = "getPrefLabel";
		public static final String getAltLabelsRequest = "getAltLabels";
		public static final String getHiddenLabelsRequest = "getHiddenLabels";
		// public static final String getSchemesMatrixPerConceptRequest = "getSchemesMatrixPerConceptRequest";

		// IS REQUESTS
		// public static final String isTopConceptRequest = "isTopConcept";

		// ADD REQUESTS
		// public static final String addTopConceptRequest = "addTopConcept";
		// public static final String addBroaderConceptRequest = "addBroaderConcept";
		// public static final String addConceptToSchemeRequest = "addConceptToScheme";
		public static final String setPrefLabelRequest = "setPrefLabel";
		public static final String addAltLabelRequest = "addAltLabel";
		public static final String addHiddenLabelRequest = "addHiddenLabel";
		public static final String changeLabelInfoRequest = "changeLabelInfo";
		public static final String prefToAltLabelRequest = "prefToAltLabel";
		public static final String altToPrefLabelRequest = "altToPrefLabel";
		
		// CREATE REQUESTS
		public static final String createConceptRequest = "createConcept";
		public static final String createSchemeRequest = "createScheme";

		// REMOVE REQUESTS
		// public static final String removeTopConceptRequest = "removeTopConcept";
		// public static final String deleteConceptRequest = "deleteConcept";
		// public static final String deleteSchemeRequest = "deleteScheme";
		public static final String removePrefLabelRequest = "removePrefLabel";
		public static final String removeAltLabelRequest = "removeAltLabel";
		public static final String removeHiddenLabelRequest = "removeHiddenLabel";
		// public static final String removeConceptFromSchemeRequest = "removeConceptFromScheme";
		// public static final String removeBroaderConcept = "removeBroaderConcept";

		// MODIFY REQUESTS
		// public static final String assignHierarchyToSchemeRequest = "assignHierarchyToScheme";

		// TREE (ONLY FOR DEBUGGING)
		// public static final String showSKOSConceptsTreeRequest = "showSKOSConceptsTree";
	}

	// PARS

	public static class Par {
		// *final static public String broaderConcept = "broaderConcept";
		// *final static public String concept = "concept";
		// final static public String conceptFrom = "conceptFrom";
		// final static public String conceptTo = "conceptTo";
		// final static public String forceDeleteDanglingConcepts = "forceDeleteDanglingConcepts";
		// final static public String setForceDeleteDanglingConcepts = "setForceDeleteDanglingConcepts";
		// *final static public String label = "label";
		// *final static public String langTag = "lang";
		// final static public String newConcept = "newConcept";
		// *final static public String prefLabel = "prefLabel";
		// final static public String relatedConcept = "relatedConcept";
		// final static public String semanticRelation = "semanticRelation";
		// *final static public String scheme = "scheme";
		// final static public String sourceScheme = "sourceScheme";
		// final static public String targetScheme = "targetScheme";
		// final static public String treeView = "treeView";
		final static public String mode = "mode";
		final static public String xlabelURI = "xlabelURI";
	}

	@Autowired
	public SKOSXL(@Value("Skosxl") String id) {
		super(id);
	}

	@Override
	public Response getPreCheckedResponse(String request) throws HTTPParameterUnspecifiedException {
		logger.debug("request to SKOS-XL");

		Response response = null;
		// all new fashioned requests are put inside these grace brackets
		if (request == null)
			return servletUtilities.createNoSuchHandlerExceptionResponse(request);

		// GET SKOS METHODS
		if (request.equals(Req.getPrefLabelRequest)) {
			System.err.println("inside SKOSXL");
			String skosConceptName = setHttpPar(SKOS.Par.concept);
			String lang = setHttpPar(SKOS.Par.lang);
			checkRequestParametersAllNotNull(SKOS.Par.concept, SKOS.Par.lang);
			response = getPrefXLabel(skosConceptName, lang);
		} else if (request.equals(Req.getAltLabelsRequest)) {
			String skosConceptName = setHttpPar(SKOS.Par.concept);
			String lang = setHttpPar(SKOS.Par.lang);
			checkRequestParametersAllNotNull(SKOS.Par.concept, SKOS.Par.lang);
			response = getAltXLabels(skosConceptName, lang);
		} else if (request.equals(Req.getHiddenLabelsRequest)) {
			String skosConceptName = setHttpPar(SKOS.Par.concept);
			String lang = setHttpPar(SKOS.Par.lang);
			checkRequestParametersAllNotNull(SKOS.Par.concept, SKOS.Par.lang);
			response = getHiddenXLabels(skosConceptName, lang);
		} else if (request.equals(Req.getLabelsRequest)) {
			String skosConceptName = setHttpPar(SKOS.Par.concept);
			String lang = setHttpPar(SKOS.Par.lang);
			checkRequestParametersAllNotNull(SKOS.Par.concept, SKOS.Par.lang);
			response = getXLabels(skosConceptName, lang);
		}

		// ADD/CREATE/SET

		else if (request.equals(Req.createConceptRequest)) {
			String conceptName = setHttpPar(SKOS.Par.concept);
			String broaderConceptName = setHttpPar(SKOS.Par.broaderConcept);
			String schemeName = setHttpPar(SKOS.Par.scheme);
			String prefLabel = setHttpPar(SKOS.Par.prefLabel);
			String prefLabelLanguage = setHttpPar(SKOS.Par.prefLabelLang);
			String language = setHttpPar(SKOS.Par.lang);
			//checkRequestParametersAllNotNull(SKOS.Par.concept, SKOS.Par.scheme);
			//the concept can be null, in this case the URI of the concept in automatically generated
			checkRequestParametersAllNotNull(SKOS.Par.scheme);
			response = createConcept(conceptName, broaderConceptName, schemeName, prefLabel,
					prefLabelLanguage, language);

		} else if (request.equals(Req.createSchemeRequest)) {
			String schemeName = setHttpPar(SKOS.Par.scheme);
			String preferredLabel = setHttpPar(SKOS.Par.prefLabel);
			String preferredLabelLanguage = setHttpPar(SKOS.Par.prefLabelLang);
			String language = setHttpPar(SKOS.Par.lang);

			checkRequestParametersAllNotNull(SKOS.Par.scheme);
			response = createConceptScheme(schemeName, preferredLabel, preferredLabelLanguage, language);

		} else if (request.equals(Req.setPrefLabelRequest)) {
			String skosConceptName = setHttpPar(SKOS.Par.concept);
			String lang = setHttpPar(SKOS.Par.lang);
			String label = setHttpPar(SKOS.Par.label);
			String modeString = setHttpPar(Par.mode);
			checkRequestParametersAllNotNull(SKOS.Par.concept, Par.mode, SKOS.Par.lang, SKOS.Par.label);
			XLabelCreationMode xLabelCreationMode = XLabelCreationMode.valueOf(modeString);
			response = setPrefXLabel(skosConceptName, xLabelCreationMode, label, lang);
		} else if (request.equals(Req.addAltLabelRequest)) {
			String skosConceptName = setHttpPar(SKOS.Par.concept);
			String lang = setHttpPar(SKOS.Par.lang);
			String label = setHttpPar(SKOS.Par.label);
			String modeString = setHttpPar(Par.mode);
			checkRequestParametersAllNotNull(SKOS.Par.concept, Par.mode, SKOS.Par.lang, SKOS.Par.label);
			XLabelCreationMode xLabelCreationMode = XLabelCreationMode.valueOf(modeString);
			response = addAltXLabel(skosConceptName, xLabelCreationMode, label, lang);
		} else if (request.equals(Req.addHiddenLabelRequest)) {
			String skosConceptName = setHttpPar(SKOS.Par.concept);
			String lang = setHttpPar(SKOS.Par.lang);
			String label = setHttpPar(SKOS.Par.label);
			String modeString = setHttpPar(Par.mode);
			checkRequestParametersAllNotNull(SKOS.Par.concept, Par.mode, SKOS.Par.lang, SKOS.Par.label);
			XLabelCreationMode xLabelCreationMode = XLabelCreationMode.valueOf(modeString);
			response = addHiddenXLabel(skosConceptName, xLabelCreationMode, label, lang);
		} else if (request.equals(Req.changeLabelInfoRequest)) {
			String xlabelURI = setHttpPar(SKOSXL.Par.xlabelURI);
			String label = setHttpPar(SKOS.Par.label);
			String lang = setHttpPar(SKOS.Par.lang);
			checkRequestParametersAllNotNull(SKOSXL.Par.xlabelURI, SKOS.Par.label);
			response = changeLabelInfo(xlabelURI, label, lang);
		} else if (request.equals(Req.prefToAltLabelRequest)) {
			String conceptName = setHttpPar(SKOS.Par.concept);
			String xlabelURI = setHttpPar(SKOSXL.Par.xlabelURI);
			checkRequestParametersAllNotNull(SKOS.Par.concept, SKOSXL.Par.xlabelURI);
			response = prefToAtlLabel(conceptName, xlabelURI);
		} else if (request.equals(Req.altToPrefLabelRequest)) {
			String conceptName = setHttpPar(SKOS.Par.concept);
			String xlabelURI = setHttpPar(SKOSXL.Par.xlabelURI);
			checkRequestParametersAllNotNull(SKOS.Par.concept, SKOSXL.Par.xlabelURI);
			response = altToPrefLabel(conceptName, xlabelURI);
		} 

		// REMOVE
		else if (request.equals(Req.removePrefLabelRequest)) {
			String skosConceptName = setHttpPar(SKOS.Par.concept);
			String lang = setHttpPar(SKOS.Par.lang);
			String label = setHttpPar(SKOS.Par.label);
			checkRequestParametersAllNotNull(SKOS.Par.concept, SKOS.Par.label);
			response = removePrefXLabel(skosConceptName, label, lang);
		} else if (request.equals(Req.removeAltLabelRequest)) {
			String skosConceptName = setHttpPar(SKOS.Par.concept);
			String lang = setHttpPar(SKOS.Par.lang);
			String label = setHttpPar(SKOS.Par.label);
			checkRequestParametersAllNotNull(SKOS.Par.concept, SKOS.Par.label);
			response = removeAltXLabel(skosConceptName, label, lang);
		} else if (request.equals(Req.removeHiddenLabelRequest)) {
			String skosConceptName = setHttpPar(SKOS.Par.concept);
			String lang = setHttpPar(SKOS.Par.lang);
			String label = setHttpPar(SKOS.Par.label);
			checkRequestParametersAllNotNull(SKOS.Par.concept, SKOS.Par.label);
			response = removeHiddenXLabel(skosConceptName, label, lang);
		}

		else
			response = super.getPreCheckedResponse(request);

		this.fireServletEvent();
		return response;
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	/**
	 * this service removes the preferred label for a given language
	 * 
	 * @param skosConceptName
	 * @param label
	 * @param lang
	 * @return
	 */
	public Response removePrefXLabel(String skosConceptName, String label, String lang) {
		SKOSXLModel model = getSKOSXLModel();

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);

		try {
			ARTURIResource skosConcept = model.createURIResource(model.expandQName(skosConceptName));

			STRDFResource prefXLabel = getPrefXLabel(model, skosConcept, lang, getWorkingGraph());

			ARTResource tmp = prefXLabel.getARTNode().asResource();

			if (prefXLabel.getRendering().compareTo(label) == 0) {
				model.deleteTriple(skosConcept, it.uniroma2.art.owlart.vocabulary.SKOSXL.Res.PREFLABEL, tmp,
						getWorkingGraph());
				// TODO this should be a full delete of a resource (following also BNodes).
				// ModelUtilities#deepDeleteIndividual would be ok, but we need a simpler one with: no delTree
				// but which automatically follows recursive elimination of bnodes
				model.deleteTriple(tmp, NodeFilters.ANY, NodeFilters.ANY, getWorkingGraph());
			}

		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
		return response;
	}

	/**
	 * this service removes an alternative label for a given language
	 * 
	 * @param skosConceptName
	 * @param label
	 * @param lang
	 * @return
	 */
	public Response removeAltXLabel(String skosConceptName, String label, String lang) {
		SKOSXLModel model = getSKOSXLModel();

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);

		try {
			ARTURIResource skosConcept = model.createURIResource(model.expandQName(skosConceptName));

			Collection<STRDFResource> altLabels = listAltXLabels(model, skosConcept, lang, getWorkingGraph());

			for (STRDFResource strdfRes : altLabels) {
				if (strdfRes.getRendering().compareTo(label) == 0) {
					ARTResource tmp = strdfRes.getARTNode().asResource();
					model.deleteTriple(skosConcept,
							model.createURIResource(it.uniroma2.art.owlart.vocabulary.SKOSXL.ALTLABEL), tmp,
							getWorkingGraph());
					// TODO this should be a full delete of a resource (following also BNodes).
					// ModelUtilities#deepDeleteIndividual would be ok, but we need a simpler one with: no delTree
					// but which automatically follows recursive elimination of bnodes
					model.deleteTriple(tmp, NodeFilters.ANY, NodeFilters.ANY, getWorkingGraph());
				}
			}

		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
		return response;
	}

	
	/**
	 * this service removes an hidden label for a given language
	 * 
	 * @param skosConceptName
	 * @param label
	 * @param lang
	 * @return
	 */
	public Response removeHiddenXLabel(String skosConceptName, String label, String lang) {
		SKOSXLModel model = getSKOSXLModel();

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);

		try {
			ARTURIResource skosConcept = model.createURIResource(model.expandQName(skosConceptName));

			Collection<STRDFResource> hiddenLabels = listHiddenXLabels(model, skosConcept, lang, getWorkingGraph());

			for (STRDFResource strdfRes : hiddenLabels) {
				if (strdfRes.getRendering().compareTo(label) == 0) {
					ARTResource tmp = strdfRes.getARTNode().asResource();
					model.deleteTriple(skosConcept,
							model.createURIResource(it.uniroma2.art.owlart.vocabulary.SKOSXL.HIDDENLABEL), tmp,
							getWorkingGraph());
					// TODO this should be a full delete of a resource (following also BNodes).
					// ModelUtilities#deepDeleteIndividual would be ok, but we need a simpler one with: no delTree
					// but which automatically follows recursive elimination of bnodes
					model.deleteTriple(tmp, NodeFilters.ANY, NodeFilters.ANY, getWorkingGraph());
				}
			}

		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
		return response;
	}
	
	// SERVICE METHODS

	/**
	 * this service gets the preferred label for a given language
	 * 
	 * 
	 * @param skosConceptName
	 * @param label
	 * @param lang
	 * @return
	 */
	public Response getPrefXLabel(String skosConceptName, String lang) {

		SKOSXLModel model = getSKOSXLModel();

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		try {
			ARTResource[] graphs = getUserNamedGraphs();
			ARTURIResource skosConcept = retrieveExistingURIResource(model, skosConceptName, graphs);

			if (!lang.equals("*")) {
				STRDFResource prefLabel = getPrefXLabel(model, skosConcept, lang, graphs);
				if (prefLabel != null)
					RDFXMLHelp.addRDFNode(response, prefLabel);
			} else {
				Collection<STRDFResource> prefLabels = listPrefXLabels(model, skosConcept, graphs);
				RDFXMLHelp.addRDFNodes(response, prefLabels);
			}

		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
		return response;
	}

	/**
	 * this service gets the alternative label for a given language
	 * 
	 * 
	 * @param skosConceptName
	 * @param lang
	 * @return
	 */
	public Response getAltXLabels(String skosConceptName, String lang) {

		SKOSXLModel model = getSKOSXLModel();

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		try {
			ARTResource[] graphs = getUserNamedGraphs();
			ARTURIResource skosConcept = retrieveExistingURIResource(model, skosConceptName, graphs);

			Collection<STRDFResource> altLabels = listAltXLabels(model, skosConcept, lang, graphs);
			RDFXMLHelp.addRDFNodes(response, altLabels);

		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
		return response;
	}

	/**
	 * this service gets the hidden label for a given language
	 * 
	 * 
	 * @param skosConceptName
	 * @param lang
	 * @return
	 */
	public Response getHiddenXLabels(String skosConceptName, String lang) {

		SKOSXLModel model = getSKOSXLModel();

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		try {
			ARTResource[] graphs = getUserNamedGraphs();
			ARTURIResource skosConcept = retrieveExistingURIResource(model, skosConceptName, graphs);

			Collection<STRDFResource> hiddenLabels = listHiddenXLabels(model, skosConcept, lang, graphs);
			RDFXMLHelp.addRDFNodes(response, hiddenLabels);

		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
		return response;
	}

	/**
	 * this service gets all the xlabels for a given language, ordered by type
	 * 
	 * 
	 * @param skosConceptName
	 * @param lang
	 * @return
	 */
	public Response getXLabels(String skosConceptName, String lang) {

		SKOSXLModel model = getSKOSXLModel();

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		try {
			ARTResource[] graphs = getUserNamedGraphs();
			ARTURIResource skosConcept = retrieveExistingURIResource(model, skosConceptName, graphs);

			if (!lang.equals("*")) {
				STRDFResource prefLabel = getPrefXLabel(model, skosConcept, lang, graphs);
				if (prefLabel != null)
					RDFXMLHelp.addRDFNode(XMLHelp.newElement(response.getDataElement(), "prefLabel"),
							prefLabel);
			} else {
				Collection<STRDFResource> prefLabels = listPrefXLabels(model, skosConcept, graphs);
				RDFXMLHelp.addRDFNodes(response, "prefLabels", prefLabels);
			}

			Collection<STRDFResource> altLabels = listAltXLabels(model, skosConcept, lang, graphs);
			RDFXMLHelp.addRDFNodes(response, "altLabels", altLabels);

			Collection<STRDFResource> hiddenLabels = listHiddenXLabels(model, skosConcept, lang, graphs);
			RDFXMLHelp.addRDFNodes(response, "hiddenLabels", hiddenLabels);

		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
		return response;
	}

	public Response createConcept(String conceptName, String superConceptName, String schemeName,
			String prefLabel, String prefLabelLang, String language) {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		logger.debug("conceptName: " + conceptName);
		logger.debug("schemeName: " + schemeName);

		try {
			ARTResource wrkGraph = getWorkingGraph();
			SKOSXLModel skosxlModel = getSKOSXLModel();
			ARTResource[] graphs = getUserNamedGraphs();

			//there are two possibilities:
			// - the uri is passed, so that URI should be used to create the concept
			// - the uri is null, so it should be created using the specified method
			ARTURIResource newConcept = null;
			if(conceptName == null){	
				newConcept = generateURI("concept", Collections.<String,String>emptyMap());
			} else{
				newConcept = createNewURIResource(skosxlModel, conceptName, graphs);
			}
			//ARTURIResource newConcept = createNewResource(skosxlModel, conceptName, graphs);

			ARTURIResource superConcept;
			if (superConceptName != null)
				superConcept = retrieveExistingURIResource(skosxlModel, superConceptName, graphs);
			else
				superConcept = NodeFilters.NONE;

			ARTURIResource conceptScheme = retrieveExistingURIResource(skosxlModel, schemeName, graphs);

			logger.debug("adding concept to graph: " + wrkGraph);
			skosxlModel.addConceptToScheme(newConcept.getURI(), superConcept, conceptScheme, wrkGraph);

			//ARTURIResource prefXLabel = skosxlModel.addXLabel(createURIForXLabel(skosxlModel), prefLabel,
			//		prefLabelLang, getWorkingGraph());
			
			ARTURIResource prefXLabel = null;
			
			if (prefLabel != null && prefLabelLang != null) {
				ARTURIResource prefXLabelURI = generateXLabelURI(skosxlModel, 
						prefLabelLang, graphs);
				prefXLabel = skosxlModel.addXLabel(prefXLabelURI.getURI(), prefLabel,
						prefLabelLang, getWorkingGraph());
				
				skosxlModel.setPrefXLabel(newConcept, prefXLabel, getWorkingGraph());				
			}


			RDFXMLHelp.addRDFNode(response, createSTConcept(skosxlModel, newConcept, true, language));
			
			if (prefXLabel != null) {
				RDFXMLHelp.addRDFNode(response, createSTXLabel(skosxlModel, prefXLabel, true));
			}
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		} catch (DuplicatedResourceException e) {
			return logAndSendException(e);
		} catch (IOException e) {
			return logAndSendException(e);
		} catch (InvalidProjectNameException e) {
			return logAndSendException(e);
		} catch (ProjectInexistentException e) {
			return logAndSendException(e);
		} catch (MalformedURIException e) {
			return logAndSendException(e);
		} catch (URIGenerationException e) {
			return logAndSendException(e);
		}
		return response;
	}

	public Response createConceptScheme(String schemeQName, String prefLabel, String prefLabelLang,
			String language) {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		logger.debug("new scheme name: " + schemeQName);

		try {

			SKOSXLModel skosxlModel = getSKOSXLModel();
			ARTURIResource newScheme = createNewURIResource(skosxlModel, schemeQName, getUserNamedGraphs());

			// add a new concept scheme...
			skosxlModel.addSKOSConceptScheme(newScheme, getWorkingGraph());

			// add skos:preferredLabel
			if (prefLabel != null && prefLabelLang != null) {
				ARTURIResource prefXLabel = skosxlModel.addXLabel(createURIForXLabel(skosxlModel), prefLabel,
						prefLabelLang, getWorkingGraph());
				skosxlModel.setPrefXLabel(newScheme, prefXLabel, getWorkingGraph());

				// addPrefLabel(skosModel, newScheme, prefLabel, lang);
			}

			RDFXMLHelp.addRDFNode(response, createSTScheme(skosxlModel, newScheme, true, language));

		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (DuplicatedResourceException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		} catch (MalformedURIException e) {
			return logAndSendException(e);
		}
		return response;
	}

	/**
	 * this service sets the preferred label for a given language
	 * 
	 * 
	 * @param skosConceptName
	 * @param mode
	 *            bnode or uri: if uri a URI generator is used to create the URI for the xlabel
	 * @param label
	 * @param lang
	 * @return
	 */
	public Response setPrefXLabel(String skosConceptName, XLabelCreationMode mode, String label, String lang) {

		SKOSXLModel skosxlmodel = getSKOSXLModel();

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);

		try {
			ARTResource graph = getWorkingGraph();
			ARTURIResource skosConcept = retrieveExistingURIResource(skosxlmodel, skosConceptName, graph);

			// change the other preferred label (of the same language) to alternative label, as only one
			// preferred label per language can exist
			ARTResource oldPrefLabelRes = skosxlmodel.getPrefXLabel(skosConcept, lang, graph);
			if (oldPrefLabelRes != null && oldPrefLabelRes.isURIResource()) {
				ARTURIResource oldxlabel = oldPrefLabelRes.asURIResource();
				skosxlmodel.deleteTriple(skosConcept,
						skosxlmodel.createURIResource(it.uniroma2.art.owlart.vocabulary.SKOSXL.PREFLABEL),
						oldxlabel, graph);
				skosxlmodel.addTriple(skosConcept,
						skosxlmodel.createURIResource(it.uniroma2.art.owlart.vocabulary.SKOSXL.ALTLABEL),
						oldxlabel, graph);
			}

			if (mode == XLabelCreationMode.bnode){
				skosxlmodel.setPrefXLabel(skosConcept, label, lang, getWorkingGraph());
			} else {
				ARTURIResource prefXLabelURI = generateXLabelURI(skosxlmodel, 
						lang, getUserNamedGraphs());
				ARTURIResource prefXLabel = skosxlmodel.addXLabel(prefXLabelURI.getURI(), label, lang,
						getWorkingGraph());
				skosxlmodel.setPrefXLabel(skosConcept, prefXLabel, getWorkingGraph());
				RDFXMLHelp.addRDFNode(response, createSTXLabel(skosxlmodel, prefXLabel, true));
			}

		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		} catch (IOException e) {
			return logAndSendException(e);
		} catch (InvalidProjectNameException e) {
			return logAndSendException(e);
		} catch (ProjectInexistentException e) {
			return logAndSendException(e);
		} catch (URIGenerationException e) {
			return logAndSendException(e);
		}
		return response;
	}

	/**
	 * this service adds an alternative label to a concept for a given language
	 * 
	 * 
	 * @param skosConceptName
	 * @param mode
	 *            bnode or uri: if uri a URI generator is used to create the URI for the xlabel
	 * @param label
	 * @param lang
	 * @return
	 */
	public Response addAltXLabel(String skosConceptName, XLabelCreationMode mode, String label, String lang) {

		SKOSXLModel skosxlmodel = getSKOSXLModel();

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);

		try {
			ARTURIResource skosConcept = retrieveExistingURIResource(skosxlmodel, skosConceptName,
					getUserNamedGraphs());
			if (mode == XLabelCreationMode.bnode) {
				skosxlmodel.addAltXLabel(skosConcept, label, lang, getWorkingGraph());
			}else {
				ARTURIResource altXLabelURI = generateXLabelURI(skosxlmodel, 
						lang, getUserNamedGraphs());
				ARTURIResource altXLabel = skosxlmodel.addXLabel(altXLabelURI.getURI(), label, lang,
						getWorkingGraph());
				skosxlmodel.addAltXLabel(skosConcept, altXLabel, getWorkingGraph());
				RDFXMLHelp.addRDFNode(response, createSTXLabel(skosxlmodel, altXLabel, true));
			}

		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		} catch (IOException e) {
			return logAndSendException(e);
		} catch (InvalidProjectNameException e) {
			return logAndSendException(e);
		} catch (ProjectInexistentException e) {
			return logAndSendException(e);
		} catch (URIGenerationException e) {
			return logAndSendException(e);
		}
		return response;
	}

	/**
	 * this service adds an hidden label to a concept for a given language
	 * 
	 * 
	 * @param skosConceptName
	 * @param mode
	 *            bnode or uri: if uri a URI generator is used to create the URI for the xlabel
	 * @param label
	 * @param lang
	 * @return
	 */
	public Response addHiddenXLabel(String skosConceptName, XLabelCreationMode mode, String label, String lang) {

		SKOSXLModel skosxlmodel = getSKOSXLModel();

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);

		try {
			ARTURIResource skosConcept = retrieveExistingURIResource(skosxlmodel, skosConceptName,
					getUserNamedGraphs());
			if (mode == XLabelCreationMode.bnode) {
				skosxlmodel.addHiddenXLabel(skosConcept, label, lang, getWorkingGraph());
			} else {
				ARTURIResource hiddenXLabelURI = generateXLabelURI(skosxlmodel, lang,
						getUserNamedGraphs());
				ARTURIResource hiddenXLabel = skosxlmodel.addXLabel(hiddenXLabelURI.getURI(), label, lang,
						getWorkingGraph());
				skosxlmodel.addHiddenXLabel(skosConcept, hiddenXLabel, getWorkingGraph());
				RDFXMLHelp.addRDFNode(response, createSTXLabel(skosxlmodel, hiddenXLabelURI, true));
			}

		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		} catch (IOException e) {
			return logAndSendException(e);
		} catch (InvalidProjectNameException e) {
			return logAndSendException(e);
		} catch (ProjectInexistentException e) {
			return logAndSendException(e);
		} catch (URIGenerationException e) {
			return logAndSendException(e);
		}
		return response;
	}
	
	public Response changeLabelInfo(String xlabelURI, String label, String lang) {
		SKOSXLModel skosxlModel = getSKOSXLModel();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element xlabel = XMLHelp.newElement(dataElement, Par.xlabelURI);

		try {
			ARTLiteral xlabelLiteral = skosxlModel.createLiteral(label, lang);
			ARTURIResource xLabel = retrieveExistingURIResource(skosxlModel, 
					skosxlModel.expandQName(xlabelURI));
			ARTResource graph = getWorkingGraph();
			// get the old value
			ARTStatementIterator artStatIter = skosxlModel.listStatements(xLabel,
					it.uniroma2.art.owlart.vocabulary.SKOSXL.Res.LITERALFORM, NodeFilters.ANY, false, graph);

			// check if the old value is different form the new one, in this case remove the relative triple
			// and add the new triple
			if (artStatIter.hasNext()) {
				ARTLiteral oldLabel = artStatIter.next().getObject().asLiteral();
				xlabel.setAttribute("oldLabel", oldLabel.getLabel());
				xlabel.setAttribute("oldLang", oldLabel.getLanguage());
				if (oldLabel.getLabel().compareTo(label) != 0 || oldLabel.getLanguage().compareTo(lang) != 0) {
					// remove the old values and add the new one
					skosxlModel.deleteTriple(xLabel,
							it.uniroma2.art.owlart.vocabulary.SKOSXL.Res.LITERALFORM, oldLabel, graph);
					skosxlModel.addTriple(xLabel, it.uniroma2.art.owlart.vocabulary.SKOSXL.Res.LITERALFORM,
							xlabelLiteral, graph);
				}

			} else { //there was no value associated to the xlabel (which is strange)
				skosxlModel.addTriple(xLabel, it.uniroma2.art.owlart.vocabulary.SKOSXL.Res.LITERALFORM,
						xlabelLiteral, graph);
			}

			xlabel.setAttribute("newLabel", label);
			xlabel.setAttribute("newLang", lang);

		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
		return response;
	}
	
	public Response prefToAtlLabel(String conceptURI, String xlabelURI) {
		SKOSXLModel skosxlModel = getSKOSXLModel();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element xlabelElem = XMLHelp.newElement(dataElement, SKOSXL.Par.xlabelURI);
		try {
			ARTURIResource skosConcept = skosxlModel.createURIResource(skosxlModel.expandQName(conceptURI));
			ARTURIResource xLabel = retrieveExistingURIResource(skosxlModel, 
					skosxlModel.expandQName(xlabelURI));
			ARTResource graph = getWorkingGraph();
			skosxlModel.deleteTriple(skosConcept,
					skosxlModel.createURIResource(it.uniroma2.art.owlart.vocabulary.SKOSXL.PREFLABEL), xLabel,
					graph);
			skosxlModel.addTriple(skosConcept,
					skosxlModel.createURIResource(it.uniroma2.art.owlart.vocabulary.SKOSXL.ALTLABEL), xLabel, 
					graph);

			xlabelElem.setAttribute("xlabelURI", xlabelURI);
			xlabelElem.setAttribute("xlabconceptURIelURI", conceptURI);
			xlabelElem.setAttribute("info", "xlabel changed from Preferred to Alternative");

		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
		return response;
	}

	public Response altToPrefLabel(String conceptURI, String xlabelURI) {
		SKOSXLModel skosxlModel = getSKOSXLModel();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element xlabelElem = XMLHelp.newElement(dataElement, SKOSXL.Par.xlabelURI);
		try {
			ARTURIResource skosConcept = skosxlModel.createURIResource(skosxlModel.expandQName(conceptURI));
			ARTURIResource xLabel = retrieveExistingURIResource(skosxlModel, 
					skosxlModel.expandQName(xlabelURI));
			ARTResource graph = getWorkingGraph();

			// change the other preferred label (of the same language) to alternative label, as only one
			// preferred label per language can exist
			String lang = skosxlModel.getLiteralForm(xLabel, graph).getLanguage();
			ARTResource oldPrefLabelRes = skosxlModel.getPrefXLabel(skosConcept, lang, graph);
			if (oldPrefLabelRes != null && oldPrefLabelRes.isURIResource()) {
				ARTURIResource oldxlabel = oldPrefLabelRes.asURIResource();
				skosxlModel.deleteTriple(skosConcept,
						skosxlModel.createURIResource(it.uniroma2.art.owlart.vocabulary.SKOSXL.PREFLABEL),
						oldxlabel, graph);
				skosxlModel.addTriple(skosConcept,
						skosxlModel.createURIResource(it.uniroma2.art.owlart.vocabulary.SKOSXL.ALTLABEL),
						oldxlabel, graph);
			}

			skosxlModel.deleteTriple(skosConcept,
					skosxlModel.createURIResource(it.uniroma2.art.owlart.vocabulary.SKOSXL.ALTLABEL), xLabel, 
					graph);
			skosxlModel.addTriple(skosConcept,
					skosxlModel.createURIResource(it.uniroma2.art.owlart.vocabulary.SKOSXL.PREFLABEL), xLabel,
					graph);

			xlabelElem.setAttribute("xlabelURI", xlabelURI);
			xlabelElem.setAttribute("xlabconceptURIelURI", conceptURI);
			xlabelElem.setAttribute("info", "xlabel changed from Alternative to Preferred");
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
		return response;
	}

	// **************
	// INNER METHODS
	// **************

	public STRDFResource getPrefXLabel(SKOSXLModel model, ARTURIResource skosConcept, String lang,
			ARTResource... graphs) throws ModelAccessException, NonExistingRDFResourceException {
		ARTResource prefLabel = model.getPrefXLabel(skosConcept, lang, graphs);
		if (prefLabel != null)
			return createSTXLabel(model, prefLabel, true);
		return null;
	}

	public Collection<STRDFResource> listPrefXLabels(SKOSXLModel model, ARTURIResource skosConcept,
			ARTResource... graphs) throws ModelAccessException, NonExistingRDFResourceException {
		ARTResourceIterator prefLabels = model.listPrefXLabels(skosConcept, graphs);
		return createSTXLabelCollection(model, prefLabels, true);
	}

	public Collection<STRDFResource> listAltXLabels(SKOSXLModel model, ARTURIResource skosConcept,
			String lang, ARTResource... graphs) throws ModelAccessException, NonExistingRDFResourceException {
		ARTResourceIterator altLabels;
		if (Objects.equals(lang, "*"))
			altLabels = model.listAltXLabels(skosConcept, graphs);

		else
			altLabels = model.listAltXLabels(skosConcept, lang, graphs);

		return createSTXLabelCollection(model, altLabels, true);
	}

	public Collection<STRDFResource> listHiddenXLabels(SKOSXLModel model, ARTURIResource skosConcept,
			String lang, ARTResource... graphs) throws ModelAccessException, NonExistingRDFResourceException {
		ARTResourceIterator altLabels;
		if (Objects.equals(lang, "*"))
			altLabels = model.listHiddenXLabels(skosConcept, graphs);
		else
			altLabels = model.listHiddenXLabels(skosConcept, lang, graphs);

		return createSTXLabelCollection(model, altLabels, true);
	}

	// **************
	// FACILITY METHODS
	// **************

	public Collection<STRDFResource> createSTXLabelCollection(SKOSXLModel model, ARTResourceIterator it,
			boolean explicit) throws ModelAccessException, NonExistingRDFResourceException {
		Collection<STRDFResource> xLabels = new ArrayList<STRDFResource>();
		while (it.streamOpen()) {
			xLabels.add(createSTXLabel(model, it.getNext(), explicit));
		}
		it.close();
		return xLabels;
	}

	private STRDFResource createSTXLabel(SKOSXLModel skosModel, ARTResource xLabel, boolean explicit)
			throws ModelAccessException, NonExistingRDFResourceException {
		String show;
		ARTLiteral lbl = skosModel.getLiteralForm(xLabel, getUserNamedGraphs());
		if (lbl != null)
			show = lbl.getLabel();
		else
			show = skosModel.getQName(RDFNodeSerializer.toNT(xLabel));

		STRDFResource res = STRDFNodeFactory.createSTRDFResource(xLabel, RDFResourceRolesEnum.xLabel,
				explicit, show);
		if (lbl != null)
			res.setInfo("lang", lbl.getLanguage());

		return res;
	}

	private String createURIForXLabel(RDFModel model) {
		return model.getDefaultNamespace() + "xl-" + UUID.randomUUID().toString();
	}

	
	public SKOSXLModel getSKOSXLModel() {
		return (SKOSXLModel)getOntModel();
	}

	
	protected ARTURIResource generateXLabelURI(SKOSXLModel skosxlModel, String lang,
			ARTResource[] graphs) 
			throws ModelAccessException, IOException, InvalidProjectNameException, ProjectInexistentException, URIGenerationException {
		
		Map<String, String> valueMapping = new HashMap<String, String>();
		valueMapping.put("lang", lang);
		return generateURI("xLabel", valueMapping);
	}
	
}
