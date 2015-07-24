if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Context.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Alignment.jsm", art_semanticturkey);

var sessionToken;
var serviceInstance;

window.onload = function() {
	sessionToken = art_semanticturkey.generateSessionRandomToken();
	var specifiedContext = new art_semanticturkey.Context();
	specifiedContext.setToken(sessionToken);
	serviceInstance = art_semanticturkey.STRequests.Alignment.getAPI(specifiedContext);
	
	document.getElementById("selectBtn").addEventListener("command", art_semanticturkey.selectAlignment, false);
	document.getElementById("loadBtn").addEventListener("command", art_semanticturkey.loadAlignment, false);
	document.getElementById("quickActionMenu").addEventListener("command", art_semanticturkey.quickActionMenuListener);
	document.getElementById("quickActionBtn").addEventListener("command", art_semanticturkey.quickActionButtonListener);
	document.getElementById("saveAlignmentBtn").addEventListener("command", art_semanticturkey.saveAlignment);
}

window.onunload = function() {
	serviceInstance.closeSession();
}

art_semanticturkey.generateSessionRandomToken = function(){
	var result = '';
	var chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	for (var i=0; i<16; i++) {
		var idx = Math.round(Math.random()*(chars.length-1));
		result = result + chars[idx];
	}
	return result;
}

art_semanticturkey.selectAlignment = function() {
	var nsIFilePicker = Components.interfaces.nsIFilePicker;
	var fp = Components.classes["@mozilla.org/filepicker;1"].createInstance(nsIFilePicker);
	fp.init(window, "Select an alignment file", nsIFilePicker.modeOpen);
	fp.appendFilter("Alignment files (*.xml; *.rdf, *.owl)","*.xml; *.rdf; *.owl;");
	var res = fp.show();
	if (res == nsIFilePicker.returnOK){
			var pickedFile = fp.file;
			var filePathTxt = document.getElementById("filePathTxt");
			filePathTxt.value = pickedFile.path;
			document.getElementById("loadBtn").disabled=false;
	}
}

art_semanticturkey.loadAlignment = function() {
	
	var listbox = document.getElementById("alignmentList");
	//empty listbox
	while (listbox.itemCount > 0){
		listbox.removeItemAt(0);
	}
	
	var filePath = document.getElementById("filePathTxt").value;
	var file = new File(filePath);
	
	try{
		var xmlResp = serviceInstance.loadAlignment(file);
		
		var replyXml = xmlResp.getElementsByTagName("reply")[0];
		if (replyXml.getAttribute("status") == "fail") {
			var message = replyXml.textContent;
			art_semanticturkey.Alert.alert("Alignment file loading failed", message);
			return;
		}
		
		var alignmentXml = xmlResp.getElementsByTagName("Alignment")[0];
		
		var onto1 = alignmentXml.getElementsByTagName("onto1")[0].getElementsByTagName("Ontology")[0].textContent;
		document.getElementById("onto1txt").setAttribute("value", onto1);
		var onto2 = alignmentXml.getElementsByTagName("onto2")[0].getElementsByTagName("Ontology")[0].textContent;
		document.getElementById("onto2txt").setAttribute("value", onto2);
		
		var mappingCellsXml = alignmentXml.getElementsByTagName("map");
		for (var i=0; i<mappingCellsXml.length; i++){
			var cellXml = mappingCellsXml[i].getElementsByTagName("Cell")[0];
			var entity1 = cellXml.getElementsByTagName("entity1")[0].textContent;
			var entity2 = cellXml.getElementsByTagName("entity2")[0].textContent;
			var measure = cellXml.getElementsByTagName("measure")[0].textContent;
			var relation = cellXml.getElementsByTagName("relation")[0].textContent;
			
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var listcell = document.createElement("listcell");
			listcell.setAttribute("label", entity1);
			listitem.appendChild(listcell);
			
			listcell = document.createElement("listcell");
			listcell.setAttribute("label", entity2);
			listitem.appendChild(listcell);
			
			listitem.appendChild(art_semanticturkey.createMeter(relation, measure));
			
			listcell = document.createElement("listcell");
			var buttonBox = document.createElement("hbox");
			var button = document.createElement("button");
			button.setAttribute("label", "Validate");
			button.addEventListener("command", art_semanticturkey.validateButtonListener, false);
			buttonBox.appendChild(button);
			button = document.createElement("button");
			button.setAttribute("label", "Reject");
			button.addEventListener("command", art_semanticturkey.rejectButtonListener, false);
			buttonBox.appendChild(button);
			listitem.appendChild(buttonBox);
			
			listbox.appendChild(listitem);
		}
		
		document.getElementById("quickActionMenu").setAttribute("disabled", "false");
		document.getElementById("saveAlignmentBtn").setAttribute("disabled", "false");
		document.getElementById("tresholdTxt").setAttribute("disabled", "true");
		document.getElementById("tresholdTxt").setAttribute("value", "0.0");
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
	
}

art_semanticturkey.createMeter = function(relation, measure) {
	var stack = document.createElement("stack");
	var outBox = document.createElement("box");
	outBox.setAttribute("style", "background-color: #BEF781; outline: 1px solid black;");
	var inBox = document.createElement("box");
	inBox.setAttribute("style", "background-color: #01DF01;");
	inBox.setAttribute("width", measure*100 + "px");
	outBox.appendChild(inBox);
	
	var relationBox = document.createElement("hbox");
	relationBox.setAttribute("pack", "center");
	relationBox.setAttribute("align", "center");
	var label = document.createElement("label");
	label.setAttribute("style", "color: black; font-weight: bold;");
	label.setAttribute("value", art_semanticturkey.convertRelationToSymbol(relation));
	
	relationBox.appendChild(label);
	
	stack.appendChild(outBox);
	stack.appendChild(relationBox);
	
	stack.setAttribute("tooltiptext", "Relation: " + art_semanticturkey.convertRelationToSymbol(relation)
			+ " (" + art_semanticturkey.getRelationDescription(relation) + ")\nConfidence: " + measure);
	stack.setAttribute("relation", relation);
	stack.setAttribute("measure", measure);
	
	return stack;
}

art_semanticturkey.validateButtonListener = function() {
	//button > hbox > listitem
	var currentItem = this.parentNode.parentNode;
	var entity1 = currentItem.children[0].getAttribute("label");
	var entity2 = currentItem.children[1].getAttribute("label");
	var relation = currentItem.children[2].getAttribute("relation");
	Logger.debug("validating " + entity1 + " " + relation + " " + entity2);
	try {
		serviceInstance.validateAlignment(entity1, entity2, relation);
		currentItem.remove();
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
}

art_semanticturkey.rejectButtonListener = function() {
	//button > hbox > listitem
	var currentItem = this.parentNode.parentNode;
	var entity1 = currentItem.children[0].getAttribute("label");
	var entity2 = currentItem.children[1].getAttribute("label");
	var relation = currentItem.children[2].getAttribute("relation");
	Logger.debug("rejecting " + entity1 + " " + relation + " " + entity2);
	try {
		serviceInstance.rejectAlignment(entity1, entity2, relation);
		currentItem.remove();
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
}

art_semanticturkey.quickActionMenuListener = function() {
	var menulist = this;
	var selectedAction = menulist.selectedItem.label;
	if (selectedAction != "---") {
		document.getElementById("quickActionBtn").setAttribute("disabled", "false");
		if (selectedAction.indexOf("treshold") > -1) {
			document.getElementById("tresholdTxt").disabled = false;
		} else {
			document.getElementById("tresholdTxt").disabled = true;
		}
	} else {
		document.getElementById("quickActionBtn").setAttribute("disabled", "true");
		document.getElementById("tresholdTxt").disabled = true;
	}
}

art_semanticturkey.quickActionButtonListener = function() {
	var report = null;
	var action = document.getElementById("quickActionMenu").selectedItem.label;
	if (action == "Validate all"){
		try {
			var xmlResp = serviceInstance.validateAllAlignment();
			report = art_semanticturkey.parseQuickActionResponse(xmlResp);
		} catch (e) {
			art_semanticturkey.Alert.alert(e);
		}
	} else if (action == "Reject all") {
		try {
			var xmlResp = serviceInstance.rejectAllAlignment();
			report = art_semanticturkey.parseQuickActionResponse(xmlResp);
		} catch (e) {
			art_semanticturkey.Alert.alert(e);
		}
	} else if (action == "Validate all above the treshold...") {
		try {
			var treshold = document.getElementById("tresholdTxt").value;
			var xmlResp = serviceInstance.validateAllAbove(treshold);
			report = art_semanticturkey.parseQuickActionResponse(xmlResp);
		} catch (e) {
			art_semanticturkey.Alert.alert(e);
		}
	} else if (action == "Reject all under the treshold...") {
		try {
			var treshold = document.getElementById("tresholdTxt").value;
			var xmlResp = serviceInstance.rejectAllUnder(treshold);
			report = art_semanticturkey.parseQuickActionResponse(xmlResp);
		} catch (e) {
			art_semanticturkey.Alert.alert(e);
		}
	}
	
	//open report dialog if changes have been performed
	if (report != null) {
		var params = {};
		params = report;
		window.openDialog("chrome://semantic-turkey/content/alignment/validation/report.xul", "_blank", 
				"chrome,dependent,dialog,modal=yes,resizable,centerscreen", params);
	}
	
	//reset quick action commands
	document.getElementById("quickActionMenu").selectedIndex = 0;
	if (document.getElementById("alignmentList").itemCount == 0) {
		document.getElementById("quickActionMenu").disabled = true;
		document.getElementById("saveAlignmentBtn").disabled = true;
	}
	document.getElementById("tresholdTxt").disabled = true;
	document.getElementById("quickActionBtn").disabled = true;
}

/**
 * Parses the response of a quick action, removes the validated/rejected items from the alignment
 * listbox and creates a report of the changes
 */
art_semanticturkey.parseQuickActionResponse = function(xmlResp) {
	var report = {}; //report of the alignments processed (validated/rejected)
	
	var actionType = xmlResp.getElementsByTagName("collection")[0].getAttribute("type");
	report.action = actionType;
	
	var alignReport = new Array();
	
	var alignmentList = document.getElementById("alignmentList");
	var alignmentXmlColl = xmlResp.getElementsByTagName("alignment");
	for (var i=0; i<alignmentXmlColl.length; i++) {
		var align = alignmentXmlColl[i];
		var entity1 = align.getElementsByTagName("entity1")[0].textContent;
		var entity2 = align.getElementsByTagName("entity2")[0].textContent;
		var relation = align.getElementsByTagName("relation")[0].textContent;
		//prepare the report
		var alignElement = {};
		alignElement.entity1 = entity1;
		alignElement.entity2 = entity2;
		alignElement.relation = relation;
		alignReport.push(alignElement);
		//remove the item from listbox
		var count = alignmentList.itemCount;
		for (var j=0; j<count; j++){
			var item = alignmentList.getItemAtIndex(j);
			var e1 = item.children[0].getAttribute("label");
			var e2 = item.children[1].getAttribute("label");
			if (e1 == entity1 && e2 == entity2){
				item.remove();
				j--;
				break;
			}
		}
	}
	report.alignReport = alignReport;
	return report;
}

var relationSymbolMap = [];
relationSymbolMap.push({relation: "=", symbol: "\u2261", description: "equivalent"});
relationSymbolMap.push({relation: ">", symbol: "\u2292", description: "subsumes"});
relationSymbolMap.push({relation: "<", symbol: "\u2291", description: "is subsumed"});
relationSymbolMap.push({relation: "%", symbol: "\u22a5", description: "incompatible"});
relationSymbolMap.push({relation: "HasInstance", symbol: "\u2192", description: "has instance"});
relationSymbolMap.push({relation: "InstanceOf", symbol: "\u2190", description: "instance of"});

art_semanticturkey.convertRelationToSymbol = function(relation) {
	for (var i=0; i<relationSymbolMap.length; i++){
		if (relationSymbolMap[i].relation == relation) {
			return relationSymbolMap[i].symbol;
		}
	}
}

art_semanticturkey.convertSymbolToRelation = function(symbol) {
	for (var i=0; i<relationSymbolMap.length; i++){
		if (relationSymbolMap[i].symbol == symbol) {
			return relationSymbolMap[i].relation;
		}
	}
}

art_semanticturkey.getRelationDescription = function(relation) {
	for (var i=0; i<relationSymbolMap.length; i++){
		if (relationSymbolMap[i].relation == relation) {
			return relationSymbolMap[i].description;
		}
	}
}

art_semanticturkey.saveAlignment = function() {
	try {
		var response = serviceInstance.saveAlignment();
		
		var nsIFilePicker = Components.interfaces.nsIFilePicker;
		var fp = Components.classes["@mozilla.org/filepicker;1"].createInstance(nsIFilePicker);
		fp.init(window, "Save alignment", nsIFilePicker.modeSave);
		fp.appendFilter("Alignment files (*.rdf)","*.rdf");
		var res = fp.show();
		if (res == nsIFilePicker.returnOK || res == nsIFilePicker.returnReplace){
			var pickedFile = fp.file;
			var foStream = Components.classes["@mozilla.org/network/file-output-stream;1"].
					createInstance(Components.interfaces.nsIFileOutputStream);
			foStream.init(pickedFile, 0x02 | 0x08 | 0x20, 0666, 0); 
			foStream.write(response, response.length);
			foStream.close();
		}
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
}


