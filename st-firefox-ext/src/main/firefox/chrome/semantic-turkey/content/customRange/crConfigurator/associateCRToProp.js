if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_CustomRanges.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Metadata.jsm", art_semanticturkey);

window.onload = function() {
	
	document.getElementById("btnSelectProp").addEventListener("command",
			art_semanticturkey.selectPropertyListener, false);
	
	//init UI of CustomRange listbox
	var crListbox = document.getElementById("crListbox");
	crListbox.addEventListener("select", function(){
		if (document.getElementById("txtboxProperty").value != ""){
			document.getElementById("crToPropDialog").setAttribute("buttondisabledaccept", "false");
		}
	}, false);
	var resp = art_semanticturkey.STRequests.CustomRanges.getAllCustomRanges();
	var crCollXml = resp.getElementsByTagName("customRange");
	for (var i=0; i<crCollXml.length; i++){
		var listitem = document.createElement("listitem");
		listitem.setAttribute("label", crCollXml[i].textContent);
		crListbox.appendChild(listitem);
	}
	
	window.sizeToContent();

}

art_semanticturkey.selectPropertyListener = function(){
	//copied from editorPanel.js
	var parameters = new Object();
	parameters.selectedProp = "";
	parameters.selectedPropType = "";
	parameters.oncancel = false;
	parameters.source = "AddNewProperty";
	parameters.type = "All";
	window.openDialog("chrome://semantic-turkey/content/editors/property/propertyTree.xul",
					"_blank", "modal=yes,resizable,centerscreen", parameters);
	if (parameters.oncancel == false) {//if propertyTree is not closed through cancel button
		//N.B. in propertyTree.js oncancel is set true only if the propertyTree window is closed through
		//cancel button, so if it is closed through X button (top right corner) oncancel is still false,
		//even if no selectedProp is returned
		var selectedPropQName = parameters.selectedProp;
		if (selectedPropQName != ""){
			var resp = art_semanticturkey.STRequests.Metadata.expandQName(selectedPropQName);
			var propURI = resp.getElementsByTagName("uri")[0].textContent;
			var txtProperty = document.getElementById("txtboxProperty");
			txtProperty.setAttribute("value", propURI);
			if (document.getElementById("crListbox").selectedItem != null)
				document.getElementById("crToPropDialog").setAttribute("buttondisabledaccept", "false");
		}
	}
}


buttonOkListener = function() {
	var prop = document.getElementById("txtboxProperty").getAttribute("value")
	var crListbox = document.getElementById("crListbox");
	var crId = crListbox.selectedItem.label;
	window.arguments[0].returnedProp = prop;
	window.arguments[0].returnedCrId = crId;
	return;
}