if (typeof art_semanticturkey == "undefined") {
	var art_semanticturkey = {};
}

if (typeof art_semanticturkey.resourceView == "undefined") {
	art_semanticturkey.resourceView = {};
}

if (typeof art_semanticturkey.resourceView.utils == "undefined") {
	art_semanticturkey.resourceView.utils = {};
}

art_semanticturkey.resourceView.utils.openResourceView = function(resourceName, aWindow) {
	var mainWindow = window.QueryInterface(Components.interfaces.nsIInterfaceRequestor).getInterface(
			Components.interfaces.nsIWebNavigation).QueryInterface(Components.interfaces.nsIDocShellTreeItem).rootTreeItem
			.QueryInterface(Components.interfaces.nsIInterfaceRequestor).getInterface(
					Components.interfaces.nsIDOMWindow);

	var params = {};
	params.resource = resourceName;
	
	var url = "chrome://semantic-turkey/content/resourceView/resourceView.xul" + art_semanticturkey.resourceView.utils.stringify(params);
	
	if (typeof aWindow == "undefined") {
		mainWindow.gBrowser.addTab(url);
	} else {
		aWindow.location = url; 
	}
};

art_semanticturkey.resourceView.utils.parseQueryString = function(queryString) {
	if (queryString[0] == "?") {
		queryString = queryString.substring(1);
	}
	
	var params = {};
	
	var keyValuePairList = queryString.split("&");

	for (var i = 0 ; i < keyValuePairList.length ; i++) {
		var parts = keyValuePairList[i].split("=");
		
		if (parts.length != 2) continue;
		
		var key = window.decodeURIComponent(parts[0]);
		var value = window.decodeURIComponent(parts[1]);
		
		params[key] = value;
	}

	return params;
};

art_semanticturkey.resourceView.utils.stringify = function(fields) {
	var queryString = "?";
	
	for (var key in fields) {
		queryString += window.encodeURIComponent(key) + "=" + window.encodeURIComponent(fields[key]) + "&";
	}
	
	return queryString.substring(0, queryString.length - 1);
};