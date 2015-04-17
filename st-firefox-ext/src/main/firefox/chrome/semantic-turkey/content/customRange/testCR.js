if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Property.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_CustomRanges.jsm", art_semanticturkey);

window.onload = function() {
	document.getElementById("enrichPropBtn").addEventListener("command", enrichPropListener, false);
	document.getElementById("readPropBtn").addEventListener("command", readPropListener, false);
	document.getElementById("testCreateCre").addEventListener("command", testCreateCre, false);
	document.getElementById("testCreateCr").addEventListener("command", testCreateCr, false);
	document.getElementById("testAddCreToCr").addEventListener("command", testAddCreToCr, false);
	document.getElementById("testAddCrToProp").addEventListener("command", testAddCrToProp, false);
}

testCreateCre = function(){
	try {
		var id = document.getElementById("crePrefixLbl").value + document.getElementById("idTxtAddCre").value;
		var name = document.getElementById("nameTxtAddCre").value;
		var entryType = document.getElementById("entryTypeMenu").selectedItem.value;
		var description = document.getElementById("descTxtAddCre").value;
		var ref = document.getElementById("refTxtAddCre").value;
		if (entryType == "graph"){
			var showProp = document.getElementById("showPropTxtAddCre").value;
			art_semanticturkey.STRequests.CustomRanges.createCustomRangeEntry(entryType, id, name, description, ref, showProp);
		} else {
			art_semanticturkey.STRequests.CustomRanges.createCustomRangeEntry(entryType, id, name, description, ref);
		}
	} catch (e){
		art_semanticturkey.Alert.alert(e);
	}
}

testCreateCr = function(){
	try {
		var id = document.getElementById("crPrefixLbl").value + document.getElementById("idTxtAddCr").value;
		art_semanticturkey.STRequests.CustomRanges.createCustomRange(id);
	} catch (e){
		art_semanticturkey.Alert.alert(e);
	}
}

testAddCreToCr = function(){
	try {
		var customRangeId = document.getElementById("crPrefixLbl").value + document.getElementById("idCrTxtAddCreToCr").value;
		var customRangeEntryId = document.getElementById("crePrefixLbl").value + document.getElementById("idCreTxtAddCreToCr").value;
		art_semanticturkey.STRequests.CustomRanges.addEntryToCustomRange(customRangeId, customRangeEntryId)
	} catch (e){
		art_semanticturkey.Alert.alert(e);
	}
}

testAddCrToProp = function(){
	try {
		var customRangeId = document.getElementById("crPrefixLbl").value + document.getElementById("idTxtAddCrToPred").value;
		var predicate = document.getElementById("propTxtAddCrToPred").value;
		var replaceRanges = false;
		art_semanticturkey.STRequests.CustomRanges.addCustomRangeToPredicate(customRangeId, predicate, replaceRanges);
	} catch (e){
		art_semanticturkey.Alert.alert(e);
	}
}

enrichPropListener = function(){
	var subject = document.getElementById("subjectTxt").value;
	var predicate = document.getElementById("propertyTxt").value;
	enrichProperty(subject, predicate);
}

readPropListener = function(){
	var parameters = {};
	parameters.subject = document.getElementById("subjectTxt").value;//TODO taken so just in this case
	parameters.predicate = document.getElementById("propertyTxt").value;//TODO taken so just in this case
	window.openDialog("chrome://semantic-turkey/content/customRange/fakeResourceView.xul",
			"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
}