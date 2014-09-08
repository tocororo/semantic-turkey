if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function() {
	var schemeList = window.arguments[0].schemeList;
	
	var childrenRoot = document.getElementById("treeChildren");
	for (var i=0; i<schemeList.length; i++){
		var treeitem = document.createElement("treeitem");
		var treerow = document.createElement("treerow");
		var treecellCheckBox = document.createElement("treecell");
		treecellCheckBox.setAttribute("value", "true");
		treerow.appendChild(treecellCheckBox);
		var treecellScheme = document.createElement("treecell");
		treecellScheme.setAttribute("label", schemeList[i]);
		treerow.appendChild(treecellScheme);
		treeitem.appendChild(treerow);
		childrenRoot.appendChild(treeitem);
	}
}

/**
 * Gets the scheme with the checkbox selected. This method is called when OK button is pressed.
 */
function buttonOkListener() {
	var checkedElement = new Array();
	var tree = document.getElementById('schemeTree');
	for (var i = 0; i < tree.view.rowCount; i++) {
		var schemaCol = tree.columns.getNamedColumn("schemaCol");
		var checkCol = tree.columns.getNamedColumn("checkCol");
		if (tree.view.getCellValue(i, checkCol) == 'true') {
			checkedElement.push(tree.view.getCellText(i, schemaCol))
		}
	}
	window.arguments[0].returnedValue = checkedElement; //value returned to calling window
}

/**
 * Sets to null the returnedValue, so that the calling script will know that the operation has been canceled.
 * This method is called when cancel button is pressed.
 */
function buttonCancelListener() {
	window.arguments[0].returnedValue = null;
}