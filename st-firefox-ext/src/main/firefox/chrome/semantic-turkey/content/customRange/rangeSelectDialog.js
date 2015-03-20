if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

var rangesListbox;

window.onload = function() {
	rangesListbox = document.getElementById("rangesListbox");
	initUI();
}

initUI = function(){
	var xmlResp = window.arguments[0].xmlResp;
	//add the custom ranges to the list
	var crEntriesXml = xmlResp.getElementsByTagName("crEntry"); 
	for (var i=0; i<crEntriesXml.length; i++){
		var listitem = document.createElement("listitem");
		var crEntryName = crEntriesXml[i].getAttribute("name");
		listitem.setAttribute("label", crEntryName);
		var description = crEntriesXml[i].getElementsByTagName("description")[0].textContent;
		listitem.setAttribute("tooltiptext", description);
		rangesListbox.appendChild(listitem);
	}
	//eventually add the classic range to the list	
	var rangesXml = xmlResp.getElementsByTagName("ranges")[0];//classic ranges
	if (typeof rangesXml != "undetermined"){
		var listitem = document.createElement("listitem");
		var classicRangeType = rangesXml.getAttribute("rngType")
		listitem.setAttribute("label", classicRangeType);
		listitem.setAttribute("tooltiptext", "Classic range");
		rangesListbox.appendChild(listitem);
	}
}

btnOkListener = function() {
	var selectedRange = rangesListbox.selectedItem.label;
	window.arguments[0].selectedRange = selectedRange;
	return;
}