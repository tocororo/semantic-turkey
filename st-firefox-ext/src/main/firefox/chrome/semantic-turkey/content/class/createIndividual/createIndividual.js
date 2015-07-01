if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Sanitizer.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Cls.jsm", art_semanticturkey);

window.onload = function() {
	document.getElementById("createIndividual").addEventListener("click",
			art_semanticturkey.onAccept, true);
	document.getElementById("IndividualName").addEventListener("command",
			art_semanticturkey.onAccept, true);
	art_semanticturkey.Sanitizer.makeAutosanitizing(document.getElementById("IndividualName"));
	document.getElementById("cancel").addEventListener("click",
			art_semanticturkey.onClose, true);
			
	document.getElementById("IndividualName").focus();
};

art_semanticturkey.onAccept = function() {
	var instanceName = document.getElementById("IndividualName").value;
	var clsName = window.arguments[0].name;
	try {
		var responseArray = art_semanticturkey.STRequests.Cls.addIndividual(
			clsName, instanceName);
		window.arguments[0].created = true;
		close();
	}
	catch (e) {
		alert(e.name + ": " + e.message);
	}
};

art_semanticturkey.onClose = function() {
	close();
};
