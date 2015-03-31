if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function() {
	var projectOntoType = art_semanticturkey.CurrentProject.getOntoType();
	
	var gridRows = document.getElementById("gridRows");
	var rows = gridRows.children;
	var j=0;
	for (var i=0; i<rows.length; i++){
		var rowsItem = rows[i];
		if (!isOntoTypeCompatible(projectOntoType, rowsItem.getAttribute("ontoType"))){
			rowsItem.hidden = true;
		}
		if (rowsItem.tagName == "row"){
			if (j%2==0)
				rowsItem.setAttribute("class", "evenHeader");
			else
				rowsItem.setAttribute("class", "oddHeader");
			j++;
			//add a listener on the "run check" button
			var btn = rowsItem.children[2];
			btn.addEventListener("command", art_semanticturkey.btnCheckListener, false);
		}
	}
}

art_semanticturkey.btnCheckListener = function() {
	var btn = this;
	btn.setAttribute("disabled", "true");
	var row = btn.parentNode;
	var target = row.getAttribute("target");
	getChromeWindow().setAttribute("style","cursor:progress");
//	art_semanticturkey.Logger.debug("opening dialog " + target);
	window.openDialog(target, "_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen");
	getChromeWindow().setAttribute("style","cursor:auto");
	btn.setAttribute("disabled", "false");
}

isOntoTypeCompatible = function(projectOntoType, icvOntoType){
	if (icvOntoType == "ANY") {
		return true;
	} else if (icvOntoType == "OWL") {
		return (projectOntoType == icvOntoType);
	} else if (icvOntoType == "SKOS-XL") {
		return (projectOntoType == icvOntoType);
	} else if (icvOntoType == "SKOS") {
		return ((projectOntoType == icvOntoType) || (projectOntoType == "SKOS-XL"));
	}
	return false;
}

getChromeWindow = function(){
	return document.getElementsByTagName("window")[0];
}