if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_SKOS_ICV.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function(){
	var rows = document.getElementById("gridRows");
	try {
		//for SKOS
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listConceptsWithNoLanguageTagSKOSLabel();
		var data = xmlResp.getElementsByTagName("data")[0];
		var record = data.getElementsByTagName("record");
		//init UI
		for (var i=0; i<record.length; i++){
			var concept = record[i].getAttribute("concept");
			var label = record[i].getAttribute("label");
			var labelPred = record[i].getAttribute("labelPred");
			var row = document.createElement("row");
			row.setAttribute("align", "center");
			var conceptLabel = document.createElement("label");
			conceptLabel.setAttribute("value", concept);
			conceptLabel.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
			var labelPredLabel = document.createElement("label");
			if (labelPred == "http://www.w3.org/2004/02/skos/core#prefLabel") 
				labelPredLabel.setAttribute("value", "skos:prefLabel");
			else if (labelPred == "http://www.w3.org/2004/02/skos/core#altLabel")
				labelPredLabel.setAttribute("value", "skos:altLabel");
			var labelLabel = document.createElement("label");
			labelLabel.setAttribute("value", label);
			var btnSetLanguageTag = document.createElement("button");
			btnSetLanguageTag.setAttribute("label", "Set language tag");
			btnSetLanguageTag.addEventListener("command", art_semanticturkey.setLanguageTagButtonListener, false);
			row.appendChild(conceptLabel);
			row.appendChild(labelPredLabel);
			row.appendChild(labelLabel);
			row.appendChild(btnSetLanguageTag);
			rows.appendChild(row);
		}
		//for SKOSXL
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listConceptsWithNoLanguageTagSKOSXLLabel();
		var data = xmlResp.getElementsByTagName("data")[0];
		var record = data.getElementsByTagName("record");
		//init UI
		for (var i=0; i<record.length; i++){
			var concept = record[i].getAttribute("concept");
			var label = record[i].getAttribute("label");
			var labelPred = record[i].getAttribute("labelPred");
			var row = document.createElement("row");
			row.setAttribute("align", "center");
			var conceptLabel = document.createElement("label");
			conceptLabel.setAttribute("value", concept);
			conceptLabel.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
			var labelPredLabel = document.createElement("label");
			if (labelPred == "http://www.w3.org/2008/05/skos-xl#prefLabel") 
				labelPredLabel.setAttribute("value", "skosxl:prefLabel");
			else if (labelPred == "http://www.w3.org/2008/05/skos-xl#altLabel")
				labelPredLabel.setAttribute("value", "skosxl:altLabel");
			var labelLabel = document.createElement("label");
			labelLabel.setAttribute("value", label);
			var btnSetLanguageTag = document.createElement("button");
			btnSetLanguageTag.setAttribute("label", "Set language tag");
			btnSetLanguageTag.addEventListener("command", art_semanticturkey.setLanguageTagButtonListener, false);
			row.appendChild(conceptLabel);
			row.appendChild(labelPredLabel);
			row.appendChild(labelLabel);
			row.appendChild(btnSetLanguageTag);
			rows.appendChild(row);
		}
	} catch (e){
		alert(e.message);
	}
}

/**
 * Listener to the buttons to set a label.
 */
art_semanticturkey.setLanguageTagButtonListener = function() {
	var btn = this;
	var row = btn.parentNode;
	var concept = row.children[0].getAttribute("value");
	var labelPred = row.children[1].getAttribute("value");
	var label = row.children[2].getAttribute("value");
	//open dialog to set a language tag
	var parameters = new Object();
	parameters.concept = concept;
	parameters.label = label;
	parameters.labelPred = labelPred;
//	parameters.returnedValue = null;
	window.openDialog("chrome://semantic-turkey/content/skos_icv/noLangTagLabel/setLangTagDialog.xul",
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