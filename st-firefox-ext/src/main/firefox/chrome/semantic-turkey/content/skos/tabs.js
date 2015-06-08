if (typeof art_semanticturkey == "undefined") {
	var art_semanticturkey = {};
}

Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/SkosScheme.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/stEvtMgr.jsm", art_semanticturkey);

art_semanticturkey.init = function(event) {
	window.removeEventListener("load", art_semanticturkey.init, false);

	var stEventArray = new art_semanticturkey.eventListenerArrayClass()
	
	stEventArray.addEventListenerToArrayAndRegister("projectClosed", function(eventId, projectClosedObj) {
		var mainWindow = window.QueryInterface(Components.interfaces.nsIInterfaceRequestor)
	    .getInterface(Components.interfaces.nsIWebNavigation)
	    .QueryInterface(Components.interfaces.nsIDocShellTreeItem)
	    .rootTreeItem
	    .QueryInterface(Components.interfaces.nsIInterfaceRequestor)
	    .getInterface(Components.interfaces.nsIDOMWindow);

		mainWindow.toggleSidebar();
	}, null);
	
	window.addEventListener("unload", function(event){
		try {
		stEventArray.deregisterAllListener();
		} catch(e) {
			alert(e.name + ":" + e.message);
		}
	}, false);
	
	document.getElementById("skosTab").contentWindow.addEventListener("it.uniroma2.art.skos.intent.select_scheme", function(event) {
		var tabs = document.getElementById("tabs");
		tabs.selectedIndex = 2;	// select scheme tab
		
		var schemeTab = document.getElementById("schemeTab");
		var schemeTabDocument = schemeTab.contentWindow.document;
		
		if (schemeTabDocument.readyState == "complete") {
			var schemeList = schemeTabDocument.getElementById("schemeList");
			
			if(schemeList._view.rowCount == 1) {
				var choice = window.confirm("It appears that there is only one concept scheme.\nDo you want to select it?");
				
				if (choice == true) {
					art_semanticturkey.SkosScheme.setSelectedScheme(
							art_semanticturkeyCurrentProject.getProjectName(), schemeList._view.visibleRows2[0].id);
				}
			}
		}
	});
};

window.addEventListener("load", art_semanticturkey.init, false);