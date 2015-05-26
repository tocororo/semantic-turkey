if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_CustomRanges.jsm", art_semanticturkey);


window.onload = function() {
	
	var propCrListbox = document.getElementById("propCrList");
	propCrListbox.addEventListener("select", function(){
		document.getElementById("removePropCr").setAttribute("disabled", "false");
	}, false);
	var resp = art_semanticturkey.STRequests.CustomRanges.getCustomRangeConfigMap();
	var configEntries = resp.getElementsByTagName("configEntry");
	for (var i=0; i<configEntries.length; i++){
		var idCr = configEntries[i].getAttribute("idCustomRange");
		var prop = configEntries[i].getAttribute("property");
		var listitem = document.createElement("listitem");
		var listcellProp = document.createElement("listcell");
		listcellProp.setAttribute("label", prop);
		listitem.appendChild(listcellProp);
		var listcellIdCr = document.createElement("listcell");
		listcellIdCr.setAttribute("label", idCr);
		listitem.appendChild(listcellIdCr);
		propCrListbox.appendChild(listitem);
	}
	document.getElementById("newPropCr").addEventListener("command", art_semanticturkey.newPropCustomRangeListener, false);
	document.getElementById("removePropCr").addEventListener("command", art_semanticturkey.removePropCustomRangeListener, false);
	
	var crListbox = document.getElementById("crList");
	crListbox.addEventListener("select", function(){
		document.getElementById("editCr").setAttribute("disabled", "false");
		document.getElementById("removeCr").setAttribute("disabled", "false");
	}, false);
	resp = art_semanticturkey.STRequests.CustomRanges.getAllCustomRanges();
	var crCollXml = resp.getElementsByTagName("customRange");
	for (var i=0; i<crCollXml.length; i++){
		var listitem = document.createElement("listitem");
		listitem.setAttribute("label", crCollXml[i].textContent);
		crListbox.appendChild(listitem);
	}
	document.getElementById("newCr").addEventListener("command", art_semanticturkey.newCustomRangeListener, false);
	document.getElementById("editCr").addEventListener("command", art_semanticturkey.editCustomRangeListener, false);
	document.getElementById("removeCr").addEventListener("command", art_semanticturkey.removeCustomRangeListener, false);
	
	var creListbox = document.getElementById("creList");
	creListbox.addEventListener("select", function(){
		document.getElementById("editCre").setAttribute("disabled", "false");
		document.getElementById("removeCre").setAttribute("disabled", "false");
	}, false);
	resp = art_semanticturkey.STRequests.CustomRanges.getAllCustomRangeEntries();
	var creCollXml = resp.getElementsByTagName("customRangeEntry");
	for (var i=0; i<creCollXml.length; i++){
		var listitem = document.createElement("listitem");
		listitem.setAttribute("label", creCollXml[i].textContent);
		creListbox.appendChild(listitem);
	}
	document.getElementById("newCre").addEventListener("command", art_semanticturkey.newCustomRangeEntryListener, false);
	document.getElementById("editCre").addEventListener("command", art_semanticturkey.editCustomRangeEntryListener, false);
	document.getElementById("removeCre").addEventListener("command", art_semanticturkey.removeCustomRangeEntryListener, false);
	
	window.sizeToContent();
}

art_semanticturkey.newPropCustomRangeListener = function(){
	var propCrListbox = document.getElementById("propCrList");
	var parameters = {};
	parameters.returnedProp = null;
	parameters.returnedCrId = null;
	window.openDialog("chrome://semantic-turkey/content/customRange/crConfigurator/associateCRToPropDialog.xul",
			"_blank", "modal=yes,resizable,centerscreen", parameters);
	if (parameters.returnedCrId != null && parameters.returnedProp != null){
		try{
			//call service to add CR to the predicate
			var crId = parameters.returnedCrId;
			var prop = parameters.returnedProp;
			art_semanticturkey.STRequests.CustomRanges.addCustomRangeToPredicate(crId, prop);
			//update UI
			var propCrListbox = document.getElementById("propCrList");
			var listitem = document.createElement("listitem");
			var listcellProp = document.createElement("listcell");
			listcellProp.setAttribute("label", prop);
			listitem.appendChild(listcellProp);
			var listcellIdCr = document.createElement("listcell");
			listcellIdCr.setAttribute("label", crId);
			listitem.appendChild(listcellIdCr);
			propCrListbox.appendChild(listitem);
		} catch (e) {
			art_semanticturkey.Alert.alert(e);
		}
	}
}

art_semanticturkey.removePropCustomRangeListener = function(){
	var propCrListbox = document.getElementById("propCrList");
	var selectedItem = propCrListbox.selectedItem;
	var prop = selectedItem.children[0].getAttribute("label");
	var cr = selectedItem.children[1].getAttribute("label");
	art_semanticturkey.STRequests.CustomRanges.removeCustomRangeFromPredicate(prop, cr);
	propCrListbox.removeItemAt(propCrListbox.selectedIndex);
	this.setAttribute("disabled", "true");
}

art_semanticturkey.newCustomRangeListener = function(){
	var crListbox = document.getElementById("crList");
	var parameters = {};
	parameters.mode = "create";//customRangeEditorDialog should work in "create" mode
	parameters.crId = null;//ID of the new CR created (to add to the listbox)
	parameters.alreadyExistingCr = [];//useful to prevent creation of CR with already used ID
	for (var i=0; i<crListbox.itemCount; i++){
		parameters.alreadyExistingCr.push(crListbox.getItemAtIndex(i).label);
	}
	window.openDialog("chrome://semantic-turkey/content/customRange/crConfigurator/customRangeEditorDialog.xul",
			"_blank", "modal=yes,resizable,centerscreen", parameters);
	//add the new created CR to the listbox
	if (parameters.crId != null){
		var listitem = document.createElement("listitem");
		listitem.setAttribute("label", parameters.crId);
		crListbox.appendChild(listitem);
	}
}

art_semanticturkey.editCustomRangeListener = function(){
	var crListbox = document.getElementById("crList");
	var crId = crListbox.selectedItem.label;
	var parameters = {};
	parameters.mode = "edit";//customRangeEditorDialog should work in "edit" mode
	parameters.crId = crId;
	window.openDialog("chrome://semantic-turkey/content/customRange/crConfigurator/customRangeEditorDialog.xul",
			"_blank", "modal=yes,resizable,centerscreen", parameters);
}

art_semanticturkey.removeCustomRangeListener = function(){
	var crListbox = document.getElementById("crList");
	var crId = crListbox.selectedItem.label;
	art_semanticturkey.STRequests.CustomRanges.deleteCustomRange(crId);
	crListbox.removeItemAt(crListbox.selectedIndex);
	this.setAttribute("disabled", "true");
}

art_semanticturkey.newCustomRangeEntryListener = function(){
	var creListbox = document.getElementById("creList");
	var parameters = {};
	parameters.mode = "create";//customRangeEditorDialog should work in "create" mode
	parameters.creId = null;//ID of the new CRE created (to add to the listbox)
	parameters.alreadyExistingCre = [];//useful to prevent creation of CRE with already used ID
	for (var i=0; i<creListbox.itemCount; i++){
		parameters.alreadyExistingCre.push(creListbox.getItemAtIndex(i).label);
	}
	window.openDialog("chrome://semantic-turkey/content/customRange/crConfigurator/customRangeEntryEditorDialog.xul",
			"_blank", "modal=yes,resizable,centerscreen", parameters);
	//add the new created CRE to the listbox
	if (parameters.creId != null){
		var listitem = document.createElement("listitem");
		listitem.setAttribute("label", parameters.creId);
		creListbox.appendChild(listitem);
	}
}

art_semanticturkey.editCustomRangeEntryListener = function(){
	var creListbox = document.getElementById("creList");
	var creId = creListbox.selectedItem.label;
	var parameters = {};
	parameters.mode = "edit";//customRangeEntryEditorDialog should work in "edit" mode
	parameters.creId = creId;
	window.openDialog("chrome://semantic-turkey/content/customRange/crConfigurator/customRangeEntryEditorDialog.xul",
			"_blank", "modal=yes,resizable,centerscreen", parameters);
}

art_semanticturkey.removeCustomRangeEntryListener = function(){
	var creListbox = document.getElementById("creList");
	var creId = creListbox.selectedItem.label;
	art_semanticturkey.STRequests.CustomRanges.deleteCustomRangeEntry(creId);
	creListbox.removeItemAt(creListbox.selectedIndex);
	this.setAttribute("disabled", "true");
}