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


if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stservices/SERVICE_Administration.jsm",
		art_semanticturkey);
Components.utils.import("resource://stmodules/SemanticTurkeyMetadata.jsm",
		art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);


/**
 * @author NScarpato 21/04/2008 manageInput
 */
art_semanticturkey.manageInput = function(type, txbox) {
	var isurl = art_semanticturkey.isUrl(txbox.value);
	if (isurl == true) {
		txbox.style.color = 'blue';
	} else {
		txbox.style.color = 'red';
	}
};

art_semanticturkey.isUrl = function(s) {
	if(s.indexOf(" ") != -1)
		return false;
	if((s.indexOf("ftp://") != 0) && (s.indexOf("http://") != 0) && (s.indexOf("https://") != 0))
		return false;
	var regexp = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/;
	return regexp.test(s);
};
/**
 * @author NScarpato 21/04/2008 Checks if a string ends with the specified
 *         substring or char
 * @param str
 *            the sub string or char to check for.
 * @returns true if the string ends with the sub string or char, otherwise
 *          false.
 */

art_semanticturkey.stringEndsWith = function(string, str){
	var offset = string.length - str.length;
	return offset >= 0 && string.lastIndexOf(str) === offset;
};


/**
 * @author NScarpato
 * @date 19-12-2008
 * @param url:
 *            string that represent the selected url
 * @description open selected url in a new tab
 */
art_semanticturkey.openUrl = function(url) {
	var win = Components.classes['@mozilla.org/appshell/window-mediator;1']
			.getService(Components.interfaces.nsIWindowMediator)
			.getMostRecentWindow('navigator:browser');
	win.openUILinkIn(url, "tab");
};




art_semanticturkey.getImgFromType = function(type, explicit) {
	var imgType;
	if (type == "individual") {
		if (explicit == "false")
			imgType = "chrome://semantic-turkey/skin/images/individual_noexpl.png";
		else
			imgType = "chrome://semantic-turkey/skin/images/individual.png";
	} else if (type == "cls") {
		if (explicit == "false")
			imgType = "chrome://semantic-turkey/skin/images/class_imported.png";
		else
			imgType = "chrome://semantic-turkey/skin/images/class.png";
	} else if (type.indexOf("ObjectProperty") != -1) {
		if (explicit == "false")
			imgType = "chrome://semantic-turkey/skin/images/propObject_imported.png";
		else
			imgType = "chrome://semantic-turkey/skin/images/propObject20x20.png";
	} else if (type.indexOf("DatatypeProperty") != -1) {
		if (explicit == "false")
			imgType = "chrome://semantic-turkey/skin/images/propDatatype_imported.png";
		else
			imgType = "chrome://semantic-turkey/skin/images/propDatatype20x20.png";
	} else if (type.indexOf("AnnotationProperty") != -1) {
		if (explicit == "false")
			imgType = "chrome://semantic-turkey/skin/images/propAnnotation_imported.png";
		else
			imgType = "chrome://semantic-turkey/skin/images/propAnnotation20x20.png";
	} else if (type.indexOf("Property") != -1) {
		if (explicit == "false")
			imgType = "chrome://semantic-turkey/skin/images/prop_imported.png";
		else
			imgType = "chrome://semantic-turkey/skin/images/prop.png";
	} else if(type.indexOf("concept") != -1) {
		if (explicit == "false")
			imgType = "chrome://semantic-turkey/skin/images/skosConcept_imported.png";
		else
			imgType = "chrome://semantic-turkey/skin/images/skosConcept.png";		
	} else if (type.indexOf("literal") != -1) {
		// vedere se mettere img o no
		imgType = "";
	} else if (type.indexOf("bnodes") != -1) {
		// vedere se mettere img o no
		imgType = "";
	}
	return imgType;
};

art_semanticturkey.compareVersions  = function(){
	//first take the client version
	var serverVersionResponse = art_semanticturkey.STRequests.Administration.getVersion();
	var serverVersion = serverVersionResponse.getElementsByTagName("value")[0].textContent; // TODO check
	art_semanticturkey.Logger.debug("server Version = "+serverVersion);
	
	//then take the server version
	var clientVersion = art_semanticturkey.SemanticTurkeyMetadata.getClientVersion();
	
	art_semanticturkey.Logger.debug("client Version = "+clientVersion);
	
	
	//compare the two versions
	if(serverVersion != clientVersion){
		//the server and the client have two different version
		var text = "The client and the server have two different versions:"+
				"\n\tserver version = "+serverVersion+
				"\n\tclient version = "+clientVersion;
		
		art_semanticturkey.Logger.debug(text);
		alert(text);
		
	}
}


