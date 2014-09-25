if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_SKOS_ICV.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function() {
	try {
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listConceptSchemesWithNoLabel();
		var data = xmlResp.getElementsByTagName("data")[0];
		var schemes = data.getElementsByTagName("scheme");
		var listbox = document.getElementById("listbox");
		for (var i=0; i<schemes.length; i++){
			var scheme = schemes[i].textContent;
			
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var cell = document.createElement("listcell");
		    cell.setAttribute("label", scheme);
		    cell.addEventListener("dblclick", art_semanticturkey.schemeDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    var button = document.createElement("button");
		    button.setAttribute("label", "Set skos:prefLabel");
		    button.setAttribute("flex", "1");
		    button.addEventListener("command", art_semanticturkey.fixButtonClickListener, false);
		    listitem.appendChild(button);
		    
		    listbox.appendChild(listitem);
		}
	} catch (e){
		alert(e.message);
	}
}


art_semanticturkey.schemeDblClickListener = function() {
	var scheme = this.getAttribute("label");//this in an actionListener represents the target of the listener
	var parameters = new Object();
	parameters.sourceType = "conceptscheme";
	parameters.sourceElement = null;
	parameters.sourceElementName = scheme;
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

art_semanticturkey.fixButtonClickListener = function() {
	var btn = this;
	var listitem = btn.parentNode;
	var scheme = listitem.children[0].getAttribute("label");
	var labelType = "skos:prefLabel";
	var parameters = new Object();
	parameters.resource = scheme;
	parameters.editLabel = true;
	parameters.editLang = true;
	parameters.labelType = labelType;
	window.openDialog("chrome://semantic-turkey/content/skos_icv/setLabelDialog.xul",
			"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen",
			parameters);
}

