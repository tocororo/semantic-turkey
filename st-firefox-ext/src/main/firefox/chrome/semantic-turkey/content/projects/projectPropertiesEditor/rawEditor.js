if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

var contentChanged = false;

window.onload = function() {
	var contentTxtbox = document.getElementById("contentTxt");
	contentTxtbox.addEventListener("keypress", art_semanticturkey.contentChangeListener(), false);
	//load current project.info content
	var projectName = window.arguments[0].projectName;
	var xmlReply = art_semanticturkey.STRequests.Projects.getProjectPropertyFileContent(projectName);
	var data = xmlReply.getElementsByTagName("data")[0];
	var content = data.getElementsByTagName("content")[0].childNodes[0].nodeValue;
	contentTxtbox.setAttribute("value", content);
}

function buttonOkListener() {
	var content = document.getElementById("contentTxt").value;
	art_semanticturkey.Logger.debug("saving content " + content);
	//save the file only if has been modified
	if (contentChanged){
		let prompts = Components.classes["@mozilla.org/embedcomp/prompt-service;1"].getService(Components.interfaces.nsIPromptService);
		if (prompts.confirm(window, "Warning", "Changing project property file may corrupt the project. Do you want to proceed?")) {
			try {
//				var content = document.getElementById("contentTxt").getAttribute("value");
				var content = document.getElementById("contentTxt").value;
				var projectName = window.arguments[0].projectName;
				art_semanticturkey.Logger.debug("saving on project '"+projectName+"' the content:\n\n"+content+"\n\n");
				art_semanticturkey.STRequests.Projects.saveProjectPropertyFileContent(projectName, content);
				window.arguments[0].changeApplied = true;
			} catch (e) {
				art_semanticturkey.Alert.alert(e);
			}
		}

	}
}


art_semanticturkey.contentChangeListener = function() {
	contentChanged = true;
}