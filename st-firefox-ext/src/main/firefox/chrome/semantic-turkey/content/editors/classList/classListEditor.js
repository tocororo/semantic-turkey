if (typeof art_semanticturkey == "undefined")
	var art_semanticturkey = {};
if (typeof art_semanticturkey.classListEditor == "undefined")
	art_semanticturkey.classListEditor = {};

Components.utils.import("resource://stservices/SERVICE_Manchester.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);

art_semanticturkey.classListEditor.init = function() {

	var currentProject = art_semanticturkey.CurrentProject.getProjectName();
	document.getElementById("classTree").projectName = currentProject;

	document.getElementById("classTree").addEventListener(
			"it.uniroma2.art.semanticturkey.event.widget.tree.select",
			art_semanticturkey.classListEditor.classSelectionHandler, false);

	document.getElementById("classList").addEventListener("select",
			art_semanticturkey.classListEditor.classListSelectionHandler, false);

	document.getElementById("addNamedClassButton").addEventListener("command",
			art_semanticturkey.classListEditor.addNamedClassHandler, true);
	document.getElementById("addAnonymousClassButton").addEventListener("command",
			art_semanticturkey.classListEditor.addAnonymousClassHandler, true);
	document.getElementById("removeClassButton").addEventListener("command",
			art_semanticturkey.classListEditor.removeClassHandler, true);
	document.getElementById("moveUpButton").addEventListener("command",
			art_semanticturkey.classListEditor.moveUpHandler, true);
	document.getElementById("moveDownButton").addEventListener("command",
			art_semanticturkey.classListEditor.moveDownHandler, true);

	document.addEventListener("dialogaccept", art_semanticturkey.classListEditor.dialogAcceptHandlder, true);
};

art_semanticturkey.classListEditor.classSelectionHandler = function(event) {
	var selectedClassResource = event.target.selectedClassResource;

	document.getElementById("addNamedClassButton").setAttribute("disabled", !selectedClassResource + "");
};

art_semanticturkey.classListEditor.classListSelectionHandler = function(event) {
	var currentIndex = event.target.currentIndex;

	var isDisabled = currentIndex == -1;

	if (isDisabled) {
		document.getElementById("moveUpButton").setAttribute("disabled", "true");
		document.getElementById("moveDownButton").setAttribute("disabled", "true");
	} else {
		document.getElementById("moveUpButton").setAttribute("disabled",
				event.target.currentIndex == 0 ? "true" : "false");
		document.getElementById("moveDownButton").setAttribute("disabled",
				event.target.currentIndex == event.target.view.rowCount - 1 ? "true" : "false");
	}

	document.getElementById("removeClassButton").setAttribute("disabled", isDisabled ? "true" : "false");
};

art_semanticturkey.classListEditor.addClassListItem = function(label, value) {
	var treeItem = document.createElement("treeitem");
	var treeRow = document.createElement("treerow");
	var treeCell = document.createElement("treecell");

	treeCell.setAttribute("label", label);
	treeCell.setAttribute("value", value);

	treeRow.appendChild(treeCell);
	treeItem.appendChild(treeRow);

	var classList = document.getElementById("classList");
	classList.getElementsByTagName("treechildren")[0].appendChild(treeItem);

	classList.boxObject.ensureRowIsVisible(classList.view.rowCount - 1);
	classList.view.selection.select(classList.view.rowCount - 1);

	document.documentElement.setAttribute("buttondisabledaccept", "false");
};

art_semanticturkey.classListEditor.addAnonymousClassHandler = function(event) {
	var parameters = {
		expression : ""
	};
	window.openDialog("chrome://semantic-turkey/content/editors/classExpression/classExpressionEditor.xul",
			"_blank", "chrome=yes,dialog,resizable=yes,modal,centerscreen", parameters);

	if (!!parameters.expression) {
		art_semanticturkey.classListEditor.addClassListItem(parameters.expression, parameters.expression);
	}
};

art_semanticturkey.classListEditor.addNamedClassHandler = function(event) {
	var selectedClass = document.getElementById("classTree").selectedClassResource;

	if (selectedClass == null)
		return;

	art_semanticturkey.classListEditor.addClassListItem(selectedClass.getShow(), selectedClass.toNT());
};

art_semanticturkey.classListEditor.removeClassHandler = function(event) {
	var classList = document.getElementById("classList");

	var ci = classList.currentIndex;

	if (ci != -1) {
		var item = classList.view.getItemAtIndex(ci);

		item.parentElement.removeChild(item);

		var newCurrent = ci < classList.view.rowCount ? ci : classList.view.rowCount - 1;

		if (newCurrent == -1) {
			classList.view.selection.clearSelection();
		} else {
			classList.boxObject.ensureRowIsVisible(newCurrent);
			classList.view.selection.select(newCurrent);
		}

		document.documentElement.setAttribute("buttondisabledaccept", classList.view.rowCount == 0 ? "true"
				: "false");
	}
}
art_semanticturkey.classListEditor.dialogAcceptHandlder = function(event) {

	var clsDescriptions = [];

	var classListBox = document.getElementById("classList");

	for (var i = 0; i < classListBox.view.rowCount; i++) {
		clsDescriptions.push(classListBox.view.getItemAtIndex(i).getElementsByTagName("treecell")[0]
				.getAttribute("value"));
	}

	window.arguments[0].clsDescriptions = clsDescriptions;
};

art_semanticturkey.classListEditor.moveUpHandler = function(event) {
	var classList = document.getElementById("classList");

	var currentIndex = classList.currentIndex;

	if (currentIndex == -1) {
		return;
	}

	var selectedItem = classList.view.getItemAtIndex(currentIndex);

	if (selectedItem == null)
		return;

	var previous = selectedItem.previousSibling;

	if (previous == null)
		return;

	selectedItem.parentElement.insertBefore(selectedItem, previous);

	classList.boxObject.ensureRowIsVisible(currentIndex - 1);
	classList.view.selection.select(currentIndex - 1);
};

art_semanticturkey.classListEditor.moveDownHandler = function(event) {
	var classList = document.getElementById("classList");

	var currentIndex = classList.currentIndex;

	if (currentIndex == -1) {
		return;
	}

	var selectedItem = classList.view.getItemAtIndex(currentIndex);

	if (selectedItem == null)
		return;

	var next = selectedItem.nextSibling;

	if (next == null)
		return;

	selectedItem.parentElement.insertBefore(selectedItem, next.nextSibling);

	classList.boxObject.ensureRowIsVisible(currentIndex + 1);
	classList.view.selection.select(currentIndex + 1);
};

window.addEventListener("load", art_semanticturkey.classListEditor.init, false);