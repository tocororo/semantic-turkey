if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Context.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Alignment.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);

//window arguments
var sourceResource = window.arguments[0].resource;
var property = window.arguments[0].property;

var openProjects = []; //contains the project already open when the alignment window is invoked
var previousSelectedProject; //used to close project when selection change

window.onload = function() {
	
	sourceResource = window.arguments[0].resource;
	if (typeof sourceResource == "undefined" || sourceResource == ""){
		art_semanticturkey.Alert.alert("No source resource has been passed for the alignment. " +
				"Please, provide a \"resource\" argument from the calling window");
		window.close();
	}
	property = window.arguments[0].property;
	
	document.getElementById("okBtn").addEventListener("command", art_semanticturkey.accept, false);
	document.getElementById("cancelBtn").addEventListener("command", art_semanticturkey.cancel, false);
	document.getElementById("browseBtn").addEventListener("command", art_semanticturkey.browse, false);
	document.getElementById("propertiesMenulist").addEventListener("command", art_semanticturkey.updateOkButtonStatus, false);
	
	var projectOntoType = art_semanticturkey.CurrentProject.getOntoType();
	if (projectOntoType == "OWL"){
		document.getElementById("alsoSKOSPropCheck").hidden=false;
		document.getElementById("alsoSKOSPropCheck").addEventListener(
				"command", function() {art_semanticturkey.populatePropertiesList(this.checked);}, false);
	}
	
	art_semanticturkey.populatePropertiesList(false);
}

art_semanticturkey.browse = function() {
	var parameters = {};
	parameters.selectedResource = null;
	var projectOntoType = art_semanticturkey.CurrentProject.getOntoType();
	if (projectOntoType == "SKOS-XL" || projectOntoType == "SKOS"){
		window.openDialog("chrome://semantic-turkey/content/alignment/browseExternalConcept.xul", "_blank",
				"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
	} else if (projectOntoType == "OWL"){
		window.openDialog("chrome://semantic-turkey/content/alignment/browseExternalClass.xul", "_blank",
				"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
	}
	if (parameters.selectedResource != null){
		document.getElementById("resourceTxt").setAttribute("value", parameters.selectedResource);
	}
	art_semanticturkey.updateOkButtonStatus();
}

/**
 * disable temporarily the menu, empty it, populate it again and finally enable back
 */
art_semanticturkey.populatePropertiesList = function(alsoSKOSProps) {
	var propertiesMenulist = document.getElementById("propertiesMenulist");
	
	propertiesMenulist.disabled = true;
	while (propertiesMenulist.itemCount > 1) {
		propertiesMenulist.removeItemAt(1);
	}
	propertiesMenulist.selectedIndex = 0;
	
	var propList = art_semanticturkey.STRequests.Alignment.getMappingRelations(alsoSKOSProps);
	for (var i=0; i<propList.length; i++){
		var propShow = propList[i].getShow();
		var propUri = propList[i].getURI();
		propertiesMenulist.appendItem(propShow, propUri);
	}
	
	//in case a property is provided from the calling window, select it in the property menu
	if (typeof property != "undefined") {
		for (var i=0; i<propertiesMenulist.itemCount; i++){
			var propUri = propertiesMenulist.getItemAtIndex(i).value;
			if (property == propUri){
				propertiesMenulist.selectedIndex = i;
				break;
			}
		}
	}
	
	propertiesMenulist.disabled = false;
}

art_semanticturkey.updateOkButtonStatus = function() {
	var alignProp = document.getElementById("propertiesMenulist").selectedItem.value;
	var targetResource = document.getElementById("resourceTxt").value;
	if (alignProp != "---" && targetResource != ""){
		document.getElementById("okBtn").setAttribute("disabled", "false");
	} else {
		document.getElementById("okBtn").setAttribute("disabled", "true");
	}
}

art_semanticturkey.accept = function() {
	var alignProp = document.getElementById("propertiesMenulist").selectedItem.value;
	var targetResource = document.getElementById("resourceTxt").value;
	art_semanticturkey.STRequests.Alignment.addAlignment(sourceResource, alignProp, targetResource);
	//return to the calling window the property chosen for the alignment (useful to the resource view
	//in case the user chooses an alignment property different from the one passed)
	window.property = alignProp;
	close();
}

art_semanticturkey.cancel = function() {
	window.close();
}