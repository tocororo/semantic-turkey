if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_CustomRanges.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Metadata.jsm", art_semanticturkey);

/*
 * This .js controls a dialog for creating and editing a CustomRange. Its behaviour depends on the
 * "mode" parameter (that could be "edit" or "create") passed from the calling dialog.
 * In "edit mode" this dialog allows only to add and remove CustomRangeEntry from the given CR.
 * It receives the sole following parameter (in addition to "mode"):
 * - "crId" param that determine which CustomRange the dialog should allow to modify.
 * 
 * In "create mode" this dialog allows to create a new CustomRange providing an ID and a list of CRE.
 * It receives the following two parameters (in addition to "mode"):
 * - "crId" NULL param which should be filled with the ID of the CustomRange created.
 * - "alreadyExistingCr" array param that contains the list of CR already existing in order to avoid
 * the creation of duplicate CR.
 */

var originalCreList = []; //useful in "edit" mode to check the CRE to add and to remove at the end of the changes. 

window.onload = function() {
	
	var creListbox = document.getElementById("creListbox");
	creListbox.addEventListener("select", function(){
		document.getElementById("removeCre").setAttribute("disabled", "false");
	}, false);
	
	var addCreBtn = document.getElementById("addCre");
	addCreBtn.addEventListener("command", art_semanticturkey.addCustomRangeEntryListener, false);
	
	var removeCreBtn = document.getElementById("removeCre");
	removeCreBtn.addEventListener("command", art_semanticturkey.removeCustomRangeEntryListener, false);
	
	var txtboxCrId = document.getElementById("txtboxCrId");
	
	if (window.arguments[0].mode == "edit"){
		//fill and disable the ID textbox
		var crId = window.arguments[0].crId.substring(document.getElementById("crPrefix").value.length);
		txtboxCrId.setAttribute("value", crId);
		txtboxCrId.setAttribute("disabled", "true");
		txtboxCrId.setAttribute("readOnly", "true");
		//fill the CRE listbox
		var resp = art_semanticturkey.STRequests.CustomRanges.getCustomRange(window.arguments[0].crId);
		var crEntriesXml = resp.getElementsByTagName("entry");
		for (var i=0; i<crEntriesXml.length; i++){
			var listitem = document.createElement("listitem");
			var creId = crEntriesXml[i].getAttribute("id");
			listitem.setAttribute("label", creId);
			creListbox.appendChild(listitem);
			originalCreList.push(creId);
		}
	} else { //window.arguments[0].mode == "create"
		//add a listener that eventually enable the OK button when the content of ID textbox changes.
		txtboxCrId.addEventListener("input", function(){
			if (this.value != "" && creListbox.itemCount > 0)
				document.getElementById("crEditor").setAttribute("buttondisabledaccept", "false");
		}, false);
	}
	
	window.sizeToContent();

}

art_semanticturkey.addCustomRangeEntryListener = function(){
	var parameters = {};
	parameters.returnedCreId = null;
	parameters.alreadySelectedCre = [];//useful to avoid adding duplicate CRE to the current CR 
	var creListbox = document.getElementById("creListbox");
	for (var i=0; i<creListbox.itemCount; i++){
		parameters.alreadySelectedCre.push(creListbox.getItemAtIndex(i).label);
	}
	window.openDialog("chrome://semantic-turkey/content/customRange/crConfigurator/selectCustomRangeEntryDialog.xul",
			"_blank", "modal=yes,resizable,centerscreen", parameters);
	
	var creId = parameters.returnedCreId;
	if (creId != null){
		var listitem = document.createElement("listitem");
		listitem.setAttribute("label", creId);
		creListbox.appendChild(listitem);
		window.sizeToContent();
		if (document.getElementById("txtboxCrId").value != "")
			document.getElementById("crEditor").setAttribute("buttondisabledaccept", "false");
	}
}

art_semanticturkey.removeCustomRangeEntryListener = function(){
	var creListbox = document.getElementById("creListbox");
	creListbox.removeItemAt(creListbox.selectedIndex);
	if (creListbox.itemCount == 0)
		document.getElementById("crEditor").setAttribute("buttondisabledaccept", "true");
	else
		document.getElementById("crEditor").setAttribute("buttondisabledaccept", "false");
}


buttonOkListener = function() {
	if (window.arguments[0].mode == "create"){
		var crId = document.getElementById("txtboxCrId").value;
		if (!crId.match(/^[a-zA-Z0-9]+$/i)){
			alert("Invalid CustomRange ID. Only alphanumeric characters are allowed (no whitespaces).");
			return false; //prevent closing dialog
		} else {
			var completeCrId = document.getElementById("crPrefix").value + crId;
			if (window.arguments[0].alreadyExistingCr.indexOf(completeCrId) != -1) {
				alert("A CustomRange with the ID '" + completeCrId + "' already exists. Please, change the ID and retry.");
			} else {
				try{
					//create the new CR
					art_semanticturkey.STRequests.CustomRanges.createCustomRange(completeCrId);
					//add all the CRE to the CR
					var creListbox = document.getElementById("creListbox");
					for (var i=0; i<creListbox.itemCount; i++){
						var creId = creListbox.getItemAtIndex(i).label;
						art_semanticturkey.STRequests.CustomRanges.addEntryToCustomRange(completeCrId, creId);
					}
					window.arguments[0].crId = completeCrId;
				} catch (e) {
					art_semanticturkey.Alert.alert(e);
				}
			}
		}
	} else { //window.arguments[0].mode == "edit"
		var creListbox = document.getElementById("creListbox");
		//collects the CRE and checks which have been added and which have been removed
		var updatedCreList = [];
		for (var i=0; i<creListbox.itemCount; i++){
			updatedCreList.push(creListbox.getItemAtIndex(i).label);
		}
		var creToAdd = [];
		for (var i=0; i<updatedCreList.length; i++){
			if (originalCreList.indexOf(updatedCreList[i]) == -1){
				creToAdd.push(updatedCreList[i]);
			}
		}
		var creToRemove = [];
		for (var i=0; i<originalCreList.length; i++){
			if (updatedCreList.indexOf(originalCreList[i]) == -1)
				creToRemove.push(originalCreList[i]);
		}
		//call services to add and remove CRE from current CR
		try {
			for (var i=0; i<creToAdd.length; i++){
				art_semanticturkey.STRequests.CustomRanges.addEntryToCustomRange(
						window.arguments[0].crId, creToAdd[i]);
			}
			for (var i=0; i<creToRemove.length; i++){
				art_semanticturkey.STRequests.CustomRanges.removeEntryFromCustomRange(
						window.arguments[0].crId, creToRemove[i])
			}
		} catch (e) {
			art_semanticturkey.Alert.alert(e);
		}
	}
}