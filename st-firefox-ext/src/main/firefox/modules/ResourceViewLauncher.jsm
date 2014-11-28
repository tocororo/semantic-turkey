if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Preferences.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_ResourceView.jsm", art_semanticturkey);

EXPORTED_SYMBOLS = ["ResourceViewLauncher"];

ResourceViewLauncher = {}

ResourceViewLauncher.prototype = {
	
	/**
	 * possible inputs
	 * -resourceName:	simply the resource name (qname or uri)
	 * -params:			an object with its attributes (like the input of the old editor panel)
	 * based on the useEditor preference value it launches 
	 * -editor panel:	it needs the params as input
	 * -resource view:	it needs simply the resourceName as input
	 * So, the resourceViewLauncher should work in these scenarios:
	 * 1) Simple scenarios
	 * 	1.a) Input: params; Launch: editor panel
	 * 	simply launch the editor panel with the input given
	 * 	1.b) Input: resourceName; Launch: resource view
	 * 	simply launch the resource view with the input given
	 * 2) Hybrid scenarios:
	 * 	2.a) Input: resourceName; Launch: editor panel
	 * 	it should build a params object to pass to the editor panel.
	 * 	2.b) Input: params; Launch: resource view
	 *  it should get the sourceElementName and pass to the resource view
	 */
	openResourceView: function(params){
		var wm = Components.classes["@mozilla.org/appshell/window-mediator;1"]
			.getService(Components.interfaces.nsIWindowMediator);
		var mainWindow = wm.getMostRecentWindow("navigator:browser");
		//three options: legacy (open old editor panel), tab (new res view in a tab), window (new res view in a window).
		//legacy is the default option
		var useEditor = art_semanticturkey.Preferences.get("extensions.semturkey.useEditor", "legacy").toLowerCase();
		if (useEditor == "tab" || useEditor == "window") {
			//detect type of the input (params) and build the queryString
			var queryString = "?";
			if (typeof params.sourceElementName == 'undefined'){ //params is simply a string (resource name)
				queryString += "resource=" + mainWindow.encodeURIComponent(params);
			} else { //params is an object
				queryString += "resource=" + mainWindow.encodeURIComponent(params.sourceElementName);
			}
			//open the resource view
			if (useEditor == "tab"){//as tab
				mainWindow.gBrowser.addTab("chrome://semantic-turkey/content/resourceView/resourceView.xul" + 
						queryString);
			} else { //as window
				mainWindow.openDialog(
						"chrome://semantic-turkey/content/resourceView/resourceView.xul" + queryString,
						"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen");
			}
		} else { //legacy
			if (typeof params.sourceElementName == 'undefined'){ //params is simply a string (resource name)
				var p = new Object();
				p.isFirstEditor = true;
				p.parentWindow = window;
				p.sourceType = "cls";//???
				p.sourceElement = params;
				p.sourceElementName = params;
				p.deleteForbidden = false;
				params = p;
			}
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