if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ResourceViewLauncher.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Preferences.jsm", art_semanticturkey);

window.onload = function() {
	document.getElementById("resourceViewButton").addEventListener("command", art_semanticturkey.launchResourceView, true);
}

art_semanticturkey.launchResourceView = function(){
	
	var resourceName = document.getElementById("resourceNameBox").value;
	resourceName = resourceName.trim();
	
	if (resourceName.length == 0) {
		alert("No resource name has been provided");
		return;
	}
	
	//update the preference set from the menulist
	var useEditorPref = document.getElementById("useEditorMenulist").selectedItem.value;
	art_semanticturkey.Preferences.set("extensions.semturkey.useEditor", useEditorPref);
	
	
	var params = new Object();
	if (useEditorPref == "legacy"){
		//this parameters should be provided already in the .js where the old editor panel is used
		params.isFirstEditor = true;
		params.parentWindow = window;
		params.sourceType = "cls";//just for demo
		params.sourceElement = resourceName;
		params.sourceElementName = resourceName;
		params.deleteForbidden = false;
		art_semanticturkey.ResourceViewLauncher.openResourceView(params);
	} else {
		params.resource = resourceName;
		art_semanticturkey.ResourceViewLauncher.openResourceView(params);
	}
}