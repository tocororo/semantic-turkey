/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
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

package it.uniroma2.art.semanticturkey.servlet;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

/**Classe che contiene le utilities per le servlet, oltre ad alcuni metodi di supporto la maggior parte dei metodi
 * dichiarati aggiungono elementi xml al documento che costituiscono le risposte alle servlet invocate da client*/
/**
 * @author Donato Griesi Contributor(s): Andrea Turbati
 */
public class ServletUtilities {
	protected static Logger logger = LoggerFactory.getLogger(ServletUtilities.class);
	static final private ServletUtilities service;

	static {
		service = new ServletUtilities();
	}

	public static ServletUtilities getService() {
		return service;
	}

	// Ramon Orrù (2010) : modifica per introduzione serializzazione JSON
	public ResponseREPLY createReplyResponse(String request, RepliesStatus status, SerializationType ser_type) {
		if (ser_type == SerializationType.xml) {
			Document xml = XMLHelp.createNewDoc();
			return new XMLResponseREPLY(xml, request, status);
		} else {
			JSONObject json_content = new JSONObject();
			try {
				return new JSONResponseREPLY(json_content, request, status);
			} catch (JSONException e) {
				logger.error("Error in Json response creation:" + e.getMessage());
				e.printStackTrace();
			}
		}
		return null;
	}

	public JSONResponseEXCEPTION createExceptionResponse(String request, Exception ex, String msg) {
		return (JSONResponseEXCEPTION) createExceptionResponse(request, ex, msg, SerializationType.json);
	}

	/**
	 * produces a response (xml,json) telling the client that some exception has occurred
	 * 
	 * @param value
	 * @return
	 * @throws JSONException
	 */
	public ResponseProblem createExceptionResponse(String request, Exception ex, String msg, SerializationType ser_type) {
		if (ser_type == SerializationType.xml) {
			Document xml = XMLHelp.createNewDoc();
			return new XMLResponseEXCEPTION(xml, request, ex, msg);
		} else {
			JSONObject json_content = new JSONObject();
			try {
				return new JSONResponseEXCEPTION(json_content, request, ex, msg);
			} catch (JSONException e) {
				logger.error("Error in Json response creation:" + e.getMessage());
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * produces an xml document telling the client that some error has occurred
	 * 
	 * @param value
	 * @return
	 * @throws JSONException
	 */
	public ResponseProblem createErrorResponse(String request, String msg, SerializationType ser_type) {
		if (ser_type == SerializationType.xml) {
			Document xml = XMLHelp.createNewDoc();
			return new XMLResponseERROR(xml, request, msg);
		} else {
			JSONObject json_content = new JSONObject();
			try {
				return new JSONResponseERROR(json_content, request, msg);
			} catch (JSONException e) {
				logger.error("Error in Json response creation:" + e.getMessage());
				e.printStackTrace();
			}
		}
		return null;
	}

	public JSONResponseERROR createErrorResponse(String request, String msg) {
		return (JSONResponseERROR) createErrorResponse(request, msg, SerializationType.json);
	}

	public ResponseProblem createNoSuchHandlerExceptionResponse(String request, SerializationType ser_type) {
		if (ser_type == SerializationType.xml) {
			Document xml = XMLHelp.createNewDoc();
			return new XMLResponseEXCEPTION(xml, request, "no handler for request: " + request + " !!!");
		} else {
			JSONObject json_content = new JSONObject();
			try {
				return new JSONResponseEXCEPTION(json_content, request, "no handler for such a request: "
						+ request + " !!!");
			} catch (JSONException e) {
				logger.error("Error in Json response creation:" + e.getMessage());
				e.printStackTrace();
			}
		}
		return null;
	}

	public static final String ontAccessProblem = "problems in accessing the ontology";

	static final String ontUpdateProblem = "problems in updating the ontology";

}
