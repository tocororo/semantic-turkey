if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_CustomRanges.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Metadata.jsm", art_semanticturkey);

window.onload = function() {
	
	var alreadySelectedCre = window.arguments[0].alreadySelectedCre;

	var creListbox = document.getElementById("creListbox");
	try {
		var resp = art_semanticturkey.STRequests.CustomRanges.getAllCustomRangeEntries();
		var creCollXml = resp.getElementsByTagName("customRangeEntry");
		if (creCollXml.length != 0){
			for (var i=0; i<creCollXml.length; i++){
				var creId = creCollXml[i].textContent;
				var listitem = document.createElement("listitem");
				listitem.setAttribute("label", creId);
				//if the current CRE is already assigned to the CR, disable it
				if (alreadySelectedCre.indexOf(creId) != -1){
					listitem.setAttribute("disabled", "true");
				}
				creListbox.appendChild(listitem);
			}
			creListbox.addEventListener("select", function(event){
				document.getElementById("creSelection").setAttribute("buttondisabledaccept", "false");
			}, false);
		} else {
			alert("There are no CustomRangeEntry available");
		}
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
	window.sizeToContent();

}

buttonOkListener = function() {
	var creListbox = document.getElementById("creListbox");
	if (creListbox.selectedItem.getAttribute("disabled") == "true"){
		alert("Cannot add the selected CustomRangeEntry because is already in the current Custom Range.");
		return false;
	} else {
		var creId = creListbox.selectedItem.label;
		window.arguments[0].returnedCreId = creId;
	}
}