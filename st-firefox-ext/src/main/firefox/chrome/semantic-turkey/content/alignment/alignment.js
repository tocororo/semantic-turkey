if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Context.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Property.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Alignment.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);

var openProjects = []; //contains the project already open when the alignment window is invoked
var previousSelectedProject; //used to close project when selection change

window.onload = function() {
	document.getElementById("okBtn").addEventListener("command", art_semanticturkey.accept, false);
	document.getElementById("cancelBtn").addEventListener("command", art_semanticturkey.cancel, false);
	document.getElementById("browseBtn").addEventListener("command", art_semanticturkey.browseConcept, false);
	document.getElementById("propertiesMenulist").addEventListener("command", art_semanticturkey.updateOkButtonStatus, false);
	art_semanticturkey.populatePropertiesList();
}

art_semanticturkey.browseConcept = function() {
	var parameters = {};
	parameters.selectedConcept = null;
	window.openDialog("chrome://semantic-turkey/content/alignment/browseExternalConcept.xul", "_blank",
			"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
	if (parameters.selectedConcept != null){
		document.getElementById("conceptTxt").setAttribute("value", parameters.selectedConcept);
	}
	art_semanticturkey.updateOkButtonStatus();
}

art_semanticturkey.populatePropertiesList = function() {
	var propertiesMenupopup = document.getElementById("propertiesMenupopup"); 
	var role = "objectProperty";
	var subPropOf = "http://www.w3.org/2004/02/skos/core#mappingRelation";
	var propList = art_semanticturkey.STRequests.Property.getPropertyList(role, subPropOf);
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
	var targetConcept = document.getElementById("conceptTxt").value;
	if (alignProp != "---" && targetConcept != ""){
		document.getElementById("okBtn").setAttribute("disabled", "false");
	} else {
		document.getElementById("okBtn").setAttribute("disabled", "true");
	}
}

art_semanticturkey.accept = function() {
	var sourceConcept = window.arguments[0].concept;
	var alignProp = document.getElementById("propertiesMenulist").selectedItem.value;
	var targetConcept = document.getElementById("conceptTxt").value;
	art_semanticturkey.STRequests.Alignment.addAlignment(sourceConcept, alignProp, targetConcept);
	close();
}

art_semanticturkey.cancel = function() {
	window.close();
}