if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_ICV.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ResourceViewLauncher.jsm", art_semanticturkey);

var limit = 20; //# max of record that the check should returns (get from preferences?)

window.onload = function() {
	//list of event listeners associated to elements
	document.getElementById("btnApply").addEventListener("command", art_semanticturkey.apply, true);
	art_semanticturkey.initUI();
}

var danglingConceptList = new Array(); //list of the dangling concepts
var danglingConceptSchemeMap = new Array(); //array of pair <danglingConcept; scheme>

var SET_AS_TOP_CONCEPT_OF_ACTION = "Set as topConceptOf";
var SELECT_BROADER_CONCEPT_ACTION = "Select broader concept";
var DO_NOTHING_ACTION = "Do nothing";

/**
 * initializes danglingConceptList and danglingConceptSchemeMap, then initializes the UI
 */
art_semanticturkey.initUI = function() {
	try {
		var xmlResp = art_semanticturkey.STRequests.ICV.listDanglingConcepts(limit);
		var records = xmlResp.getElementsByTagName("record");
		var nResult = records.length;
		var resultCountLabelMsg = "Result: " + nResult;
		var count = xmlResp.getElementsByTagName("collection")[0].getAttribute("count");
		if (count > nResult)
			var resultCountLabelMsg = resultCountLabelMsg + " of " + count;
		document.getElementById("resultCountLabel").setAttribute("value", resultCountLabelMsg);
		for (var i=0; i<records.length; i++){
			var concept = records[i].getAttribute("concept");
			var scheme = records[i].getAttribute("scheme");
			//fill the list of dangling concept (without duplicate if a concept is dangling in multiple sch.)
			if (danglingConceptList.indexOf(concept) == -1) //if the concept is not already in the list
				danglingConceptList.push(concept);
			//fill the map that associate a dangling concept to his scheme(s)
			var pair = new Object();
			pair.concept = concept;
			pair.scheme = scheme;
			danglingConceptSchemeMap.push(pair);
		}
		createDanglingConceptListbox(danglingConceptList);//init UI
	} catch (e){
		alert(e.message);
	}
}

/**
 * Initialize the UI. It adds a row for each dangling concept.
 * @param danglingConceptList
 */
function createDanglingConceptListbox(danglingConceptList) {
	var listbox = document.getElementById("dangling_concept_listbox");
	listbox.setAttribute("rows", danglingConceptList.length);
	var count = danglingConceptList.length;
	if (count == 0)
		document.getElementById("btnApply").disabled = true;
	for (var i=0; i<count; i++){
		var concept = danglingConceptList[i];
		var row = document.createElement("listitem");
		row.setAttribute("allowevents", "true");
		row.setAttribute("idxRow", i);
		//add concept (first column)
		var conceptCell = document.createElement("listcell");
		conceptCell.setAttribute("idxRow", i);
	    conceptCell.setAttribute("label", concept);
	    conceptCell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
	    row.appendChild(conceptCell);
	    //add menuList (second column)
		var menuList = document.createElement("menulist");
		menuList.setAttribute("row", i);
		menuList.setAttribute("concept", concept);
		var menuPopup = document.createElement("menupopup");
		var menuItemDoNothing = document.createElement("menuitem");
		menuItemDoNothing.setAttribute("label", DO_NOTHING_ACTION);
		menuPopup.appendChild(menuItemDoNothing);
		var menuItemTopConcept = document.createElement("menuitem");
		menuItemTopConcept.setAttribute("label", SET_AS_TOP_CONCEPT_OF_ACTION);
		menuPopup.appendChild(menuItemTopConcept);
		var menuItemBroader = document.createElement("menuitem");
		menuItemBroader.setAttribute("label", SELECT_BROADER_CONCEPT_ACTION);
		menuPopup.appendChild(menuItemBroader);
		menuList.appendChild(menuPopup);
		menuList.addEventListener("command", art_semanticturkey.menuListener, false);
		row.appendChild(menuList);
		//add cell for selection to be filled later (third column)
		var selectionCell = document.createElement("listcell");
		selectionCell.setAttribute("row", i);
		row.appendChild(selectionCell);
		//add row to listbox
		listbox.appendChild(row);
	}
}

/**
 * Listener to the concept, when double clicked it opens the editor panel
 */
art_semanticturkey.conceptDblClickListener = function() {
	var concept = this.getAttribute("label");//this in an actionListener represents the target of the listener
	var parameters = new Object();
	parameters.sourceType = "concept";
	parameters.sourceElement = concept;
	parameters.sourceElementName = concept;
	parameters.parentWindow = window;
	parameters.isFirstEditor = true;
	art_semanticturkey.ResourceViewLauncher.openResourceView(parameters);
}

/**
 * Listener to menulist. Dispatches the action to perform
 */
art_semanticturkey.menuListener = function() {
	var menulist = this;//this in an actionListener represents the target of the listener (exactly the menulist)
	var selectedItem = menulist.selectedItem.label;
	var idxRow = menulist.getAttribute("row");
	if (selectedItem == SET_AS_TOP_CONCEPT_OF_ACTION){
		art_semanticturkey.setAsTopConcept(idxRow);
	} else if (selectedItem == SELECT_BROADER_CONCEPT_ACTION){
		art_semanticturkey.selectBroaderConcept(idxRow);
	} else if (selectedItem == DO_NOTHING_ACTION) {
		art_semanticturkey.doNothing(idxRow);
	}
}

/**
 * Called if from the menulist is selected "Set as topConceptOf". Open a dialog with a list of the scheme that
 * the concept belong to, then wait for the choice and apply the changes to the UI.
 * Resets the 2nd and 3rd column if any choice is canceled.
 */
art_semanticturkey.setAsTopConcept = function(idxRow) {
	var listbox = document.getElementById("dangling_concept_listbox");
	var row = listbox.getItemAtIndex(idxRow);
	var concept = row.children[0].getAttribute("label");
	//retrieve list of scheme where the concept is dangling
	var danglingInScheme = new Array();
	for (var i=0; i<danglingConceptSchemeMap.length; i++){
		art_semanticturkey.Logger.debug("concept "+concept+" danglConcept "+danglingConceptSchemeMap[i].concept);
		if (danglingConceptSchemeMap[i].concept == concept){
			danglingInScheme.push(danglingConceptSchemeMap[i].scheme);
		}
	}
	var parameters = new Object();
	parameters.schemeList = danglingInScheme;
	parameters.returnedValue = null;	
	window.openDialog("chrome://semantic-turkey/content/integrityConstraintValidator/danglingConcept/setAsTopConceptOfDialog.xul",
			"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen",
			parameters);
	var selectedScheme = parameters.returnedValue;
	var listbox = document.getElementById("dangling_concept_listbox");
	if (selectedScheme != null){ //if a value is returned by selectSchemeDialog
		var selectionCell = row.children[2];
		selectionCell.setAttribute("label", selectedScheme);
		selectionCell.selectedScheme = selectedScheme; //javascript object added to cell, so the array can be get again later
	} else {
		art_semanticturkey.Logger.debug("menulist "+row.children[1]);
		art_semanticturkey.Logger.debug("selectedItem "+row.children[1].selectedItem);
		row.children[1].selectedIndex = 0;//reset menulist (2n column)
		row.children[2].setAttribute("label", "");//reset selection (3d column)
	}
	art_semanticturkey.checkForApplyButtonStatus();
}

/**
 * Called if from the menulist is selected "Select broader concept". Open a dialog with a concept tree (if the
 * selected concept belongs to more schemes, asks to the user to chose one), then wait for the choice and apply
 * the changes to the UI. Resets the 2nd and 3rd column if any choice is canceled.
 */
art_semanticturkey.selectBroaderConcept = function(idxRow) {
	var listbox = document.getElementById("dangling_concept_listbox");
	var row = listbox.getItemAtIndex(idxRow)
	var concept = row.children[0].getAttribute("label");
	//count in how many scheme the concept is dangling
	var danglingInScheme = new Array(); //array of scheme in which the concept is dangling
	for (var i=0; i<danglingConceptSchemeMap.length; i++){
		art_semanticturkey.Logger.debug("concept "+concept+" danglConcept "+danglingConceptSchemeMap[i].concept);
		if (danglingConceptSchemeMap[i].concept == concept){
			danglingInScheme.push(danglingConceptSchemeMap[i].scheme);
		}
	}
	//if the concept is dangling in multiple scheme, then it asks to the user which one choose
	var scheme; //scheme where to search the broader for the dangling concept
	if (danglingInScheme.length > 1) {
		var parameters = new Object();
		parameters.concept = concept;
		parameters.inScheme = danglingInScheme;
		parameters.returnedValue == null;
		window.openDialog("chrome://semantic-turkey/content/integrityConstraintValidator/danglingConcept/selectSchemeDialog.xul",
				"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
		scheme = parameters.returnedValue;
		if (scheme == null){//if the above dialog has been canceled
			art_semanticturkey.Logger.debug("menulist "+row.children[1]);
			art_semanticturkey.Logger.debug("selectedItem "+row.children[1].selectedItem);
			row.children[1].selectedIndex = 0;//reset menulist (2n column)
			row.children[2].setAttribute("label", "");//reset selection (3d column)
			return; //finally stop the flow of the method
		}
	} else { //if is dangling in only one scheme, set that scheme
		scheme = danglingInScheme[0];
	}
	//prepare and launch the dialog to choose the broader in a concept tree
	var parameters = new Object();
	parameters.concept = concept;
	parameters.scheme = scheme;
	parameters.returnedValue == null;
	window.openDialog("chrome://semantic-turkey/content/integrityConstraintValidator/danglingConcept/selectBroaderDialog.xul",
			"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
	var selectedBroader = parameters.returnedValue;
	if (selectedBroader != null){//if a broader is returned, update 3rd column
		row.children[2].setAttribute("label", selectedBroader);
	} else { //otherwise reset 2nd and 3rd column
		art_semanticturkey.Logger.debug("menulist "+row.children[1]);
		art_semanticturkey.Logger.debug("selectedItem "+row.children[1].selectedItem);
		row.children[1].selectedIndex = 0;//reset menulist (2n column)
		row.children[2].setAttribute("label", "");//reset selection (3d column)
	}
	art_semanticturkey.checkForApplyButtonStatus();
}

/**
 * Called if from the menulist is selected doNothing. Resets the content of third column (selection) 
 */
art_semanticturkey.doNothing = function(idxRow) {
	var listbox = document.getElementById("dangling_concept_listbox");
	var row = listbox.getItemAtIndex(idxRow);
	var selectionCell = row.children[2];
	selectionCell.setAttribute("label", "");
	art_semanticturkey.checkForApplyButtonStatus();
}

/**
 * Checks if there is at least one action to perform. In case enables the apply button, otherwise disables it.
 */
art_semanticturkey.checkForApplyButtonStatus = function(){
	var listBox = document.getElementById("dangling_concept_listbox");
	listBox.itemCount;
	var count = listBox.itemCount;
	var actionToPerform = false;
	for (var i=0; i<count; i++){
		var row = listBox.getItemAtIndex(i);
		var action = row.children[1].selectedItem.label;//action to perform
		if (action != DO_NOTHING_ACTION)
			actionToPerform = true;
	}
	document.getElementById("btnApply").disabled = !actionToPerform;
}

/**
 * Listener to apply button. Applies the changes, so add a concept as topConceptOf a schema or set a broader
 * concept for another one
 */
art_semanticturkey.apply = function() {
	var listBox = document.getElementById("dangling_concept_listbox");
	var count = listBox.itemCount;
    for (var i=0; i<count; i++){//for each row of listbox
        var row = listBox.getItemAtIndex(i);        
        var cells = row.children;
        var concept = cells[0].getAttribute("label");//retieve concept
        var action = cells[1].selectedItem.label;//action to perform
        var selection = cells[2].getAttribute("label");//and selection
        if (typeof selection != "undefined")
        	art_semanticturkey.Logger.debug("Performing " + action + " on concept " + concept + " on selection " + selection);
        if (action == SELECT_BROADER_CONCEPT_ACTION) {
            var broader = cells[2].getAttribute("label");
        	art_semanticturkey.Logger.debug("setting "+broader+" as broader concept of "+concept);
        	art_semanticturkey.STRequests.SKOS.addBroaderConcept(concept, selection);
        } else if (action == SET_AS_TOP_CONCEPT_OF_ACTION) {
        	var schemes = cells[2].selectedScheme; //can retrieve javascript object cause in setAsTopConcept it was added directly to cell[2]
        	for (var j=0; j < schemes.length; j++){
        		art_semanticturkey.Logger.debug("setting "+concept+" as topConceptOf "+schemes[j]);
        		art_semanticturkey.STRequests.SKOS.addTopConcept(schemes[j], concept);
        	}
        }
    }
    alert("Change(s) done");
    window.location.reload();
}

