if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_SKOS_ICV.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function(){
	var rows = document.getElementById("gridRows");
	try {
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listConceptSchemesWithNoTopConcept();
		var data = xmlResp.getElementsByTagName("data")[0];
		var schemes = data.getElementsByTagName("conceptScheme");
		for (var i=0; i<schemes.length; i++){
			var row = document.createElement("row");
			row.setAttribute("align", "center");
			var scheme = schemes[i].textContent;
			var schemeLabel = document.createElement("label");
			schemeLabel.setAttribute("value", scheme);
			var fixButton = document.createElement("button");
			fixButton.setAttribute("label", "Edit skos:ConceptScheme");
			fixButton.addEventListener("command", art_semanticturkey.fixButtonClickListener, false);
			row.appendChild(schemeLabel);
			row.appendChild(fixButton);
			rows.appendChild(row);
		}
	} catch (e){
		alert(e.message);
	}
}


art_semanticturkey.fixButtonClickListener = function() {
	var btn = this;
	var row = btn.parentNode;
	var scheme = row.children[0].value;
//	var scheme = label.value;
	//code copied from skos/widget/schemeList/impl/schemeList.xml
	var parameters = {
		sourceElement : null,		// elemento contenente i dati della riga corrente
		sourceType : "conceptScheme",		// tipo di editor: clss, ..., determina le funzioni custom ed il titolo della finestra
		sourceElementName : scheme,	// nome dell'elemento corrente (quello usato per identificazione: attualmente il qname)
		sourceParentElementName : "", // nome dell'elemento genitore
		isFirstEditor : true,		 // l'editor è stato aperto direttamente dall class/... tree o da un altro editor?
		deleteForbidden : false, 	 // cancellazione vietata 
		parentWindow : window		 // finestra da cui viene aperto l'editor
	};
	window.openDialog("chrome://semantic-turkey/content/editors/editorPanel.xul", "_blank",
				"chrome,dependent,dialog,modal=yes,resizable,centerscreen",	parameters);
}