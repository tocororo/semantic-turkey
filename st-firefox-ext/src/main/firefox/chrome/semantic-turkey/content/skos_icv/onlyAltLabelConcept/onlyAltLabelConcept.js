if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_SKOS_ICV.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

var BTN_SET_SKOS_LABEL = "Set skos:prefLabel";
var BTN_SET_SKOSXL_LABEL = "Set skosxl:prefLabel";

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function(){
	var rows = document.getElementById("gridRows");
	try {
		//for SKOS
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listConceptsWithOnlySKOSAltLabel();
		var data = xmlResp.getElementsByTagName("data")[0];
		var record = data.getElementsByTagName("record");
		for (var i=0; i<record.length; i++){
			var concept = record[i].getAttribute("concept");
			var lang = record[i].getAttribute("lang");
			var row = document.createElement("row");
			row.setAttribute("align", "center");
			var conceptLabel = document.createElement("label");
			conceptLabel.setAttribute("value", concept);
			conceptLabel.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
			var langLabel = document.createElement("label");
			langLabel.setAttribute("value", lang);
			var btnAdd = document.createElement("button");
			btnAdd.setAttribute("label", BTN_SET_SKOS_LABEL);
			btnAdd.addEventListener("command", art_semanticturkey.addLabelButtonListener, false);
			row.appendChild(conceptLabel);
			row.appendChild(langLabel);
			row.appendChild(btnAdd);
			rows.appendChild(row);
		}
		//for SKOSXL
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listConceptsWithOnlySKOSXLAltLabel();
		var data = xmlResp.getElementsByTagName("data")[0];
		var record = data.getElementsByTagName("record");
		for (var i=0; i<record.length; i++){
			var concept = record[i].getAttribute("concept");
			var lang = record[i].getAttribute("lang");
			var row = document.createElement("row");
			row.setAttribute("align", "center");
			var conceptLabel = document.createElement("label");
			conceptLabel.setAttribute("value", concept);
			conceptLabel.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
			var langLabel = document.createElement("label");
			langLabel.setAttribute("value", lang);
			var btnAdd = document.createElement("button");
			btnAdd.setAttribute("label", BTN_SET_SKOSXL_LABEL);
			btnAdd.addEventListener("command", art_semanticturkey.addLabelButtonListener, false);
			row.appendChild(conceptLabel);
			row.appendChild(langLabel);
			row.appendChild(btnAdd);
			rows.appendChild(row);
		}
	} catch (e){
		alert(e.message);
	}
}

/**
 * Listener to the "add skos:prefLabel" button. It opens a dialog to add a skos:prefLabel in the specified language
 */
art_semanticturkey.addLabelButtonListener = function() {
	var btn = this;
	var row = btn.parentNode;
	var concept = row.children[0].getAttribute("value");
	var lang = row.children[1].getAttribute("value");
	//open dialog to set a skos:prefLabel
	var parameters = new Object();
	parameters.concept = concept;
	parameters.lang = lang;
	if (btn.label == BTN_SET_SKOS_LABEL)
		parameters.labelType = "skos";
	if (btn.label == BTN_SET_SKOSXL_LABEL)
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
