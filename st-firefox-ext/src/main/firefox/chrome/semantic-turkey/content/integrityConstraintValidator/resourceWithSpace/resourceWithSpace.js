if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_ICV.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Refactor.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ResourceViewLauncher.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Sanitizer.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

var limit = 10; //# max of record that the check should returns (get from preferences?)

window.onload = function() {
	document.getElementById("sanitizeAllBtn").addEventListener("command", art_semanticturkey.sanitizeAllListener, false);
	
	art_semanticturkey.initUI();
}

art_semanticturkey.initUI = function(){
	var listbox = document.getElementById("listbox");
	try {
		var xmlResp = art_semanticturkey.STRequests.ICV.listResourcesURIWithSpace(limit);
		var resources = xmlResp.getElementsByTagName("resource");
		var nResult = resources.length;
		var resultCountLabelMsg = "Result: " + nResult;
		var count = xmlResp.getElementsByTagName("collection")[0].getAttribute("count");
		if (count > nResult)
			var resultCountLabelMsg = resultCountLabelMsg + " of " + count;
		document.getElementById("resultCountLabel").setAttribute("value", resultCountLabelMsg);
		
		for (var i=0; i<nResult; i++){
			var resource = resources[i].textContent;
			
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var cell = document.createElement("listcell");
		    cell.setAttribute("label", resource);
		    cell.addEventListener("dblclick", art_semanticturkey.resourceDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    var actionBox = document.createElement("hbox");
		    var sanitizeBtn = document.createElement("button");
		    sanitizeBtn.setAttribute("label", "Sanitize");
		    sanitizeBtn.setAttribute("flex", "1");
		    sanitizeBtn.addEventListener("command", art_semanticturkey.sanitizeButtonClickListener, false);
		    actionBox.appendChild(sanitizeBtn);
		    var renameBtn = document.createElement("button");
		    renameBtn.setAttribute("flex", "1");
		    renameBtn.setAttribute("label", "Rename");
		    renameBtn.addEventListener("command", art_semanticturkey.renameButtonClickListener, false);
		    actionBox.appendChild(renameBtn);
		    
		    listitem.appendChild(actionBox);
			listbox.appendChild(listitem)
		}
	} catch (e){
		alert(e.message);
	}
}

art_semanticturkey.sanitizeButtonClickListener = function() {
	var btn = this;
	var listitem = btn.parentNode.parentNode;//button->hbox->listitem
	var resource = listitem.children[0].getAttribute("label");
	var sanitizedRes = sanitize(resource);
	art_semanticturkey.Logger.debug("sanitized " + resource + " and renaming with " + sanitizedRes);
	try {
		art_semanticturkey.STRequests.Refactor.rename(resource, sanitizedRes);
		alert("Resource sanitized!");
		window.location.reload();
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
}

art_semanticturkey.renameButtonClickListener = function() {
	var btn = this;
	var listitem = btn.parentNode.parentNode;//button->hbox->listitem
	var resource = listitem.children[0].getAttribute("label");
	try {
		var parameters = {};
		parameters.currentName = resource;
		window.openDialog("chrome://semantic-turkey/content/resourceView/renameDialog/renameDialog.xul", 
				"dlg", "chrome=yes,dialog,resizable=yes,modal,centerscreen", parameters);
		window.location.reload();
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
}

art_semanticturkey.sanitizeAllListener = function() {
	var listbox = document.getElementById("listbox");
	var listitems = listbox.getElementsByTagName("listitem");
	var failedSanitization = [];//array containing the resource which the sanitization is failed 
	for (var i=0; i<listitems.length; i++){
		var resource = listitems[i].children[0].getAttribute("label");
		var sanitizedRes = sanitize(resource);
		try {
			art_semanticturkey.STRequests.Refactor.rename(resource, sanitizedRes);
		} catch (e) {
			var pair = {oldName : resource, newName : sanitizedRes};
			failedSanitization.push(pair);
		}
	}
	//check and eventually warn the user if some sanitization failed
	if (failedSanitization.length > 0){
		var details = "";
		for (var i=0; i<failedSanitization.length; i++){
			details += "'" + failedSanitization[i].oldName + "' (trying to rename in '" + failedSanitization[i].newName + "')\n"; 
		}
		art_semanticturkey.Alert.alert("Some sanitization has been failed. Resources with the same name already exists." +
				" Please, rename manually.", details);
		window.location.reload();
	} else {
		alert("Sanitization done!");
		window.location.reload();
	}
}

/**
 * Listener to the resource, when double clicked it opens the editor panel
 */
art_semanticturkey.resourceDblClickListener = function() {
	var resource = this.getAttribute("label");//this in an actionListener represents the target of the listener
	var parameters = new Object();
	parameters.sourceType = "concept";
	parameters.sourceElement = resource;
	parameters.sourceElementName = resource;
	parameters.parentWindow = window;
	parameters.isFirstEditor = true;
	art_semanticturkey.ResourceViewLauncher.openResourceView(parameters);
}

sanitize = function(str) {
	return art_semanticturkey.Sanitizer.sanitize(str.trim());
}