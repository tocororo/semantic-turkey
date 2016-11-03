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
 * The Original Code is SemanticTurkeySE.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2009.
 * All Rights Reserved.
 *
 * SemanticTurkeySE was developed by the Artificial Intelligence Research Group
 * (ai-nlp.info.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about SemanticTurkeySE can be obtained at 
 * http//ai-nlp.info.uniroma2.it/software/...
 *
 */

/*
 * Contributor(s): Armando Stellato stellato@info.uniroma2.it
 */
package it.uniroma2.art.semanticturkey.servlet.main;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.navigation.ARTStatementIterator;
import it.uniroma2.art.semanticturkey.exceptions.HTTPParameterUnspecifiedException;
import it.uniroma2.art.semanticturkey.plugin.extpts.ServiceAdapter;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.JSONHelp;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

@Component
public class Statement extends ServiceAdapter {

	protected static Logger logger = LoggerFactory.getLogger(Statement.class);

	// requests
	public static class Req {
		public final static String getStatementsRequest = "getStatements";
		public final static String hasStatementRequest = "hasStatement";
	}

	// parameters
	public static class Par {
		public final static String subjectPar = "subj";
		public final static String predicatePar = "pred";
		public final static String objectPar = "obj";
		public final static String graphsPar = "ngs";
		public final static String inferencePar = "inference";
	}

	public final static String resultTag = "result";

	public final static String statementTag = "stm";
	public final static String subjTag = "subj";
	public final static String predTag = "pred";
	public final static String objTag = "obj";

	public final static String typeAttr = "type";
	public final static String mainGraphValue = "main";
	public final static String anyNodeValue = "any";
	
	public final static String sparqlJSONAttr = "sparql";
	public final static String headJSONAttr = "head";
	public final static String varsJSONAttr = "vars";
	public final static String resultsJSONAttr = "results";
	public final static String bindingsJSONAttr = "bindings";
	public final static String valueJSONAttr = "value";
	public final static String xmlLangJSONAttr = "xml:lang";
	public final static String datatypeJSONAAttr = "datatype";
	public final static String uriJSONAValue = "uri";
	public final static String bnodeJSONAValue = "bnode";
	public final static String literalJSONAValue = "literal";
	
	public final static String uriXMLValue = "uri";
	public final static String literalXMLValue = "lit";
	public final static String bnodeXMLValue = "bn";

	@Autowired
	public Statement(@Value("Statement") String id) {
		super(id);
	}

	public Logger getLogger() {
		return logger;
	}

	@Override
	public Response getPreCheckedResponse(String request) throws HTTPParameterUnspecifiedException {
		if (request.equals(Req.getStatementsRequest) || request.equals(Req.hasStatementRequest)) {
			String subject = setHttpPar(Par.subjectPar);
			String predicate = setHttpPar(Par.predicatePar);
			String object = setHttpPar(Par.objectPar);
			String graphs = setHttpPar(Par.graphsPar);
			String inference = setHttpPar(Par.inferencePar);
			checkRequestParametersAllNotNull(Par.subjectPar, Par.predicatePar, Par.objectPar);
			return get_has_Statements(request, subject, predicate, object, graphs, inference);
		} else
			return servletUtilities.createNoSuchHandlerExceptionResponse(request);

	}

	public Response get_has_Statements(String request, String subjectField, String predicateField,
			String objectField, String ngsField, String inferenceString) {

		logger.debug("processing request: " + request);

		RDFModel ontModel = getOntModel();
		ARTURIResource subject;
		ARTURIResource predicate;
		ARTNode object;
		boolean inference = false;
		if (inferenceString != null)
			inference = Boolean.parseBoolean(inferenceString);

		try {
			// SUBJECT HANDLING
			String subjectType = getNodeType(subjectField);
			if (subjectType.equals(anyNodeValue))
				subject = NodeFilters.ANY;
			else
				subject = ontModel.createURIResource(ontModel.expandQName(getNodeValue(subjectField)));
			// PREDICATE HANDLING
			String predicateType = getNodeType(predicateField);
			if (predicateType.equals(anyNodeValue))
				predicate = NodeFilters.ANY;
			else
				predicate = ontModel.createURIResource(ontModel.expandQName(getNodeValue(predicateField)));
			// OBJECT HANDLING
			String objectValue = getNodeValue(objectField);
			String objectType = getNodeType(objectField);
			if (objectType.equals(anyNodeValue))
				object = NodeFilters.ANY;
			else if (objectType.equals(literalXMLValue)) // object is a literal
				object = ontModel.createLiteral(objectValue);
			else
				object = ontModel.createURIResource(ontModel.expandQName(objectValue));
			// GRAPHS HANDLING
			ARTResource[] ngs;
			if (ngsField != null) {
				String[] ngsString = ngsField.split(";");
				ngs = new ARTResource[ngsString.length];
				for (int i = 0; i < ngsString.length; i++) {
					String currentGraphName = ngsString[i];
					if (currentGraphName.equals(anyNodeValue))
						ngs[i] = NodeFilters.ANY;
					else if (currentGraphName.equals(mainGraphValue))
						ngs[i] = NodeFilters.MAINGRAPH;
					else
						ngs[i] = ontModel.createURIResource(currentGraphName);
				}
			} else {
				ngs = new ARTResource[0];
			}

			XMLResponseREPLY response = servletUtilities.createReplyResponse(request, RepliesStatus.ok);
			Element dataElement = response.getDataElement();

			if (request.equals(Req.getStatementsRequest)) {
				ARTStatementIterator statIt = ontModel.listStatements(subject, predicate, object, inference,
						ngs);
				createStatementsList(ontModel, statIt, dataElement);
			} else {
				boolean result = ontModel.hasTriple(subject, predicate, object, inference, ngs);
				XMLHelp.newElement(dataElement, resultTag, Boolean.toString(result));
			}
			return response;
		} catch (ModelAccessException e) {
			logger.debug(request + ":" + e);
			return servletUtilities.createExceptionResponse(request, e);
		}

	}

	private String getNodeType(String nodePar) {
		// logger.info( "nodePar: " + nodePar );
		// logger.info( nodePar.substring(0, nodePar.indexOf('$')) );
		if (nodePar.equals(anyNodeValue))
			return nodePar;
		else
			return nodePar.substring(0, nodePar.indexOf('$'));
	}

	private String getNodeValue(String nodePar) {
		logger.trace(nodePar.substring(nodePar.indexOf('$') + 1, nodePar.length()));
		return nodePar.substring(nodePar.indexOf('$') + 1, nodePar.length());
	}

	public static void createStatementsList(RDFModel owlModel, ARTStatementIterator statIt,
			Element dataElement) throws DOMException, ModelAccessException {
		while (statIt.streamOpen()) {
			ARTStatement stat = statIt.getNext();
			Element statElement = XMLHelp.newElement(dataElement, statementTag);
			ARTResource subj = stat.getSubject();
			if (subj.isURIResource())
				XMLHelp.newElement(statElement, subjTag, owlModel.getQName(subj.asURIResource().getURI()))
						.setAttribute(typeAttr, uriXMLValue);
			else
				XMLHelp.newElement(statElement, subjTag, subj.toString()).setAttribute(typeAttr,
						bnodeXMLValue);

			XMLHelp.newElement(statElement, predTag, owlModel.getQName(stat.getPredicate().getURI()))
					.setAttribute(typeAttr, uriXMLValue);

			ARTNode obj = stat.getObject();
			if (obj.isResource()) {
				if (obj.isURIResource())
					XMLHelp.newElement(statElement, objTag, owlModel.getQName(obj.asURIResource().getURI()))
							.setAttribute(typeAttr, uriXMLValue);
				else
					XMLHelp.newElement(statElement, objTag, obj.toString()).setAttribute(typeAttr,
							bnodeXMLValue);
			} else
				XMLHelp.newElement(statElement, objTag, obj.asLiteral().toString()).setAttribute(typeAttr,
						literalXMLValue);
		}
		statIt.close();
	}

	public static void createStatementsList(RDFModel owlModel, ARTStatementIterator statIt, JSONObject data)
			throws ModelAccessException, JSONException {
		JSONObject sparqlObject = new JSONObject();
		
		JSONObject headObject = new JSONObject();
		List<String> vars = new ArrayList<String>();
		vars.add(subjTag);
		vars.add(predTag);
		vars.add(objTag);
		headObject.put(varsJSONAttr, new JSONArray(vars));
		sparqlObject.put(headJSONAttr, headObject);
		
		JSONObject resultsObject = new JSONObject();
		JSONArray bindingsArray = new JSONArray();
		
		while (statIt.streamOpen()) {
			ARTStatement stat = statIt.getNext();
			
			JSONObject bindingObject = new JSONObject();
			
			JSONObject subjObject = new JSONObject();
			ARTResource subj = stat.getSubject();
			if (subj.isURIResource()) {
				JSONHelp.newObject(subjObject, typeAttr, uriJSONAValue);
				JSONHelp.newObject(subjObject, valueJSONAttr, subj.asURIResource().getURI());
			} else { //bnode
				JSONHelp.newObject(subjObject, typeAttr, bnodeJSONAValue);
				JSONHelp.newObject(subjObject, valueJSONAttr, subj.toString());
			}
			bindingObject.put(subjTag, subjObject);

			JSONObject predObject = new JSONObject();
			JSONHelp.newObject(predObject, typeAttr, uriJSONAValue);
			JSONHelp.newObject(predObject, valueJSONAttr, stat.getPredicate().getURI());
			bindingObject.put(predTag, predObject);

			JSONObject objObject = new JSONObject();
			ARTNode obj = stat.getObject();
			if (obj.isResource()) {
				if (obj.isURIResource()) {
					JSONHelp.newObject(objObject, typeAttr, uriJSONAValue);
					JSONHelp.newObject(objObject, valueJSONAttr, obj.asURIResource().getURI());
				} else { //bnode
					JSONHelp.newObject(objObject, typeAttr, bnodeJSONAValue);
					JSONHelp.newObject(objObject, valueJSONAttr, obj.toString());
				}
			} else { //literal
				ARTLiteral objLiteral = obj.asLiteral();
				JSONHelp.newObject(objObject, typeAttr, literalJSONAValue);
				JSONHelp.newObject(objObject, valueJSONAttr, objLiteral.getLabel());
				if (objLiteral.getDatatype() != null) {
					JSONHelp.newObject(objObject, datatypeJSONAAttr, objLiteral.getDatatype().getURI());
				}
				if (objLiteral.getLanguage() != null) {
					JSONHelp.newObject(objObject, xmlLangJSONAttr, objLiteral.getLanguage());
				}
			}
			bindingObject.put(objTag, objObject);
			
			bindingsArray.put(bindingObject);
		}
		resultsObject.put(bindingsJSONAttr, bindingsArray);
		sparqlObject.put(resultsJSONAttr, resultsObject);
		data.put(sparqlJSONAttr, sparqlObject);
		
		statIt.close();
	}

}
