if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_ICV.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ResourceViewLauncher.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function() {
	try {
		//for SKOS
		var xmlResp = art_semanticturkey.STRequests.ICV.listConceptsWithSameSKOSPrefLabel();
		var data = xmlResp.getElementsByTagName("data")[0];
		var record = data.getElementsByTagName("record");
		var listbox = document.getElementById("listbox");
		for (var i=0; i<record.length; i++){
			var concept1 = record[i].getAttribute("concept1");
			var concept2 = record[i].getAttribute("concept2");
			var label = record[i].getAttribute("label");
			var lang = record[i].getAttribute("lang");
			
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var cell = document.createElement("listcell");
		    cell.setAttribute("label", concept1);
		    cell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    cell.setAttribute("label", concept2);
		    cell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    cell.setAttribute("label", label);
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    cell.setAttribute("label", lang);
		    listitem.appendChild(cell);
		    
		    var button = document.createElement("button");
		    button.setAttribute("label", "skos:prefLabel");
		    button.setAttribute("flex", "1");
		    button.setAttribute("concept", concept1);
		    button.addEventListener("command", art_semanticturkey.fixButtonClickListener, false);
		    listitem.appendChild(button);
		    
		    button = document.createElement("button");
		    button.setAttribute("label", "skos:prefLabel");
		    button.setAttribute("flex", "1");
		    button.setAttribute("concept", concept2);
		    button.addEventListener("command", art_semanticturkey.fixButtonClickListener, false);
		    listitem.appendChild(button);
		    
		    listbox.appendChild(listitem);
		}
		//for SKOSXL
		xmlResp = art_semanticturkey.STRequests.ICV.listConceptsWithSameSKOSXLPrefLabel();
		data = xmlResp.getElementsByTagName("data")[0];
		record = data.getElementsByTagName("record");
		for (var i=0; i<record.length; i++){
			var concept1 = record[i].getAttribute("concept1");
			var concept2 = record[i].getAttribute("concept2");
			var label = record[i].getAttribute("label");
			var lang = record[i].getAttribute("lang");
			
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var cell = document.createElement("listcell");
		    cell.setAttribute("label", concept1);
		    cell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    cell.setAttribute("label", concept2);
		    cell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    cell.setAttribute("label", label);
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    cell.setAttribute("label", lang);
		    listitem.appendChild(cell);
		    
		    var button = document.createElement("button");
		    button.setAttribute("label", "skosxl:prefLabel");
		    button.setAttribute("flex", "1");
		    button.setAttribute("concept", concept1);
		    button.addEventListener("command", art_semanticturkey.fixButtonClickListener, false);
		    listitem.appendChild(button);
		    
		    button = document.createElement("button");
		    button.setAttribute("label", "skosxl:prefLabel");
		    button.setAttribute("flex", "1");
		    button.setAttribute("concept", concept2);
		    button.addEventListener("command", art_semanticturkey.fixButtonClickListener, false);
		    listitem.appendChild(button);
		    
		    listbox.appendChild(listitem);
		}
	} catch (e){
		alert(e.message);
	}
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

art_semanticturkey.fixButtonClickListener = function() {
	var btn = this;
	var concept = btn.getAttribute("concept");
	var listitem = btn.parentNode;
	var label = listitem.children[2].getAttribute("label");
	var lang = listitem.children[3].getAttribute("label");
	var btnLabel = btn.getAttribute("label");
	var parameters = new Object();
	parameters.resource = concept;
	parameters.lang = lang;
	parameters.labelType = btnLabel;
	parameters.label = label;
	parameters.editLabel = true;
	parameters.editLang = true;	
	parameters.replaceLabel = true;
	window.openDialog("chrome://semantic-turkey/content/integrityConstraintValidator/setLabelDialog.xul",
			"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen",
			parameters);
}

