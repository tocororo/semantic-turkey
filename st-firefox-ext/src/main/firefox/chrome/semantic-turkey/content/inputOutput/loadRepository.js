if (typeof art_semanticturkey == 'undefined') var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_InputOutput.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

window.onload = function(){
	document.getElementById("dirBtn").addEventListener("click", art_semanticturkey.chooseFile, true);
	document.getElementById("loadRepository").addEventListener("click", art_semanticturkey.onAccept, true);
	document.getElementById("cancel").addEventListener("click", art_semanticturkey.cancel, true);
};


art_semanticturkey.onAccept = function() {
	document.getElementById("loadRepository").setAttribute("disabled", "true");
	try{
		var file = new File(document.getElementById("srcLocalFile").value);
		var responseXML = art_semanticturkey.STRequests.InputOutput.loadRDF(
				file, document.getElementById("baseURI").value);
		art_semanticturkey.loadRepository_RESPONSE(responseXML);
	}
	catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
};

art_semanticturkey.loadRepository_RESPONSE = function(responseElement){
	if(responseElement.getElementsByTagName("reply")[0].getAttribute("status") == "ok"){
		var msg = responseElement.getElementsByTagName('msg')[0];
		alert(msg.getAttribute("content"));
		close();
	}
};

art_semanticturkey.cancel = function() {
	close();
};