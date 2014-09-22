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
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listCyclicConcept();
		var data = xmlResp.getElementsByTagName("data")[0];
		var records = data.getElementsByTagName("record");
		//init UI
		for (var i=0; i<records.length; i++){
			var topCyclicConcept = records[i].getAttribute("topCyclicConcept");
			var cyclicConcept = records[i].getAttribute("cyclicConcept");
			var row = document.createElement("row");
			row.setAttribute("align", "center");
			
//			var conceptLabel = document.createElement("label");
//			conceptLabel.setAttribute("value", concept);
//			conceptLabel.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
//			var btnSetSkosLabel = document.createElement("button");
//			btnSetSkosLabel.setAttribute("label", BTN_SET_SKOS_LABEL);
//			btnSetSkosLabel.addEventListener("command", art_semanticturkey.setLabelButtonListener, false);			
//			var btnSetSkosxlLabel = document.createElement("button");
//			btnSetSkosxlLabel.setAttribute("label", BTN_SET_SKOSXL_LABEL);
//			btnSetSkosxlLabel.addEventListener("command", art_semanticturkey.setLabelButtonListener, false);
//			row.appendChild(conceptLabel);
//			row.appendChild(btnSetSkosLabel);
//			row.appendChild(btnSetSkosxlLabel);
//			rows.appendChild(row);
		}
	} catch (e){
		alert(e.message);
	}
}