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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniroma2.art.semanticturkey.mvc.RequestMappingHandlerAdapterPostProcessor;
import it.uniroma2.art.semanticturkey.services.ExceptionFacet;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

 /**
 * Defines a JSON realization of ResponseProblem interface  
 * 
 * @autor Ramon Orrù
 * 
 */
public abstract class JSONResponseProblem extends JSONResponse implements ResponseProblem {
    
    JSONResponseProblem(JSONObject json_content, String request) throws JSONException {
		super(json_content, request);
		content.getJSONObject(ServiceVocabulary.responseRoot).put(ServiceVocabulary.msg,"");
    }

    public void setMessage(String msg){
    	try {
    		content.getJSONObject(ServiceVocabulary.responseRoot).put(ServiceVocabulary.msg,msg);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    }

    public String getMessage(){
    	try {
			return content.getJSONObject(ServiceVocabulary.responseRoot).getString(ServiceVocabulary.msg);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
    }

    protected void setExceptionFacets(Exception e) {
		ObjectMapper om = RequestMappingHandlerAdapterPostProcessor.createObjectMapper();

		addExceptionFacets(e, om);
		addExceptionFacets(e.getCause(), om);
	}

	 private void addExceptionFacets(Throwable ex, ObjectMapper om) {
		 if (ex == null) return;

		 try {
			 JSONObject responseContent = (JSONObject) this.getResponseObject().get(ServiceVocabulary.responseRoot);
			 for (Method m : MethodUtils.getMethodsListWithAnnotation(ex.getClass(), ExceptionFacet.class, true, false)) {
				 Object value = m.invoke(ex);
				 responseContent.
				 	put(m.getAnnotation(ExceptionFacet.class).value(),
							toJSONValue(om, value));
			 }
		 } catch (IllegalAccessException | InvocationTargetException | JSONException | JsonProcessingException e) {
			 e.printStackTrace();
		 }
	 }

	 private Object toJSONValue(ObjectMapper om, Object value) throws JSONException, JsonProcessingException {
    	if (value == null) return null;

		 JsonNode jsonNode = om.valueToTree(value);
		 if (jsonNode.isObject()) {
		 	return new JSONObject(om.writeValueAsString(jsonNode));
		 } else if (jsonNode.isArray()) {
		 	return new JSONArray(om.writeValueAsString(jsonNode));
		 } else if (jsonNode.isTextual()) {
		 	return jsonNode.textValue();
		 } else if (jsonNode.isBoolean()) {
		 	return jsonNode.asBoolean();
		 } else if (jsonNode.isFloatingPointNumber()) {
		 	return jsonNode.asDouble();
		 } else if (jsonNode.isIntegralNumber()) {
		 	return jsonNode.asLong();
		 } else {
		 	throw new IllegalArgumentException("Cannot convert jackson node " + jsonNode.toString());
		 }
	 }
 }
