if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Context.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Alignment.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);

var openProjects = []; //contains the project already open when the alignment window is invoked
var previousSelectedProject; //used to close project when selection change

window.onload = function() {
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

art_semanticturkey.populatePropertiesList = function(alsoSKOSProps) {
	var propertiesMenupopup = document.getElementById("propertiesMenupopup");
	while (propertiesMenupopup.children.length > 1){
		propertiesMenupopup.removeChild(propertiesMenupopup.lastChild);
	}
	var propList = art_semanticturkey.STRequests.Alignment.getMappingRelations(alsoSKOSProps);
	for (var i=0; i<propList.length; i++){
		var propShow = propList[i].getShow();
		var propUri = propList[i].getURI();
		var menuitem = document.createElement("menuitem");
		menuitem.setAttribute("label", propShow);
		menuitem.setAttribute("value", propUri);
		propertiesMenupopup.appendChild(menuitem);
	}
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
	var sourceResource = window.arguments[0].resource;
	var alignProp = document.getElementById("propertiesMenulist").selectedItem.value;
	var targetResource = document.getElementById("resourceTxt").value;
	art_semanticturkey.STRequests.Alignment.addAlignment(sourceResource, alignProp, targetResource);
	close();
}

art_semanticturkey.cancel = function() {
	window.close();
}