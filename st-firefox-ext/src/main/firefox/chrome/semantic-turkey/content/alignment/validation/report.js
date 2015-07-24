if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

window.onload = function() {
	var report = window.arguments[0];
	
	var action = report.action;
	var reportDescr = document.getElementById("reportDescription");
	if (action == "validate"){
		reportDescr.setAttribute("value", "The following triples has been added as a result of the alignment validation");
	} else if (action == "reject") {
		reportDescr.setAttribute("value", "Alignments rejected");
	}

	var reportList = document.getElementById("reportList");
	var alignReport = report.alignReport;
	for (var i=0; i<alignReport.length; i++){
		
		var listItem = document.createElement("listitem");

		var listCell = document.createElement("listcell");
		listCell.setAttribute("label", alignReport[i].entity1);
		listItem.appendChild(listCell);
		
		listCell = document.createElement("listcell");
		listCell.setAttribute("label", alignReport[i].relation);
		listItem.appendChild(listCell);
		
		listCell = document.createElement("listcell");
		listCell.setAttribute("label", alignReport[i].entity2);
		listItem.appendChild(listCell);
		
		reportList.appendChild(listItem);
	}
	
	window.sizeToContent();
}
