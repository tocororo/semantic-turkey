if (typeof art_semanticturkey == "undefined") {
	var art_semanticturkey = {};
}

Components.utils.import("resource://stservices/SERVICE_Refactor.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Sanitizer.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

art_semanticturkey.renameDialog = {};
art_semanticturkey.renameDialog.loadHandler = function(event) {
	try {
		var currentName = window.arguments[0].currentName;

		if (typeof currentName == "undefined") {
			throw Error("Missing argument \"currentName\"");
		}

		var nameBoxElement = document.getElementById("nameBox");
		nameBoxElement.addEventListener("keypress", art_semanticturkey.renameDialog.possibleChangerHandler, false);
		nameBoxElement.addEventListener("paste", art_semanticturkey.renameDialog.possibleChangerHandler, false);
		art_semanticturkey.Sanitizer.makeAutosanitizing(nameBoxElement);
		
		window.addEventListener("dialogaccept", art_semanticturkey.renameDialog.acceptHander, false);
		
		
		nameBoxElement.value = currentName;
		nameBoxElement.defaultValue = currentName;

	} catch(e) {
		close();
		throw e;
	}
};

art_semanticturkey.renameDialog.acceptHander = function(event) {
	try {
		var nameBoxElement = document.getElementById("nameBox");
		var currentName = nameBoxElement.defaultValue;
		var newName = nameBoxElement.value;
		
		var response = art_semanticturkey.STRequests.Refactor.rename(currentName, newName);
	} catch (e) {
		event.preventDefault();
		art_semanticturkey.Alert.alert(e);
	}
};

art_semanticturkey.renameDialog.possibleChangerHandler = function(event) {
	window.setTimeout(art_semanticturkey.renameDialog.checkTextChanged,0);
};

art_semanticturkey.renameDialog.checkTextChanged = function() {
	var nameBoxElement = document.getElementById("nameBox");
	var changedWrtOriginal = nameBoxElement.value != nameBoxElement.defaultValue;
	document.documentElement.setAttribute("buttondisabledaccept", "" + !changedWrtOriginal);
};

window.addEventListener("load", art_semanticturkey.renameDialog.loadHandler, false);