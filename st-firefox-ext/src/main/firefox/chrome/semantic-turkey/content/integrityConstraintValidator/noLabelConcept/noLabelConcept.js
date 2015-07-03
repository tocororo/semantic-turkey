if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_ICV.jsm", art_semanticturkey);
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
			xmlResp = art_semanticturkey.STRequests.ICV.listConceptsWithNoSKOSPrefLabel();
		} else if (ontoType == "SKOS-XL") {
			xmlResp = art_semanticturkey.STRequests.ICV.listConceptsWithNoSKOSXLPrefLabel();
		}
		var concepts = xmlResp.getElementsByTagName("concept");
		for (var i=0; i<concepts.length; i++){
			var concept = concepts[i].textContent;
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var cell = document.createElement("listcell");
		    cell.setAttribute("label", concept);
		    cell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    var button = document.createElement("button");
		    if (ontoType == "SKOS"){
		    	button.setAttribute("label", "skos:prefLabel");
			} else if (ontoType == "SKOS-XL") {
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

/**
 * Listener to the buttons to set a label.
 */
art_semanticturkey.fixButtonClickListener = function() {
	var btn = this;
	var listitem = btn.parentNode;
	//open dialog to set a label
	var parameters = new Object();
	parameters.resource = listitem.children[0].getAttribute("label");
	parameters.editLabel = true;
	parameters.editLang = true;
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