package it.uniroma2.art.semanticturkey.servlet.main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.navigation.ARTStatementIterator;
import it.uniroma2.art.owlart.query.BooleanQuery;
import it.uniroma2.art.owlart.query.GraphQuery;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.Query;
import it.uniroma2.art.owlart.query.QueryLanguage;
import it.uniroma2.art.owlart.query.TupleQuery;
import it.uniroma2.art.owlart.query.Update;
import it.uniroma2.art.owlart.query.io.TupleBindingsWriterException;
import it.uniroma2.art.owlart.query.io.TupleBindingsWritingFormat;
import it.uniroma2.art.semanticturkey.exceptions.HTTPParameterUnspecifiedException;
import it.uniroma2.art.semanticturkey.plugin.extpts.ServiceAdapter;
import it.uniroma2.art.semanticturkey.servlet.JSONResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.JSONHelp;
import it.uniroma2.art.semanticturkey.utilities.Utilities;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

/**
 * This service replies to SPARQL queries (or queries expressed in other query languages if supported by the
 * current {@link STOntologyManager} implementation)
 * 
 * @author Armando Stellato
 * 
 */
@Component

public class SPARQL extends ServiceAdapter {

	protected static Logger logger = LoggerFactory.getLogger(SPARQL.class);

	public final static String resolveQueryRequest = "resolveQuery";

	public final static String queryPar = "query";
	public final static String languagePar = "lang";
	public final static String inferPar = "infer";
	public final static String modePar = "mode";

	public final static String resultTypeAttr = "resulttype";

	@Autowired public SPARQL(@Value("Sparql") String id) {
		super(id);
	}

	/**
	 * 
	 * @return
	 */
	public Response getPreCheckedResponse(String request) throws HTTPParameterUnspecifiedException {
		SerializationType ser_type = req().getAcceptContent();

		if (request.equals(resolveQueryRequest)) {
			String query = setHttpPar(queryPar);
			String lang = setHttpPar(languagePar);
			String infer = setHttpPar(inferPar);
			String mode = setHttpPar(modePar);

			checkRequestParametersAllNotNull(queryPar);
			return resolveQuery(query, lang, infer, mode, ser_type);
		} else {
			return servletUtilities.createNoSuchHandlerExceptionResponse(request);
		}

	}

	public Logger getLogger() {
		return logger;
	}

	public Response resolveQuery(String queryString, String lang, String inferString, String mode,
			SerializationType ser_type) {

		String request = resolveQueryRequest;

		logger.debug("resolving query:\n" + queryString);

		QueryLanguage ql;
		if (lang != null)
			ql = QueryLanguage.parseLanguage(lang);
		else
			ql = QueryLanguage.SPARQL;
		logger.debug("query language: " + ql);

		boolean infer;
		if (inferString != null)
			infer = Boolean.parseBoolean(inferString);
		else
			infer = false;
		logger.debug("inference set: " + ql);

		if (mode == null) {
			mode = "query";
			logger.debug("query mode default to query");
		} else {
			if (!mode.equals("query") && !mode.equals("update")) {
				return logAndSendException("Unknown mode " + mode);
			}
			logger.debug("query mode: " + mode);
		}
		logger.debug("query language: " + ql);

		
		RDFModel owlModel = getOntModel();

		ResponseREPLY response = ServletUtilities.getService().createReplyResponse(request,
				RepliesStatus.ok, ser_type);
		
		try {

			
			if (response instanceof XMLResponseREPLY) {
				
				// XML REQUEST
				
				Element dataElement = ((XMLResponseREPLY) response).getDataElement();
				if ("query".equalsIgnoreCase(mode)) {  // a query
					Query query = owlModel.createQuery(ql, queryString);
					if (query instanceof TupleQuery) {
						logger.debug("query is a tuple query");
						dataElement.setAttribute(resultTypeAttr, "tuple");
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						((TupleQuery) query).evaluate(infer, TupleBindingsWritingFormat.XML, baos);
						Document doc = XMLHelp.byteArrayOutputStream2XML(baos);
						Node importedSPARQLResult = ((Document) response.getResponseObject()).importNode(doc
								.getDocumentElement(), true);
						dataElement.appendChild(importedSPARQLResult);
						logger.debug(XMLHelp.XML2String(doc));
					} else if (query instanceof GraphQuery) {
						logger.debug("query is a graph query");
						dataElement.setAttribute(resultTypeAttr, "graph");
						ARTStatementIterator statIt = ((GraphQuery) query).evaluate(infer);
						createStatementsList(owlModel, statIt, dataElement);
					} else if (query instanceof BooleanQuery) {
						logger.debug("query is a boolean query");
						dataElement.setAttribute(resultTypeAttr, "boolean");
						boolean result = ((BooleanQuery) query).evaluate(infer);
						XMLHelp.newElement(dataElement, "result", Boolean.toString(result));
					}
				} else { // an update
					Update update = owlModel.createUpdate(ql, queryString,
							getProject().getNewOntologyManager().getBaseURI());
					update.evaluate(infer);
					// Nothing to return in case of a successful update
				}
			} else {
				
				// JSON REQUEST
					
				if (response instanceof JSONResponseREPLY) {
					JSONObject data = null;
					data = ((JSONResponseREPLY) response).getDataElement();
					if ("query".equalsIgnoreCase(mode)) {  // a query
						Query query = owlModel.createQuery(ql, queryString);
						if (query instanceof TupleQuery) {
							logger.debug("query is a tuple query");
							data.put(resultTypeAttr, "tuple");
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							((TupleQuery) query).evaluate(infer, TupleBindingsWritingFormat.JSON, baos);
							String sparql_result = baos.toString();
							data.put("sparql", new JSONObject(sparql_result));
						} else if (query instanceof GraphQuery) {
							logger.debug("query is a graph query");
							data.put(resultTypeAttr, "graph");
							ARTStatementIterator statIt = ((GraphQuery) query).evaluate(infer);
							createStatementsList(owlModel, statIt, data);
						} else if (query instanceof BooleanQuery) {
							logger.debug("query is a boolean query");
							data.put(resultTypeAttr, "boolean");
							boolean result = ((BooleanQuery) query).evaluate(infer);
							JSONObject sparqlObj = new JSONObject();
							sparqlObj.put("head", new JSONObject());
							sparqlObj.put("boolean", result);
							data.put("sparql", sparqlObj);
						}
	
						logger.debug("JSON data: \n" + data.toString(3));
					} else {  // an update
						Update update = owlModel.createUpdate(ql, queryString,
								getProject().getNewOntologyManager().getBaseURI());
						update.evaluate(infer);
						// Nothing to return in case of a successful update
					}
					
				}
			}
			return response;

		} catch (UnsupportedQueryLanguageException e) {
			logger.error(Utilities.printFullStackTrace(e));
			return servletUtilities.createExceptionResponse(request, e.toString(), ser_type);
		} catch (ModelAccessException e) {
			logger.error(Utilities.printFullStackTrace(e));
			return servletUtilities.createExceptionResponse(request, e.toString(), ser_type);
		} catch (MalformedQueryException e) {
			response.setReplyStatusFAIL("malformed query: " + e.getMessage());
			return response;
		} catch (QueryEvaluationException e) {
			logger.error(Utilities.printFullStackTrace(e));
			return servletUtilities.createExceptionResponse(request, e.toString(), ser_type);
		} catch (TupleBindingsWriterException e) {
			logger.error(Utilities.printFullStackTrace(e));
			return servletUtilities.createExceptionResponse(request, e.toString(), ser_type);
		} catch (UnsupportedEncodingException e) {
			logger.error(Utilities.printFullStackTrace(e));
			return servletUtilities.createExceptionResponse(request, e.toString(), ser_type);
		} catch (SAXException e) {
			logger.error(Utilities.printFullStackTrace(e));
			return servletUtilities.createExceptionResponse(request, e.toString(), ser_type);
		} catch (IOException e) {
			logger.error(Utilities.printFullStackTrace(e));
			return servletUtilities.createExceptionResponse(request, e.toString(), ser_type);
		} catch (JSONException e) {
			logger.error(Utilities.printFullStackTrace(e));
			logger.error("******Errore JSON*****");
			return servletUtilities.createExceptionResponse(request, e.toString(), ser_type);
		}
	}
	
	private final static String statementTag = "stm";
	private final static String subjTag = "subj";
	private final static String predTag = "pred";
	private final static String objTag = "obj";
	private final static String typeAttr = "type";
	private final static String uriXMLValue = "uri";
	private final static String literalXMLValue = "lit";
	private final static String bnodeXMLValue = "bn";
	private final static String sparqlJSONAttr = "sparql";
	private final static String headJSONAttr = "head";
	private final static String varsJSONAttr = "vars";
	private final static String resultsJSONAttr = "results";
	private final static String bindingsJSONAttr = "bindings";
	private final static String valueJSONAttr = "value";
	private final static String xmlLangJSONAttr = "xml:lang";
	private final static String datatypeJSONAAttr = "datatype";
	private final static String uriJSONAValue = "uri";
	private final static String bnodeJSONAValue = "bnode";
	private final static String literalJSONAValue = "literal";
	
	private void createStatementsList(RDFModel owlModel, ARTStatementIterator statIt,
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