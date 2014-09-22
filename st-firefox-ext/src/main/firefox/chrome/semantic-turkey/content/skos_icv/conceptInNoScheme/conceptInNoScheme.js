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
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listConceptsWithNoScheme();
		var data = xmlResp.getElementsByTagName("data")[0];
		var concepts = data.getElementsByTagName("concept");
		for (var i=0; i<concepts.length; i++){
			var row = document.createElement("row");
			row.setAttribute("align", "center");
			var concept = concepts[i].textContent;
			var conceptLabel = document.createElement("label");
			conceptLabel.setAttribute("value", concept);
			conceptLabel.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
			var fixButton = document.createElement("button");
			fixButton.setAttribute("label", "Add concept to a skos:ConceptScheme");
			fixButton.addEventListener("command", art_semanticturkey.fixButtonClickListener, false);
			row.appendChild(conceptLabel);
			row.appendChild(fixButton);
			rows.appendChild(row);
		}
	} catch (e){
		alert(e.message);
	}
}


art_semanticturkey.fixButtonClickListener = function() {
	var btn = this;
	var row = btn.parentNode;
	var concept = row.children[0].value;
	var parameters = new Object();
	parameters.concept = concept;
	window.openDialog("chrome://semantic-turkey/content/skos_icv/conceptInNoScheme/selectSchemeDialog.xul",
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