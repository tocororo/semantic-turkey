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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLResponseEXCEPTION extends XMLResponseProblem implements ResponseException {

	XMLResponseEXCEPTION(Document xml, String request, String msg) {
		super(xml, request);
		getResponseElement().setAttribute(ServiceVocabulary.responseType, ServiceVocabulary.type_exception);
		setMessage(msg);
	}

	XMLResponseEXCEPTION(Document xml, String request, Exception e, String msg, boolean reportStackTrace) {
		super(xml, request);
		getResponseElement().setAttribute(ServiceVocabulary.responseType, ServiceVocabulary.type_exception);
		getResponseElement().setAttribute(ServiceVocabulary.exceptionName, e.getClass().getCanonicalName());
		setMessage(msg);
		if (reportStackTrace) {
			Element stackElement = xml.createElement(ServiceVocabulary.stackTrace);
			getResponseElement().appendChild(stackElement);
			setStackTrace(e);
		}
	}

	public boolean isAffirmative() {
		return false;
	}
	
	public void setStackTrace(Exception e) {
		getStackTraceElement().setTextContent(ExceptionUtils.getStackTrace(e));
    }

    public String getStackTrace() {
    	return getStackTraceElement().getTextContent();
    }
    
    private Element getStackTraceElement(){
    	return (Element) getResponseElement().getElementsByTagName(ServiceVocabulary.stackTrace).item(0);
    }

}
