if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

window.onload = function() {

	var radioGroup = document.getElementById("radiogroup");
	var confCheckbox = document.getElementById("confCheck");
	
	var rel = window.arguments[0].rel;
	var conf = window.arguments[0].conf;
	
	//initialize with passed parameters
	radioGroup.selectedIndex = radioGroup.getIndexOfItem(document.getElementById(rel));
	confCheckbox.checked = conf;
	
}

doOk = function() {
	var rel = window.arguments[0].rel;
	var conf = window.arguments[0].conf;
	var newRel = document.getElementById("radiogroup").selectedItem.id;
	var newConf = document.getElementById("confCheck").checked;
	if (newRel != rel || newConf != conf) {
		window.arguments[0].rel = newRel;
		window.arguments[0].conf = newConf;
		window.arguments[0].changed = true;
	}
	close();
}