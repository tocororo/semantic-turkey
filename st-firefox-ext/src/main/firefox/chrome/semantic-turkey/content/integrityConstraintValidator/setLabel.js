if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOSXL.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Property.jsm", art_semanticturkey);

var langsPrefsEntry = "extensions.semturkey.annotprops.langs";
var defaultLangPref = "extensions.semturkey.annotprops.defaultlang";

//parameters received from parent
var resource;//the skos:Concept or skos:ConceptScheme whose label should be edited 
var labelType;//skos:prefLabel, skos:altLabel, skosxl:prefLabel, skosxl:altLabel
var label;//the optional label of the concept or scheme
var lang;//the optional language tag of the label
var editLabel;//true if it's allowed to edit the lexical label
var editLang;//true if it's allowed to edit the language tag
var replaceLabel;//true if the old label must be replaced by the new one
				//not necessary if it's changing the lexical label of a skos(xl):prefLabel 

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function() {
	//init parameters
	resource = window.arguments[0].resource;
	labelType = window.arguments[0].labelType;
	label = window.arguments[0].label;
	lang = window.arguments[0].lang;
	editLabel = window.arguments[0].editLabel;
	if (typeof editLabel == "undefined")
		editLabel = true;
	editLang = window.arguments[0].editLang;
	if (typeof editLang == "undefined")
		editLang = true;
	replaceLabel = window.arguments[0].replaceLabel;
	if (typeof replaceLabel == "undefined")
		replaceLabel = false;
	var txtLabel = document.getElementById("textBoxLabel");
	if (!editLabel)
		txtLabel.setAttribute("disabled", "true");
	if (typeof label != "undefined")
		txtLabel.setAttribute("value", label);
	document.title = "Set " + labelType;
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
	if (!editLang)
		menulist.setAttribute("disabled", "true");
}

/**
 * Gets the label written in the textbox and set it as prefLabel. This method is called when OK button is pressed.
 */
function buttonOkListener() {
	var txtLabel = document.getElementById("textBoxLabel");
	var menulist = document.getElementById("menulist");
	var newLabel = txtLabel.value;
	var newLang = menulist.selectedItem.id;
	if (labelType == "skosxl:prefLabel") {
		if (replaceLabel)
			art_semanticturkey.STRequests.SKOSXL.removePrefLabel(resource, label, lang);//remove old label
		art_semanticturkey.STRequests.SKOSXL.setPrefLabel(resource, newLabel, newLang, "bnode");
	} else if (labelType == "skos:prefLabel") {
		if (replaceLabel)
			art_semanticturkey.STRequests.SKOS.removePrefLabel(resource, label, lang);//remove old label
		art_semanticturkey.STRequests.SKOS.setPrefLabel(resource, newLabel, newLang);
	} else if (labelType == "skosxl:altLabel") {
		if (replaceLabel)
			art_semanticturkey.STRequests.SKOSXL.removeAltLabel(resource, label, lang);//remove old label
		art_semanticturkey.STRequests.SKOSXL.addAltLabel(resource, newLabel, newLang, "bnode");
	} else if (labelType == "skos:altLabel") {
		if (replaceLabel)
			art_semanticturkey.STRequests.Property.removePropValue(resource, "skos:altLabel", label, null, "plainLiteral", lang);//remove old label
		art_semanticturkey.STRequests.Property.createAndAddPropValue(resource, "skos:altLabel", newLabel, null, "plainLiteral", newLang);
	}
//	window.arguments[0].returnedValue = "ok";
}

/**
 * Sets to null the returnedValue, so that the calling script will know that the operation has been canceled.
 * This method is called when cancel button is pressed.
 */
function buttonCancelListener() {
//	window.arguments[0].returnedValue = null;
}