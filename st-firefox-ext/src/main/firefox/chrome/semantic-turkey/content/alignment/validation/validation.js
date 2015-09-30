if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Preferences.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Context.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ResourceViewLauncher.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Alignment.jsm", art_semanticturkey);

const relationMeterLabelPrefsEntry = "extensions.semturkey.alignmentValidation.relationMeterLabel";
const relationMeterShowMeasurePrefsEntry = "extensions.semturkey.alignmentValidation.relationMeterShowMeasure";
const maxAlignmentPerPagePrefsEntry = "extensions.semturkey.alignmentValidation.maxAlignmentPerPage";
const rejectedActionPrefsEntry = "extensions.semturkey.alignmentValidation.rejectedAction";

var sessionToken;
var serviceInstance;

var relationMeterLabel; //tells if the meter in "Relation" column should show the relation as 
						//Description Logic Symbol ("dlSymbol"), Alignment format relation ("relation") or text ("text")
var relationMeterShowMeasure; //tells if the meter should show the confidence
var currentPage = 0;

window.onload = function() {
	sessionToken = art_semanticturkey.generateSessionRandomToken();
	var specifiedContext = new art_semanticturkey.Context();
	specifiedContext.setToken(sessionToken);
	serviceInstance = art_semanticturkey.STRequests.Alignment.getAPI(specifiedContext);
	
	relationMeterLabel = art_semanticturkey.Preferences.get(relationMeterLabelPrefsEntry, "relation");
	relationMeterShowMeasure = art_semanticturkey.Preferences.get(relationMeterShowMeasurePrefsEntry, false);
	
	document.getElementById("selectBtn").addEventListener("command", art_semanticturkey.selectAlignment, false);
	document.getElementById("loadBtn").addEventListener("command", art_semanticturkey.loadAlignment, false);
	document.getElementById("nextPageBtn").addEventListener("command", art_semanticturkey.pageController, false);
	document.getElementById("previousPageBtn").addEventListener("command", art_semanticturkey.pageController, false);
	document.getElementById("quickActionMenu").addEventListener("command", art_semanticturkey.quickActionMenuListener, false);
	document.getElementById("quickActionBtn").addEventListener("command", art_semanticturkey.quickActionButtonListener, false);
	document.getElementById("exportAlignmentBtn").addEventListener("command", art_semanticturkey.exportAlignment, false);
	document.getElementById("applyValidationBtn").addEventListener("command", art_semanticturkey.applyValidation, false);
	document.getElementById("editRelationMeterBtn").addEventListener("command", art_semanticturkey.editRelationMeter, false);
	document.getElementById("optionBtn").addEventListener("command", art_semanticturkey.openOptions, false);
}

window.onunload = function() {
	serviceInstance.closeSession();
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
		
		art_semanticturkey.populateAlignmentList(currentPage);
		
		document.getElementById("quickActionMenu").disabled = false;
		document.getElementById("exportAlignmentBtn").disabled = false;
		document.getElementById("applyValidationBtn").disabled = false;
		document.getElementById("thresholdTxt").hidden = true;
		document.getElementById("thresholdTxt").setAttribute("value", "0.0");
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
}

art_semanticturkey.populateAlignmentList = function(page) {
	var listbox = document.getElementById("alignmentList");
	//empty listbox
	while (listbox.itemCount > 0){
		listbox.removeItemAt(0);
	}
	
	try {
		var alignmentPerPage = art_semanticturkey.Preferences.get(maxAlignmentPerPagePrefsEntry, 0);
		var xmlResp = serviceInstance.listCells(page, alignmentPerPage);
		
		//handle page controls
		var mapXml = xmlResp.getElementsByTagName("map")[0];
		var page = mapXml.getAttribute("page");
		var totPage = mapXml.getAttribute("totPage");
		if (totPage != 1) {
			document.getElementById("pageCtrlBox").setAttribute("hidden", "false");
			document.getElementById("pageLabel").setAttribute("value", "Page " + page + "/" + totPage);
			document.getElementById("previousPageBtn").setAttribute("disabled", (page == "1"));
			document.getElementById("nextPageBtn").setAttribute("disabled", (page == totPage));
		}
		
		//populate list
		var cellListXml = xmlResp.getElementsByTagName("cell");
		for (var i=0; i<cellListXml.length; i++){
			var cellXml = cellListXml[i];
			
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			//entity1
			var entity1 = cellXml.getElementsByTagName("entity1")[0].textContent;
			var listcell = document.createElement("listcell");
			listcell.setAttribute("label", entity1);
			listcell.setAttribute("tooltiptext", entity1);
			listcell.addEventListener("dblclick", function() {
				art_semanticturkey.ResourceViewLauncher.openResourceView(entity1);
			}, false);
			listitem.appendChild(listcell);
			listitem.setAttribute("entity1", entity1); //set as attribute so it can be get directly from item
			
			//entity2
			var entity2 = cellXml.getElementsByTagName("entity2")[0].textContent;
			listcell = document.createElement("listcell");
			listcell.setAttribute("tooltiptext", entity2);
			listcell.setAttribute("label", entity2);
			listitem.appendChild(listcell);
			listitem.setAttribute("entity2", entity2); //set as attribute so it can be get directly from item
			
			//relation (with meter based on measure)
			var relation = cellXml.getElementsByTagName("relation")[0].textContent;
			var measure = cellXml.getElementsByTagName("measure")[0].textContent;
			var relationStack = art_semanticturkey.createRelationStack(relation, measure);
			listitem.appendChild(relationStack);
			listitem.setAttribute("relation", relation); //set as attribute so it can be get directly from item
			listitem.setAttribute("measure", measure); //set as attribute so it can be get directly from item
			
			//mapping property
			var mpXml = cellXml.getElementsByTagName("mappingProperty")[0];
			var mappingProp = null;
			var mappingPropQName = null;
			if (mpXml != undefined) {
				var mappingProp = mpXml.textContent;
				var mappingPropQName = mpXml.getAttribute("show");
				listitem.setAttribute("mappingProperty", mappingPropQName);
			}
			var mapPropStack = art_semanticturkey.createMappingPropertyStack(mappingProp, mappingPropQName);
			listitem.appendChild(mapPropStack);
			
			//Actions
			listcell = document.createElement("listcell");
			var buttonBox = document.createElement("hbox");
			var button = document.createElement("button");
			button.setAttribute("label", "Accept");
			button.addEventListener("command", art_semanticturkey.actionButtonListener, false);
			buttonBox.appendChild(button);
			button = document.createElement("button");
			button.setAttribute("label", "Reject");
			button.addEventListener("command", art_semanticturkey.actionButtonListener, false);
			buttonBox.appendChild(button);
			listitem.appendChild(buttonBox);
			
			//Status
			listcell = document.createElement("listcell");
			listcell.setAttribute("flex", "1");
			listcell.setAttribute("pack", "end");
			var statusImg = document.createElement("image");
			
			var s = cellXml.getElementsByTagName("status")[0];
			if (s != undefined) {
				var status = s.textContent;
				listcell.setAttribute("status", status);
				statusImg.setAttribute("src", art_semanticturkey.getImageSrcForStatus(status));
				//since comment is shown as tooltip of status, check its existence only when status is defined
				var comment = cellXml.getElementsByTagName("comment")[0];
				if (comment != undefined) {
					statusImg.setAttribute("tooltiptext", comment.textContent);
				} else {
					statusImg.setAttribute("tooltiptext", status);
				}
			}
			listcell.appendChild(statusImg);
			listitem.appendChild(listcell);
			
			//finally add the build item to the listbox
			listbox.appendChild(listitem);
		}
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
}

/**
 * Creates a stack  within:
 * - "backgroundBox" a light green box (representing the background of the meter)
 * 		- "meterBox" a green box inside backgroundBox that represent the filler of the meter
 * - "relationBox" a box overlapping "backgroundBox"
 * 		- "label" a label in relationBox showing the relation
 * - "editBox" a box overlapping all the previous boxes
 * 		- "editButton" to edit the relation
 */
art_semanticturkey.createRelationStack = function(relation, measure) {
	var stack = document.createElement("stack");
	stack.setAttribute("class", "meterStack");//useful to update UI after changes (check editRelationMeter method)
	stack.setAttribute("tooltiptext", "Relation: " + art_semanticturkey.getCurrentShowForRelation(relation)
			+ "\nConfidence: " + measure);
	
	var backgroundBox = document.createElement("box");
	backgroundBox.setAttribute("style", "background-color: #BEF781; outline: 1px solid black;");
	var meterBox = document.createElement("box");
	meterBox.setAttribute("class", "meterFiller");
	meterBox.setAttribute("style", "background-color: #01DF01;");
	meterBox.setAttribute("width", measure*100 + "px");
	backgroundBox.appendChild(meterBox);
	stack.appendChild(backgroundBox);
	
	var relationBox = document.createElement("vbox");
	relationBox.setAttribute("pack", "center");
	relationBox.setAttribute("align", "center");
	var label = document.createElement("label");
	label.setAttribute("style", "max-width: 100px; color: black; font-weight: bold; text-overflow: ellipsis;");
	label.setAttribute("value", art_semanticturkey.getLabelForMeter(relation, measure));
	label.setAttribute("class", "meterLabel");//useful to update UI after changes (check editRelationMeter method)
	relationBox.appendChild(label);
	stack.appendChild(relationBox);
	
	var editBox = document.createElement("hbox");
	editBox.setAttribute("align", "start");
	editBox.setAttribute("pack", "end");
	editBox.setAttribute("hidden", "true");
	var editBtn = document.createElement("toolbarbutton");
	editBtn.setAttribute("type", "menu");
	var menu = art_semanticturkey.createEditRelationMenu(relation);
	editBtn.appendChild(menu);
	editBox.appendChild(editBtn);
	stack.appendChild(editBox);
	
	stack.addEventListener("mouseover", function() {editBox.setAttribute("hidden", "false");}, false);
	stack.addEventListener("mouseleave", function() {editBox.setAttribute("hidden", "true");}, false);
	
	return stack;
}

/**
 * Creates a menu to allows to change the relation of an alignment
 */
art_semanticturkey.createEditRelationMenu = function(currentRelation) {
	var menu = document.createElement("menupopup");
	var relList = art_semanticturkey.getRelationList();
	for (var i=0; i<relList.length; i++) {
		var menuitem = document.createElement("menuitem");
		menuitem.setAttribute("type", "radio");
		menuitem.setAttribute("label", relList[i]);
		menuitem.setAttribute("relation", relList[i]); //maintain the relation (could be different from the label e.g. ">" != "subsumes (1.0)")
		menuitem.setAttribute("class", "editRelationMenuitem");//useful to update UI after changes (check editRelationMeter method)
		if (relList[i] == art_semanticturkey.getCurrentShowForRelation(currentRelation)){
			menuitem.setAttribute("checked", "true");
		}
		//when an item is clicked, update the label of the meter and apply the changes to the align model
		menuitem.addEventListener("command", art_semanticturkey.changeRelationListener, false);
		menu.appendChild(menuitem);
	}
	return menu;
}

/**
 * Creates a stack  within:
 * - "propBox" a box containing the property
 * 		- "propLabel" a label in propBox showing the property
 * - "editBox" a box overlapping all the previous boxes 
 * 		- "editButton" to edit the relation
 */
art_semanticturkey.createMappingPropertyStack = function(mappingProp, mappingPropQName) {
	var stack = document.createElement("stack");
	stack.setAttribute("class", "mapPropStack");
	
	var propBox = document.createElement("hbox");
	propBox.setAttribute("pack", "center");
	propBox.setAttribute("align", "center");
	var propLabel = document.createElement("label");
	propLabel.setAttribute("class", "mapPropLabel");//useful to update UI after changes
	if (mappingPropQName != null && mappingProp != null) {
		propLabel.setAttribute("value", mappingPropQName);
		stack.setAttribute("tooltiptext", mappingProp);
	}
	propBox.appendChild(propLabel);
	stack.appendChild(propBox);
	
	//prepare the edit button (and append an empty menupopup, it will be populated once the button is clicked)
	var editBox = document.createElement("hbox");
	editBox.setAttribute("align", "start");
	editBox.setAttribute("pack", "end");
	editBox.setAttribute("class", "mapPropEditorBox");
	if (mappingPropQName == null && mappingProp == null) {
		editBox.setAttribute("hidden", "true");
	}
	var editBtn = document.createElement("toolbarbutton");
	editBtn.setAttribute("type", "menu");
	editBtn.setAttribute("hidden", "true");
	//"mousedown" rather than "click" so it populate earlier the menu
	editBtn.addEventListener("mousedown", art_semanticturkey.editMappingPropertyBtnListener, false);
	var menu = document.createElement("menupopup");
	editBtn.appendChild(menu);
	editBox.appendChild(editBtn);
	stack.appendChild(editBox);
	
	stack.addEventListener("mouseover", function() {editBtn.setAttribute("hidden", "false");}, false);
	stack.addEventListener("mouseleave", function() {editBtn.setAttribute("hidden", "true");}, false);
	
	return stack;
}

/**
 * Listener to edit button of the mapping property.
 * Populates the menu to allows to change the mapping property of an alignment.
 */
art_semanticturkey.editMappingPropertyBtnListener = function() {
	var button = this;
	var menu = button.children[0];
	//empty menu
	while (menu.hasChildNodes()) {
		menu.removeChild(menu.lastChild);
	}
	var listitem = art_semanticturkey.getRelatedListitem(button);
	var entity1 = listitem.getAttribute("entity1");
	var relation = listitem.getAttribute("relation");
	var xmlResp = serviceInstance.listSuggestedProperties(entity1, relation);
	var mpColl = xmlResp.getElementsByTagName("mappingProperty");
	var currentMapProp = listitem.getAttribute("mappingProperty");
	for (var i=0; i<mpColl.length; i++) {
		var mappingProperty = mpColl[i].textContent;
		var mappingPropertyQName = mpColl[i].getAttribute("show");
		var menuitem = document.createElement("menuitem");
		menuitem.setAttribute("type", "radio");
		menuitem.setAttribute("label", mappingPropertyQName);
		menuitem.setAttribute("value", mappingProperty);
		menuitem.addEventListener("command", art_semanticturkey.changePropertyListener, false);
		if (currentMapProp == mappingPropertyQName) {
			Logger.debug("checking " + mappingPropertyQName);
			menuitem.setAttribute("checked", "true"); //TODO it doesn't work, why?
		}
		menu.appendChild(menuitem);
		
	}
}

/**
 * Method called when the user change the relation of an alignment through the menu.
 * Update the alignment model and the UI. 
 */
art_semanticturkey.changeRelationListener = function() {
	var menuitem = this;
	var listitem = art_semanticturkey.getRelatedListitem(menuitem);
	var oldRelation = listitem.getAttribute("relation");
	var newRelation = menuitem.getAttribute("relation");
	var oldMeasure = listitem.getAttribute("measure");
	var newMeasure = "1.0";
	try{
		if (newRelation != oldRelation) { //if user has really changed relation apply changes
			if (oldMeasure != newMeasure) { //if old measure was != 1.0 warn that new measure will be 1.0
				var message = "Manually changing the relation will set automatically the measure " +
					"of the alignment to 1.0. Do you want to continue?";
				if (!window.confirm(message)){
					//if user doesn't confirm cancel its choice and restore old relation
					var menupopup = menuitem.parentNode;
					var menuitems = menupopup.children;
					for (var i=0; i<menuitems.length; i++) {
						if (menuitems[i].getAttribute("label") == oldRelation) {
							menuitems[i].setAttribute("checked", "true");
						} else if (menuitems[i].getAttribute("label") == newRelation) {
							menuitems[i].setAttribute("checked", "false");
						}
					}
					return;
				}
			}
			//update attributes and UI (label , tooltip and filler of meter)
			listitem.setAttribute("measure", newMeasure);
			listitem.setAttribute("relation", newRelation);
			listitem.removeAttribute("status");
			listitem.removeAttribute("mappingProperty");
			var meterLabel = listitem.getElementsByClassName("meterLabel")[0];
			meterLabel.setAttribute("value", art_semanticturkey.getLabelForMeter(newRelation, newMeasure));
			var meterStack = listitem.getElementsByClassName("meterStack")[0];
			meterStack.setAttribute("tooltiptext", 
					"Relation: " + art_semanticturkey.getCurrentShowForRelation(newRelation) + "\nConfidence: " + newMeasure);
			var meterFiller = listitem.getElementsByClassName("meterFiller")[0];
			meterFiller.setAttribute("width", "100px");
			var mapPropLabel = listitem.getElementsByClassName("mapPropLabel")[0];
			mapPropLabel.setAttribute("value", "");
			var mapPropStack = listitem.getElementsByClassName("mapPropStack")[0];
			meterStack.removeAttribute("tooltiptext");
			var mapPropEditorBox = listitem.getElementsByClassName("mapPropEditorBox")[0];
			mapPropEditorBox.setAttribute("hidden", "true");
			var statusImage = listitem.getElementsByTagName("image")[0];
			statusImage.removeAttribute("src");
			statusImage.removeAttribute("tooltiptext");
			//apply change to alignment model
			var entity1 = listitem.getAttribute("entity1");
			var entity2 = listitem.getAttribute("entity2");
			serviceInstance.changeRelation(entity1, entity2, newRelation);
		}
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
}

/**
 * Method called when the user change the mappingProperty of an alignment through the menu.
 * Update the label of the mappingProperty cell and apply the changes to the align model
 */
art_semanticturkey.changePropertyListener = function() {
	var menuitem = this;
	var listitem = art_semanticturkey.getRelatedListitem(menuitem);
	var oldMapPropQName = listitem.getAttribute("mappingProperty");
	var newMapPropQName = menuitem.getAttribute("label");
	if (oldMapPropQName != newMapPropQName) {
		try {
			var entity1 = listitem.getAttribute("entity1");
			var entity2 = listitem.getAttribute("entity2");
			var mappingProperty = menuitem.getAttribute("value");
			serviceInstance.changeMappingProperty(entity1, entity2, mappingProperty);
			listitem.setAttribute("mappingProperty", newMapPropQName);
			var mapPropStack = listitem.getElementsByClassName("mapPropStack")[0];
			mapPropStack.setAttribute("tooltiptext", mappingProperty);
			var mapPropLabel = listitem.getElementsByClassName("mapPropLabel")[0];
			mapPropLabel.setAttribute("value", newMapPropQName);
		} catch(e) {
			art_semanticturkey.Alert.alert(e);
		}
	}
}

/**
 * Listener of accept/reject buttons.
 */
art_semanticturkey.actionButtonListener = function() {
	var button = this;
	var currentItem = art_semanticturkey.getRelatedListitem(button);
	var entity1 = currentItem.getAttribute("entity1");
	var entity2 = currentItem.getAttribute("entity2");
	var relation = currentItem.getAttribute("relation");
	try {
		var xmlResp;
		if (button.label == "Accept") {
			xmlResp = serviceInstance.acceptAlignment(entity1, entity2, relation);
		} else {
			xmlResp = serviceInstance.rejectAlignment(entity1, entity2, relation);
		} 
		art_semanticturkey.updateListItemAfterAction(currentItem, xmlResp.getElementsByTagName("cell")[0], false);
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
}

/**
 * Update listitem after accept/reject action: update mapping property and status
 * Item is the listitem to update;
 * cellXml is a portion of xml response containing the cell info;
 * mutlipleAction is a boolean telling if the update is following a single or multiple accept/reject
 */
art_semanticturkey.updateListItemAfterAction = function(item, cellXml, multipleAction) {
	//update mapping property
	var mp = cellXml.getElementsByTagName("mappingProperty")[0];
	var mapPropStack = item.getElementsByClassName("mapPropStack")[0];
	var mapPropLabel = item.getElementsByClassName("mapPropLabel")[0];
	var mapPropEditorBox = item.getElementsByClassName("mapPropEditorBox")[0];
	if (mp != undefined) {
		var mappingProperty = mp.textContent;
		var mappingPropertyQName = mp.getAttribute("show");
		mapPropLabel.setAttribute("value", mappingPropertyQName);
		mapPropStack.setAttribute("tooltiptext", mappingProperty);
		mapPropEditorBox.setAttribute("hidden", "false");
		item.setAttribute("mappingProperty", mappingPropertyQName);
	} else {
		mapPropLabel.setAttribute("value", "");
		mapPropStack.removeAttribute("tooltiptext");
		mapPropEditorBox.setAttribute("hidden", "true");
		item.removeAttribute("mappingProperty");
	}
	//update status
	var status = cellXml.getElementsByTagName("status")[0].textContent;
	item.setAttribute("status", status);
	var statusImg = item.children[5].children[0];//listitem > listcell > image
	statusImg.setAttribute("src", art_semanticturkey.getImageSrcForStatus(status));
	statusImg.removeAttribute("tooltiptext");
	var commentElem = cellXml.getElementsByTagName("comment")[0];
	if (commentElem != undefined) {
		var comment = commentElem.textContent;
		statusImg.setAttribute("tooltiptext", comment);
		if (status = "error" && !multipleAction) {
			var entity1 = item.getAttribute("entity1");
			var entity2 = item.getAttribute("entity2");
			var relation = item.getAttribute("relation");
			art_semanticturkey.Alert.alert("Has not been possible to validate the given alignment: "
					+ entity1 + " " + relation + " " + entity2, comment);
		}
	} else {
		statusImg.setAttribute("tooltiptext", status);
	}
}

/**
 * Listener of changing on quick action menu (accept all (above), reject all (under))
 */
art_semanticturkey.quickActionMenuListener = function() {
	var menulist = this;
	var selectedAction = menulist.selectedItem.label;
	if (selectedAction != "---") {
		document.getElementById("quickActionBtn").setAttribute("disabled", "false");
		if (selectedAction.indexOf("threshold") > -1) {
			document.getElementById("thresholdTxt").hidden = false;
		} else {
			document.getElementById("thresholdTxt").hidden = true;
		}
	} else {
		document.getElementById("quickActionBtn").setAttribute("disabled", "true");
		document.getElementById("thresholdTxt").hidden = true;
	}
}

/**
 * Listener of perform quick action button.
 */
art_semanticturkey.quickActionButtonListener = function() {
	var report = null;
	var actionId = document.getElementById("quickActionMenu").selectedItem.id;
	if (actionId == "acceptAll"){
		try {
			var xmlResp = serviceInstance.acceptAllAlignment();
			art_semanticturkey.updateUIAfterQuickAction(xmlResp);
		} catch (e) {
			art_semanticturkey.Alert.alert(e);
		}
	} else if (actionId == "rejectAll") {
		try {
			var xmlResp = serviceInstance.rejectAllAlignment();
			art_semanticturkey.updateUIAfterQuickAction(xmlResp);
		} catch (e) {
			art_semanticturkey.Alert.alert(e);
		}
	} else if (actionId == "acceptAboveThreshold") {
		try {
			var threshold = document.getElementById("thresholdTxt").value;
			var xmlResp = serviceInstance.acceptAllAbove(threshold);
			art_semanticturkey.updateUIAfterQuickAction(xmlResp);
		} catch (e) {
			art_semanticturkey.Alert.alert(e);
		}
	} else if (actionId == "rejectUnderThreshold") {
		try {
			var threshold = document.getElementById("thresholdTxt").value;
			var xmlResp = serviceInstance.rejectAllUnder(threshold);
			art_semanticturkey.updateUIAfterQuickAction(xmlResp);
		} catch (e) {
			art_semanticturkey.Alert.alert(e);
		}
	}
	
	//reset quick action commands
	document.getElementById("quickActionMenu").selectedIndex = 0;
	if (document.getElementById("alignmentList").itemCount == 0) {
		document.getElementById("quickActionMenu").disabled = true;
		document.getElementById("exportAlignmentBtn").disabled = true;
		document.getElementById("applyValidationBtn").disabled = true;
	}
	document.getElementById("thresholdTxt").hidden = true;
	document.getElementById("quickActionBtn").disabled = true;
}

/**
 * Parse the response of a quick action (accept/rejectAll accept/rejectAllAbove/Under)
 */
art_semanticturkey.updateUIAfterQuickAction = function(xmlResp) {
	var cellXmlColl = xmlResp.getElementsByTagName("cell");
	for (var i=0; i < cellXmlColl.length; i++) {
		var cellXml = cellXmlColl[i];
		var entity1 = cellXml.getElementsByTagName("entity1")[0].textContent;
		var entity2 = cellXml.getElementsByTagName("entity2")[0].textContent;
		var relation = cellXml.getElementsByTagName("relation")[0].textContent;
		
		//look for the listitem with entity1 and entity2
		var listItem = null;
		var alignmentList = document.getElementById("alignmentList");
		for (var j=0; j < alignmentList.itemCount; j++) {
			var li = alignmentList.getItemAtIndex(j);
			if (li.getAttribute("entity1") == entity1 && li.getAttribute("entity2") == entity2) {
				listItem = li;
				break;
			}
		}
		if (listItem != null) {
			art_semanticturkey.updateListItemAfterAction(listItem, cellXml, true);
		}
	}
}

/**
 * Adds the accepted alignments to the ontology model.
 */
art_semanticturkey.applyValidation = function() {
	var deleteRejected;
	var confirmed;
	var message = "This operation will add to the ontology the triples of the accepted alignments"
	
	var rejectedActionPref = art_semanticturkey.Preferences.get(rejectedActionPrefsEntry, "skip");
	if (rejectedActionPref == "skip") {
		var message = message + ". Are you sure to continue?";
		confirmed = window.confirm(message);
		deleteRejected = false;
	} else if (rejectedActionPref == "delete") {
		var message = message + "and delete the triples of the ones rejected. Are you sure to continue?";
		confirmed = window.confirm(message);
		deleteRejected = true;
	} else if (rejectedActionPref == "ask") {
		var message = message + ". Are you sure to continue?";
		var prompts = Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
			.getService(Components.interfaces.nsIPromptService);
		var check = {value: false};
		confirmed = prompts.confirmCheck(null, "Validate alignment", message, 
				"Delete triples of rejected alignments", check);
		deleteRejected = check.value;
	}
	
	if (confirmed) {
		try {
			var xmlResp = serviceInstance.applyValidation(deleteRejected);
			art_semanticturkey.parseApplyValidationResponse(xmlResp);
		} catch (e) {
			art_semanticturkey.Alert.alert(e)
		}
	}
}

/**
 * Parses the response of the apply validation and show a report window
 */
art_semanticturkey.parseApplyValidationResponse = function(xmlResp) {
	var alignReport = new Array(); //report of the alignments processed (acceptd/rejected)
	
	var listCellXml = xmlResp.getElementsByTagName("cell");
	for (var i=0; i<listCellXml.length; i++) {
		var cell = listCellXml[i];
		var entity1 = cell.getAttribute("entity1");
		var entity2 = cell.getAttribute("entity2");
		var property = cell.getAttribute("property");
		var action = cell.getAttribute("action");
		
		var alignCell = {};
		alignCell.entity1 = entity1;
		alignCell.entity2 = entity2;
		alignCell.property = property;
		alignCell.action = action;
		alignReport.push(alignCell);
	}
	var params = {};
	params.alignReport = alignReport;
	window.openDialog("chrome://semantic-turkey/content/alignment/validation/report.xul", 
			"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", params);
}

/**
 * exports the alignment model in a file
 */
art_semanticturkey.exportAlignment = function() {
	try {
		var response = serviceInstance.exportAlignment();
		
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

/**
 * opens a dialog that allows to edit the relation meter label
 */
art_semanticturkey.editRelationMeter = function() {
	var params = {
		changed: false
	}
	window.openDialog("chrome://semantic-turkey/content/alignment/validation/editRelationMeter.xul", 
			"_blank", "chrome,dependent,dialog,modal=yes,centerscreen", params);
	//if there have been changes update some elements of the UI
	if (params.changed) {
		relationMeterLabel = art_semanticturkey.Preferences.get(relationMeterLabelPrefsEntry, "relation");
		relationMeterShowMeasure = art_semanticturkey.Preferences.get(relationMeterShowMeasurePrefsEntry, false);
		//updates the menuitems of the edit relation menu
		var editRelItems = document.getElementsByClassName("editRelationMenuitem");
		for (var i=0; i<editRelItems.length; i++){
			var relation = editRelItems[i].getAttribute("relation");
			editRelItems[i].setAttribute("label", art_semanticturkey.getCurrentShowForRelation(relation));
		}
		//updates the label of the meters
		var labels = document.getElementsByClassName("meterLabel");
		for (var i=0; i<labels.length; i++){
			var relation = art_semanticturkey.getRelatedListitem(labels[i]).getAttribute("relation");
			var measure = art_semanticturkey.getRelatedListitem(labels[i]).getAttribute("measure");
			labels[i].setAttribute("value", art_semanticturkey.getLabelForMeter(relation, measure));
		}
		//updates the tooltip of the meters
		var stacks = document.getElementsByClassName("meterStack");
		for (var i=0; i<stacks.length; i++){
			var relation = art_semanticturkey.getRelatedListitem(stacks[i]).getAttribute("relation");
			var measure = art_semanticturkey.getRelatedListitem(stacks[i]).getAttribute("measure");
			stacks[i].setAttribute("tooltiptext", "Relation: " + art_semanticturkey.getCurrentShowForRelation(relation)
					+ "\nConfidence: " + measure);
		}
	}
}

/**
 * Handles the button to change alignment page
 */
art_semanticturkey.pageController = function() {
	if (this.id == "nextPageBtn") {
		currentPage++;
	} else { //id == "previousPageBtn"
		currentPage--;
	}
	art_semanticturkey.populateAlignmentList(currentPage);
}

art_semanticturkey.openOptions = function() {
	/* according to prefwindow documentation:
	 * - "You can pass the id of a particular pane as the fourth argument to openDialog to open 
	 *   a specific pane by default";
	 * - "Prefer the classical window.openDialog() with the following window features: 
	 * 	 'chrome,titlebar,toolbar,centerscreen,dialog=yes'" */
	var focusPanelId = "alignmentValidationPanel";
	window.openDialog("chrome://semantic-turkey/content/options.xul", 
			"_blank", "chrome,titlebar,toolbar,centerscreen,dialog=yes", focusPanelId);
}

//UTILS

art_semanticturkey.generateSessionRandomToken = function(){
	var result = '';
	var chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	for (var i=0; i<16; i++) {
		var idx = Math.round(Math.random()*(chars.length-1));
		result = result + chars[idx];
	}
	return result;
}

art_semanticturkey.getRelatedListitem = function(element) {
	var listitem = element;
	do {
		listitem = listitem.parentNode;
	}
	while (listitem.tagName != "listitem");
	return listitem;
}

var relationSymbolMap = [];
relationSymbolMap.push({relation: "=", dlSymbol: "\u2261", text: "equivalent"});
relationSymbolMap.push({relation: ">", dlSymbol: "\u2292", text: "subsumes"});
relationSymbolMap.push({relation: "<", dlSymbol: "\u2291", text: "is subsumed"});
relationSymbolMap.push({relation: "%", dlSymbol: "\u22a5", text: "incompatible"});
relationSymbolMap.push({relation: "HasInstance", dlSymbol: "\u2192", text: "has instance"});
relationSymbolMap.push({relation: "InstanceOf", dlSymbol: "\u2190", text: "instance of"});

art_semanticturkey.getRelationList = function() {
	var list = new Array();
	for (var i=0; i<relationSymbolMap.length; i++){
		list.push(relationSymbolMap[i].relation);
	}
	return list;
}

art_semanticturkey.getCurrentShowForRelation = function(relation) {
	for (var i=0; i<relationSymbolMap.length; i++){
		if (relationSymbolMap[i].relation == relation) {
			return relationSymbolMap[i][relationMeterLabel];
		}
	}
}

art_semanticturkey.getLabelForMeter = function(relation, measure) {
	var label = art_semanticturkey.getCurrentShowForRelation(relation);
	if (relationMeterShowMeasure) {
		label = label + " (" + measure + ")";  
	}
	return label;
}

art_semanticturkey.getImageSrcForStatus = function(status) {
	if (status == "accepted") {
		return "chrome://semantic-turkey/skin/images/accept_24x24.png";
	} else if (status == "rejected") {
		return "chrome://semantic-turkey/skin/images/reject_24x24.png";
	} else if (status == "error") {
		return "chrome://semantic-turkey/skin/images/error_24x24.png";
	}
}


