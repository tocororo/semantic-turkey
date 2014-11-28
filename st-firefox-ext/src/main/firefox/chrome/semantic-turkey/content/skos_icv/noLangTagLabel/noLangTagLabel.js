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
		//SKOS
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listConceptsWithNoLanguageTagSKOSLabel();
		var data = xmlResp.getElementsByTagName("data")[0];
		var records = data.getElementsByTagName("record");
		for (var i=0; i<records.length; i++){
			var concept = records[i].getAttribute("concept");
			var label = records[i].getAttribute("label");
			var labelPred = records[i].getAttribute("labelPred");
			
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var cell = document.createElement("listcell");
		    cell.setAttribute("label", concept);
		    cell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    if (labelPred == "http://www.w3.org/2004/02/skos/core#prefLabel") 
		    	cell.setAttribute("label", "skos:prefLabel");
			else if (labelPred == "http://www.w3.org/2004/02/skos/core#altLabel")
				cell.setAttribute("label", "skos:altLabel");
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    cell.setAttribute("label", label);
		    listitem.appendChild(cell);
		    
		    var button = document.createElement("button");
		    button.setAttribute("label", "Set language tag");
		    button.setAttribute("flex", "1");
		    button.addEventListener("command", art_semanticturkey.fixButtonClickListener, false);
		    listitem.appendChild(button);
		    
		    listbox.appendChild(listitem);
		}
		//SKOSXL
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listConceptsWithNoLanguageTagSKOSXLLabel();
		var data = xmlResp.getElementsByTagName("data")[0];
		var records = data.getElementsByTagName("record");
		for (var i=0; i<records.length; i++){
			var concept = records[i].getAttribute("concept");
			var label = records[i].getAttribute("label");
			var labelPred = records[i].getAttribute("labelPred");
			
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var cell = document.createElement("listcell");
		    cell.setAttribute("label", concept);
		    cell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    if (labelPred == "http://www.w3.org/2008/05/skos-xl#prefLabel") 
		    	cell.setAttribute("label", "skosxl:prefLabel");
			else if (labelPred == "http://www.w3.org/2008/05/skos-xl#altLabel")
				cell.setAttribute("label", "skosxl:altLabel");
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    cell.setAttribute("label", label);
		    listitem.appendChild(cell);
		    
		    var button = document.createElement("button");
		    button.setAttribute("label", "Set language tag");
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
	var concept = listitem.children[0].getAttribute("label");
	var labelType = listitem.children[1].getAttribute("label");
	var label = listitem.children[2].getAttribute("label");
	//open dialog to set a language tag
	var parameters = new Object();
	parameters.resource = concept;
	parameters.label = label;
	parameters.labelType = labelType;
	parameters.lang = "";
	parameters.editLabel = false;
	parameters.editLang = true;
	parameters.replaceLabel = true;
	window.openDialog("chrome://semantic-turkey/content/skos_icv/setLabelDialog.xul",
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
	art_semanticturkey.ResourceViewLauncher.openResourceView(parameters);
}