if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Preferences.jsm", art_semanticturkey);

EXPORTED_SYMBOLS = ["ResourceViewLauncher"];

ResourceViewLauncher = {}

ResourceViewLauncher.prototype = {
	
	/**
	 * params for the old editor panel should contain the same params object that is passed to the openDialog
	 * method (as done so far), while for the new ResourceView params should contains only a parameter named
	 * resource that represents the resource URI.
	 */
	openResourceView: function(params){
		var wm = Components.classes["@mozilla.org/appshell/window-mediator;1"]
			.getService(Components.interfaces.nsIWindowMediator);
		var mainWindow = wm.getMostRecentWindow("navigator:browser");
		//three options: legacy (open old editor panel), tab (new res view in a tab), window (new res view in a window).
		//legacy is the default option
		var useEditor = art_semanticturkey.Preferences.get("extensions.semturkey.useEditor", "legacy").toLowerCase();
		if (useEditor == "tab" || useEditor == "window") {
			var queryString = "?";
			for (var key in params) {
				queryString += mainWindow.encodeURIComponent(key) + "=" + mainWindow.encodeURIComponent(params[key]) + "&";
			}
			queryString = queryString.substring(0, queryString.length - 1);
			if (useEditor == "tab"){//open as tab
				mainWindow.gBrowser.addTab("chrome://semantic-turkey/content/resourceView/resourceView.xul" + 
						queryString);
			} else { //open as window
				mainWindow.openDialog(
						"chrome://semantic-turkey/content/resourceView/resourceView.xul" + queryString,
						"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen");
			}
		} else { //legacy
			mainWindow.openDialog(
					"chrome://semantic-turkey/content/editors/editorPanel.xul",
					"_blank",
					"chrome,dependent,dialog,modal=yes,resizable,centerscreen",
					params);
		}
	}
}

//Give the constructor the same prototype as its instances, so users can access
//preferences directly via the constructor without having to create an instance
//first.
ResourceViewLauncher.__proto__ = ResourceViewLauncher.prototype;