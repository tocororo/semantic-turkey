if (typeof art_semanticturkey == 'undefined') var art_semanticturkey = {};

Components.utils.import("resource://stmodules/stEvtMgr.jsm");
Components.utils.import("resource://stservices/SERVICE_Refactor.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

window.onload = function() {
	document.getElementById("labelToShow").value = "Execute the BaseUri refactor (it can be a long process)?";
	
	document.getElementById("RefactorBaseuri").addEventListener("command", 
			art_semanticturkey.doRefactor, true);
	
};

art_semanticturkey.doRefactor = function(){
	document.getElementById("labelToShow").value = "BaseUri in progress, please wait ...";
	
	document.getElementById("RefactorBaseuri").disabled = true;
	
	var responseXML;
	//execute the request to the server to do the baseUri refactor
	var newBaseUri = window.arguments[0].targetBaseuri;
	//check if the default BaseUri is used or not 
	var useDefault = window.arguments[0].useDefault;
	if(useDefault){ // use the dafault BaseUri
		responseXML = art_semanticturkey.STRequests.Refactor.replaceBaseURI(newBaseUri);
	} else { // use the manual input for the old BaseUri
		var oldBaseUri = window.arguments[0].sourceBaseuri;
		responseXML = art_semanticturkey.STRequests.Refactor.replaceBaseURI(newBaseUri, oldBaseUri);
	}
	window.arguments[0].responseXML = responseXML;
	
	
	//the baseUri refactor is complete
	document.getElementById("RefactorBaseuri").removeEventListener("command", 
			art_semanticturkey.doRefactor, true);
	document.getElementById("RefactorBaseuri").addEventListener("command", 
			art_semanticturkey.onClose, true);
	document.getElementById("labelToShow").value = "BaseUri Refactor Complete";
	document.getElementById("RefactorBaseuri").disabled = false;
}


art_semanticturkey.onClose = function() {
	close();
};