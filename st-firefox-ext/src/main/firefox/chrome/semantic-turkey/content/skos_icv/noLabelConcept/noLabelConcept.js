if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_SKOS_ICV.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ResourceViewLauncher.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function(){
	var listbox = document.getElementById("listbox");
	try {
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listConceptsWithNoLabel();
		var data = xmlResp.getElementsByTagName("data")[0];
		var concepts = data.getElementsByTagName("concept");
		for (var i=0; i<concepts.length; i++){
			var concept = concepts[i].textContent;
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var cell = document.createElement("listcell");
		    cell.setAttribute("label", concept);
		    cell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    var button = document.createElement("button");
		    button.setAttribute("label", "skos:prefLabel");
		    button.setAttribute("flex", "1");
		    button.addEventListener("command", art_semanticturkey.fixButtonClickListener, false);
		    listitem.appendChild(button);
		    
		    button = document.createElement("button");
		    button.setAttribute("label", "skosxl:prefLabel");
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
	btnValue = btn.label;
	var listitem = btn.parentNode;
	var concept = listitem.children[0].getAttribute("label");
	//open dialog to set a label
	var parameters = new Object();
	parameters.resource = concept;
	parameters.editLabel = true;
	parameters.editLang = true;
	parameters.labelType = btnValue;
	window.openDialog("chrome://semantic-turkey/content/skos_icv/setLabelDialog.xul",
			"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen",
			parameters);
	//refresh listbox (commented: there's non way to know if user fix the problem from the editor panel (dbl clicking on the concept))
//	if (parameters.returnedValue != null){
//		var listbox = document.getElementById("listbox");
//		listbox.removeItemAt(listbox.getIndexOfItem(listitem));
//	}
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
}