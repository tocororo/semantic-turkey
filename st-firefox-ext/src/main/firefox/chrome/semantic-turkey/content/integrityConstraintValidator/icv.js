if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/stEvtMgr.jsm", art_semanticturkey);

window.onload = function() {
	//add a listener to projectOpened event, so when the project changes, the window reloads
	art_semanticturkey.eventListenerPropertyArrayObject = new art_semanticturkey.eventListenerArrayClass();
	art_semanticturkey.eventListenerPropertyArrayObject.addEventListenerToArrayAndRegister("projectOpened", function(){window.location.reload();}, null);
	
	art_semanticturkey.init();
}

art_semanticturkey.init = function() {
	
	var projectOntoType = art_semanticturkey.CurrentProject.getOntoType();
	
	var rowList = document.getElementsByTagName("row");
	var j=0;
	for (var i=0; i<rowList.length; i++) {
		var row = rowList[i];
		if (!isOntoTypeCompatible(projectOntoType, row.getAttribute("ontoType"))){
			row.hidden = true;
			continue;
		}
		j++;
		if (j%2==0)
			row.setAttribute("class", "evenHeader");
		else
			row.setAttribute("class", "oddHeader");
		//add a listener on the "run check" button
		var btn = row.children[2];
		btn.addEventListener("command", art_semanticturkey.btnCheckListener, false);
	}
	
	if (projectOntoType == "SKOS-XL") {
		var alsoSKOScheck = document.getElementById("alsoSKOScheck");
		alsoSKOScheck.setAttribute("hidden", "false");
	}
	
}

art_semanticturkey.expandCollapseListener = function (button){
	//button > hbox > stack > caption > groupbox
	var groupbox = button.parentNode.parentNode.parentNode.parentNode;
	
	//look for child vbox or grid, namely the target to collapse or expand
	var children = groupbox.children;
	var expandable = null;
	for (var i=0; i<children.length; i++){
		if (children[i].tagName == "vbox" || children[i].tagName == "grid"){
			expandable = children[i];
		}
	}
	//now expande or collapse the target
	if (button.label == "+") {
		button.label = "-";
		button.setAttribute("tooltiptext","Collapse");
		if (expandable != null){
			expandable.setAttribute("hidden", "false");
		}
	} else { // button.label == "-"
		button.label = "+";
		button.setAttribute("tooltiptext","Expand");
		if (expandable != null){
			expandable.setAttribute("hidden", "true");
		}
	}
}

art_semanticturkey.skosCheckListener = function(checkbox) {
	var checked = checkbox.checked;
	var labelCheckGroupbox = document.getElementById("labelCheckGroupbox");
	var rowList = labelCheckGroupbox.getElementsByTagName("row");

	var j=0;
	for (var i=0; i<rowList.length; i++) {
		var row = rowList[i];
		var icvOntoTypes = row.getAttribute("ontoType");
		var icvOntoTypeList = icvOntoTypes.replace(/ /g,"").split(",");
		if (checked) { //show all the row compatible with SKOS and SKOS-XL
			if (icvOntoTypeList.indexOf("SKOS-XL") > -1 || icvOntoTypeList.indexOf("SKOS") > -1){
				row.hidden = false;
				j++;
				if (j%2==0)
					row.setAttribute("class", "evenHeader");
				else
					row.setAttribute("class", "oddHeader");
			} else {
				row.hidden = true;
			}
		} else {
			if (icvOntoTypeList.indexOf("SKOS-XL") > -1){
				row.hidden = false;
				j++;
				if (j%2==0)
					row.setAttribute("class", "evenHeader");
				else
					row.setAttribute("class", "oddHeader");
			} else {
				row.hidden = true;
			}
		}
	}
}

art_semanticturkey.btnCheckListener = function() {
	var btn = this;
	btn.setAttribute("disabled", "true");
	var row = btn.parentNode;
	var target = row.getAttribute("target");
	getChromeWindow().setAttribute("style","cursor:progress");
	var parameters = {};
	parameters.ontoType = row.getAttribute("ontoType"); //currently useful just for SKOS/SKOS-XL labeling checks
//	Logger.debug("opening dialog " + target);
	window.openDialog(target, "_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
	getChromeWindow().setAttribute("style","cursor:auto");
	btn.setAttribute("disabled", "false");
}

isOntoTypeCompatible = function(projectOntoType, icvOntoTypes){
	var icvOntoTypeList = icvOntoTypes.replace(/ /g,"").split(",");
	return (icvOntoTypeList.indexOf(projectOntoType) > -1);
}

getChromeWindow = function(){
	return document.getElementsByTagName("window")[0];
}