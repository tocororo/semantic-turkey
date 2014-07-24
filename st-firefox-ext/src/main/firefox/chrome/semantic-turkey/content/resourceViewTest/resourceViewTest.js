if (typeof art_semanticturkey == "undefined") {
	var art_semanticturkey = {};
}

Components.utils.import("resource://stservices/SERVICE_ResourceView.jsm", art_semanticturkey);

if (typeof art_semanticturkey.resourceView == "undefined") {
	art_semanticturkey.resourceView = {};
}

art_semanticturkey.resourceView.init = function() {
	document.getElementById("resourceViewButton").addEventListener("command",
			art_semanticturkey.resourceView.resourceViewButtonCommand, false);

	document.getElementById("rawRequestButton").addEventListener("command",
			art_semanticturkey.resourceView.rawRequestButtonCommand, false);

};

art_semanticturkey.resourceView.rawRequestButtonCommand = function(event) {
	var resourceName = document.getElementById("resourceNameBox").value;

	resourceName = resourceName.trim();

	if (resourceName.length == 0) {
		alert("No resource name has been provided");
		return;
	}

	var mainWindow = window.QueryInterface(Components.interfaces.nsIInterfaceRequestor).getInterface(
			Components.interfaces.nsIWebNavigation).QueryInterface(Components.interfaces.nsIDocShellTreeItem).rootTreeItem
			.QueryInterface(Components.interfaces.nsIInterfaceRequestor).getInterface(
					Components.interfaces.nsIDOMWindow);

	var ctx = art_semanticturkey.STRequests.ResourceView.context;
	
	mainWindow.gBrowser
			.addTab("http://localhost:1979/semanticturkey/it.uniroma2.art.semanticturkey/st-core-services/ResourceView/getResourceView?resource="
					+ window.encodeURIComponent(resourceName) + "&" + ctx.getContextValuesAsString());

};

art_semanticturkey.resourceView.resourceViewButtonCommand = function(event) {
	var resourceName = document.getElementById("resourceNameBox").value;

	resourceName = resourceName.trim();

	if (resourceName.length == 0) {
		alert("No resource name has been provided");
		return;
	}

	art_semanticturkey.resourceView.utils.openResourceView(resourceName);
};

window.addEventListener("load", art_semanticturkey.resourceView.init, true);
