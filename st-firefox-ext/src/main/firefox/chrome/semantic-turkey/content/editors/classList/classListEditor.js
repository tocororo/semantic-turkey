if (typeof art_semanticturkey == "undefined")
	var art_semanticturkey = {};
if (typeof art_semanticturkey.classListEditor == "undefined")
	art_semanticturkey.classListEditor = {};

Components.utils.import("resource://stservices/SERVICE_Manchester.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

art_semanticturkey.classListEditor.init = function() {

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

art_semanticturkey.classListEditor.addAnonymousClassHandler = function(event) {
	var parameters = {
		expression : ""
	};
	window.openDialog("chrome://semantic-turkey/content/editors/classExpression/classExpressionEditor.xul",
			"_blank", "chrome=yes,dialog,resizable=yes,modal,centerscreen", parameters);

	if (!!parameters.expression) {
		document.getElementById("classListBox").appendItem(parameters.expression, parameters.expression);
		document.documentElement.setAttribute("buttondisabledaccept", "false");
	}
};

art_semanticturkey.classListEditor.addNamedClassHandler = function(event) {
	var parameters = {};
	parameters.source = "editorIndividual";
	parameters.selectedClass = "";
	parameters.selectedClassResource = null;

	// parameters.parentWindow =
	// window.arguments[0].parentWindow;
	parameters.parentWindow = window;

	window.openDialog("chrome://semantic-turkey/content/editors/class/newClassTree.xul", "_blank",
			"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);

	if (parameters.selectedClassResource != null) {
		var classListBox = document.getElementById("classListBox");
		classListBox.appendItem(parameters.selectedClassResource.getShow(), parameters.selectedClassResource
				.toNT());
		document.documentElement.setAttribute("buttondisabledaccept", "false");
	}
};

art_semanticturkey.classListEditor.removeClassHandler = function(event) {
	var classListBox = document.getElementById("classListBox");

	var ci = classListBox.currentIndex;

	if (ci != -1) {
		classListBox.removeItemAt(ci);

		if (classListBox.getRowCount() == 0) {
			document.documentElement.setAttribute("buttondisabledaccept", "true");
		}
	}
}
art_semanticturkey.classListEditor.dialogAcceptHandlder = function(event) {

	var clsDescriptions = [];

	var classListBox = document.getElementById("classListBox");

	for (var i = 0; i < classListBox.itemCount; i++) {
		clsDescriptions.push(classListBox.getItemAtIndex(i).getAttribute("value"));
	}

	window.arguments[0].clsDescriptions = clsDescriptions;
};

art_semanticturkey.classListEditor.moveUpHandler = function(event) {
	var classListBox = document.getElementById("classListBox");

	var selectedItem = classListBox.selectedItem;
	
	if (selectedItem == null) return;
	
	var previous = selectedItem.previousSibling;
	
	if (previous == null) return;
	
	selectedItem.parentElement.insertBefore(selectedItem, previous);
	
	classListBox.selectItem(selectedItem);
};

art_semanticturkey.classListEditor.moveDownHandler = function(event) {
	var classListBox = document.getElementById("classListBox");

	var selectedItem = classListBox.selectedItem;
	
	if (selectedItem == null) return;
	
	var next = selectedItem.nextSibling;
	
	if (next == null) return;
	
	selectedItem.parentElement.insertBefore(selectedItem, next.nextSibling);
	
	classListBox.selectItem(selectedItem);
};

window.addEventListener("load", art_semanticturkey.classListEditor.init, false);