if (typeof art_semanticturkey == "undefined")
	var art_semanticturkey = {};
if (typeof art_semanticturkey.individualListEditor == "undefined")
	art_semanticturkey.individualListEditor = {};

Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

art_semanticturkey.individualListEditor.init = function() {

	document.getElementById("addIndividualButton").addEventListener("command",
			art_semanticturkey.individualListEditor.addIndividualHandler, true);
	document.getElementById("removeIndividualButton").addEventListener("command",
			art_semanticturkey.individualListEditor.removeIndividualHandler, true);
	document.getElementById("moveUpButton").addEventListener("command",
			art_semanticturkey.individualListEditor.moveUpHandler, true);
	document.getElementById("moveDownButton").addEventListener("command",
			art_semanticturkey.individualListEditor.moveDownHandler, true);

	document.addEventListener("dialogaccept", art_semanticturkey.individualListEditor.dialogAcceptHandlder,
			true);
};

art_semanticturkey.individualListEditor.addIndividualHandler = function(event) {
	var parameters = {
		individualResource : null
	};
	window.openDialog("chrome://semantic-turkey/content/editors/individual/individualPicker.xul", "_blank",
			"chrome=yes,dialog,resizable=yes,modal,centerscreen", parameters);

	if (!!parameters.individualResource) {
		document.getElementById("individualListBox").appendItem(parameters.individualResource.getShow(),
				parameters.individualResource.getNominalValue());
		document.documentElement.setAttribute("buttondisabledaccept", "false");
	}
};

art_semanticturkey.individualListEditor.removeIndividualHandler = function(event) {
	var individualListBox = document.getElementById("individualListBox");

	var ci = individualListBox.currentIndex;

	if (ci != -1) {
		individualListBox.removeItemAt(ci);

		if (individualListBox.getRowCount() == 0) {
			document.documentElement.setAttribute("buttondisabledaccept", "true");
		}
	}
}
art_semanticturkey.individualListEditor.dialogAcceptHandlder = function(event) {

	var individuals = [];

	var individualListBox = document.getElementById("individualListBox");

	for (var i = 0; i < individualListBox.itemCount; i++) {
		individuals.push(individualListBox.getItemAtIndex(i).getAttribute("value"));
	}

	window.arguments[0].individuals = individuals;
};

art_semanticturkey.individualListEditor.moveUpHandler = function(event) {
	var individualListBox = document.getElementById("individualListBox");

	var selectedItem = individualListBox.selectedItem;

	if (selectedItem == null)
		return;

	var previous = selectedItem.previousSibling;

	if (previous == null)
		return;

	selectedItem.parentElement.insertBefore(selectedItem, previous);

	individualListBox.selectItem(selectedItem);
};

art_semanticturkey.individualListEditor.moveDownHandler = function(event) {
	var individualListBox = document.getElementById("individualListBox");

	var selectedItem = individualListBox.selectedItem;

	if (selectedItem == null)
		return;

	var next = selectedItem.nextSibling;

	if (next == null)
		return;

	selectedItem.parentElement.insertBefore(selectedItem, next.nextSibling);

	individualListBox.selectItem(selectedItem);
};

window.addEventListener("load", art_semanticturkey.individualListEditor.init, false);