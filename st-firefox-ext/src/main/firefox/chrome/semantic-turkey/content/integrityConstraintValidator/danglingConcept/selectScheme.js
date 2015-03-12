if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}


art_semanticturkey.init = function() {
	var concept = window.arguments[0].concept;
	var inScheme = window.arguments[0].inScheme;
	
	//setting header
	document.getElementById("header").setAttribute("value", "The dangling concept "+concept+" is in multiple schemes. Please, select a scheme.");
	//populating radiogroup with schemes passed through arguments
	var radioGroup = document.getElementById("radiogroup");
	for (var i=0; i<inScheme.length; i++){
		var radio = document.createElement("radio");
		radio.setAttribute("label", inScheme[i]);
		if (i==0)
			radio.setAttribute("selected", true);//by default set first radio selected (so, if OK button is clicked, at least an element is selected) 
		radioGroup.appendChild(radio);
	}
	
}

/**
 * Gets the scheme with the checkbox selected. This method is called when OK dialog button is pressed as
 * specified in selectSchemeDialog.xul
 */
function buttonOkListener() {
	var radiogroup = document.getElementById("radiogroup");
	window.arguments[0].returnedValue = radiogroup.selectedItem.getAttribute("label"); //value returned to calling window
}


/**
 * Sets to null the returnedValue, so that the calling script will know that the operation has been canceled.
 * This method is called when cancel button is pressed.
 */
function buttonCancelListener() {
	window.arguments[0].returnedValue = null;
}