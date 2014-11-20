if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

window.onload = function() {
	document.getElementById("simpleAlertBtn").addEventListener("command", art_semanticturkey.simpleAlertListener, true);
	document.getElementById("detailedAlertBtn").addEventListener("command", art_semanticturkey.detailedAlertListener, true);
	document.getElementById("exceptionAlertBtn").addEventListener("command", art_semanticturkey.exceptionAlertListener, true);
}

art_semanticturkey.simpleAlertListener = function(){
	var message = document.getElementById("simpleAlertMessage").value;
	art_semanticturkey.Alert.alert(message);
}

art_semanticturkey.detailedAlertListener = function(){
	var message = document.getElementById("detailedAlertMessage").value;
	var details = document.getElementById("detailedAlertDetails").value;
	art_semanticturkey.Alert.alert(message, details);
}

art_semanticturkey.exceptionAlertListener = function(){
	var message = document.getElementById("exceptionAlertMessage").value;
	try{
		if (message == "")
			throw new UserException();
		else
			throw new UserException(message);	
	} catch (e){
		art_semanticturkey.Alert.alert(e);
	}
}


function UserException(message) {
	this.message = message;
	this.name = "UserException";
}