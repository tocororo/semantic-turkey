if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOSXL.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);

var langsPrefsEntry = "extensions.semturkey.annotprops.langs";
var defaultLangPref = "extensions.semturkey.annotprops.defaultlang";

//parameters received from parent
var lang;
var concept;
var labelType;//skos, skosxl

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function() {
	//init parameters
	lang = window.arguments[0].lang;
	concept = window.arguments[0].concept;
	labelType = window.arguments[0].labelType; 
	document.title = "Set " + labelType + ":prefLabel";
	art_semanticturkey.initLangMenulist();
	
}

art_semanticturkey.initLangMenulist = function() {
	var menulist = document.getElementById("menulist");
	var langMenupopup = document.getElementById("menupopup");
	var prefs = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);
	var langList = prefs.getCharPref(langsPrefsEntry).split(",");
	langList.sort();
	var langMenuitem = document.createElement("menuitem");
	langMenuitem.setAttribute('label', "no language");
	langMenuitem.setAttribute('id', "");
	langMenupopup.appendChild(langMenuitem);
	for (var i = 0; i < langList.length; i++) {
		langMenuitem = document.createElement("menuitem");
		langMenuitem.setAttribute('label', langList[i]);
		langMenuitem.setAttribute('id', langList[i]);
		langMenupopup.appendChild(langMenuitem);
	}
	if (typeof lang == 'undefined') {
		var defaultLang = prefs.getCharPref(defaultLangPref);
		menulist.selectedItem = document.getElementById(defaultLang);
	} else {
		menulist.selectedItem = document.getElementById(lang);
	}
}

/**
 * Gets the label written in the textbox and set it as prefLabel. This method is called when OK button is pressed.
 */
function buttonOkListener() {
	var txtLabel = document.getElementById("textBoxLabel");
	var menulist = document.getElementById("menulist");
	var label = txtLabel.value;
	var lang = menulist.selectedItem.id;
	if (labelType == "skosxl")
		art_semanticturkey.STRequests.SKOSXL.setPrefLabel(concept, label, lang);
	else if (labelType == "skos")
		art_semanticturkey.STRequests.SKOS.setPrefLabel(concept, label, lang);
}

/**
 * Sets to null the returnedValue, so that the calling script will know that the operation has been canceled.
 * This method is called when cancel button is pressed.
 */
function buttonCancelListener() {
//	window.arguments[0].returnedValue = null;
}