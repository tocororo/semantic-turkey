if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

window.onload = function() {
	var alignReport = window.arguments[0].alignReport;
	
	var reportList = document.getElementById("reportList");
	
	for (var i=0; i<alignReport.length; i++){
		var listItem = document.createElement("listitem");
		listItem.setAttribute("allowevents", "true");

		var listCell = document.createElement("listcell");
		listCell.setAttribute("label", alignReport[i].entity1);
		listCell.setAttribute("tooltiptext", alignReport[i].entity1);
		listItem.appendChild(listCell);
		
		listCell = document.createElement("listcell");
		listCell.setAttribute("label", alignReport[i].property);
		listCell.setAttribute("tooltiptext", alignReport[i].property);
		listItem.appendChild(listCell);
		
		listCell = document.createElement("listcell");
		listCell.setAttribute("label", alignReport[i].entity2);
		listCell.setAttribute("tooltiptext", alignReport[i].entity2);
		listItem.appendChild(listCell);
		
		listCell = document.createElement("listcell");
		listCell.setAttribute("label", alignReport[i].action);
		listCell.setAttribute("tooltiptext", alignReport[i].action);
		listItem.appendChild(listCell);
		
		listItem.appendChild(listCell);
		
		reportList.appendChild(listItem);
	}
	
	window.sizeToContent();
}
