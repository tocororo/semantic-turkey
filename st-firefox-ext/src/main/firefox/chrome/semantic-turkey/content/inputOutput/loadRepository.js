if (typeof art_semanticturkey == 'undefined') var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_InputOutput.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

window.onload = function(){
	document.getElementById("dirBtn").addEventListener("click", art_semanticturkey.chooseFile, true);
	document.getElementById("loadRepository").addEventListener("click", art_semanticturkey.onAccept, true);
	document.getElementById("cancel").addEventListener("click", art_semanticturkey.cancel, true);
	
	document.getElementById("forceFormat").addEventListener("command", art_semanticturkey.setForceFormat, true);
};


art_semanticturkey.onAccept = function() {
	document.getElementById("loadRepository").setAttribute("disabled", "true");
	try{
		//check if the force format was check
		var sel = document.getElementById("forceFormat");
		var format;
		if(sel.checked){
			//the force format was set, so get the selected value
			format = document.getElementById("forceFormatML").selectedItem.label;
		}
		var file = new File(document.getElementById("srcLocalFile").value);
		var responseXML = art_semanticturkey.STRequests.InputOutput.loadRDF(
				file, document.getElementById("baseURI").value, format);
		art_semanticturkey.loadRepository_RESPONSE(responseXML);
	}
	catch (e) {
		document.getElementById("loadRepository").setAttribute("disabled", "false");
		art_semanticturkey.Alert.alert(e);
	}
};

art_semanticturkey.loadRepository_RESPONSE = function(responseElement){
	if(responseElement.getElementsByTagName("reply")[0].getAttribute("status") == "ok"){
		var msg = responseElement.getElementsByTagName('msg')[0];
		alert(msg.getAttribute("content"));
		close();
	} else{
		//TODO there was an errore, do something
		document.getElementById("loadRepository").setAttribute("disabled", "false");
		art_semanticturkey.Alert.alert(e);
	}
};

art_semanticturkey.cancel = function() {
	close();
};

art_semanticturkey.setForceFormat = function() {
	var sel = document.getElementById("forceFormat");
	//var rows = art_semanticturkey.getRows();
	
	var hboxForFormatList = document.getElementById("hboxForFormatList");
	
	if(sel.checked){
		var menulist = document.createElement("menulist");
		menulist.setAttribute("id", "forceFormatML");
		var menupopup = document.createElement("menupopup");
		menupopup.setAttribute("id", "forceFormatMP");
		
		var menuItem1 = document.createElement("menuitem");
		menuItem1.setAttribute("id", "RDF/XML");
		menuItem1.setAttribute("label", "RDF/XML");
		menupopup.appendChild(menuItem1);
		
		var menuItem2 = document.createElement("menuitem");
		menuItem2.setAttribute("id", "N-TRIPLES");
		menuItem2.setAttribute("label", "N-TRIPLES");
		menupopup.appendChild(menuItem2);
		
		var menuItem3 = document.createElement("menuitem");
		menuItem3.setAttribute("id", "N3");
		menuItem3.setAttribute("label", "N3");
		menupopup.appendChild(menuItem3);
		
		var menuItem4 = document.createElement("menuitem");
		menuItem4.setAttribute("id", "TURTLE");
		menuItem4.setAttribute("label", "TURTLE");
		menupopup.appendChild(menuItem4);
		
		var menuItem5 = document.createElement("menuitem");
		menuItem5.setAttribute("id", "TRIG");
		menuItem5.setAttribute("label", "TRIG");
		menupopup.appendChild(menuItem5);
		
		var menuItem6 = document.createElement("menuitem");
		menuItem6.setAttribute("id", "TRIX");
		menuItem6.setAttribute("label", "TRIX");
		menupopup.appendChild(menuItem6);
		
		var menuItem7 = document.createElement("menuitem");
		menuItem7.setAttribute("id", "TRIX-EXT");
		menuItem7.setAttribute("label", "TRIXEXT");
		menupopup.appendChild(menuItem7);
		
		var menuItem8 = document.createElement("menuitem");
		menuItem8.setAttribute("id", "NQUADS");
		menuItem8.setAttribute("label", "NQUADS");
		menupopup.appendChild(menuItem8);
		
		var row = document.createElement("row");
		row.setAttribute("flex", "1");
		menulist.appendChild(menupopup);
		menupopup.setAttribute("flex", "1");
		row.appendChild(menulist);
		row.setAttribute("id", "forceFormatRow");
		hboxForFormatList.appendChild(row);
		menulist.setAttribute("flex", "1");
		
	}else{
		var row = document.getElementById("forceFormatRow");
		hboxForFormatList.removeChild(row);
	}
}

