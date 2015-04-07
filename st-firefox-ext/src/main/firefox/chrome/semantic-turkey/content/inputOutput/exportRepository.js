if (typeof art_semanticturkey == 'undefined') var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_InputOutput.jsm", art_semanticturkey);

buttonOkListener = function() {
	var format = document.getElementById("formatMenu").selectedItem.value;
	var fileType = document.getElementById("formatMenu").selectedItem.label;
	art_semanticturkey.STRequests.InputOutput.saveRDF(format);
};