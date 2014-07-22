if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

var lastSPARQLResults;

art_semanticturkey.saveSparqlResults = function(){
	var saveFormat = document.getElementById("saveSelector").selectedItem.value;
	
	
	/*var filePath;
	var parameters = new Object();
	parameters.parentWindow = window;
	window.openDialog(
			"chrome://semantic-turkey/content/sparql/saveResults.xul",
			"_blank", "modal=yes,resizable,centerscreen", parameters);
	if(typeof parameters.filePath != 'undefined' && parameters.filePath != ""){
		filePath = parameters.filePath;
	}
	alert("filepath = "+filePath);*/
	
	var nsIFilePicker = Components.interfaces.nsIFilePicker;
	 var fp = Components.classes["@mozilla.org/filepicker;1"].createInstance(nsIFilePicker);
	 fp.init(window, "Export pearl", nsIFilePicker.modeSave);
	 
	 if(saveFormat == "json"){
		 fp.appendFilter("Json Files (*.json)","*.json");
	 }
	 else {
		 fp.appendFilter("Text Files (*.txt)","*.txt");
	 }
	 
	 var res = fp.show();
	 if (res == nsIFilePicker.returnOK || res == nsIFilePicker.returnReplace ){
	   var pickedFile = fp.file;
	   //save pearl code
	   var foStream = Components.classes["@mozilla.org/network/file-output-stream;1"].
	                  createInstance(Components.interfaces.nsIFileOutputStream);
	   foStream.init(pickedFile, 0x02 | 0x08 | 0x20, 0666, 0); 
	   var fileContent = art_semanticturkey.prepareFileContent(saveFormat);
	   foStream.write(fileContent, fileContent.length);
	 }
}


art_semanticturkey.prepareFileContent = function(saveFormat){
	var fileContent;
	
	if(saveFormat == "json"){
		fileContent = art_semanticturkey.prepareFileContentForJson();
	} else{
		fileContent = art_semanticturkey.prepareFileContentForTxt();
	}
		
	return fileContent
}

art_semanticturkey.prepareFileContentForJson = function(){
	var fileContent;
	var resultType = lastSPARQLResults.resulttype;
	if(resultType == "tuple"){
		fileContent = JSON.stringify(lastSPARQLResults.sparql.results);
	} else if(resultType ==  "graph"){
		fileContent = JSON.stringify(lastSPARQLResults.stm);
	}
	return fileContent;
	
}

art_semanticturkey.prepareFileContentForTxt = function(){
	var fileContent="";
	var resultType = lastSPARQLResults.resulttype;
	var separator = "; ";
	if(resultType == "tuple"){
		var cols = lastSPARQLResults.sparql.head.vars;
		for (var i = 0; i < cols.length; i++) {
			var variable_name = cols[i];
			fileContent += variable_name+separator;
		}
		fileContent += "\n\n";
		
		var bindings = lastSPARQLResults.sparql.results.bindings;
		for (var bind in bindings) {
			for (var i = 0; i < cols.length; i++) {
				var variable_name = cols[i];
				var element = (bindings[bind])[variable_name];

				if (typeof element != "undefined") {
					var lblValue = "";
					var type = "";

					if (element.type == "uri") {
						lblValue = element.value;
					} else if (element.type == "literal") {
						lblValue = element.value;
						if (element["xml:lang"] != null) {
							lblValue = lblValue + " (" + element["xml:lang"]
									+ ")";
						}
					} else if (element.type == "typed-literal") {
						lblValue = element.value;
					} else if (element.type == "bnode") {
						lblValue = element.value;
					}

					fileContent += lblValue+separator;
				} else {
					
					fileContent += separator;
				}
			}
			fileContent += "\n";
		}
	} else if(resultType ==  "graph"){
		
		var stms = lastSPARQLResults.stm;
		
		for ( var stm in stms) {
			var sbjName = JSON.stringify(stms[stm].subj).replace(/\"/g, "");
			var preName = JSON.stringify(stms[stm].pred).replace(/\"/g, "");
			var objName = JSON.stringify(stms[stm].obj).replace(/\"/g, "");
			fileContent += sbjName +separator + preName + separator + objName + separator +"\n";
		}
		
	}
	return fileContent;
	
}