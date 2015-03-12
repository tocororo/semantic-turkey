if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_ICV.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ResourceViewLauncher.jsm", art_semanticturkey);

var limit = 20; //# max of record that the check should returns (get from preferences?)

window.onload = function() {
	document.getElementById("quickFixBtn").addEventListener("command", art_semanticturkey.quickFixListener, false);
	art_semanticturkey.initUI();
}

art_semanticturkey.initUI = function(){
	var listbox = document.getElementById("listbox");
	try {
		var xmlResp = art_semanticturkey.STRequests.ICV.listConceptsWithNoScheme(limit);
		var data = xmlResp.getElementsByTagName("data")[0];
		var concepts = data.getElementsByTagName("concept");
		var nResult = concepts.length;
		var resultCountLabelMsg = "Result: " + nResult;
		var count = xmlResp.getElementsByTagName("collection")[0].getAttribute("count");
		if (count > nResult)
			var resultCountLabelMsg = resultCountLabelMsg + " of " + count;
		document.getElementById("resultCountLabel").setAttribute("value", resultCountLabelMsg);
		for (var i=0; i<nResult; i++){
			var concept = concepts[i].textContent;

			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var cell = document.createElement("listcell");
		    cell.setAttribute("label", concept);
		    cell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    var button = document.createElement("button");
		    button.setAttribute("label", "Add concept to a skos:ConceptScheme");
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
	var parameters = new Object();
	parameters.concepts = [concept];
	window.openDialog("chrome://semantic-turkey/content/integrityConstraintValidator/conceptInNoScheme/selectSchemeDialog.xul",
			"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen",
			parameters);
	window.location.reload();
}

art_semanticturkey.quickFixListener = function() {
	var listbox = document.getElementById("listbox");
	var concArray = []
	for (var i=0; i<listbox.itemCount; i++){
		var concept = listbox.getItemAtIndex(i).children[0].getAttribute("label");
		concArray.push(concept);
	}
	var parameters = new Object();
	parameters.concepts = concArray;
	window.openDialog("chrome://semantic-turkey/content/integrityConstraintValidator/conceptInNoScheme/selectSchemeDialog.xul",
			"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
	window.location.reload();
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
	window.location.reload();
}