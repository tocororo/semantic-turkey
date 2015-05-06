if (typeof art_semanticturkey == 'undefined') var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_InputOutput.jsm", art_semanticturkey);

buttonOkListener = function() {
	var format = document.getElementById("formatMenu").selectedItem.value;
	var fileType = document.getElementById("formatMenu").selectedItem.label;
	var response = art_semanticturkey.STRequests.InputOutput.saveRDF(format);
	var nsIFilePicker = Components.interfaces.nsIFilePicker;
	var fp = Components.classes["@mozilla.org/filepicker;1"].createInstance(nsIFilePicker);
	fp.init(window, "Export pearl", nsIFilePicker.modeSave);
	fp.appendFilter(fileType + " Files (*." + format + ")","*." + format);
	var res = fp.show();
	if (res == nsIFilePicker.returnOK || res == nsIFilePicker.returnReplace){
		var pickedFile = fp.file;
		var foStream = Components.classes["@mozilla.org/network/file-output-stream;1"].
				createInstance(Components.interfaces.nsIFileOutputStream);
		foStream.init(pickedFile, 0x02 | 0x08 | 0x20, 0666, 0); 
		foStream.write(response, response.length);
		foStream.close();
	}
};