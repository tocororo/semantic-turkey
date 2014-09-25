if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_SKOS_ICV.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function(){
	var listbox = document.getElementById("listbox");
	try {
		//SKOS
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listConceptsWithMultipleSKOSPrefLabel();
		var data = xmlResp.getElementsByTagName("data")[0];
		var records = data.getElementsByTagName("record");
		for (var i=0; i<records.length; i++){
			var concept = records[i].getAttribute("concept");
			var lang = records[i].getAttribute("lang");
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var cell = document.createElement("listcell");
		    cell.setAttribute("label", concept);
		    cell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    cell.setAttribute("label", "skos:prefLabel");
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    cell.setAttribute("label", lang);
		    listitem.appendChild(cell);
		    
		    var button = document.createElement("button");
		    button.setAttribute("label", "Edit concept");
		    button.setAttribute("flex", "1");
		    button.addEventListener("command", art_semanticturkey.fixButtonClickListener, false);
		    listitem.appendChild(button);
		    
		    listbox.appendChild(listitem);
		}
		//SKOSXL
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listConceptsWithMultipleSKOSXLPrefLabel();
		var data = xmlResp.getElementsByTagName("data")[0];
		var records = data.getElementsByTagName("record");
		for (var i=0; i<records.length; i++){
			var concept = records[i].getAttribute("concept");
			var lang = records[i].getAttribute("lang");
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var cell = document.createElement("listcell");
		    cell.setAttribute("label", concept);
		    cell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    cell.setAttribute("label", "skosxl:prefLabel");
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    cell.setAttribute("label", lang);
		    listitem.appendChild(cell);
		    
		    var button = document.createElement("button");
		    button.setAttribute("label", "Edit concept");
		    button.setAttribute("flex", "1");
		    button.addEventListener("command", art_semanticturkey.fixButtonClickListener, false);
		    listitem.appendChild(button);
		    
		    listbox.appendChild(listitem);
		}
	} catch (e){
		alert(e.message);
	}
}

art_semanticturkey.fixButtonClickListener = function() {
	var listitem = this.parentNode;
	var concept = listitem.children[0].getAttribute("label");
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

/**
 * Listener to the concept, when double clicked it opens the editor panel
 */
art_semanticturkey.conceptDblClickListener = function() {
	var concept = this.getAttribute("label");//this in an actionListener represents the target of the listener
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
