if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Property.jsm", art_semanticturkey);

window.onload = function() {
	document.getElementById("btn").addEventListener("command", buttonListener, false);
}

buttonListener = function(){
	var property = document.getElementById("propertyTxt").value;
	var xmlResp = art_semanticturkey.STRequests.Property.getRange(property, "false");
	
	var customRangeXml = xmlResp.getElementsByTagName("customRange")[0];
	//if the getRange response has customRange section look for the node crEntry and open dialog to fill the userPrompt form
	if (typeof customRangeXml != "undefined"){
		var crEntriesXml = customRangeXml.getElementsByTagName("crEntry");
		for (var i=0; i<crEntriesXml.length; i++){
			var crEntryXml = crEntriesXml[i];
			if (crEntryXml.getAttribute("type") == "graph"){
				var parameters = new Object();
				parameters.crEntryXml = crEntryXml;
				window.openDialog("chrome://semantic-turkey/content/customRange/customRangeForm.xul",
						"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
			}
		}
	} else {
		alert("apertura finestra classica per valorizzare le proprieta'");
	}
	
	
	
}