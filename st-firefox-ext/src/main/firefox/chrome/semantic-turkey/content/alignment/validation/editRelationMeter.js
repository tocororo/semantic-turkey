if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Preferences.jsm", art_semanticturkey);

const relationMeterLabelPrefsEntry = "extensions.semturkey.alignmentValidation.relationMeterLabel";
const relationMeterShowMeasurePrefsEntry = "extensions.semturkey.alignmentValidation.relationMeterShowMeasure";

var rel;
var showMeas;

window.onload = function() {

	var radioGroup = document.getElementById("radiogroup");
	var confCheckbox = document.getElementById("confCheck");
	
	rel = art_semanticturkey.Preferences.get(relationMeterLabelPrefsEntry, "relation");
	showMeas = art_semanticturkey.Preferences.get(relationMeterShowMeasurePrefsEntry, false);
	
	//initialize with passed parameters
	radioGroup.selectedIndex = radioGroup.getIndexOfItem(document.getElementById(rel));
	confCheckbox.checked = showMeas;
	
}

doOk = function() {
	var newRel = document.getElementById("radiogroup").selectedItem.id;
	var newConf = document.getElementById("confCheck").getAttribute("checked");
	if (newRel != rel || newConf != showMeas) {
		
		art_semanticturkey.Preferences.set(relationMeterLabelPrefsEntry, newRel);
		console.log(newConf == "true");
		art_semanticturkey.Preferences.set(relationMeterShowMeasurePrefsEntry, newConf == "true");
		
		window.arguments[0].changed = true;
	}
	close();
}