if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

var width=320;
var minHeight=95;
var maxHeight=185;

window.onload = function() {
	art_semanticturkey.init();
	document.getElementById("btnOK").addEventListener("command", art_semanticturkey.btnOkListener, true);
	document.getElementById("btnDetails").addEventListener("command", art_semanticturkey.btnDetailsListener, true);
	window.addEventListener("keydown", function (event) {
		if (event.key == "Enter"){
			window.close();
		}
	}, true);
}

art_semanticturkey.init = function() {
	window.resizeTo(width, minHeight);
	//initialize UI components
	document.title = window.arguments[0].title;
	var msgLabel = document.getElementById("message");
	msgLabel.setAttribute("value", window.arguments[0].message);
}

art_semanticturkey.btnOkListener = function() {
	window.close();
}

art_semanticturkey.btnDetailsListener = function() {
	var btn = this;
	var box = document.getElementById("mainBox");
	
	//show details
	if (btn.label == "Details >>>") {
		btn.label = "<<< Details";
		var txtDetails = document.createElement("textbox");
		txtDetails.setAttribute("id", "txtDet");
		txtDetails.setAttribute("value", window.arguments[0].details);
		txtDetails.setAttribute("multiline", "true");
		txtDetails.setAttribute("readonly", "true");
		txtDetails.setAttribute("rows", "4");
		var box = document.getElementById("mainBox");
		box.appendChild(txtDetails);
		window.resizeTo(width, maxHeight);
	} else if (btn.label == "<<< Details") {//hide details
		btn.label = "Details >>>";
		var txtDetails = document.getElementById("txtDet");
		box.removeChild(txtDetails);
		do {
			/* This is necessary to wait the txtDetails is removed from UI too.
			 * Without this, resizeTo() doesn't work properly, the dialog will be resized till the bottom
			 * of txtDialog (even if it is not visible in UI.
			 */ 
		} while (box.clientHeight > minHeight);
		
		window.resizeTo(width, minHeight);
	}
}
