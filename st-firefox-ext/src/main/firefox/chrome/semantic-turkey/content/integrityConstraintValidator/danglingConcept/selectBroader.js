if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}


art_semanticturkey.init = function() {
	var concept = window.arguments[0].concept;
	var scheme = window.arguments[0].scheme;
	
	var label = document.getElementById("description");
	label.value = "Select a concept to set as skos:broader of concept <" + concept + ">";
	//se lo scheme è vuoto? -> messaggio con solo buttonCancel disponibile
	
	var conceptTree = document.getElementById("conceptTree");
	conceptTree.conceptScheme = scheme;
}

/**
 * This method is called when ok button is pressed.
 */
function buttonOkListener() {//TODO
	var conceptTree = document.getElementById("conceptTree");
	var selectedConcept = conceptTree.selectedConcept;
	window.arguments[0].returnedValue = selectedConcept;
}

/**
 * This method is called when cancel button is pressed.
 */
function buttonCancelListener() {
	window.arguments[0].returnedValue = null;
}