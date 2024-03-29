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
 * The Original Code is Semantic Turkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2009.
 * All Rights Reserved.
 *
 * Semantic Turkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about Semantic Turkey can be obtained at 
 * http//ai-nlp.info.uniroma2.it/software/...
 *
 */

/*
 * Contributor(s): Armando Stellato stellato@info.uniroma2.it
 */
package it.uniroma2.art.semanticturkey.servlet;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Defines a JSON serialization based ERROR response  
 * 
 */

public class JSONResponseERROR extends JSONResponseProblem {

	JSONResponseERROR(JSONObject json_content, String request, Exception e, String msg, boolean reportStackTrace) throws JSONException {
		super(json_content, request);
		content.getJSONObject(ServiceVocabulary.responseRoot).put(ServiceVocabulary.responseType, ServiceVocabulary.type_error);
		content.getJSONObject(ServiceVocabulary.responseRoot).put(ServiceVocabulary.exceptionName, e.getClass().getCanonicalName());
		setMessage(msg);
		setExceptionFacets(e);
		setStackTrace(e);
	}

	public boolean isAffirmative() {
		return false;
	}
	
	public void setStackTrace(Exception e) {
		try {
    		content.getJSONObject(ServiceVocabulary.responseRoot).put(ServiceVocabulary.stackTrace, ExceptionUtils.getStackTrace(e));
		} catch (JSONException ex) {
			e.printStackTrace();
		}
    }

    public String getStackTrace() {
    	try {
			return content.getJSONObject(ServiceVocabulary.responseRoot).getString(ServiceVocabulary.stackTrace);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
    }

}
