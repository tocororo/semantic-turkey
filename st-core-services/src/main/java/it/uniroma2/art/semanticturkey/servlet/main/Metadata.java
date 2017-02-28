/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http//www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is SemanticTurkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2007.
 * All Rights Reserved.
 *
 * SemanticTurkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata (ART)
 * Current information about SemanticTurkey can be obtained at 
 * http://semanticturkey.uniroma2.it
 *
 */

package it.uniroma2.art.semanticturkey.servlet.main;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.RDF4JException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.UnsupportedRDFFormatException;
import it.uniroma2.art.owlart.io.RDFFormat;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.PrefixMapping;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.navigation.ARTResourceIterator;
import it.uniroma2.art.owlart.navigation.ARTURIResourceIterator;
import it.uniroma2.art.owlart.rdf4jimpl.io.RDFFormatConverter;
import it.uniroma2.art.owlart.utilities.ModelUtilities;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.exceptions.HTTPParameterUnspecifiedException;
import it.uniroma2.art.semanticturkey.exceptions.ImportManagementException;
import it.uniroma2.art.semanticturkey.exceptions.NonExistingRDFResourceException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.ontology.ImportStatus;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappingUpdateException;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;
import it.uniroma2.art.semanticturkey.ontology.OntologyManagerException;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.OntologiesMirror;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.Utilities;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

/**
 * @author Armando Stellato <stellato@info.uniroma2.it>
 * @author Andrea Turbati <turbati@info.uniroma2.it>
 */
@Component(value = "MetadataOLD")
public class Metadata extends ResourceOld {

	protected static Logger logger = LoggerFactory.getLogger(Metadata.class);
	public String XSLpath = Resources.getXSLDirectoryPath() + "createClassForm.xsl";

	public Logger getLogger() {
		return logger;
	}

	// TODO raccogliere opportunamente le eccezioni!
	public int fromWebToMirror = 0;
	public int fromWeb = 1;
	public int fromLocalFile = 2;
	public int fromOntologyMirror = 3;
	public int toOntologyMirror = 4;

	// REQUESTS
	// baseuri/defaultnamespace management
	public static final String setDefaultNamespaceRequest = "setDefaultNamespace";
	public static final String getDefaultNamespaceRequest = "getDefaultNamespace";
	public static final String setBaseuriRequest = "setBaseuri";
	public static final String getBaseuriRequest = "getBaseuri";
	public static final String setBaseuriDefNamespaceRequest = "setBaseuriDefNamespace";
	// prefix/namespace mappings
	public static final String getNSPrefixMappingsRequest = "getNSPrefixMappings";
	public static final String setNSPrefixMappingRequest = "setNSPrefixMapping";
	public static final String changeNSPrefixMappingRequest = "changeNSPrefixMapping";
	public static final String removeNSPrefixMappingRequest = "removeNSPrefixMapping";
	public static final String expandQNameRequest = "expandQName";
	// imports info
	public static final String getImportsRequest = "getImports";
	public static final String removeImportRequest = "removeImport";
	// import I/O
	public static final String addFromWebToMirrorRequest = "addFromWebToMirror";
	public static final String addFromWebRequest = "addFromWeb";
	public static final String addFromLocalFileRequest = "addFromLocalFile";
	public static final String addFromOntologyMirrorRequest = "addFromOntologyMirror";
	public static final String downloadFromWebToMirrorRequest = "downloadFromWebToMirror";
	public static final String downloadFromWebRequest = "downloadFromWeb";
	public static final String getFromLocalFileRequest = "getFromLocalFile";
	public static final String mirrorOntologyRequest = "mirrorOntology";

	public static final String getNamedGraphsRequest = "getNamedGraphs";

	// parameters
	public static final String baseuriPar = "baseuri";
	public static final String namespacePar = "namespace";
	public static final String mirrorFilePar = "mirrorFile";
	public static final String prefixPar = "prefix";
	public static final String localFilePathPar = "localFilePath";
	public static final String localFilePar = "localFile";
	public static final String alturlPar = "alturl";
	public static final String rdfFormatPar = "rdfFormat";
	public static final String qnamePar = "qname";

	// response tags
	public static final String baseuriTag = "BaseURI";

	@Autowired
	public Metadata(@Value("Metadata") String id) {
		super(id);
	}

	public Response getPreCheckedResponse(String request) throws HTTPParameterUnspecifiedException {
		ServletUtilities servletUtilities = new ServletUtilities();

		this.fireServletEvent();

		if (request.equals(ontologyDescriptionRequest)) {
			return getOntologyDescription();
		} else if (request.equals(setDefaultNamespaceRequest)) {
			String namespace = setHttpPar(namespacePar);
			checkRequestParametersAllNotNull(namespacePar);
			return setDefaultNamespace(namespace);
		} else if (request.equals(getDefaultNamespaceRequest)) {
			return getDefaultNamespace();
		} /*
			 * else if (request.equals(setBaseuriRequest)) { String baseuri = setHttpPar(baseuriPar);
			 * checkRequestParametersAllNotNull(baseuriPar); return setBaseURI(baseuri); }
			 */ else if (request.equals(getBaseuriRequest)) {
			return getBaseURI();
		} /*
			 * else if (request.equals(setBaseuriDefNamespaceRequest)) { String baseURI =
			 * setHttpPar(baseuriPar); String defaultNamespace = setHttpPar(namespacePar);
			 * checkRequestParametersAllNotNull(baseuriPar, namespacePar); return
			 * setBaseURIAndDefaultNamespace(baseURI, defaultNamespace); }
			 */ else if (request.equals(getNSPrefixMappingsRequest)) {
			return getNamespaceMappings();
		} else if (request.equals(setNSPrefixMappingRequest)) {
			String namespace = setHttpPar(namespacePar);
			String prefix = setHttpPar(prefixPar);
			checkRequestParametersAllNotNull(prefixPar, namespacePar);
			return setNamespaceMapping(prefix, namespace);
		} else if (request.equals(changeNSPrefixMappingRequest)) {
			String namespace = setHttpPar(namespacePar);
			String prefix = setHttpPar(prefixPar);
			checkRequestParametersAllNotNull(prefixPar, namespacePar);
			return changeNamespaceMapping(prefix, namespace);
		} else if (request.equals(removeNSPrefixMappingRequest)) {
			String namespace = setHttpPar(namespacePar);
			checkRequestParametersAllNotNull(namespacePar);
			return removeNamespaceMapping(namespace);
		} else if (request.equals(expandQNameRequest)) {
			String qname = setHttpPar(qnamePar);
			checkRequestParametersAllNotNull(qnamePar);
			return expandQName(qname);
		} else if (request.equals(getImportsRequest)) {
			return getOntologyImports();
		}
		// imports an ontology which is already present in the ontology mirror location
		else if (request.equals(removeImportRequest)) {
			String uri = setHttpPar(baseuriPar);
			checkRequestParametersAllNotNull(baseuriPar);
			return removeOntImport(uri);
		}

		// the next four invocations deal with ontologies directly imported into the main model

		// downloads and imports an ontology from the web, caching it into a local file in the ontology
		// mirror
		// location
		else if (request.equals(addFromWebToMirrorRequest)) {
			String toImport = setHttpPar(baseuriPar);
			String destLocalFile = setHttpPar(mirrorFilePar);
			String altURL = setHttpPar(alturlPar);
			String rdfFormat = setHttpPar(rdfFormatPar);
			checkRequestParametersAllNotNull(baseuriPar, mirrorFilePar);
			return addOntImport(fromWebToMirror, toImport, altURL, destLocalFile, rdfFormat,
					addFromWebToMirrorRequest);
		}
		// downloads and imports an ontology from the web; next time the turkey is started, the ontology
		// will be imported again
		else if (request.equals(addFromWebRequest)) {
			String baseuri = setHttpPar(baseuriPar);
			String altURL = setHttpPar(alturlPar);
			String rdfFormat = setHttpPar(rdfFormatPar);
			checkRequestParametersAllNotNull(baseuriPar);
			return addOntImport(fromWeb, baseuri, altURL, null, rdfFormat, addFromWebRequest);
		}
		// downloads and imports an ontology from a local file; caching it into a local file in the
		// ontology mirror location
		else if (request.equals(addFromLocalFileRequest)) {
			String baseuri = setHttpPar(baseuriPar);

			File sourceFile = setHttpMultipartFilePar(localFilePar);
			if (sourceFile == null) // in case of missing multipart file in POST request
				throw new HTTPParameterUnspecifiedException(localFilePar);
			String localFilePath = sourceFile.getAbsolutePath();
			String mirrorFile = setHttpPar(mirrorFilePar);
			// String rdfFormat = setHttpPar(rdfFormatPar); commented, unless able to specifiy it even in
			// OntManager
			checkRequestParametersAllNotNull(baseuriPar, mirrorFilePar);
			return addOntImport(fromLocalFile, baseuri, localFilePath, mirrorFile, null,
					addFromLocalFileRequest);
		}
		// imports an ontology which is already present in the ontology mirror location
		else if (request.equals(addFromOntologyMirrorRequest)) {
			String baseuri = setHttpPar(baseuriPar);
			String mirrorFile = setHttpPar(mirrorFilePar);
			// String rdfFormat = setHttpPar(rdfFormatPar); commented, unless able to specifiy it even in
			// OntManager
			checkRequestParametersAllNotNull(baseuriPar, mirrorFilePar);
			return addOntImport(fromOntologyMirror, baseuri, null, mirrorFile, null,
					addFromOntologyMirrorRequest);
		}

		// the next four invocations deal with inherited imported ontologies (they are declared imports of
		// imported ontologies) downloaded and loaded into the main model

		// downloads an imported ontology from the web, caching it into a local file in the ontology
		// mirror
		// location
		else if (request.equals(downloadFromWebToMirrorRequest)) {
			String baseURI = setHttpPar(baseuriPar);
			String altURL = setHttpPar(alturlPar);
			String toLocalFile = setHttpPar(mirrorFilePar);
			checkRequestParametersAllNotNull(baseuriPar, mirrorFilePar);
			return getImportedOntology(fromWebToMirror, baseURI, altURL, null, toLocalFile);
		} else if (request.equals(downloadFromWebRequest)) {
			String baseURI = setHttpPar(baseuriPar);
			String altURL = setHttpPar(alturlPar);
			checkRequestParametersAllNotNull(baseuriPar);
			return getImportedOntology(fromWeb, baseURI, altURL, null, null);
		}
		// downloads an imported ontology from a local file; caching it into a local file in the ontology
		// mirror location
		else if (request.equals(getFromLocalFileRequest)) {
			String baseURI = setHttpPar(baseuriPar);
			String altURL = setHttpPar(alturlPar);
			String localFilePath = setHttpPar(localFilePathPar);
			String mirrorFile = setHttpPar(mirrorFilePar);
			checkRequestParametersAllNotNull(baseuriPar, localFilePathPar);
			return getImportedOntology(fromLocalFile, baseURI, altURL, localFilePath, mirrorFile);
		}
		// mirrors an ontology
		else if (request.equals(mirrorOntologyRequest)) {
			String baseURI = setHttpPar(baseuriPar);
			String mirrorFile = setHttpPar(mirrorFilePar);
			checkRequestParametersAllNotNull(baseuriPar, mirrorFilePar);
			return getImportedOntology(toOntologyMirror, baseURI, null, null, mirrorFile);
		}

		// NAMED GRAPHS
		else if (request.equals(getNamedGraphsRequest)) {
			return getNamedGraphs();
		}

		else
			return servletUtilities.createNoSuchHandlerExceptionResponse(request);

	}

	public Response getOntologyDescription() {
		OWLModel model = getOWLModel();
		String ontQName;
		try {
			ontQName = model.getQName(getProject().getNewOntologyManager().getBaseURI());
			return getResourceDescription(ontQName, RDFResourceRolesEnum.ontology, templateandvalued);
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		}
	}

	/**
	 * returns a the list of namedgraphs declared in the current ontology
	 * 
	 * @return
	 */
	public Response getNamedGraphs() {
		String request = getNamedGraphsRequest;
		try {
			ARTResourceIterator it = listNamedGraphs();
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
			Element dataElement = response.getDataElement();
			while (it.streamOpen()) {
				Element ngElem = XMLHelp.newElement(dataElement, "namedgraph");
				ngElem.setAttribute("uri", it.next().asURIResource().getURI());
			}
			it.close();
			return response;
		} catch (ModelAccessException e) {
			logger.error("" + e);
			return servletUtilities.createExceptionResponse(request, e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
	}

	/**
	 * sets the Default Namespace for the loaded ontology. The client just inspects the Ack tag; the ns
	 * attribute of the DefaultNamespace tag should always be inspected by the client (expecially if the ack
	 * is a failed updated)
	 * 
	 * @param namespace
	 * @return
	 */
	public Response setDefaultNamespace(String namespace) {
		XMLResponseREPLY response = ServletUtilities.getService()
				.createReplyResponse(setDefaultNamespaceRequest, RepliesStatus.ok);
		Element dataElement = response.getDataElement();

		Project<? extends RDFModel> currProj = getProject();

		try {
			currProj.setDefaultNamespace(namespace);
		} catch (ProjectUpdateException e) {
			e.printStackTrace();
			response.setReplyStatus(RepliesStatus.fail);
			response.setReplyMessage("not able to set Default Namespace" + e.getMessage());
		}
		// status already set to "ok"

		Element defaultNamespaceElement = XMLHelp.newElement(dataElement, "DefaultNamespace");
		defaultNamespaceElement.setAttribute("ns", currProj.getDefaultNamespace());

		return response;
	}

	/**
	 * gets the Default Namespace for the loaded ontology
	 * 
	 * 
	 */
	public Response getDefaultNamespace() {
		XMLResponseREPLY response = ServletUtilities.getService()
				.createReplyResponse(getDefaultNamespaceRequest, RepliesStatus.ok);
		Element dataElement = response.getDataElement();

		RDFModel ontModel = getOntModel();

		Element defaultNamespaceElement = XMLHelp.newElement(dataElement, "DefaultNamespace");
		defaultNamespaceElement.setAttribute("ns", ontModel.getDefaultNamespace());

		return response;
	}

	/**
	 * sets the baseuri for the loaded ontology. The client just inspects the Ack tag; content of the uri
	 * attribute of the baseuri tag should always be inspected by the client (expecially if the ack is a
	 * failed updated)
	 * 
	 */
	// Method moved to the service ModifyName
	/*
	 * public Response setBaseURI(String uri) { XMLResponseREPLY response =
	 * ServletUtilities.getService().createReplyResponse(setBaseuriRequest, RepliesStatus.ok); Element
	 * dataElement = response.getDataElement();
	 * 
	 * Project<? extends RDFModel> currProj = getProject();
	 * 
	 * try { currProj.setBaseURI(uri); // the status is already ok } catch (ProjectUpdateException e) {
	 * e.printStackTrace(); response.setReplyStatus(RepliesStatus.fail);
	 * response.setReplyMessage("not able to set BaseURI\n" + e.getMessage()); }
	 * 
	 * Element baseURI = XMLHelp.newElement(dataElement, baseuriTag); baseURI.setAttribute("uri",
	 * currProj.getBaseURI());
	 * 
	 * return response; }
	 */

	/**
	 * gets the baseuri for the loaded ontology
	 * 
	 * 
	 */
	public Response getBaseURI() {

		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(getBaseuriRequest,
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();

		String baseURI = getProject().getNewOntologyManager().getBaseURI();

		Element baseuri = XMLHelp.newElement(dataElement, baseuriTag);
		baseuri.setAttribute("uri", baseURI);

		return response;
	}

	/**
	 * sets the baseuri for the loaded ontology. The client just inspects the Ack tag; content of the uri
	 * attribute of the baseuri tag should always be inspected by the client (expecially if the ack is a
	 * failed updated)
	 * 
	 * <Tree type="setBaseURIAndDefaultNamespace"> <Ack msg="ok"/> (OR
	 * <Ack msg="failed" reason="put in a alert the text in this attribute"/>
	 * <BaseURI uri="http://art.info.uniroma2.it/ontologies/st"/>
	 * <DefaultNamespace ns="http://art.info.uniroma2.it/ontologies/st#"/> </Tree>
	 * 
	 */
	// Method moved to the service ModifyName, or at least the setBaseURI, the setDefaultNamespace is still
	// here
	/*
	 * public Response setBaseURIAndDefaultNamespace(String uri, String namespace) {
	 * 
	 * XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(
	 * setBaseuriDefNamespaceRequest, RepliesStatus.ok); Element dataElement = response.getDataElement();
	 * 
	 * Project<? extends RDFModel> currProj = getProject();
	 * 
	 * String oldDefNS = currProj.getDefaultNamespace();
	 * 
	 * try { currProj.setDefaultNamespace(namespace); // status already ok } catch (ProjectUpdateException e)
	 * { e.printStackTrace(); response.setReplyStatus(RepliesStatus.fail);
	 * response.setReplyMessage("defaultNamespace update failed:\n" + e.getMessage()); } try {
	 * currProj.setBaseURI(uri); // status already ok } catch (ProjectUpdateException e) { try {
	 * currProj.setDefaultNamespace(oldDefNS); } catch (ProjectUpdateException e1) { String errMsg =
	 * "when trying to update both baseuri and defaultnamespace, the defautnamespace has been changed, the baseuri update failed, then tried to roll back to the old defaultNameSpace and it failed too"
	 * ; logger.debug(errMsg, e1); e1.printStackTrace(); response.setReplyStatus(RepliesStatus.fail);
	 * response.setReplyMessage(errMsg + "\n" + e.getMessage()); } // TODO with transactions this method would
	 * be cleaner and more simple e.printStackTrace(); response.setReplyStatus(RepliesStatus.fail);
	 * response.setReplyMessage("baseURI update failed:\n" + e.getMessage()); }
	 * 
	 * Element baseURIElement = XMLHelp.newElement(dataElement, baseuriTag);
	 * baseURIElement.setAttribute("uri", currProj.getBaseURI()); Element defaultNamespaceElement =
	 * XMLHelp.newElement(dataElement, "DefaultNamespace"); defaultNamespaceElement.setAttribute("ns",
	 * currProj.getDefaultNamespace());
	 * 
	 * return response; }
	 */

	/**
	 * gets the namespace mapping for the loaded ontology
	 * 
	 * <Tree type="getNSPrefixMapping">
	 * <Mapping ns="http://www.w3.org.2002/07/owl#" prefix="owl" explicit="false"/>
	 * <Mapping ns="http://sweet.jpl.nasa.gov/ontology/earthrealm.owl#" prefix="earthrealm" explicit="true"/>
	 * </Tree>
	 * 
	 */
	public Response getNamespaceMappings() {
		XMLResponseREPLY response = ServletUtilities.getService()
				.createReplyResponse(getNSPrefixMappingsRequest, RepliesStatus.ok);
		Element dataElement = response.getDataElement();

		String request = getNSPrefixMappingsRequest;

		OntologyManager ontManager = getProject().getNewOntologyManager();

		Map<String, String> innerPrefixMap;
		try {
			innerPrefixMap = ontManager.getNSPrefixMappings(false);
			Set<String> explicitPrefixes = ontManager.getNSPrefixMappings(true).keySet();
			Set<Map.Entry<String, String>> mapEntries = innerPrefixMap.entrySet();
			for (Map.Entry<String, String> entry : mapEntries) {
				String namespace = entry.getValue();
				String prefix = entry.getKey();
				Element nsPrefMapElement = XMLHelp.newElement(dataElement, "Mapping");
				nsPrefMapElement.setAttribute(prefixPar, prefix);
				nsPrefMapElement.setAttribute("ns", namespace);
				nsPrefMapElement.setAttribute("explicit",
						Boolean.toString(explicitPrefixes.contains(prefix)));
			}
		} catch (OntologyManagerException e) {
			return ServletUtilities.getService().createExceptionResponse(request, e.getMessage());
		}

		return response;
	}

	/**
	 * sets the namespace mapping for the loaded ontology
	 * 
	 * <Tree type="NSPrefixMappingChanged"/>
	 */
	public Response setNamespaceMapping(String prefix, String namespace) {
		String request = setNSPrefixMappingRequest;
		ServletUtilities servletUtilities = new ServletUtilities();
		ResponseREPLY response = ServletUtilities.getService().createReplyResponse(request, RepliesStatus.ok);

		OntologyManager ontManager = getProject().getNewOntologyManager();

		try {
			ontManager.setNSPrefixMapping(prefix, namespace);
		} catch (ModelUpdateException e) {
			logger.error(Utilities.printFullStackTrace(e));
			return servletUtilities.createExceptionResponse(request,
					"prefix-namespace mapping update failed!\n\nreason: " + e.getMessage());
		} catch (NSPrefixMappingUpdateException e) {
			logger.error(Utilities.printFullStackTrace(e));
			return servletUtilities.createExceptionResponse(request, e.toString());
		}

		return response;
	}

	/**
	 * changes the namespace mapping for the loaded ontology. Since there is no evidence that any ontology API
	 * will ever use this (there is typically only a setNamespaceMapping method) we have not included a
	 * changeNamespaceMapping in the API and consequently we delegate here setNamespaceMapping. Should this
	 * situation change, this method will require a proper implementation.
	 * 
	 * <Tree type="NSPrefixMappingChanged"/>
	 */
	public Response changeNamespaceMapping(String prefix, String namespace) {
		return setNamespaceMapping(prefix, namespace);
	}

	/**
	 * remove the namespace mapping for the loaded ontology
	 * 
	 * <Tree type="NSPrefixMappingChanged"> <Mapping ns="http://www.w3.org.2002/07/owl#" prefix="owl" />
	 * </Tree>
	 */
	public Response removeNamespaceMapping(String namespace) {
		String request = removeNSPrefixMappingRequest;
		ServletUtilities servletUtilities = new ServletUtilities();
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(request,
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();

		OntologyManager ontManager = getProject().getNewOntologyManager();

		try {
			ontManager.removeNSPrefixMapping(namespace);
		} catch (ModelUpdateException e) {
			e.printStackTrace();
			return servletUtilities.createExceptionResponse(request,
					"prefix-namespace mapping update failed on the loaded ontology!\n\nreason: "
							+ e.getMessage());
		} catch (NSPrefixMappingUpdateException e) {
			e.printStackTrace();
			return servletUtilities.createExceptionResponse(request, e.toString());
		}

		Element nsPrefMapElement = XMLHelp.newElement(dataElement, "Mapping");
		nsPrefMapElement.setAttribute(prefixPar, namespace);

		return response;
	}

	/**
	 * Expands the given qname and returns the URI
	 * 
	 * @param qname
	 * @return
	 */
	public Response expandQName(String qname) {
		String request = expandQNameRequest;
		ServletUtilities servletUtilities = new ServletUtilities();
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(request,
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		OWLModel ontModel = getOWLModel();
		String uri = null;
		try {
			uri = ontModel.expandQName(qname);
		} catch (ModelAccessException e) {
			e.printStackTrace();
			return servletUtilities.createExceptionResponse(request, e.getMessage());
		}
		Element uriElem = XMLHelp.newElement(dataElement, "uri");
		uriElem.setAttribute("qname", qname);
		uriElem.setTextContent(uri);

		return response;
	}

	// vedere cmq se è possibile definire in qualche modo in javascript degli alberi infiniti
	/**
	 * gets the namespace mapping for the loaded ontology
	 * 
	 */
	public Response getOntologyImports() {
		String request = getImportsRequest;
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(request,
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();

		HashSet<String> importsBranch = new HashSet<String>();

		OWLModel ontModel = getOWLModel();
		OntologyManager repMgr = getProject().getNewOntologyManager();

		try {
			logger.debug("listing ontology imports");
			buildImportXMLTree(ontModel, repMgr, dataElement, getProject().getBaseURI(), importsBranch);
		} catch (ModelAccessException e) {
			return ServletUtilities.getService().createExceptionResponse(request, e);
		}

		return response;
	}

	private void buildImportXMLTree(OWLModel ontModel, OntologyManager repMgr, Element xmlElem, String uri,
			HashSet<String> importsBranch) throws ModelAccessException {
		ARTURIResource ont = ontModel.createURIResource(uri);
		ARTURIResourceIterator imports = ontModel.listOntologyImports(ont);
		while (imports.streamOpen()) {
			String importedOntURI = imports.getNext().getURI();
			logger.debug("\timport: " + importedOntURI);
			Element importedOntologyElem = XMLHelp.newElement(xmlElem, "ontology");
			importedOntologyElem.setAttribute("uri", importedOntURI);
			if (importsBranch.contains(importedOntURI))
				importedOntologyElem.setAttribute("status", "loop");
			else {
				ImportStatus importStatus = repMgr.getImportStatus(importedOntURI);
				if (importStatus != null) {
					ImportStatus.Values statusValue = importStatus.getValue();
					if (statusValue == ImportStatus.Values.LOCAL) {
						importedOntologyElem.setAttribute("localfile",
								importStatus.getCacheFile().getLocalName());
					}
					importedOntologyElem.setAttribute("status", statusValue.toString());

				} else
					importedOntologyElem.setAttribute("status", ImportStatus.Values.NULL.toString());

				HashSet<String> newImportsBranch = new HashSet<String>(importsBranch);
				newImportsBranch.add(importedOntURI);

				buildImportXMLTree(ontModel, repMgr, importedOntologyElem, importedOntURI, newImportsBranch);
			}

		}
	}

	// TODO se continuo ad usare il sistema di gestire una cache degli import, allora questo deve rimuovere in
	// cascata tutte le ontologie importate da quella che ho rimosso!
	/**
	 * answers with an ack on the result of the import deletion. The application, upon receving this ack,
	 * should request an update of the imports and namespace mappings panels
	 * 
	 * <Tree type="removeImport"> (or addFromWeb, addFromLocalFile, addFromOntologyMirror)
	 * <result level="ok"/> //oppure "failed" <msg content="bla bla bla"/> </Tree>
	 * 
	 * @param uri
	 * @return
	 */
	public Response removeOntImport(String uri) {
		String request = removeImportRequest;
		ServletUtilities servletUtilities = new ServletUtilities();
		OntologyManager repMgr = getProject().getNewOntologyManager();
		try {
			repMgr.removeOntologyImport(uri);
		} catch (IOException e) {
			e.printStackTrace();
			return servletUtilities.createExceptionResponse(request,
					"problems in accessing the Import Registry");
		} catch (ModelUpdateException e) {
			e.printStackTrace();
			return servletUtilities.createExceptionResponse(request, e);
		} catch (ModelAccessException e) {
			logger.error(Utilities.printFullStackTrace(e));
			return servletUtilities.createExceptionResponse(request, e);
		}

		String msg = null;
		ResponseREPLY response = ServletUtilities.getService().createReplyResponse(request, RepliesStatus.ok);
		if (msg == null)
			msg = uri + " correctly removed from import list";
		response.setReplyMessage(msg);
		return response;
	}

	/**
	 * answers with an ack on the result of the import. Th application, upon receving this ack, should request
	 * an update of the imports and namespace mappings panels
	 * 
	 * <Tree type="addFromWebToMirror"> (or addFromWeb, addFromLocalFile, addFromOntologyMirror)
	 * <result level="ok"/> //oppure "failed" <msg content="bla bla bla"/> </Tree>
	 * 
	 * 
	 * HINT for CLIENT: always launch a getNamespaceMappings, getOntologyImports after a setOntologyImports
	 * because an imported ontology may contain other prefix mappings to be imported
	 * 
	 */
	public Response addOntImport(int method, String baseUriToBeImported, String sourceForImport,
			String destLocalFile, String rdfFormatName, String requestString) {

		logger.debug("Import Request; method: " + method + ", baseuritobeimported: " + baseUriToBeImported
				+ "\nsourceForImport: " + sourceForImport + ", destLocalFile: " + destLocalFile + ", req: "
				+ requestString);

		// NECESSARY INITIALIZATION
		ServletUtilities servletUtilities = new ServletUtilities();
		String msg = null;
		String oldCache;
		OntologyManager ontMgr = getProject().getNewOntologyManager();

		// CHECKS THAT THE ONTOLOGY IS NOT ALREADY IMPORTED
		// previously used ImportMem, now deprecated
		ImportStatus impStatus = ontMgr.getImportStatus(baseUriToBeImported);
		if ((impStatus != null))
			if ((impStatus.getValue() == ImportStatus.Values.LOCAL)
					|| (impStatus.getValue() == ImportStatus.Values.WEB))
				return servletUtilities.createExceptionResponse(requestString,
						"this ontology is already imported");

		// if the ontology is not imported BUT is in the OntologiesMirror, then load the ontology from the
		// local file in the mirror
		if ((method != fromOntologyMirror)
				&& (oldCache = OntologiesMirror.getMirroredOntologyEntry(baseUriToBeImported)) != null) {
			msg = "this ontology has already been cached in the mirror to file: " + oldCache
					+ ". Used mirror instead of new file.";
			destLocalFile = oldCache;
			method = fromOntologyMirror;
		}

		// IMPORT METHOD SWITCHER
		String request = null;
		try {
			logger.debug("rdf format specified by user as: " + rdfFormatName);
			org.eclipse.rdf4j.rio.RDFFormat rdfFormat = (rdfFormatName != null)
					? RDFFormatConverter.convert(RDFFormat.parseFormat(rdfFormatName)) : null;
			logger.debug("selected rdf format: " + rdfFormat);

			if (method == fromWebToMirror) {
				request = addFromWebToMirrorRequest;
				String url;
				if (sourceForImport != null)
					url = sourceForImport;
				else
					url = baseUriToBeImported;
				ontMgr.addOntologyImportFromWebToMirror(baseUriToBeImported, url, destLocalFile, rdfFormat,
						TransitiveImportMethodAllowance.mirrorFallbackToWeb, new HashSet<>());
			} else if (method == fromWeb) {
				request = addFromWebRequest;
				String url;
				if (sourceForImport != null)
					url = sourceForImport;
				else
					url = baseUriToBeImported;
				ontMgr.addOntologyImportFromWeb(baseUriToBeImported, url, rdfFormat,
						TransitiveImportMethodAllowance.mirrorFallbackToWeb, new HashSet<>());
			} else if (method == fromLocalFile) {
				request = addFromLocalFileRequest;
				ontMgr.addOntologyImportFromLocalFile(baseUriToBeImported, sourceForImport, destLocalFile,
						TransitiveImportMethodAllowance.mirrorFallbackToWeb, new HashSet<>());
			} else if (method == fromOntologyMirror) {
				request = addFromOntologyMirrorRequest;
				ontMgr.addOntologyImportFromMirror(baseUriToBeImported, destLocalFile,
						TransitiveImportMethodAllowance.mirrorFallbackToWeb, new HashSet<>());
			} else
				request = "noCorrectRequestGiven!!!"; // we should never incur into it because it is filtered
			// by the getResponse switch method
		} catch (MalformedURLException e) {
			logger.error(Utilities.printStackTrace(e));
			return servletUtilities.createExceptionResponse(request, e.getMessage() + " is not a valid URI!");
		} catch (ModelUpdateException | UnsupportedRDFFormatException e) {
			logger.error(Utilities.printStackTrace(e));
			return servletUtilities.createExceptionResponse(request, e.getMessage());
		}

		// AUTOMATIC CREATION OF CUSTOM PREFIX
		PrefixMapping model = getOntModel();
		Map<String, String> nsPrefixMap;
		try {
			nsPrefixMap = model.getNamespacePrefixMapping();
			Set<Map.Entry<String, String>> mapEntries = nsPrefixMap.entrySet();
			for (Map.Entry<String, String> entry : mapEntries) {
				String prefix = entry.getValue();
				if (isAutomaticallyGeneratedPrefix(prefix)) {
					String namespace = entry.getKey();
					String newPrefix = ModelUtilities.guessPrefix(namespace);
					try {
						logger.info("prefix " + prefix + "for: " + namespace
								+ " is probably assigned by the triple store; replacing it with a guessed prefix: "
								+ newPrefix);
						model.setNsPrefix(newPrefix, namespace);
					} catch (ModelUpdateException e) {
						logger.error(Utilities.printStackTrace(e));
					}
				}
			}
		} catch (ModelAccessException e) {
			return servletUtilities.createExceptionResponse(request, e);
		}

		// FORMATTING OF XML RESPONSE
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(request,
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		dataElement.setAttribute("type", request);
		if (msg == null)
			msg = baseUriToBeImported + " correctly imported";
		response.setReplyMessage(msg);
		return response;

	}

	private boolean isAutomaticallyGeneratedPrefix(String prefix) {
		if (prefix.startsWith("ns"))
			return true;
		else
			return false;
	}

	/**
	 * answers with an ack on the result of the import. Th application, upon receving this ack, should request
	 * an update of the imports and namespace mappings panels
	 * 
	 * <Tree type="getFromWebToMirror"> (or ....) <result level="ok"/> //oppure "failed"
	 * <msg content="bla bla bla"/> </Tree>
	 * 
	 * 
	 * HINT for CLIENT: always launch a getNamespaceMappings, getOntologyImports after a setOntologyImports
	 * because an imported ontology may contain other prefix mappings to be imported
	 * 
	 */
	public Response getImportedOntology(int method, String baseURI, String altURL, String fromLocalFilePath,
			String toLocalFile) {
		ServletUtilities servletUtilities = new ServletUtilities();
		OntologyManager repMgr = getProject().getNewOntologyManager();
		String request = null;
		try {
			if (method == fromWebToMirror) {
				request = "getFromWebToMirror";
				repMgr.downloadImportedOntologyFromWebToMirror(baseURI, altURL, toLocalFile,
						TransitiveImportMethodAllowance.mirrorFallbackToWeb, new HashSet<>());
			} else if (method == fromWeb) {
				request = "getFromWeb";
				repMgr.downloadImportedOntologyFromWeb(baseURI, altURL,
						TransitiveImportMethodAllowance.mirrorFallbackToWeb, new HashSet<>());
			} else if (method == fromLocalFile) {
				request = "getFromLocalFile";
				repMgr.getImportedOntologyFromLocalFile(baseURI, fromLocalFilePath, toLocalFile,
						TransitiveImportMethodAllowance.mirrorFallbackToWeb, new HashSet<>());
			} else if (method == toOntologyMirror) {
				request = "getToOntologyMirror";
				repMgr.mirrorOntology(baseURI, toLocalFile);
			} else
				request = "noMethodGiven!!!";
		} catch (MalformedURLException e) {
			return servletUtilities.createExceptionResponse(request, altURL + " is not a valid URL!");
		} catch (ModelUpdateException e) {
			logger.debug(Utilities.printStackTrace(e));
			return servletUtilities.createExceptionResponse(request, "problems in updating the ontModel");
		} catch (ImportManagementException e) {
			logger.debug(Utilities.printStackTrace(e));
			return servletUtilities.createExceptionResponse(request, e.getMessage());
		} catch (RDF4JException e) {
			logger.debug(Utilities.printStackTrace(e));
			return servletUtilities.createExceptionResponse(request, e.getMessage());
		} catch (IOException e) {
			logger.debug(Utilities.printStackTrace(e));
			return servletUtilities.createExceptionResponse(request, e.getMessage());
		}

		ResponseREPLY response = ServletUtilities.getService().createReplyResponse(request, RepliesStatus.ok);
		response.setReplyMessage(baseURI + " correctly imported");
		return response;

	}

}
