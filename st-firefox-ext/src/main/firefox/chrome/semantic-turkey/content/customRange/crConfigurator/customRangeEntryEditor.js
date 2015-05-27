if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_CustomRanges.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Metadata.jsm", art_semanticturkey);

/*
 * This .js controls a dialog for creating and editing a CustomRangeEntry. Its behaviour depends on 
 * the "mode" parameter (that could be "edit" or "create") passed from the calling dialog.
 * In "edit mode" this dialog allows only to change the name, description and ref of the given CR.
 * It receives the sole following parameter (in addition to "mode"):
 * - "crId" param that determine which CustomRange the dialog should allow to modify.
 * 
 * In "create mode" this dialog allows to create a new CustomRangeEntry providing an ID, a name, a 
 * description, a type and a ref (all mandatory).
 * It receives the following two parameters (in addition to "mode"):
 * - "creId" NULL param which should be filled with the ID of the CustomRangeEntry created.
 * - "alreadyExistingCre" array param that contains the list of CRE already existing in order to avoid
 * the creation of duplicate CRE.
 */

window.onload = function() {
	
	document.getElementById("entryTypeMenu").addEventListener("command", function(){
		if (this.selectedItem.label == "Graph")
			document.getElementById("showPropRow").setAttribute("hidden", "false");
		else //this.selectedItem.label == "Node"
			document.getElementById("showPropRow").setAttribute("hidden", "true");
	}, false);
	
	if (window.arguments[0].mode == "edit"){
		//fill and disable the ID textbox
		var creId = window.arguments[0].creId.substring(document.getElementById("crePrefix").value.length);
		var txtboxCreId = document.getElementById("txtboxCreId");
		txtboxCreId.setAttribute("value", creId);
		txtboxCreId.setAttribute("disabled", "true");
		txtboxCreId.setAttribute("readOnly", "true");
		//fill the other fields
		var resp = art_semanticturkey.STRequests.CustomRanges.getCustomRangeEntry(window.arguments[0].creId);
		var name = resp.getElementsByTagName("customRangeEntry")[0].getAttribute("name");
		document.getElementById("txtboxCreName").setAttribute("value", name);
		var type = resp.getElementsByTagName("customRangeEntry")[0].getAttribute("type");
		var typeMenu = document.getElementById("entryTypeMenu");
		typeMenu.selectedItem = document.getElementById(type + "Item");
		typeMenu.setAttribute("disabled", "true");
		typeMenu.setAttribute("tooltiptext", "Is not possible to edit the type of a CustomRangeEntry." +
				" If you need to change it, you have to delete the current CRE and create a new one.");
		var descr = resp.getElementsByTagName("description")[0].textContent;
		document.getElementById("txtboxCreDescription").setAttribute("value", descr);
		var ref = resp.getElementsByTagName("ref")[0].textContent;
		document.getElementById("txtboxCreRef").setAttribute("value", ref);
		if (type == "graph"){
			document.getElementById("showPropRow").setAttribute("hidden", "false");
			var showProp = resp.getElementsByTagName("ref")[0].getAttribute("showProperty");
			var txtShowProp = document.getElementById("txtboxShowProp").setAttribute("value", showProp);
		}
	}
	
	document.getElementById("txtboxCreId").addEventListener("input", art_semanticturkey.updateOkButtonStatus, false);
	document.getElementById("txtboxCreName").addEventListener("input", art_semanticturkey.updateOkButtonStatus, false);
	document.getElementById("txtboxCreDescription").addEventListener("input", art_semanticturkey.updateOkButtonStatus, false);
	document.getElementById("txtboxCreRef").addEventListener("input", art_semanticturkey.updateOkButtonStatus, false);
	document.getElementById("txtboxShowProp").addEventListener("input", art_semanticturkey.updateOkButtonStatus, false);
	
}

art_semanticturkey.updateOkButtonStatus = function (){
	var idOk = (document.getElementById("txtboxCreId").value != "");
	var nameOk = (document.getElementById("txtboxCreName").value != "")
	var descOk = (document.getElementById("txtboxCreDescription").value != "")
	var refOk = (document.getElementById("txtboxCreRef").value != "")
	var showPropOk = true;
	if (document.getElementById("entryTypeMenu").selectedItem.value == "graph")
		showPropOk = (document.getElementById("txtboxShowProp").value != "");
	if (idOk && nameOk && descOk && refOk && showPropOk)
		document.getElementById("creEditor").setAttribute("buttondisabledaccept", "false");
	else
		document.getElementById("creEditor").setAttribute("buttondisabledaccept", "true");
}

buttonOkListener = function() {
	if (window.arguments[0].mode == "create"){
		var creId = document.getElementById("txtboxCreId").value;
		if (!creId.match(/^[a-zA-Z0-9]+$/i)){
			alert("Invalid CustomRangeEntry ID. Only alphanumeric characters are allowed (no whitespaces).");
			return false; //prevent closing dialog
		} else {
			var completeCreId = document.getElementById("crePrefix").value + creId;
			if (window.arguments[0].alreadyExistingCre.indexOf(completeCreId) != -1) {
				alert("A CustomRangeEntry with the ID '" + completeCreId + "' already exists. Please, change the ID and retry.");
			} else {
				try{
					//create the new CRE
					var type = document.getElementById("entryTypeMenu").selectedItem.value;
					var name = document.getElementById("txtboxCreName").value;
					var desc = document.getElementById("txtboxCreDescription").value;
					var ref = document.getElementById("txtboxCreRef").value;
					var showProp = null;
					if (type == "graph"){
						showProp = document.getElementById("txtboxShowProp").value;
					}
					art_semanticturkey.STRequests.CustomRanges.createCustomRangeEntry(type, completeCreId, name, desc, ref, showProp);
					window.arguments[0].creId = completeCreId;
				} catch (e) {
					art_semanticturkey.Alert.alert(e);
				}
			}
		}
	} else { //window.arguments[0].mode == "edit"
		try {
			var id = window.arguments[0].creId;
			var name = document.getElementById("txtboxCreName").value;
			var desc = document.getElementById("txtboxCreDescription").value;
			var ref = document.getElementById("txtboxCreRef").value;
			var showProp = null;
			if (document.getElementById("entryTypeMenu").selectedItem.value == "graph"){
				showProp = document.getElementById("txtboxShowProp").value;
			}
			art_semanticturkey.STRequests.CustomRanges.updateCustomRangeEntry(id, name, desc, ref, showProp);
		} catch (e) {
			art_semanticturkey.Alert.alert(e);
		}
	}
}