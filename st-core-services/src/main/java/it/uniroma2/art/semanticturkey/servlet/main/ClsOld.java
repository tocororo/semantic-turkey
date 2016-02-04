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
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.filter.NoLanguageResourcePredicate;
import it.uniroma2.art.owlart.filter.NoSpecificResourcePredicate;
import it.uniroma2.art.owlart.filter.RootClassesResourcePredicate;
import it.uniroma2.art.owlart.filter.URIResourcePredicate;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTBNode;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.syntax.manchester.ManchesterClassInterface;
import it.uniroma2.art.owlart.model.syntax.manchester.ManchesterParser;
import it.uniroma2.art.owlart.models.DirectReasoning;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.RDFSModel;
import it.uniroma2.art.owlart.navigation.ARTNodeIterator;
import it.uniroma2.art.owlart.navigation.ARTResourceIterator;
import it.uniroma2.art.owlart.navigation.ARTURIResourceIterator;
import it.uniroma2.art.owlart.navigation.RDFIterator;
import it.uniroma2.art.owlart.utilities.ModelUtilities;
import it.uniroma2.art.owlart.utilities.RDFIterators;
import it.uniroma2.art.owlart.vocabulary.OWL;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.semanticturkey.exceptions.HTTPParameterUnspecifiedException;
import it.uniroma2.art.semanticturkey.exceptions.NonExistingRDFResourceException;
import it.uniroma2.art.semanticturkey.filter.DomainResourcePredicate;
import it.uniroma2.art.semanticturkey.filter.STRootClassesResourcePredicate;
import it.uniroma2.art.semanticturkey.ontology.STOntologyManager;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.Config;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;

/**
 * This class provides services for
 * <ul>
 * <li>generating the class tree</li>
 * <li>generating the instance list for a given class ( <em> move it to {@link Individual} ?</em>)</li>
 * <li>creating a new instance from a given class( <em> move it to {@link Individual} !</li>
 * </ul>
 * 
 * @author Armando Stellato, Andrea Turbati, Donato Griesi
 * 
 */
@Component
public class ClsOld extends ResourceOld {

	// GET REQUESTS
	public static final String getClassTreeRequest = "getClassTree";
	public static final String getClassesInfoAsRootsForTreeRequest = "getClassesInfoAsRootsForTree";
	public static final String getClassAndInstancesInfoRequest = "getClassAndInstancesInfo";
	public static final String getSuperClassesRequest = "getSuperClasses";
	public static final String getSubClassesRequest = "getSubClasses";

	// CREATE REQUESTS
	public static final String createInstanceRequest = "createInstance";
	public static final String createClassRequest = "createClass";

	// ADD REQUESTS
	public static final String addOneOfRequest = "addOneOf";
	public static final String addTypeRequest = "addType";
	public static final String addSuperClsRequest = "addSuperCls";
	public static final String addIntersectionOfRequest = "addIntersectionOf";
	public static final String addUnionOfRequest = "addUnionOf";

	// REMOVE REQUESTS
	public static final String removeOneOfRequest = "removeOneOf";
	public static final String removeTypeRequest = "removeType";
	public static final String removeSuperClsRequest = "removeSuperCls";
	public static final String removeIntersectionOfRequest = "removeIntersectionOf";
	public static final String removeUnionOfRequest = "removeUnionOf";

	// PARS
	static final public String clsQNameField = "clsName";
	static final public String instanceNamePar = "instanceName";
	static final public String superClassNamePar = "superClassName";
	static final public String newClassNamePar = "newClassName";
	static final public String individualsPar = "individuals";

	static final public String clsQNamePar = "clsqname";
	static final public String clsesQNamesPar = "clsesqnames";
	static final public String typeQNamePar = "typeqname";
	static final public String treePar = "tree";
	static final public String instNumPar = "instNum";
	static final public String directInstPar = "direct";
	static final public String hasSubClassesPar = "hasSubClasses";
	static final public String labelQueryPar = "labelQuery";
	static final public String clsDescriptionsPar = "clsDescriptions";
	static final public String collectionNodePar = "collectionNode";

	// final private String subTree = "subTree";

	// VALUES
	static final public String templateandvalued = "templateandvalued";

	static final public String allInstances = "false";
	static final public String directInstances = "true";

	protected static Logger logger = LoggerFactory.getLogger(ClsOld.class);

	private Individual individual;

	@Autowired
	public ClsOld(@Value("Cls") String id, Individual individual) {
		super(id);
		this.individual = individual;
	}

	public Logger getLogger() {
		return logger;
	}

	public Response getPreCheckedResponse(String request) throws HTTPParameterUnspecifiedException {
		logger.debug("request to cls");

		Response response = null;
		// Individual individual = new Individual("individual");

		// all new fashoned requests are put inside these grace brackets

		if (request == null)
			return ServletUtilities.getService().createNoSuchHandlerExceptionResponse(request);

		// GET CLASS METHODS
		if (request.equals(getClassTreeRequest)) {
			response = createClassXMLTree();
		} else if (request.equals(getClassAndInstancesInfoRequest)) {
			String clsQName = setHttpPar(clsQNameField);
			String listMod = setHttpPar(directInstPar);
			boolean hasSubClassesReq = setHttpBooleanPar(hasSubClassesPar);
			checkRequestParametersAllNotNull(clsQNameField);
			response = getClassAndInstancesInfo(clsQName, listMod, hasSubClassesReq);
		} else if (request.equals(getSuperClassesRequest)) {
			String clsQName = setHttpPar(clsQNameField);
			checkRequestParametersAllNotNull(clsQNameField);
			response = getSuperClasses(clsQName);
		} else if (request.equals(getSubClassesRequest)) {
			String clsQName = setHttpPar(clsQNameField);
			boolean tree = setHttpBooleanPar(treePar);
			boolean instNum = setHttpBooleanPar(instNumPar);
			checkRequestParametersAllNotNull(clsQNameField);
			String labelQuery = setHttpPar(labelQueryPar);
			response = getSubClasses(clsQName, tree, instNum, labelQuery);
		} else if (request.equals(getClassesInfoAsRootsForTreeRequest)) {
			String clsesQNames = setHttpPar(clsesQNamesPar);
			String instNum = setHttpPar(instNumPar);
			boolean instNumBool = (instNum == null) ? false : (Boolean.parseBoolean(instNum));
			checkRequestParametersAllNotNull(clsesQNamesPar);
			response = getClassesInfoAsRootsForTree(clsesQNames, instNumBool);
		}

		// EDIT CLASS METHODS
		else if (request.equals(classDescriptionRequest)) {
			String classQNameEncoded = setHttpPar(clsQNameField);
			String method = setHttpPar("method");
			checkRequestParametersAllNotNull(clsQNameField, "method");
			response = getClassDescription(classQNameEncoded, method);
		} else if (request.equals(addOneOfRequest)) {
			String clsName = setHttpPar(clsQNameField);
			String individuals = setHttpPar(individualsPar);
			checkRequestParametersAllNotNull(clsQNameField, individualsPar);

			String[] individualsArray = individuals.split("\\|_\\|");

			response = addOneOf(clsName, individualsArray);
		} else if (request.equals(removeOneOfRequest)) {
			String clsName = setHttpPar(clsQNameField);
			String collectionNode = setHttpPar(collectionNodePar);
			checkRequestParametersAllNotNull(clsQNameField, collectionNodePar);

			response = removeOneOf(clsName, collectionNode);
		} else if (request.equals(createInstanceRequest)) {
			String clsQName = setHttpPar(clsQNameField);
			String instanceQName = setHttpPar(instanceNamePar);
			checkRequestParametersAllNotNull(instanceNamePar, clsQNameField);
			response = createInstanceOption(instanceQName, clsQName);
		} else if (request.equals(addTypeRequest)) {
			String clsQName = setHttpPar(clsQNamePar);
			String typeQName = setHttpPar(typeQNamePar);
			checkRequestParametersAllNotNull(clsQNamePar, typeQNamePar);
			response = individual.addType(clsQName, typeQName);
		} else if (request.equals(removeTypeRequest))
			response = individual.removeType(setHttpPar("clsqname"), _oReq.getParameter("typeqname"));
		else if (request.equals(addIntersectionOfRequest)) {
			String clsName = setHttpPar(clsQNameField);
			String clsDescriptions = setHttpPar(clsDescriptionsPar);
			checkRequestParametersAllNotNull(clsQNameField, clsDescriptionsPar);

			String[] clsDescriptionArray = clsDescriptions.split("\\|_\\|");

			response = addCollectionBasedClassAxiom(clsName, OWL.Res.INTERSECTIONOF, clsDescriptionArray);
		} else if (request.equals(removeIntersectionOfRequest)) {
			String clsName = setHttpPar(clsQNameField);
			String collectionNode = setHttpPar(collectionNodePar);

			checkRequestParametersAllNotNull(clsQNameField, collectionNodePar);

			response = removeCollectionBasedClassAxiom(clsName, OWL.Res.INTERSECTIONOF, collectionNode);

		} else if (request.equals(addUnionOfRequest)) {
			String clsName = setHttpPar(clsQNameField);
			String clsDescriptions = setHttpPar(clsDescriptionsPar);
			checkRequestParametersAllNotNull(clsQNameField, clsDescriptionsPar);

			String[] clsDescriptionArray = clsDescriptions.split("\\|_\\|");

			response = addCollectionBasedClassAxiom(clsName, OWL.Res.UNIONOF, clsDescriptionArray);
		} else if (request.equals(removeUnionOfRequest)) {
			String clsName = setHttpPar(clsQNameField);
			String collectionNode = setHttpPar(collectionNodePar);

			checkRequestParametersAllNotNull(clsQNameField, collectionNodePar);

			response = removeCollectionBasedClassAxiom(clsName, OWL.Res.UNIONOF, collectionNode);

		} else if (request.equals(addSuperClsRequest))
			response = addSuperClass(setHttpPar("clsqname"), setHttpPar("superclsqname"));
		else if (request.equals(removeSuperClsRequest))
			response = removeSuperClass(setHttpPar("clsqname"), _oReq.getParameter("superclsqname"));
		else if (request.equals(createClassRequest)) {
			String superClassName = setHttpPar(superClassNamePar);
			String newClassName = setHttpPar(newClassNamePar);
			checkRequestParametersAllNotNull(superClassNamePar, newClassNamePar);
			response = createClass(newClassName, superClassName);
		} else
			return ServletUtilities.getService().createNoSuchHandlerExceptionResponse(request);

		this.fireServletEvent();
		return response;
	}

	/**
	 * Enumerates (via <code>owl:oneOf</code>) all and only members of the class <code>clsName</code>, which
	 * are provided by the parameter <code>individuals</code>
	 * 
	 * @param clsName
	 * @param individuals
	 * @return
	 */
	public Response addOneOf(String clsName, String[] individuals) {
		try {
			OWLModel owlModel = getOWLModel();
			ARTResource cls = retrieveExistingResource(owlModel, clsName, userGraphs);

			Collection<ARTURIResource> items = new ArrayList<ARTURIResource>(individuals.length);
			for (String anIndividual : individuals) {
				items.add(retrieveExistingURIResource(owlModel, anIndividual, userGraphs));
			}

			owlModel.instantiatePropertyWithCollecton(cls, OWL.Res.ONEOF, items, getWorkingGraph());

			return createReplyResponse(RepliesStatus.ok);
		} catch (NonExistingRDFResourceException | ModelAccessException | ModelUpdateException e) {
			return logAndSendException(e);
		}
	}

	/**
	 * Removes the enumeration <code>collectionNode</code> from the description of the class
	 * <code>clsName</code>
	 * 
	 * @param clsName
	 * @param collectionNode
	 * @return
	 */
	public Response removeOneOf(String clsName, String collectionNode) {
		try {
			OWLModel owlModel = getOWLModel();
			ARTResource cls = retrieveExistingURIResource(owlModel, clsName, getUserNamedGraphs());
			ARTResource collection = retrieveExistingResource(owlModel, collectionNode, getUserNamedGraphs());

			if (!owlModel.hasTriple(cls, OWL.Res.ONEOF, collection, false, getUserNamedGraphs())) {
				return logAndSendException("Class " + RDFNodeSerializer.toNT(cls)
						+ " has not been defined as having the following extension: "
						+ owlModel.getItems(collection, false, getUserNamedGraphs()));
			}

			// This operation also removes the triple relating the class to the collection, as well as any
			// other triple the object of which is the collection
			owlModel.deleteCollection(collection, null, getWorkingGraph());

			return createReplyResponse(RepliesStatus.ok);
		} catch (ModelAccessException | NonExistingRDFResourceException | ModelUpdateException e) {
			return logAndSendException(e);
		}
	}

	/**
	 * Adds an axiom identified by the property <code>axiomType</code> to the description of the class
	 * identified by <code>clsName</code> using the supplied array of Manchester expressions to generate the
	 * property values.
	 * 
	 * @param clsName
	 * @param axiomType
	 * @param clsDescriptionArray
	 * @return
	 */
	public Response addCollectionBasedClassAxiom(String clsName, ARTURIResource axiomType,
			String[] clsDescriptionArray) {
		try {
			OWLModel owlModel = getOWLModel();

			ARTResource cls = retrieveExistingResource(owlModel, clsName, userGraphs);

			List<ARTResource> intesectedItems = new ArrayList<ARTResource>();

			List<ARTStatement> stmts = new ArrayList<ARTStatement>();

			for (String aClsDescr : clsDescriptionArray) {
				ManchesterClassInterface expObj = ManchesterParser.parse(aClsDescr, owlModel);

				ARTNode descriptionRootNode = owlModel.parseManchesterExpr(expObj, stmts);

				if (descriptionRootNode == null) {
					return createReplyFAIL("Cannot parse class description: " + aClsDescr);
				}
				intesectedItems.add(descriptionRootNode.asResource());
			}

			// if everything has gone well, then add the actual triples

			for (ARTStatement aStat : stmts) {
				owlModel.addStatement(aStat, getWorkingGraph());
			}
			owlModel.instantiatePropertyWithCollecton(cls, axiomType, intesectedItems, getWorkingGraph());

			return createReplyResponse(RepliesStatus.ok);
		} catch (Exception e) {
			return logAndSendException(e);
		}
	}

	/**
	 * Removes an axiom identified by the property <code>axiomType</code> based on the collection identified
	 * by <code>collectionNode</code> from the description of the class identified by <code>clsName</code>.
	 * 
	 * @param clsName
	 * @param axiomType
	 * @param collectionNode
	 * @return
	 */
	public Response removeCollectionBasedClassAxiom(String clsName, ARTURIResource axiomType,
			String collectionNode) {
		try {
			OWLModel owlModel = getOWLModel();

			ARTResource cls = retrieveExistingResource(owlModel, clsName, userGraphs);
			ARTResource collection = retrieveExistingResource(owlModel, collectionNode, userGraphs);

			Collection<ARTNode> collectionItems = owlModel.getItems(collection, true, userGraphs);

			List<ARTStatement> stmts = new ArrayList<ARTStatement>();

			for (ARTNode anItem : collectionItems) {
				if (!anItem.isResource()) {
					return createReplyFAIL("Not an RDF resource: " + RDFNodeSerializer.toNT(anItem));
				}

				if (anItem.isURIResource())
					continue;

				ARTBNode aBnode = anItem.asBNode();
				ManchesterClassInterface mci = owlModel.getManchClassFromBNode(aBnode, owlModel,
						getWorkingGraph(), stmts);

				if (mci == null) {
					return createReplyFAIL("Not the root of an anonymous class: "
							+ RDFNodeSerializer.toNT(aBnode));
				}
			}

			if (!owlModel.hasTriple(cls, axiomType, collection, false, getUserNamedGraphs())) {
				return logAndSendException("Class " + RDFNodeSerializer.toNT(cls)
						+ " has not been defined as the " + owlModel.getQName(axiomType.getURI())
						+ " of the provided collection of classes: " + collectionItems);
			}

			// This operation also removes the triple relating the class to the collection, as well as any
			// other triple the object of which is the collection
			owlModel.deleteCollection(collection, null, getWorkingGraph());

			for (ARTStatement aStmt : stmts) {
				owlModel.deleteStatement(aStmt, aStmt.getNamedGraph());
			}

			return createReplyResponse(RepliesStatus.ok);
		} catch (ModelAccessException | NonExistingRDFResourceException | ModelUpdateException e) {
			return logAndSendException(e);
		}
	}

	/**
	 * retrieves subclasses of class identified by qname clsQname. The response provides additional info
	 * depending on other arguments' values
	 * 
	 * @param clsQName
	 * @param forTree
	 *            <code>true</code> if this request has been performed to fill an ontology class tree. In this
	 *            case, an attribute called "more" is being provided for each subclass. If its value is 1 then
	 *            the subclass has subclasses itself. Also, in case of a tree request, only URI classes are
	 *            being retrieved
	 * @param instNum
	 *            <code>true</code> if the user is interested in knowing the number of instances per concept
	 * @param labelQuery
	 *            if != <code>null</code>, then this String is used to drive custom label providers to return
	 *            appropriate labels to be shown in place of the class qname. Built-in label provider allows
	 *            for retrieving values of a property attached to the class, by using this syntax:<br/>
	 * <br/>
	 *            <code>prop:&lt;propertyqname&gt;[#&lt;iso code for language&gt;]</code><br/>
	 * <br/>
	 *            example: "prop:rdfs:label###en" will show the value on property <em>rdfs:label</em> for the
	 *            english language, instead of the class qname<br/>
	 *            the reponse to this method may be a WARNING reply in case the labelQuery does not identify a
	 *            valid property to be used for retrieving labels
	 * @return
	 */
	public Response getSubClasses(String clsQName, boolean forTree, boolean instNum, String labelQuery) {
		RDFSModel ontModel = (RDFSModel) getOntModel();

		try {
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);

			ARTResource[] graphs = getUserNamedGraphs();
			ARTResource wgraph = getWorkingGraph();
			ARTURIResource cls = retrieveExistingURIResource(ontModel, clsQName, graphs);

			// THIS IS DONE HERE AS THIS DATA IS CALCULATED ONCE, while each single rendering is calculated
			// per class
			// TODO I would replace this with a general check attached to the idea of creating representation
			// of resources. I would create a class for describing "representation makers" with an interface
			// method for getting the representation. I would also create a factory which returns the
			// appropriate "repmaker" depending on the passed labelQuery
			ARTURIResource labelProp = null;
			String requestedISOLang = null;
			LabelProcessor lblProc = elaborateLabelQuery(ontModel, labelQuery);
			if (lblProc != null) {
				labelProp = lblProc.getLabelProp();
				requestedISOLang = lblProc.getRequestedISOLang();
			}

			RDFIterator<? extends ARTResource> subClassesIterator;

			STOntologyManager<?> ontManager = getProject().getOntologyManager();

			// creating subclasses iterator
			// URI filter and other complex operations for tree show
			if (forTree){
				subClassesIterator = new SubClassesForTreeIterator(ontManager, cls, graphs);
			} else {
				// simple listDirectSubClasses for standard method
				//subClassesIterator = ((DirectReasoning) ontModel).listDirectSubClasses(cls, graphs);
				subClassesIterator = ontModel.listSubClasses(cls, false, graphs);
			}

			Collection<STRDFResource> classes = STRDFNodeFactory.createEmptyResourceCollection();
			while (subClassesIterator.streamOpen()) {
				ARTResource subClass = subClassesIterator.getNext();
				STRDFResource stClass = STRDFNodeFactory.createSTRDFResource(ontModel, subClass,
						ModelUtilities.getResourceRole(subClass, ontModel),
						servletUtilities.checkWritable(ontModel, subClass, wgraph), false);
				if (forTree)
					ClsOld.decorateForTreeView(ontManager, stClass, getUserNamedGraphs());
				if (instNum) {
					ClsOld.decorateWithNumberOfIstances(ontModel, stClass, graphs);
				}
				setRendering(ontModel, stClass, labelProp, requestedISOLang, graphs);

				classes.add(stClass);
			}
			subClassesIterator.close();

			RDFXMLHelp.addRDFNodes(response, classes);

			return response;

		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}

	}

	private LabelProcessor elaborateLabelQuery(RDFModel ontModel, String labelQuery)
			throws ModelAccessException {
		if (labelQuery != null) {
			if (labelQuery.startsWith("prop:")) {
				ARTURIResource labelProp = null;
				String requestedISOLang = null;

				String queryString = labelQuery.substring(5); // remove the "prop:"
				String[] propStringElements = queryString.split("###");
				String propertyQName = propStringElements[0];
				if (propStringElements.length > 1)
					requestedISOLang = propStringElements[1];

				String propURI = ontModel.expandQName(propertyQName);
				labelProp = ontModel.retrieveURIResource(propURI);

				return new LabelProcessor(labelProp, requestedISOLang);
			}
		}
		return null;
	}

	public class LabelProcessor {
		private ARTURIResource labelProp;
		private String requestedISOLang;

		public LabelProcessor(ARTURIResource labelProp, String requestedISOLang) {
			this.labelProp = labelProp;
			this.requestedISOLang = requestedISOLang;
		}

		public ARTURIResource getLabelProp() {
			return labelProp;
		}

		public String getRequestedISOLang() {
			return requestedISOLang;
		}

	}

	// TODO I would replace this with a general check attached to the idea of creating representation
	// of resources. I would create a class for describing "representation makers" with an interface
	// method for getting the representation. I would also create a factory which returns the
	// appropriate "repmaker" depending on the passed labelQuery
	public static void setRendering(RDFSModel model, STRDFResource subClass, ARTURIResource labelProp,
			String requestedISOLang, ARTResource[] graphs) throws ModelAccessException {

		String label = null;
		if (labelProp != null) {
			ARTNodeIterator it = model.listValuesOfSubjPredPair((ARTResource) subClass.getARTNode(),
					labelProp, false, graphs);

			if (it.streamOpen()) {
				ARTNode nodeLabel = it.getNext();
				logger.debug("nodeLabel: " + nodeLabel);
				// if node is not a Literal, then get its String representation as the label for the
				// class, else if there is no specified iso-language code, get the label from the
				// literal else get the first value whose language matches the specified iso-language
				if (nodeLabel.isLiteral()) {
					if (requestedISOLang == null) {
						label = nodeLabel.asLiteral().getLabel();
						logger.debug("no preferred ISO language, getting first available label: " + label);
					} else {
						// the rationale behind this odd nested loop is to avoid all other checks
						// (such as the availability of the requestedISOLang or checking the nature of
						// the nodeLabel) to be repeated for each value of the label property
						while (label == null) {
							ARTLiteral literalLabel = nodeLabel.asLiteral();
							String gotISOLang = literalLabel.getLanguage();
							logger.debug("iso lang for " + nodeLabel + ": " + gotISOLang);
							if (gotISOLang.equals(requestedISOLang)) {
								label = literalLabel.getLabel();
							}
							if (it.streamOpen())
								nodeLabel = it.getNext();
							else
								break;
						}
					}
				} else
					label = nodeLabel.toString();
			}
		}
		String rendering = null;
		if (label != null)
			rendering = label;
		else {
			// isn't that some facility for getting this automatically?
			if (subClass.isBlank())
				rendering = subClass.toNT();
			else
				rendering = model.getQName(subClass.getARTNode().asURIResource().getURI());
		}

		subClass.setRendering(rendering);
	}

	public static void decorateForTreeView(STOntologyManager<?> ontManager, STRDFResource subClass,
			ARTResource[] graphs) throws ModelAccessException, NonExistingRDFResourceException {

		RDFIterator<ARTURIResource> subSubClassesIterator = new SubClassesForTreeIterator(ontManager,
				subClass.getARTNode().asURIResource(), graphs);
		if (subSubClassesIterator.hasNext())
			subClass.setInfo("more", "1"); // the subclass has further subclasses
		else
			subClass.setInfo("more", "0"); // the subclass has no subclasses itself
		subSubClassesIterator.close();
	}

	public static void decorateWithNumberOfIstances(RDFSModel model, STRDFResource subClass,
			ARTResource[] graphs) throws ModelAccessException, NonExistingRDFResourceException {
		int numInst = ModelUtilities.getNumberOfClassInstances((DirectReasoning) model,
				(ARTResource) subClass.getARTNode(), true, graphs);
		subClass.setInfo("numInst", Integer.toString(numInst));
	}

	/**
	 * 
	 * @author Armando Stellato, Andrea Turbati
	 * 
	 * @param clsQName
	 *            the qname of the class whose instances are being retrieved
	 * @return an xml Response listing the instances of clsQName
	 */
	public Response getClassAndInstancesInfo(String clsQName, String listMod, boolean hasSubClassesRequest) {
		boolean direct = true;
		if (listMod != null && listMod.equals(allInstances))
			direct = false;
		logger.debug("replying to \"getInstancesListXML(" + clsQName + ")\"");
		OWLModel ontModel = getOWLModel();

		try {
			ARTResource[] graphs = getUserNamedGraphs();
			ARTURIResource cls = retrieveExistingURIResource(ontModel, clsQName, graphs);
			ARTResource wgraph = getWorkingGraph();

			XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
			Element dataElement = response.getDataElement();

			Element classElement = XMLHelp.newElement(dataElement, "Class");
			STRDFResource stClass = STRDFNodeFactory.createSTRDFResource(ontModel, cls,
					ModelUtilities.getResourceRole(cls, ontModel),
					servletUtilities.checkWritable(ontModel, cls, wgraph), false);
			setRendering(ontModel, stClass, null, null, graphs);
			ClsOld.decorateWithNumberOfIstances(ontModel, stClass, graphs);

			if (hasSubClassesRequest) {
				STOntologyManager<?> ontManager = getProject().getOntologyManager();
				ClsOld.decorateForTreeView(ontManager, stClass, getUserNamedGraphs());
			}
			RDFXMLHelp.addRDFNode(classElement, stClass);

			createInstancesXMLList(ontModel, cls, direct, dataElement, graphs);
			return response;
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
	}

	private void createInstancesXMLList(RDFSModel ontModel, ARTResource cls, boolean direct,
			Element dataElement, ARTResource... graphs) throws ModelAccessException,
			NonExistingRDFResourceException {
		Element instancesElement = XMLHelp.newElement(dataElement, "Instances");
		ARTResource wgraph = getWorkingGraph();

		// TODO filter on admin also here
		ARTResourceIterator instancesIterator;
		if (direct){
			//instancesIterator = ((DirectReasoning) ontModel).listDirectInstances(cls, graphs);
			instancesIterator = ontModel.listInstances(cls, false, graphs);
		} else {
			instancesIterator = ontModel.listInstances(cls, true, graphs);
		}
		
		Collection<STRDFResource> instances = STRDFNodeFactory.createEmptyResourceCollection();
		while (instancesIterator.streamOpen()) {
			ARTResource instance = instancesIterator.getNext();
			STRDFResource stIndividual = STRDFNodeFactory.createSTRDFResource(ontModel, instance,
					ModelUtilities.getResourceRole(instance, ontModel),
					servletUtilities.checkWritable(ontModel, instance, wgraph), false);
			setRendering(ontModel, stIndividual, null, null, graphs);
			instances.add(stIndividual);
		}
		RDFXMLHelp.addRDFNodes(instancesElement, instances);
		instancesIterator.close();

	}

	/**
	 * creates an instance of the class identified by <code>clsQName</code>
	 * 
	 * @param instanceQName
	 * @param clsQName
	 * @return
	 */
	public Response createInstanceOption(String instanceQName, String clsQName) {
		RDFSModel ontModel = (RDFSModel) getOntModel();
		ARTURIResource instanceRes;
		try {
			instanceRes = ontModel.createURIResource(ontModel.expandQName(instanceQName));
			if (ontModel.existsResource(instanceRes)) {
				return logAndSendException("there is another resource with the same name!");
			}
			ARTURIResource clsRes = retrieveExistingURIResource(ontModel, clsQName, getUserNamedGraphs());
			ontModel.addInstance(ontModel.expandQName(instanceQName), clsRes);
			return updateClassOnTree(clsQName, instanceQName);
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
	}

	public Response updateClassOnTree(String clsQName, String instanceName)
			throws NonExistingRDFResourceException {
		RDFSModel ontModel = (RDFSModel) getOntModel();
		try {
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
			Element dataElement = response.getDataElement();

			Element clsElement = XMLHelp.newElement(dataElement, "Class");
			String clsURI = ontModel.expandQName(clsQName);
			ARTResource cls = ontModel.createURIResource(clsURI);
			ARTResource wgraph = getWorkingGraph();
			ARTResource[] graphs = getUserNamedGraphs();
			STRDFResource stClass = STRDFNodeFactory.createSTRDFResource(ontModel, cls,
					ModelUtilities.getResourceRole(cls, ontModel),
					servletUtilities.checkWritable(ontModel, cls, wgraph), false);
			ClsOld.decorateWithNumberOfIstances(ontModel, stClass, graphs);
			setRendering(ontModel, stClass, null, null, graphs);
			RDFXMLHelp.addRDFNode(clsElement, stClass);

			Element instanceElement = XMLHelp.newElement(dataElement, "Instance");
			ARTURIResource instanceRes = ontModel.createURIResource(ontModel.expandQName(instanceName));
			STRDFResource stInstance = STRDFNodeFactory.createSTRDFResource(ontModel, instanceRes,
					ModelUtilities.getResourceRole(instanceRes, ontModel),
					servletUtilities.checkWritable(ontModel, instanceRes, wgraph), false);
			setRendering(ontModel, stInstance, null, null, graphs);
			RDFXMLHelp.addRDFNode(instanceElement, stInstance);
			return response;
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		}

	}

	/**
	 * retrieves the class tree rooted on the class identified by <code>clsQName</code> Not used at the
	 * moment, so it is commented
	 * 
	 * @param clsQName
	 *            the qname of the class which is root for the tree
	 * @return
	 */
	/*
	 * public Response getClassSubTreeXML(String clsQName) { RDFSModel ontModel = (RDFSModel) getOntModel();
	 * ARTURIResource cls; XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok); Element
	 * dataElement = response.getDataElement(); try { cls =
	 * ontModel.createURIResource(ontModel.expandQName(clsQName));
	 * this.recursiveCreateClassesXMLTree(ontModel, cls, dataElement, getUserNamedGraphs()); } catch
	 * (DOMException e) { return logAndSendException("exception raised in building the classes tree: " +
	 * e.getMessage()); } catch (ModelAccessException e) { return logAndSendException(e); } catch
	 * (NonExistingRDFResourceException e) { return logAndSendException(e); } return response; }
	 */

	// TODO, se possibile, togliamo anche quell'odioso: subclasses. Non serve a niente e complica la vita a
	// tutti!
	/**
	 * Crea l'elemento tree nel file xml che contiene l'elenco delle classi e sottoclassi <tt>
	 * <data type="AllClassesTree">
	 * 	<Class deleteForbidden="true" name="filas:Person" numInst="0">
	 * 		<SubClasses>
	 * 			<Class deleteForbidden="true" name="Researcher" numInst="1"/>
	 * 		<SubClasses/>
	 * 	</Class>
	 * 	<Class deleteForbidden="true" name="filas:Organization" numInst="1">
	 * 		<SubClasses/>
	 * 	</Class>
	 * </data>
	 * </tt>
	 * 
	 * @return Response tree
	 */
	public Response createClassXMLTree() {

		RDFSModel ontModel = (RDFSModel) getOntModel();

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();

		Predicate<ARTResource> exclusionPredicate;
		if (Config.isAdminStatus())
			exclusionPredicate = NoLanguageResourcePredicate.nlrPredicate;
		else
			exclusionPredicate = DomainResourcePredicate.getPredicate(getProject());

		// TODO I should have "graphs" in the filter constructor too
		Predicate<ARTResource> rootUserClsPred = Predicates.and(new RootClassesResourcePredicate(ontModel),
				exclusionPredicate);
		try {
			ARTResource[] graphs = getUserNamedGraphs();
			ARTURIResourceIterator namedClassesIt = ontModel.listNamedClasses(true, graphs);
			Iterator<ARTURIResource> filtIt;
			filtIt = Iterators.filter(namedClassesIt, rootUserClsPred);
			while (filtIt.hasNext()) {
				ARTURIResource cls = filtIt.next().asURIResource();
				recursiveCreateClassesXMLTree(getProject(), cls, dataElement, graphs);
			}
			namedClassesIt.close();
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}

		return response;
	}

	/**
	 * recursively creates an xml representation of a class tree starting from a given <code>cls</code> and
	 * from the element <code>element</code> to which the new information need to be attached
	 * <p>
	 * OWL distinguishes six types of class descriptions:
	 * <ul>
	 * <li>a class identifier (a URI reference)</li>
	 * <li>an exhaustive enumeration of individuals that together form the instances of a class</li>
	 * <li>a property restriction</li>
	 * <li>the intersection of two or more class descriptions</li>
	 * <li>the union of two or more class descriptions</li>
	 * <li>the complement of a class description</li>
	 * </ul>
	 * for each of these, a dedicated
	 * </p>
	 * 
	 * @param project
	 * @param cls
	 * @param element
	 * @throws DOMException
	 * @throws ModelAccessException
	 */
	public static void recursiveCreateClassesXMLTree(Project<?> project, ARTURIResource cls, Element element,
			ARTResource... graphs) throws DOMException, ModelAccessException {
		RDFModel ontModel = project.getOWLModel();
		Element classElement = XMLHelp.newElement(element, "Class");
		boolean deleteForbidden = ServletUtilities.getService().checkReadOnly(cls, project);
		classElement.setAttribute("name", ontModel.getQName(cls.getURI()));

		int numInst = ModelUtilities.getNumberOfClassInstances((DirectReasoning) ontModel, cls, true, graphs);
		if (numInst > 0)
			classElement.setAttribute("numInst", "(" + numInst + ")");
		else
			classElement.setAttribute("numInst", "0");

		classElement.setAttribute("deleteForbidden", Boolean.toString(deleteForbidden));

		ARTResourceIterator subClassesIterator = ((DirectReasoning) ontModel).listDirectSubClasses(cls,
				graphs);
		Iterator<ARTResource> namedSubClassesIterator = Iterators.filter(subClassesIterator,
				URIResourcePredicate.uriFilter);
		Element subClassesElem = XMLHelp.newElement(classElement, "SubClasses");
		while (namedSubClassesIterator.hasNext()) {
			ARTURIResource subClass = namedSubClassesIterator.next().asURIResource();
			recursiveCreateClassesXMLTree(project, subClass, subClassesElem, graphs);
		}
		subClassesIterator.close();
	}

	/**
	 * An iterator which retrieves subclasses of a class for the specific case of populating a class tree.<br/>
	 * This means the subclasses are limited to URI resources (e.g. not class descriptions coded as bnodes)
	 * 
	 * @author Armando Stellato &lt;stellato@info.uniroma2.it&gt; -Xmx400m
	 * 
	 */
	private static class SubClassesForTreeIterator implements RDFIterator<ARTURIResource> {

		RDFIterator<? extends ARTResource> subClassesIterator;
		Iterator<? extends ARTResource> finalIterator;

		SubClassesForTreeIterator(STOntologyManager<?> ontManager, ARTResource superCls,
				ARTResource... graphs) throws ModelAccessException {
			RDFSModel ontModel = ontManager.getOWLModel();
			// to check that even with a non-owl reasoning triple-store, root classes are computed as children
			// of owl:Thing
			if (superCls.equals(OWL.Res.THING)) {
				logger.debug("looking for subclasses of owl:Thing, using method for retrieving root classes");
				Predicate<ARTResource> exclusionPredicate;
				if (Config.isAdminStatus())
					exclusionPredicate = NoLanguageResourcePredicate.nlrPredicate;
				else
					exclusionPredicate = DomainResourcePredicate.getPredicate(ontManager);

				Predicate<ARTResource> rootUserClsPred = Predicates.and(new STRootClassesResourcePredicate(
						ontModel), exclusionPredicate);

				subClassesIterator = ontModel.listNamedClasses(true, graphs);
				finalIterator = Iterators.filter(subClassesIterator, rootUserClsPred);

			} else {
				//subClassesIterator = ((DirectReasoning) ontModel).listDirectSubClasses(superCls, graphs);
				subClassesIterator = ontModel.listSubClasses(superCls, false, graphs);
				
				Predicate<ARTResource> namedClasseAndNotItself = Predicates.and(new URIResourcePredicate(), 
						new NoSpecificResourcePredicate(superCls));
				finalIterator = Iterators.filter(subClassesIterator, namedClasseAndNotItself);
			}
		}

		public void close() throws ModelAccessException {
			subClassesIterator.close();
		}

		public ARTURIResource getNext() throws ModelAccessException {
			return finalIterator.next().asURIResource();
		}

		public boolean streamOpen() throws ModelAccessException {
			return finalIterator.hasNext();
		}

		public boolean hasNext() {
			return finalIterator.hasNext();
		}

		public ARTURIResource next() {
			return finalIterator.next().asURIResource();
		}

		public void remove() {
			subClassesIterator.remove();
		}
	}

	/**
	 * This call has to be performed when a given list of classes has to be shown as roots for a given class
	 * tree<br/>
	 * The method retrieves info about classes identified by qnames concatenated through symbol |_| into
	 * <code>clsesQNamesString</code> (classes shown in a classtree are always URI, as bnodes are only shown
	 * as superclasses in the descriptions of other classes ) <br/>
	 * The response provides additional info depending on other arguments' values.<br/>
	 * 
	 * 
	 * @param clsesQNamesString
	 * @param instNumBool
	 *            <code>true</code> if the requester is interested also in the number of instances of these
	 *            classes
	 * @return
	 */
	public Response getClassesInfoAsRootsForTree(String clsesQNamesString, boolean instNumBool) {
		RDFSModel ontModel = (RDFSModel) getOntModel();

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		// Element dataElement = response.getDataElement();

		String[] clsesQNames = clsesQNamesString.split("\\|_\\|");
		try {
			ARTResource[] graphs = getUserNamedGraphs();
			Collection<STRDFResource> classes = STRDFNodeFactory.createEmptyResourceCollection();
			ARTResource wgraph = getWorkingGraph();
			for (String clsQName : clsesQNames) {

				ARTURIResource cls = retrieveExistingURIResource(ontModel, clsQName, graphs);

				STRDFResource stClass = STRDFNodeFactory.createSTRDFResource(ontModel, cls,
						ModelUtilities.getResourceRole(cls, ontModel),
						servletUtilities.checkWritable(ontModel, cls, wgraph), false);
				setRendering(ontModel, stClass, null, null, graphs);

				STOntologyManager<?> ontManager = getProject().getOntologyManager();

				ClsOld.decorateForTreeView(ontManager, stClass, getUserNamedGraphs());
				if (instNumBool)
					ClsOld.decorateWithNumberOfIstances(ontModel, stClass, graphs);

				classes.add(stClass);
			}
			RDFXMLHelp.addRDFNodes(response, classes);
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}
		return response;

	}

	/*
	 * public representRestriction(Element el, OWLModel ontModel) { OWL i; ontModel.listStatements(subj,
	 * OWL.Res., obj, inferred, graphs); }
	 */

	/**
	 * adds an rdfs:superClassOf relationship between two resources (already defined in the ontology)
	 * 
	 * <Tree type="add_superclass"> <Type qname="rtv:Person"/> </Tree>
	 * 
	 */
	public Response addSuperClass(String clsQName, String superclsQName) {
		logger.debug("replying to \"addSuperClass(" + clsQName + "," + superclsQName + ")\".");
		RDFSModel ontModel = (RDFSModel) getOntModel();

		try {
			ARTResource[] graphs = getUserNamedGraphs();
			ARTURIResource cls = retrieveExistingURIResource(ontModel, clsQName, graphs);
			ARTURIResource superCls = retrieveExistingURIResource(ontModel, superclsQName, graphs);

			Collection<ARTResource> superClasses = RDFIterators
					.getCollectionFromIterator(((DirectReasoning) ontModel).listDirectSuperClasses(cls,
							graphs));

			if (superClasses.contains(superCls))
				return logAndSendException(superclsQName + " is already a superclass of: " + clsQName);

			ontModel.addSuperClass(cls, superCls);

			XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
			Element dataElement = response.getDataElement();

			Element clsElement = XMLHelp.newElement(dataElement, "Class");
			ARTResource wgraph = getWorkingGraph();
			STRDFResource stClass = STRDFNodeFactory.createSTRDFResource(ontModel, cls,
					ModelUtilities.getResourceRole(cls, ontModel),
					servletUtilities.checkWritable(ontModel, cls, wgraph), false);
			ClsOld.decorateWithNumberOfIstances(ontModel, stClass, graphs);
			setRendering(ontModel, stClass, null, null, graphs);
			RDFXMLHelp.addRDFNode(clsElement, stClass);

			Element superClsElement = XMLHelp.newElement(dataElement, "SuperClass");
			STRDFResource stSuperClass = STRDFNodeFactory.createSTRDFResource(ontModel, superCls,
					ModelUtilities.getResourceRole(superCls, ontModel),
					servletUtilities.checkWritable(ontModel, superCls, wgraph), false);
			ClsOld.decorateWithNumberOfIstances(ontModel, stSuperClass, graphs);
			setRendering(ontModel, stSuperClass, null, null, graphs);
			RDFXMLHelp.addRDFNode(superClsElement, stSuperClass);

			return response;

		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}

	}

	// STARRED ti serve pure il nome della istanza?
	/**
	 * gets the namespace mapping for the loaded ontology
	 * 
	 * <Tree type="remove_superclass"> <Type qname="rtv:Person"/> </Tree>
	 * 
	 */
	public Response removeSuperClass(String clsQName, String superClassQName) {
		logger.debug("replying to \"removeType(" + clsQName + "," + superClassQName + ")\".");
		RDFSModel ontModel = (RDFSModel) getOntModel();

		try {
			ARTResource[] graphs = getUserNamedGraphs();
			ARTResource cls = retrieveExistingURIResource(ontModel, clsQName, graphs);
			ARTResource superCls = retrieveExistingURIResource(ontModel, superClassQName, graphs);

			Collection<ARTResource> superClasses = RDFIterators
					.getCollectionFromIterator(((DirectReasoning) ontModel).listDirectSuperClasses(cls,
							graphs));

			if (!superClasses.contains(superCls))
				return logAndSendException(superClassQName + " is not a superclass for: " + clsQName);

			if (!ontModel.hasTriple(cls, RDFS.Res.SUBCLASSOF, superCls, false, getWorkingGraph()))
				return logAndSendException("this sublcass relationship comes from an imported ontology or has been inferred, so it cannot be deleted explicitly");

			ontModel.removeSuperClass(cls, superCls);

			XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
			Element dataElement = response.getDataElement();

			Element clsElement = XMLHelp.newElement(dataElement, "Class");
			ARTResource wgraph = getWorkingGraph();
			STRDFResource stClass = STRDFNodeFactory.createSTRDFResource(ontModel, cls,
					ModelUtilities.getResourceRole(cls, ontModel),
					servletUtilities.checkWritable(ontModel, cls, wgraph), false);
			ClsOld.decorateWithNumberOfIstances(ontModel, stClass, graphs);
			setRendering(ontModel, stClass, null, null, graphs);
			RDFXMLHelp.addRDFNode(clsElement, stClass);

			Element superClsElement = XMLHelp.newElement(dataElement, "SuperClass");
			STRDFResource stSuperClass = STRDFNodeFactory.createSTRDFResource(ontModel, superCls,
					ModelUtilities.getResourceRole(superCls, ontModel),
					servletUtilities.checkWritable(ontModel, superCls, wgraph), false);
			ClsOld.decorateWithNumberOfIstances(ontModel, stSuperClass, graphs);
			setRendering(ontModel, stSuperClass, null, null, graphs);
			RDFXMLHelp.addRDFNode(superClsElement, stSuperClass);

			return response;

		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}

	}

	/**
	 * very similar to {@link INSIndividual#getIndividualDescription(String, String)} , it contains additional
	 * features for classes (rdfs:subClassOf property is reported apart).
	 * 
	 * @param subjectClassQName
	 *            the instance to which the new instance is related to
	 * @return
	 * 
	 *         This is a sample of the
	 * 
	 *         <pre>
	 * prova
	 * <Tree request="getClsDescription" type="templateandvalued">
	 * 	<Types>
	 * 		<Type class="owl:Class" explicit="true"/>
	 * </Types>
	 * <SuperTypes>
	 * 	<SuperType explicit="true" resource="rtv:Person"/>
	 * </SuperTypes>
	 * <Properties/>
	 * </Tree>
	 * </pre>
	 * 
	 */
	public Response getClassDescription(String subjectClassQName, String method) {
		logger.debug("getClassDescription; qname: " + subjectClassQName);
		return getResourceDescription(subjectClassQName, RDFResourceRolesEnum.cls, method);
	}

	public Response createClass(String newClassQName, String superClassQName) {
		logger.debug("creating class: " + newClassQName + " as subClassOf: " + superClassQName);
		RDFSModel ontModel = (RDFSModel) getOntModel();
		try {
			ARTResource wgraph = getWorkingGraph();
			ARTResource[] graphs = getUserNamedGraphs();
			String newClassURI = ontModel.expandQName(newClassQName);
			ARTURIResource classRes = ontModel.createURIResource(newClassURI);
			boolean exists = ontModel.existsResource(classRes);
			if (exists) {
				return logAndSendException("there is a resource with the same name!");
			}
			ARTResource superClassResource = retrieveExistingURIResource(ontModel, superClassQName,
					getUserNamedGraphs());

			ontModel.addClass(newClassURI);
			ontModel.addSuperClass(classRes, superClassResource);

			XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
			Element dataElement = response.getDataElement();

			Element clsElement = XMLHelp.newElement(dataElement, "Class");
			STRDFResource stClass = STRDFNodeFactory.createSTRDFResource(ontModel, classRes,
					ModelUtilities.getResourceRole(classRes, ontModel),
					servletUtilities.checkWritable(ontModel, classRes, wgraph), false);
			ClsOld.decorateWithNumberOfIstances(ontModel, stClass, graphs);

			STOntologyManager<?> ontManager = getProject().getOntologyManager();

			ClsOld.decorateForTreeView(ontManager, stClass, getUserNamedGraphs());
			setRendering(ontModel, stClass, null, null, graphs);
			RDFXMLHelp.addRDFNode(clsElement, stClass);

			Element superClsElement = XMLHelp.newElement(dataElement, "SuperClass");
			STRDFResource stSuperClass = STRDFNodeFactory.createSTRDFResource(ontModel, superClassResource,
					ModelUtilities.getResourceRole(superClassResource, ontModel),
					servletUtilities.checkWritable(ontModel, superClassResource, wgraph), false);
			ClsOld.decorateWithNumberOfIstances(ontModel, stSuperClass, graphs);
			setRendering(ontModel, stSuperClass, null, null, graphs);
			ClsOld.decorateForTreeView(ontManager, stSuperClass, getUserNamedGraphs());
			RDFXMLHelp.addRDFNode(superClsElement, stSuperClass);

			return response;

		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (ModelUpdateException e) {
			return logAndSendException(e);
		} catch (NonExistingRDFResourceException e) {
			return logAndSendException(e);
		}

	}

	public Response getSuperClasses(String clsQName) {
		return getSuperTypes(clsQName, RDFResourceRolesEnum.cls);
	}

}
