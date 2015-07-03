if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_ICV.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ResourceViewLauncher.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function(){
	var ontoType = window.arguments[0].ontoType;
	var listbox = document.getElementById("listbox");
	//empty listbox
	while (listbox.itemCount > 0){
		listbox.removeItemAt(0);
	}
	
	try {
		var xmlResp;
		if (ontoType == "SKOS"){
			xmlResp = art_semanticturkey.STRequests.ICV.listConceptsWithOnlySKOSAltLabel();
		} else if (ontoType == "SKOS-XL") {
			xmlResp = art_semanticturkey.STRequests.ICV.listConceptsWithOnlySKOSXLAltLabel();
		}
		
		var records = xmlResp.getElementsByTagName("record");
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
		    cell.setAttribute("label", lang);
		    listitem.appendChild(cell);
		    
		    var button = document.createElement("button");
		    if (ontoType == "SKOS"){
		    	button.setAttribute("label", "skos:prefLabel");
		    } else if (ontoType = "SKOS-XL") {
		    	button.setAttribute("label", "skosxl:prefLabel");
		    }
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
	var btn = this;
	var listitem = btn.parentNode;
	var concept = listitem.children[0].getAttribute("label");
	var lang = listitem.children[1].getAttribute("label");
	//open dialog to set a skos:prefLabel
	var parameters = new Object();
	parameters.resource = concept;
	parameters.lang = lang;
	parameters.editLabel = true;
	parameters.editLang = false;
	parameters.labelType = btn.label;
	window.openDialog("chrome://semantic-turkey/content/integrityConstraintValidator/setLabelDialog.xul",
			"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen",
			parameters);
	art_semanticturkey.init();
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
	art_semanticturkey.ResourceViewLauncher.openResourceView(parameters);
	art_semanticturkey.init();
}
