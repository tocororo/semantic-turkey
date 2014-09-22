if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOSXL.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Property.jsm", art_semanticturkey);

var langsPrefsEntry = "extensions.semturkey.annotprops.langs";
var defaultLangPref = "extensions.semturkey.annotprops.defaultlang";
//parameters received from parent
var concept;
var label;
var labelPred;

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function() {
	//init parameters
	concept = window.arguments[0].concept;
	label = window.arguments[0].label;
	document.getElementById("label").setAttribute("value", label);
	labelPred = window.arguments[0].labelPred; //skos:prefLabel, skos:altLabel
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
	var defaultLang = prefs.getCharPref(defaultLangPref);
	menulist.selectedItem = document.getElementById(defaultLang);
}

/**
 * Gets the label written in the textbox and set it as prefLabel. This method is called when OK button is pressed.
 */
function buttonOkListener() {
	var menulist = document.getElementById("menulist");
	var lang = menulist.selectedItem.id;
	if (labelPred == "skos:prefLabel"){
		//rimozione e aggiunta label con language tag
		art_semanticturkey.STRequests.SKOS.removePrefLabel(concept, label, "");
		art_semanticturkey.STRequests.SKOS.setPrefLabel(concept, label, lang);
	} else if (labelPred == "skos:altLabel"){
		//rimozione e aggiunta label con language tag
		art_semanticturkey.STRequests.Property.removePropValue(concept, "skos:altLabel", label, null, "plainLiteral", "");
		art_semanticturkey.STRequests.Property.createAndAddPropValue(concept, "skos:altLabel", label, null, "plainLiteral", lang);
	} else if (labelPred == "skosxl:prefLabel"){
		//rimozione e aggiunta label con language tag
		art_semanticturkey.STRequests.SKOS.removePrefLabel(concept, label, "");
		art_semanticturkey.STRequests.SKOS.setPrefLabel(concept, label, lang);
	} else if (labelPred == "skosxl:altLabel"){
		//rimozione e aggiunta label con language tag
		//TODO NON CI SONO SERVIZI PER RIMUOVERE/AGGIUNGERE SKOSXL:ALTLABEL
	}
}

/**
 * Sets to null the returnedValue, so that the calling script will know that the operation has been canceled.
 * This method is called when cancel button is pressed.
 */
function buttonCancelListener() {
//	window.arguments[0].returnedValue = null;
}