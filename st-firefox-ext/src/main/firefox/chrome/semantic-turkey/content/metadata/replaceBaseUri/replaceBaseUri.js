if (typeof art_semanticturkey == 'undefined') var art_semanticturkey = {};

Components.utils.import("resource://stmodules/stEvtMgr.jsm");
Components.utils.import("resource://stservices/SERVICE_Refactor.jsm", art_semanticturkey);

window.onload = function() {
	document.getElementById("RefactorBaseuri").addEventListener("click",
			art_semanticturkey.onAccept, true);

	document.getElementById("defaultBaseUri").addEventListener("click", 
			art_semanticturkey.defaultBaseUri, true);
	
	document.getElementById("cancel").addEventListener("click",
			art_semanticturkey.onClose, true);
	
	
	var responseXML = art_semanticturkey.STRequests.Metadata.getBaseuri();
	var baseuri = responseXML.getElementsByTagName('BaseURI')[0].getAttribute('uri');
	document.getElementById("defaultBaseUri").defaultBaseUri = baseuri;
	document.getElementById("defaultBaseUri").manualInput = "";
};

art_semanticturkey.defaultBaseUri = function(){
	//get the status of the checkbox
	var checked = document.getElementById("defaultBaseUri").checked;
	if(checked){ // the checkbox has been checked
		//save the manualInput
		document.getElementById("defaultBaseUri").manualInput = document.getElementById("sourceBaseuri").value;
		//set the default baseuri in the texybox
		document.getElementById("sourceBaseuri").value = document.getElementById("defaultBaseUri")
			.defaultBaseUri;
		//disable the manualSource textbox
		document.getElementById("sourceBaseuri").disabled = true;
	} else{ // the checkbox has been unchecked
		//set the previous manuelInput in the manualSource textbox
		document.getElementById("sourceBaseuri").value = document.getElementById("defaultBaseUri").manualInput;
		//enable the manualSource textbox
		document.getElementById("sourceBaseuri").disabled = false;
	}
}

art_semanticturkey.onAccept = function() {
	var parentWindow = window.arguments[0].parentWindow;
	try{
		var newBaseUri = document.getElementById("targetBaseuri").value;
		//check if the default BaseUri is used or not 
		
		var parameters = new Object();
		parameters.useDefault = document.getElementById("defaultBaseUri").checked;
		parameters.targetBaseuri = document.getElementById("targetBaseuri").value;
		parameters.sourceBaseuri = document.getElementById("sourceBaseuri").value;
		window.openDialog("chrome://semantic-turkey/content/metadata/replaceBaseUri/replaceBaseUriInProgress.xul",
					"_blank", "modal=yes,resizable,centerscreen", parameters);
		
		//get the response 
		var responseXML = parameters.responseXML;
		if(!parameters.cancelRefactor){
			art_semanticturkey.setBaseuri_RESPONSE(responseXML);
		}
		/*var checked = document.getElementById("defaultBaseUri").checked;
		if(checked){ // use the dafault BaseUri
			var responseXML = art_semanticturkey.STRequests.Refactor.replaceBaseURI(newBaseUri);
		} else { // use the manual input for the old BaseUri
			var oldBaseUri = document.getElementById("sourceBaseuri").value;
			var responseXML = art_semanticturkey.STRequests.Refactor.replaceBaseURI(newBaseUri, oldBaseUri);
		}
		//changebaseURI
		art_semanticturkey.setBaseuri_RESPONSE(responseXML, checked);*/
	}
	catch (e) {
		alert(e.name + ": " + e.message);
	}
	close();
	
};

art_semanticturkey.setBaseuri_RESPONSE = function(responseElement){
	var status = responseElement.getElementsByTagName("reply")[0].getAttribute("status");
	if(status == "ok") {
		var parentWindow = window.arguments[0].parentWindow;
		if(parentWindow != null){
			//a BaseUri has changed, close the sidebar
			parentWindow.art_semanticturkey.toggleSidebarFromImports();
		}
	}
};

art_semanticturkey.onClose = function() {
	close();
};