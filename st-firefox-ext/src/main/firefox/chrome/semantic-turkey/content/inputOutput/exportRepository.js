if (typeof art_semanticturkey == 'undefined') var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://gre/modules/NetUtil.jsm");
Components.utils.import("resource://gre/modules/FileUtils.jsm");
Components.utils.import("resource://stservices/SERVICE_InputOutput.jsm", art_semanticturkey);

buttonOkListener = function() {
	var ext = document.getElementById("formatMenu").selectedItem.value;
	var format = document.getElementById("formatMenu").selectedItem.label;
	var response = art_semanticturkey.STRequests.InputOutput.saveRDF(format);
	var nsIFilePicker = Components.interfaces.nsIFilePicker;
	var fp = Components.classes["@mozilla.org/filepicker;1"].createInstance(nsIFilePicker);
	fp.init(window, "Export pearl", nsIFilePicker.modeSave);
	fp.appendFilter(format + " Files (*." + ext + ")","*." + ext);
	var res = fp.show();
	if (res == nsIFilePicker.returnOK || res == nsIFilePicker.returnReplace){
		var pickedFile = fp.file;
		
		//write response to file
		var ostream = FileUtils.openSafeFileOutputStream(pickedFile);
		var converter = Components.classes["@mozilla.org/intl/scriptableunicodeconverter"].
		                createInstance(Components.interfaces.nsIScriptableUnicodeConverter);
		converter.charset = "UTF-8";
		var istream = converter.convertToInputStream(response);
		NetUtil.asyncCopy(istream, ostream);
		alert("Repository exported succesfully");
		
		//Alternative valid solution
//		var foStream = Components.classes["@mozilla.org/network/file-output-stream;1"].
//				createInstance(Components.interfaces.nsIFileOutputStream);
//		foStream.init(pickedFile, 0x02 | 0x08 | 0x20, 0666, 0); 
//		var converter = Components.classes["@mozilla.org/intl/converter-output-stream;1"].
//		         createInstance(Components.interfaces.nsIConverterOutputStream);
//		converter.init(foStream, "UTF-8", 0, 0);
//		converter.writeString(response);
//		converter.close(); // this closes foStream
	}
};