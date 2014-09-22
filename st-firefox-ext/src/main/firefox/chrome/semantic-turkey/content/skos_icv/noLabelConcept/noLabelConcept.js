if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_SKOS_ICV.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

var BTN_SET_SKOS_LABEL = "Set skos:prefLabel";
var BTN_SET_SKOSXL_LABEL = "Set skosxl:prefLabel";

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function(){
	var rows = document.getElementById("gridRows");
	try {
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listConceptsWithNoLabel();
		var data = xmlResp.getElementsByTagName("data")[0];
		var concepts = data.getElementsByTagName("concept");
		//init UI
		for (var i=0; i<concepts.length; i++){
			var concept = concepts[i].textContent;
			var row = document.createElement("row");
			row.setAttribute("align", "center");
			var conceptLabel = document.createElement("label");
			conceptLabel.setAttribute("value", concept);
			conceptLabel.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
			var btnSetSkosLabel = document.createElement("button");
			btnSetSkosLabel.setAttribute("label", BTN_SET_SKOS_LABEL);
			btnSetSkosLabel.addEventListener("command", art_semanticturkey.setLabelButtonListener, false);			
			var btnSetSkosxlLabel = document.createElement("button");
			btnSetSkosxlLabel.setAttribute("label", BTN_SET_SKOSXL_LABEL);
			btnSetSkosxlLabel.addEventListener("command", art_semanticturkey.setLabelButtonListener, false);
			row.appendChild(conceptLabel);
			row.appendChild(btnSetSkosLabel);
			row.appendChild(btnSetSkosxlLabel);
			rows.appendChild(row);
		}
	} catch (e){
		alert(e.message);
	}
}

/**
 * Listener to the buttons to set a label.
 */
art_semanticturkey.setLabelButtonListener = function() {
	var btn = this;
	btnValue = btn.label;
	var row = btn.parentNode;
	var concept = row.children[0].getAttribute("value");
	//open dialog to set a label
	var parameters = new Object();
	parameters.concept = concept;
	if (btnValue == BTN_SET_SKOS_LABEL)
		parameters.labelType = "skos";
	else if (btnValue == BTN_SET_SKOSXL_LABEL)
		parameters.labelType = "skosxl";
//	parameters.returnedValue = null;
	window.openDialog("chrome://semantic-turkey/content/skos_icv/setPrefLabelDialog.xul",
			"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen",
			parameters);
}

/**
 * Listener to the concept, when double clicked it opens the editor panel
 */
art_semanticturkey.conceptDblClickListener = function() {
	var concept = this.getAttribute("value");//this in an actionListener represents the target of the listener
	var parameters = new Object();
	parameters.sourceType = "concept";
	parameters.sourceElement = concept;
	parameters.sourceElementName = concept;
	parameters.parentWindow = window;
	parameters.isFirstEditor = true;
	//TODO: restore if the new editor panel becomes active
//	window.openDialog("chrome://semantic-turkey/content/editorsNew/editorPanel.xul", 
//			"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", 
//			parameters);
	window.openDialog("chrome://semantic-turkey/content/editors/editorPanel.xul",
			"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", 
			parameters);
}