if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Property.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}


art_semanticturkey.init = function() {
	var childrenRoot = document.getElementById("treeChildren");
	try {
		var schemes = art_semanticturkey.STRequests.SKOS.getAllSchemesList();
		for (var i=0; i<schemes.length; i++){
			var scheme = schemes[i].getURI();
			var treeitem = document.createElement("treeitem");
			var treerow = document.createElement("treerow");
			var treecellCheckBox = document.createElement("treecell");
			treecellCheckBox.setAttribute("value", "true");
			treerow.appendChild(treecellCheckBox);
			var treecellScheme = document.createElement("treecell");
			treecellScheme.setAttribute("label", scheme);
			treerow.appendChild(treecellScheme);
			treeitem.appendChild(treerow);
			childrenRoot.appendChild(treeitem);
		}
	} catch (e){
		alert(e.message);
	}
}

/**
 * Gets the scheme with the checkbox selected. This method is called when OK button is pressed.
 */
function buttonOkListener() {
	var concept = window.arguments[0].concept; //passed parameter
	var checkedElement = new Array();
	var tree = document.getElementById('schemeTree');
	for (var i = 0; i < tree.view.rowCount; i++) {
		var schemaCol = tree.columns.getNamedColumn("schemaCol");
		var checkCol = tree.columns.getNamedColumn("checkCol");
		if (tree.view.getCellValue(i, checkCol) == 'true') {
			checkedElement.push(tree.view.getCellText(i, schemaCol))
		}
	}
	for (var i=0; i<checkedElement.length; i++){
		art_semanticturkey.STRequests.Property.addExistingPropValue(concept, "skos:inScheme", checkedElement[i], "resource");
	}
	alert("Warning: adding the concept to a skos:ConceptScheme may be not enough to ensure that it is visible" +
			"in the concept tree. The concept may be still dangling.");
}

function buttonCancelListener() {
//	window.arguments[0].returnedValue = null;
}