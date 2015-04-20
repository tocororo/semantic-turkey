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
		listitem.setAttribute("value", crEntryName);
		var description = crEntriesXml[i].getElementsByTagName("description")[0].textContent;
		listitem.setAttribute("tooltiptext", description);
		rangesListbox.appendChild(listitem);
	}
	//eventually add the classic range to the list	
	var rangesXml = xmlResp.getElementsByTagName("ranges")[0];//classic ranges
	if (typeof rangesXml != "undefined"){
		var listitem = document.createElement("listitem");
		var classicRangeType = rangesXml.getAttribute("rngType");
		var itemLabel = classicRangeType;
		if (classicRangeType == "undetermined"){
			itemLabel = "PlainLiteral/typedLiteral/resource";
		}
		listitem.setAttribute("label", itemLabel);
		listitem.setAttribute("value", classicRangeType);
		listitem.setAttribute("tooltiptext", "Range: " + itemLabel);
		rangesListbox.appendChild(listitem);
	}
}

btnOkListener = function() {
	var selectedRange = rangesListbox.selectedItem.value;
	window.arguments[0].selectedRange = selectedRange;
	return;
}