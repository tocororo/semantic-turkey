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
	var listbox = document.getElementById("listbox");
	try {
		var xmlResp = art_semanticturkey.STRequests.ICV.listHierarchicallyRedundantConcepts();
		var records = xmlResp.getElementsByTagName("record");
		
		for (var i=0; i<records.length; i++){
			var record = records[i];
			var broaderConcept = record.getAttribute("broader");
			var narrowerConcept = record.getAttribute("narrower");
			
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var cell = document.createElement("listcell");
		    cell.setAttribute("label", narrowerConcept);
		    cell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    cell = document.createElement("listcell");
		    cell.setAttribute("label", broaderConcept);
		    cell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    var btnEdit = document.createElement("button");
		    btnEdit.setAttribute("label", "Remove redundancy");
		    btnEdit.setAttribute("flex", "1");
		    btnEdit.addEventListener("command", art_semanticturkey.fixButtonClickListener, false);
		    listitem.appendChild(btnEdit);
		    
			listbox.appendChild(listitem)
		}
	} catch (e){
		alert(e.message);
	}
}

art_semanticturkey.fixButtonClickListener = function() {
	var btn = this;
	var listitem = btn.parentNode;
	var narrowerConcept = listitem.children[0].getAttribute("label");
	var broaderConcept = listitem.children[1].getAttribute("label");
	try {
		art_semanticturkey.STRequests.SKOS.removeBroaderConcept(narrowerConcept, broaderConcept);
		alert("Broader relation removed");
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
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