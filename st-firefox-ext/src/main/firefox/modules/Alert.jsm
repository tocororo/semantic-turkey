Components.utils.import("resource://stmodules/Logger.jsm");

EXPORTED_SYMBOLS = ["alert"];

/**
 * This function should simulate overloading for these function:
 * alert(String message)
 * alert(String message, String details)
 * alert(Exception e)
 * @param msgOrExc
 * @param details
 */
function alert(msgOrExc, details){
	if (isString(msgOrExc)){//firs parameter is a message
		var message = msgOrExc;
		if (typeof details != "undefined"){ //details is also specified
			var parameters = new Object();
			parameters.message = message;
			parameters.details = details;
			parameters.title = "Alert";
			var wm = Components.classes["@mozilla.org/appshell/window-mediator;1"]
					.getService(Components.interfaces.nsIWindowMediator);
			var mainWindow = wm.getMostRecentWindow("navigator:browser");
			mainWindow.openDialog("chrome://semantic-turkey/content/alerts/alertDetailsDialog.xul",
					"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
		} else { //details is not specified
			var alertComponent = Components.classes['@mozilla.org/embedcomp/prompt-service;1']
				.getService(Components.interfaces.nsIPromptService);
			alertComponent.alert(null, "Alert", message); //2nd parameter is title, if null the title is automatically setted to "Alert" ("Avviso" in IT)

//			var wm = Components.classes["@mozilla.org/appshell/window-mediator;1"]
//				.getService(Components.interfaces.nsIWindowMediator);
//			var mainWindow = wm.getMostRecentWindow("navigator:browser");
//			mainWindow.alert(message);
		}
	} else {//first parameter is an exception
		var exception = msgOrExc;
		if (typeof exception.message == "undefined"){//no message in exception -> Use simple alert
			var alertComponent = Components.classes['@mozilla.org/embedcomp/prompt-service;1']
				.getService(Components.interfaces.nsIPromptService);
			alertComponent.alert(null, null, exception.name);
		} else { //message in exception -> Use alertDetailsDialog
			var parameters = new Object();
			parameters.message = exception.name;
			parameters.details = exception.message;
			parameters.title = exception.name;
			var wm = Components.classes["@mozilla.org/appshell/window-mediator;1"]
					.getService(Components.interfaces.nsIWindowMediator);
			var mainWindow = wm.getMostRecentWindow("navigator:browser");
			mainWindow.openDialog("chrome://semantic-turkey/content/alerts/alertDetailsDialog.xul",
					"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
		}
	}
}

function isString(s){
	if (typeof s == "object") {
		if (s instanceof String)
			return true;
		else
			return false;
	} else if (typeof s == "string"){
		return true;
	} else
		return false;
}