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

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.filter.ConceptsInSchemePredicate;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.RDFSModel;
import it.uniroma2.art.owlart.models.SKOSModel;
import it.uniroma2.art.owlart.models.SKOSXLModel;
import it.uniroma2.art.owlart.navigation.ARTLiteralIterator;
import it.uniroma2.art.owlart.navigation.ARTResourceIterator;
import it.uniroma2.art.owlart.navigation.ARTURIResourceIterator;
import it.uniroma2.art.owlart.utilities.ModelUtilities;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.exceptions.HTTPParameterUnspecifiedException;
import it.uniroma2.art.semanticturkey.exceptions.NonExistingRDFResourceException;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.plugin.extpts.ServiceAdapter;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.CompareNames;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import com.google.common.collect.Iterators;

/**Classe che effettua la ricerca di una parola all'interno dell'ontologia*/
/**
 * @author Donato Griesi, Armando Stellato Contributor(s): Andrea Turbati
 */
@Component
public class OntoSearch extends ServiceAdapter {
	protected static Logger logger = LoggerFactory.getLogger(OntoSearch.class);
	// public String XSLpath = Profile.getUserDataPath() + "/components/lib/xsl/search.xsl";
	public final double THRESHOLD = 0.70;

	public static String searchOntologyRequest = "searchOntology";
	public static String getPathFromRootRequest = "getPathFromRoot";

	@Autowired
	public OntoSearch(@Value("OntoSearch") String id) {
		super(id);
	}

	public Logger getLogger() {
		return logger;
	}

	/**
	 * Metodo che effettua la ricerca di una parola all'interno dell'ontologia e restituisce l'elemento xml
	 * contenente la lista dei risultati
	 * 
	 * @return Document xml
	 */
	public Response getPreCheckedResponse(String request) throws HTTPParameterUnspecifiedException {
		ServletUtilities servletUtilities = new ServletUtilities();
		this.fireServletEvent();
		if (request.equals(searchOntologyRequest)) {
			String inputString = setHttpPar("inputString");
			String types = setHttpPar("types");
			String scheme = setHttpPar("scheme");
			checkRequestParametersAllNotNull("inputString", "types");
			return searchOntology(inputString, types, scheme); // types
		} else if (request.equals(getPathFromRootRequest)){
			String conceptName = setHttpPar("concept");
			String schemeName = setHttpPar("scheme");
			checkRequestParametersAllNotNull("concept", "scheme");
			return getPathFromRoot(conceptName, schemeName);
		} else
			return servletUtilities.createNoSuchHandlerExceptionResponse(request);

	}

	/**
	 * if an exact match is obtained, return only this result, otherwise search for ontology objects with
	 * similar localnames. if namespace (prefix) is given, search only for objects with similar localnames
	 * with that namespace (prefix)
	 * 
	 * <Tree type="ontSearch"> <found name="rtv:phoneNumber" type="owl:DatatypeProperty"/> <found
	 * name="rtv:produces" type="owl:ObjectProperty"/> <found name="rtv:role" type="owl:ObjectProperty"/>
	 * </Tree>
	 * 
	 * admitted types are given by strings in {@link}VocabularyTypesStrings class
	 * 
	 * @param inputString
	 * @param types can be "clsNInd", "property" or "concept"
	 * @param scheme useful only if types=="concept"
	 * @return
	 */
	public Response searchOntology(String inputString, String types, String scheme) {
		ServletUtilities servletUtilities = ServletUtilities.getService();
		String request = searchOntologyRequest;
		logger.debug("searchString: " + inputString);

		// consistency check on proposed types
		if (!types.equals("property") && !types.equals("clsNInd") && !types.equals("concept"))
			return servletUtilities.createExceptionResponse(request,
					"\"types\" parameter not correctly specified in GET request");

		RDFModel ontModel = getOntModel();
		ArrayList<Struct> results = new ArrayList<Struct>();

		String inputStringExpandedQName;
		try {
			inputStringExpandedQName = ontModel.expandQName(inputString);
			URI uri;
			boolean wellFormedAndAbsolute = true;
			try {
				uri = new URI(inputStringExpandedQName);
				wellFormedAndAbsolute = uri.isAbsolute();
			} catch (URISyntaxException e) {
				wellFormedAndAbsolute = false;
			}

			logger.debug("inputStringExpandedQName: " + inputStringExpandedQName + " well-formed&Absolute: "
					+ wellFormedAndAbsolute);

			// STARRED TODO optimize it!
			ARTURIResource perfectMatchingResource = null;
			if (wellFormedAndAbsolute)
				perfectMatchingResource = ontModel.createURIResource(inputStringExpandedQName);
			/* 
			 * The following IF block has been commented and replace with the succeeding one to 
			 * avoid strange results in case of perfect match in other types of the requested one:
			 * For example, if there's a concept with local name "prefLabel", the inputString 
			 * is "prefLabel" and the types is "property", it will find and return the concept 
			 * prefLabel as perfect match, despite it was ask only "property" types. 
			 */
//			if ((perfectMatchingResource != null) && (ontModel.existsResource(perfectMatchingResource))) {
//				results.add(new Struct(ModelUtilities.getResourceRole(perfectMatchingResource, ontModel), 
//						perfectMatchingResource, null, 1));
//			}
			if ((perfectMatchingResource != null) && ontModel.existsResource(perfectMatchingResource)) {
				if (types.equals("property") && ontModel.isProperty(perfectMatchingResource, NodeFilters.MAINGRAPH)){
					results.add(new Struct(ModelUtilities.getResourceRole(perfectMatchingResource, ontModel), 
							perfectMatchingResource, null, 1));
				} else if (types.equals("clsNInd") && ontModel instanceof OWLModel){
					if (((OWLModel) ontModel).isClass(perfectMatchingResource, NodeFilters.MAINGRAPH)){
						results.add(new Struct(ModelUtilities.getResourceRole(perfectMatchingResource, ontModel), 
								perfectMatchingResource, null, 1));
					} else { //check if is an instance/individual
						ARTResourceIterator instances = ((OWLModel) ontModel).listInstances(NodeFilters.ANY, true, NodeFilters.MAINGRAPH);
						while (instances.hasNext()){
							ARTResource i = instances.next();
							if (i.equals(perfectMatchingResource)){
								results.add(new Struct(ModelUtilities.getResourceRole(perfectMatchingResource, ontModel), 
										perfectMatchingResource, null, 1));
								break;
							}
						}
					}
				} else if (types.equals("concept") && ontModel instanceof SKOSModel && 
						((SKOSModel) ontModel).isConcept(perfectMatchingResource, NodeFilters.MAINGRAPH)) {
					results.add(new Struct(ModelUtilities.getResourceRole(perfectMatchingResource, ontModel), 
							perfectMatchingResource, null, 1));
				}
			}
			//if a perfect match has been found but not for the given types, the results could be still empty
			if (results.isEmpty()) {
				System.out.println("NO perfect match "); //TODO remove
				
				String searchStringNamespace = null;
				String searchStringLocalName = null;
				String searchStringPrefix = null;
				boolean namespaceGiven = false;
				boolean prefixGiven = false;

				if (inputString.contains("#")) {
					searchStringNamespace = inputString.substring(0, inputString.lastIndexOf("#") + 1);
					searchStringLocalName = inputString.substring(inputString.lastIndexOf("#") + 1);
					namespaceGiven = true;
				} else if (inputString.contains("/")) {
					searchStringNamespace = inputString.substring(0, inputString.lastIndexOf("/") + 1);
					searchStringLocalName = inputString.substring(inputString.lastIndexOf("/") + 1);
					namespaceGiven = true;
				} else if (inputString.contains(":")) {
					searchStringLocalName = inputString.substring(inputString.lastIndexOf(":") + 1);
					searchStringPrefix = inputString.substring(0, inputString.lastIndexOf(":"));
					searchStringNamespace = ontModel.getNSForPrefix(searchStringPrefix);
					namespaceGiven = true;
					prefixGiven = true;
				} else
					searchStringLocalName = inputString;

//				System.out.println(searchStringNamespace + " " + searchStringLocalName);
//				System.out.println("searchStringNamespace availability: "
//						+ ModelUtilities.isAvailableNamespace(ontModel, searchStringNamespace));

				if (namespaceGiven && !ModelUtilities.isAvailableNamespace(ontModel, searchStringNamespace)) {
					logger.debug("namespace: "
							+ searchStringNamespace
							+ " associated to prefix: "
							+ searchStringPrefix
							+ " is not recognized in this ontology, please use an existing prefix to "
							+ "restrict your search or do not use a prefix at all to search the "
							+ "whole ontology");
					if (prefixGiven)
						return servletUtilities
								.createExceptionResponse(
										request,
										"namespace: "
												+ searchStringNamespace
												+ " associated to prefix: "
												+ searchStringPrefix
												+ " is not recognized in this ontology, please use an "
												+ "existing prefix to restrict your search or do not use "
												+ " a prefix at all to search the whole ontology");
					else
						return servletUtilities
								.createExceptionResponse(
										request,
										"namespace: "
												+ searchStringNamespace
												+ " is not recognized in this ontology, please use an "
												+ "existing namespace to restrict your search or do not "
												+ "use a namespace at all to search the whole ontology");
				}

				if (types.equals("clsNInd")) {
					ARTURIResourceIterator searchedResources;
					if (ontModel instanceof RDFSModel) {
						searchedResources = ((RDFSModel) ontModel).listNamedClasses(true,
								NodeFilters.MAINGRAPH);
						logger.debug("collectResults for classes: ");
						collectResults(searchedResources, ontModel, results, searchStringNamespace,
								searchStringLocalName, namespaceGiven);
					}
					logger.debug("collectResults for instances: ");
					searchedResources = ontModel.listNamedInstances();
					collectResults(searchedResources, ontModel, results, searchStringNamespace, 
							searchStringLocalName, namespaceGiven);
				} else if(types.equals("property")){ // property
					ARTURIResourceIterator searchedProperties;

					if (ontModel instanceof OWLModel) {

						searchedProperties = ((OWLModel) ontModel).listObjectProperties(true,
								NodeFilters.MAINGRAPH);
						collectResults(searchedProperties, ontModel, results, searchStringNamespace,
								searchStringLocalName, namespaceGiven);

						searchedProperties = ((OWLModel) ontModel).listDatatypeProperties(true,
								NodeFilters.MAINGRAPH);
						collectResults(searchedProperties, ontModel, results, searchStringNamespace,
								searchStringLocalName, namespaceGiven);

						searchedProperties = ((OWLModel) ontModel).listAnnotationProperties(true,
								NodeFilters.MAINGRAPH);
						collectResults(searchedProperties, ontModel, results, searchStringNamespace,
								searchStringLocalName, namespaceGiven);

					}

					searchedProperties = ontModel.listProperties(NodeFilters.MAINGRAPH);
					collectResults(searchedProperties, ontModel, results, searchStringNamespace, 
							searchStringLocalName, namespaceGiven);
				} else if(types.equals("concept")){
					SKOSModel skosModel = (SKOSModel) ontModel;
					ARTURIResourceIterator searchedConcepts = skosModel.listConcepts(true, NodeFilters.MAINGRAPH);
					ARTURIResource schemeRes = null;
					if (scheme != null) {
						schemeRes = skosModel.createURIResource(scheme); 
						Iterator<ARTURIResource> filteredForScheme = Iterators.filter(
								searchedConcepts, ConceptsInSchemePredicate.getFilter(skosModel, schemeRes, NodeFilters.MAINGRAPH));
						collectResults(filteredForScheme, ontModel, results, searchStringNamespace, 
								searchStringLocalName,  namespaceGiven);
					} else {					
						collectResults(searchedConcepts, ontModel, results, searchStringNamespace, 
								searchStringLocalName,  namespaceGiven);
					}
				}

			}

			StructComparator sc = new StructComparator();
			Collections.sort(results, sc);

//			System.out.println("results: " + results);

		} catch (ModelAccessException e) {
			return ServletUtilities.getService().createExceptionResponse(request, e);
		}

		return xmlizeResults(ontModel, results);
	}

	private Response xmlizeResults(RDFModel rep, ArrayList<Struct> results) {
		String request = searchOntologyRequest;
		
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(request,
				RepliesStatus.ok);
		//Element dataElement = response.getDataElement();
		try {
			ARTResource wgraph = getWorkingGraph();
			ARTResource[] graphs = getUserNamedGraphs();
			Collection<STRDFResource> resultsCollection = STRDFNodeFactory.createEmptyResourceCollection();
			for (Struct result : results) {
				STRDFResource stResult = STRDFNodeFactory.createSTRDFResource(rep, result._resource, 
						result._type,  servletUtilities.checkWritable(rep, result._resource, wgraph),
						false);
				ClsOld.setRendering((RDFSModel)rep, stResult, null, null, graphs);
				resultsCollection.add(stResult);
			}
			RDFXMLHelp.addRDFNodes(response, resultsCollection);
		} catch (DOMException e) {
			return logAndSendException(e);
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}

		return response;
	}

	private void collectResults(Iterator<ARTURIResource> searchedResources, RDFModel ontModel, 
			ArrayList<Struct> results,  String searchStringNamespace, String searchStringLocalName, 
			boolean namespaceGiven)  throws ModelAccessException {
		System.out.println("collecting result "); //TODO remove
		double match;
		while (searchedResources.hasNext()) {
			ARTURIResource nextRes = searchedResources.next();
			System.out.println("comparing resource: " + nextRes);
			if (checkNS(namespaceGiven, nextRes.getNamespace(), searchStringNamespace))
				if ((match = CompareNames
						.compareSimilarNames(nextRes.getLocalName(), searchStringLocalName)) >= THRESHOLD){
					Struct s = new Struct(ModelUtilities.getResourceRole(nextRes, ontModel), nextRes, null, match);
					if (!listContainsStruct(results, s))
						results.add(s);
				}
					
			
			//in skos/skoskl search also inside the labels/xlabels
			if(ontModel instanceof SKOSXLModel){
				SKOSXLModel skosxlModel = ((SKOSXLModel)ontModel);
				//check the Preferred labels and the Alternative Labels
				ARTResourceIterator labelIter;
				//Preferred XLabels
				labelIter = skosxlModel.listPrefXLabels(nextRes, NodeFilters.MAINGRAPH);
				while(labelIter.hasNext()){
					ARTResource prefLabelRes = labelIter.next();
					ARTLiteral labelLiteral = skosxlModel.getLiteralForm(prefLabelRes, NodeFilters.MAINGRAPH);
					String label = labelLiteral.getLabel();
					if ((match = CompareNames.compareSimilarNames(label, searchStringLocalName)) >= THRESHOLD){
						Struct s = new Struct(ModelUtilities.getResourceRole(nextRes, ontModel), nextRes, null, match);
						if (!listContainsStruct(results, s))
							results.add(s);
					}
				}
				//Alternative XLabels
				labelIter = skosxlModel.listAltXLabels(nextRes, NodeFilters.MAINGRAPH);
				while(labelIter.hasNext()){
					ARTURIResource prefLabelRes = labelIter.next().asURIResource();
					ARTLiteral labelLiteral = skosxlModel.getLiteralForm(prefLabelRes, NodeFilters.MAINGRAPH);
					String label = labelLiteral.getLabel();
					if ((match = CompareNames.compareSimilarNames(label, searchStringLocalName)) >= THRESHOLD){
						Struct s = new Struct(ModelUtilities.getResourceRole(nextRes, ontModel), nextRes, null, match);
						if (!listContainsStruct(results, s))
							results.add(s);
					}
				}
			} else if(ontModel instanceof SKOSModel){
				SKOSModel skosModel = (SKOSModel)ontModel;
				ARTLiteralIterator labelIter;
				//Preferred Label
				labelIter = skosModel.listPrefLabels(nextRes, true, NodeFilters.MAINGRAPH);
				while(labelIter.hasNext()){
					String label = labelIter.next().getLabel();
					if ((match = CompareNames.compareSimilarNames(label, searchStringLocalName)) >= THRESHOLD){
						Struct s = new Struct(ModelUtilities.getResourceRole(nextRes, ontModel), nextRes, null, match);
						if (!listContainsStruct(results, s))
							results.add(s);
					}
				}
				//Alternative Label
				labelIter = ((SKOSModel)ontModel).listAltLabels(nextRes, true, NodeFilters.MAINGRAPH);
				while(labelIter.hasNext()){
					String label = labelIter.next().getLabel();
					if ((match = CompareNames.compareSimilarNames(label, searchStringLocalName)) >= THRESHOLD){
						Struct s = new Struct(ModelUtilities.getResourceRole(nextRes, ontModel), nextRes, null, match);
						if (!listContainsStruct(results, s))
							results.add(s);
					}
				}
			}
		}
	}

	/**
	 * the test is passed if the search String did not contain a namespace or if its namespace equals the
	 * namespace of the considered resource
	 * 
	 * @param namespaceGiven
	 * @param iteratedNamespace
	 * @param searchStringNamespace
	 * @return
	 */
	private boolean checkNS(boolean namespaceGiven, String iteratedNamespace, String searchStringNamespace) {
		if (namespaceGiven)
			return (searchStringNamespace.equals(iteratedNamespace));
		else
			return true;
	}

		
	private class Struct {
		public RDFResourceRolesEnum _type;
		public ARTURIResource _resource;
		// public String _lexicalization;
		public double _value;

		public Struct(RDFResourceRolesEnum type, ARTURIResource resource, String lexicalization, 
				double value) {
			_type = type;
			_resource = resource;
			// _lexicalization = lexicalization;
			_value = value;
		}

		public String toString() {
			return (_resource + ";type:" + _type + "match:" + _value);
		}

	}
	
	private boolean listContainsStruct(List<Struct> list, Struct struct){
		for (Struct s : list){
			if (s._resource.equals(struct._resource))
				return true;
		}
		return false;
	}

	private class StructComparator implements Comparator<Struct> {

		public int compare(Struct struct0, Struct struct1) {

			if (struct0._value > struct1._value)
				return -1;
			else if (struct0._value == struct1._value)
				return 0;
			return 1;
		}
	}
	
	/**
	 * At the moment returns just a path that goes from a root to the given concept in the given scheme
	 * @param concept uri (not QName)
	 * @param scheme uri (not QName)
	 * @return
	 */
	public Response getPathFromRoot(String concept, String scheme) {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		
		SKOSModel skosModel = (SKOSModel) getOntModel();
		try {
			ARTURIResource conceptRes = retrieveExistingURIResource(skosModel, concept, getUserNamedGraphs());
			ARTURIResource schemeRes = null;
			if (scheme != null) {
				schemeRes = retrieveExistingURIResource(skosModel, scheme, getUserNamedGraphs());
			}
			List<ARTURIResource> path = new ArrayList<ARTURIResource>();
			path = getPathToRoot(path, skosModel, conceptRes, schemeRes);
			Collections.reverse(path);

			Element pathElem = XMLHelp.newElement(response.getDataElement(), "path");
			pathElem.setAttribute("concept", concept);
			if (path != null){
				for (ARTURIResource c : path){
					Element concElem = XMLHelp.newElement(pathElem, "concept");
					concElem.setAttribute("show", c.getLocalName());
					concElem.setTextContent(c.getNominalValue());
				}
			}
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
		return response;
	}
	
	//Returns a single path that goes from the given concept to a topConcept
	private List<ARTURIResource> getPathToRoot(List<ARTURIResource> path, SKOSModel skosModel, ARTURIResource concept, ARTURIResource scheme) 
			throws ModelAccessException, NonExistingRDFResourceException {
		if (path.contains(concept)){
			//detected cycle, returning null path
			return null;
		}
		path.add(concept);
		if (skosModel.isTopConcept(concept, scheme, NodeFilters.MAINGRAPH)){
			//concept is top concept, returning a valid path			
			return path;
		} else {
			Iterator<ARTURIResource> itBroad = getBroaders(skosModel, concept, scheme);
			if (itBroad.hasNext()){
				while (itBroad.hasNext()) {
					ARTURIResource broader = itBroad.next();
					ArrayList<ARTURIResource> tempPath = new ArrayList<ARTURIResource>(path);
					List<ARTURIResource> returnedPath = getPathToRoot(tempPath, skosModel, broader, scheme);
					if (returnedPath != null){
						//Returned valid path from recursive call. Return it recursively.
						return returnedPath;
					} //otherwise continue, trying to explore paths from other broaders to the root
				}
			} else { //concept is not top concept and has no broader (concept is dangling) => try next path to root
				return null;
			}
		}
		//no valid path found from concept to root, returnting null path
		return null;
	}	
	
	//Return broaders of a concept belonging to the given scheme
	private Iterator<ARTURIResource> getBroaders(SKOSModel skosModel, ARTURIResource concept, ARTURIResource scheme) 
			throws ModelAccessException, NonExistingRDFResourceException{
		ARTURIResourceIterator unfilteredIt = skosModel.listBroaderConcepts(concept, false, true, getUserNamedGraphs());
		Iterator<ARTURIResource> it;
		if (scheme != null) {
			it = Iterators.filter(unfilteredIt, ConceptsInSchemePredicate.getFilter(skosModel, scheme, getUserNamedGraphs()));
		} else {
			it = unfilteredIt;
		}
		unfilteredIt.close();
		return it;
	}

}
