if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

window.onload = function() {
	var oldValue = window.arguments[0].value;
	var txt = document.getElementById("txtValue");
	txt.setAttribute("value", oldValue);
}

function buttonOkListener() {
	let prompts = Components.classes["@mozilla.org/embedcomp/prompt-service;1"].getService(Components.interfaces.nsIPromptService);
	if (prompts.confirm(window, "Warning", "Changing project property value may corrupt the project. Do you want to proceed?")) {
		var name = window.arguments[0].name;
		var value = document.getElementById("txtValue").value;
		try {
			var projectName = window.arguments[0].projectName;
			art_semanticturkey.STRequests.Projects.setProjectProperty(projectName, name, value);
			window.arguments[0].value = value;
		} catch (e) {
			art_semanticturkey.Alert.alert(e);
		}
	}
}


