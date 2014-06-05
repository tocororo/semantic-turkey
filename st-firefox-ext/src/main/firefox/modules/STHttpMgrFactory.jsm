/*
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
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
 * (art.uniroma2.it) at the University of Roma Tor Vergata (ART) Current
 * information about SemanticTurkey can be obtained at
 * http://semanticturkey.uniroma2.it
 * 
 */


Components.utils.import("resource://stmodules/Preferences.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/ResponseContentType.jsm");
Components.utils.import("resource://stmodules/Exceptions.jsm");

let EXPORTED_SYMBOLS = [ "STHttpMgrFactory" ];

const USER_AGENT = "Semantic Turkey";
const RESULT_OK = 0;
const RESULT_PARSE_ERROR = 1;
const RESULT_NOT_FOUND = 2;
const RESULT_NOT_AVAILABLE = 3;
const RESULT_ERROR_FAILURE = 4;
const RESULT_NOT_RSS = 5;


var parameters;

STHttpMgrFactory = new function() {
	
	var instanceIdMap = new Array(); // this array contains other arrays, and each array contains
	// instanceIdMap[groupid][artifactid] is an instance of STHttpMgr
	
	
	this.getInstance = function(groupId, artifactId){
		
		if(typeof instanceIdMap[groupId] == 'undefined') { // check if this is the right way to do it
			Logger.debug("[STHttpMgrFactory.jsm] the instances map does not have any groupId = " + 
					groupId); // DEBUG
			instanceIdMap[groupId] = new Array();
		}
		if(typeof instanceIdMap[groupId][artifactId] == 'undefined') { // check if this is the right way to do it
			Logger.debug("[STHttpMgrFactory.jsm] the instances map does not have any artifactId = " +
					artifactId + "with groupId = "+groupId); // DEBUG
			instanceIdMap[groupId][artifactId] = new STHttpMgr(groupId, artifactId);
		}
		return instanceIdMap[groupId][artifactId];
	};
};

STHttpMgr = function(groupIdInput, artifactIdInput) {

	var requestHandler = new Object();

	//var serverhost = Preferences.get("extensions.semturkey.server.host", "127.0.0.1");
	//var serverport = Preferences.get("extensions.semturkey.server.port", "1979");
	//var serverpath = Preferences.get("extensions.semturkey.server.path", "/semantic_turkey/resources/stserver/STServer");

	
	var serverhost;
	var serverport;
	var serverpath;

	var groupId = groupIdInput;
	var artifactId = artifactIdInput;
	
	this.getGroupId = function(){
		return groupId;
	}
	
	this.getArtifactId = function(){
		return artifactId;
	}
	
	//the url of the new services is: http://address:port/service_turkey/SERVICE/REQUEST?ARGUMETS_WITH_&
	// this url works only with the services inside ST itself, not with the extension
	
	this.getName = function() {
		return 'STHttpMgr';
	};

	this.addRequestHandler = function(request, method) {
		requestHandler[request] = method;
	};

	this.getRequestHandler = function() {
		return requestHandler;
	};

	this.hasRequestHandler = function(request) {
		return (requestHandler[request] != null);
	};

	/**
	 * this function composes a POST request (with async argument always set to false). It can be invoked with
	 * a variable number of arguments. The first four arguments are always the respType, the service, 
	 * the request and the context. All the other ones are the parameters of the http POST request
	 *  The respType can be null or undefined, in this case the RespContType.xml is used
	 */
	this.POST = function(respType, service, request, context) {
		this.refreshServerInfo();
		// an example of a aURL is 
		// http://<serverhost>:<serverport>/semanticturkey/<groupId>/<artifactId>/<service>/<request>
		var aURL = "http://"+ serverhost + ":" + serverport + "/" + serverpath + "/" + groupId + "/" +  
			artifactId + "/" + service + "/"+ request +"?";
		
		var realRespType;
		if ((respType instanceof XMLRespContType) || (respType instanceof JSONRespContType) ) {
			realRespType = respType;
		} else{
			realRespType = RespContType.xml;
		}
		
		// now process the context
		var contextArray = context.getContextValuesForHTTPGetAsArray();
		for ( var i = 0; i < contextArray.length; i++) {
			aURL += this.splitAndEncode(arguments[i][k]);
		}
		
		
		if (arguments.length > 4) {
			for ( var i = 4; i < arguments.length; i++) {
				if(Array.isArray(arguments[i])){
					for(var k=0; k<arguments[i].length; ++k){
						parameters += this.splitAndEncode(arguments[i][k]);
					}
				} else{
					parameters += this.splitAndEncode(arguments[i]);
				}
			}
		}
		Logger.debug("POST: aURL  = "+aURL); // DEBUG
		return this.submitHTTPRequest(realRespType, aURL, "POST", false, parameters);
	};

	/**
	 * this function composes a GET request (with async argument always set to false). It can be invoked with
	 * a variable number of arguments. The first four arguments are always the respType, the service, 
	 * the request and the context. The other ones are the parameters of the http GET request.
	 * The respType can be null or undefined, in this case the RespContType.xml is used
	 */
	this.GET = function(respType, service, request, context) {
		this.refreshServerInfo();
		
		// an example of a aURL is 
		// http://<serverhost>:<serverport>/semanticturkey/<groupId>/<artifactId>/<service>/<request>
		var aURL = "http://"+ serverhost + ":" + serverport + "/" + serverpath + "/" + groupId + "/" +
			artifactId + "/" + service + "/"+ request +"?";
		
//		Logger.debug("[STHttpMgrFactory.jsm] GET: serverhost = "+serverhost+"\nserverport = "+serverport
//				+"\ngroupId = "+groupId +"\nartifactId = "+artifactId + "\nservice = "+service 
//				+ "\nrequest = "+request); // DEBUG		
		
		var realRespType;
		if ((respType instanceof XMLRespContType) || (respType instanceof JSONRespContType) ) {
			realRespType = respType;
		} else{
			realRespType = RespContType.xml;
		}
		
		// process the context
		var contextArray = context.getContextValuesForHTTPGetAsArray();
		for ( var i = 0; i < contextArray.length; i++) {
			aURL += this.splitAndEncode(arguments[i][k]);
		}
		
		//see if there are other arguments
		if (arguments.length > 4) {
			for ( var i = 4; i < arguments.length; i++) {
				if(Array.isArray(arguments[i])){
					for(var k=0; k<arguments[i].length; ++k){
						aURL += this.splitAndEncode(arguments[i][k]);
					}
				} else{
					aURL += this.splitAndEncode(arguments[i]);
				}
			}
		}
		
		
		Logger.debug("GET: aURL  = "+aURL); // DEBUG
		return this.submitHTTPRequest(realRespType, aURL, "GET", false);
	};


	// This function take a String in the form "name=value" and return "name=encode(value)&" or ""
	this.splitAndEncode = function(valueString){
		var index = valueString.indexOf("=");
		var urlPart = "";
		if(index>0){
			//encode the part of the string after the =
			urlPart = valueString.substring(0, index + 1)
					+ encodeURIComponent(valueString.substr(index + 1))+"&";
		}
		return urlPart;
	}
	
	this.submitHTTPRequest = function(respType, aURL, method, async, parameters) {
//		Logger.debug("httpRequest: " + method + ": " + aURL + "| async:" + async + " parameters: " + 
//				parameters + " port: " + serverport);
		Logger.debug(aURL);

		var httpReq;
		try {
			httpReq = new XMLHttpRequest();
		} catch (e) {
			httpReq = Components.classes["@mozilla.org/xmlextras/xmlhttprequest;1"].createInstance();
		}

		// in Firefox http.open() with two parameters behaves as if async were true, but if you called 
		// httpReq.open("GETP", aURL,"undefined") then it would behave as if async were false
		if (typeof async == 'undefined') {
			httpReq.open(method, aURL, false);
		} else {
			httpReq.open(method, aURL, async);
		}

		httpReq.onerror = httpError;

		// try {
		httpReq.setRequestHeader("User-Agent", USER_AGENT);
		if (method == "POST") {
			httpReq.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
			httpReq.setRequestHeader("Content-length", parameters.length);
			// httpReq.setRequestHeader("Connection", "close");
		}
		if (respType instanceof XMLRespContType)
			httpReq.setRequestHeader("Accept", "application/xml");
		else if (respType instanceof JSONRespContType)
			httpReq.setRequestHeader("Accept", "application/json");

		try {
			if (method == "GET")
				httpReq.send(null);
			else
				// "POST"
				httpReq.send(parameters);
		} catch (e) {
			throw new HTTPError(httpReq.status, httpReq.statusText);
		}

		// note: now calls are always synchronous; however, with async calls, this check would be made 
		// before any reply from the server

		// TODO check if other codes should be ok
		if (httpReq.status != 200) {
			throw new HTTPError(httpReq.status, httpReq.statusText);
		}
		
		// check if the desired content for the response is json or sml
		var serializationType = httpReq.getResponseHeader("Content-Type");

		if (serializationType.indexOf("application/xml") != -1) {

			Logger.debug("Parsing the XML response");

			var newResponseXML = httpReq.responseXML;
			var type = newResponseXML.getElementsByTagName("stresponse")[0].getAttribute("type");

			newResponseXML.isReply = function() {
				return (this.getElementsByTagName("stresponse")[0].getAttribute("type") == "reply");
			};

			if (newResponseXML.isReply()) {

				newResponseXML.isOk = function() {
					return (this.getElementsByTagName("reply")[0].getAttribute("status") == "ok");
				};

				newResponseXML.isWarning = function() {
					return (this.getElementsByTagName("reply")[0].getAttribute("status") == "warning");
				};

				newResponseXML.isFail = function() {
					return (this.getElementsByTagName("reply")[0].getAttribute("status") == "fail");
				};

				newResponseXML.getMsg = function() {
					return (this.getElementsByTagName("reply")[0].textContent);
				};
			}

			newResponseXML.isException = function() {
				return (this.getElementsByTagName("stresponse")[0].getAttribute("type") == "exception");
			};

			if (newResponseXML.isException()) {
				throw new STException("java.prova.exception", newResponseXML.getElementsByTagName("msg")[0].firstChild.textContent);
			}
			
			newResponseXML.isError = function() {
				return (this.getElementsByTagName("stresponse")[0].getAttribute("type") == "error");
			};
			
			if (newResponseXML.isError()) {
				throw new STError("java.prova.exception", newResponseXML.getElementsByTagName("msg")[0].firstChild.textContent);
			}
			newResponseXML.getContent = function() {
				return this.getElementsByTagName("data")[0];
			};
			return newResponseXML;
		}

		if (serializationType.indexOf("application/json") != -1) {

			// header json

			var newResponseJSON = JSON.parse(httpReq.responseText);

			Logger.debug("Parsing the JSON response: " + JSON.stringify(newResponseJSON));
			Logger.debug("Response type: " + JSON.stringify(newResponseJSON.stresponse.type));
			Logger.debug("Response data: " + JSON.stringify(newResponseJSON.stresponse.data));

			var type = newResponseJSON.stresponse.type;

			newResponseJSON.isReply = function() {
				return (JSON.stringify(this.stresponse.type) == "reply");
			};

			if (newResponseJSON.isReply()) {

				newResponseJSON.isOk = function() {
					return (JSON.stringify(this.stresponse.reply.status) == "ok");
				};

				newResponseJSON.isWarning = function() {
					return (JSON.stringify(this.stresponse.reply.status) == "warning");
				};

				newResponseJSON.isFail = function() {
					return (JSON.stringify(this.stresponse.reply.status) == "fail");
				};

				if (!newResponseJSON.isReply()) {
					newResponseJSON.getMsg = function() {
						return (JSON.stringify(this.stresponse.msg));
					};
				}
			}

			newResponseJSON.isException = function() {
				return (JSON.stringify(this.stresponse.type) == "exception");
			};

			if (newResponseJSON.isException()) {
				throw new STException("java exception to be embedded", JSON.stringify(newResponseJSON.stresponse.msg));
			}

			newResponseJSON.isError = function() {
				return (JSON.stringify(this.stresponse.type) == "error");
			};

			if (newResponseJSON.isError()) {
				throw new STError("java exception to be embedded", JSON.stringify(newResponseJSON.stresponse.msg));
			}

			newResponseJSON.getContent = function() {
				return JSON.stringify(this.stresponse.data);
			};

			return newResponseJSON;
		}

	};

	function httpError(e) {
		Logger.debug("HTTP Error: status: " + e.target.status + " - " + e.target.statusText);
	}
	;

	this.parseXMLSource = function(document) {
		var serializer = Components.classes["@mozilla.org/xmlextras/xmlserializer;1"].createInstance();
		return serializer.serializeToString(document);
	};


	this.getAuthority = function() {
		return serverhost + ":" + serverport;
	};
	
	this.setAuthority = function(host, port) {
		serverhost = host;
		serverport = port;
		Preferences.set("extensions.semturkey.server.host", host);
		Preferences.set("extensions.semturkey.server.port", port);
	};
	
	//this function should be called when the paramet
	this.refreshServerInfo = function(){
		serverhost = Preferences.get("extensions.semturkey.server.host", "127.0.0.1");
		serverport = Preferences.get("extensions.semturkey.server.port", "1979");
		serverpath = Preferences.get("extensions.semturkey.server.serverid", "semanticturkey");
	}
	
	
	
	
};


