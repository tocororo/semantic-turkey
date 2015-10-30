if (typeof art_semanticturkey == "undefined")
	var art_semanticturkey = {};
if (typeof art_semanticturkey.individualListEditor == "undefined")
	art_semanticturkey.individualListEditor = {};

Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);

art_semanticturkey.individualListEditor.init = function() {

	document.getElementById("classTree").addEventListener(
			"it.uniroma2.art.semanticturkey.event.widget.tree.select",
			art_semanticturkey.individualListEditor.classSelectionHandler, false);

	var currentProject = art_semanticturkey.CurrentProject.getProjectName();
	document.getElementById("classTree").projectName = currentProject;
	document.getElementById("classInstanceList").projectName = currentProject;

	document.getElementById("classInstanceList").addEventListener(
			"it.uniroma2.art.semanticturkey.event.widget.tree.select",
			art_semanticturkey.individualListEditor.classInstanceSelectionHandler);

	document.getElementById("individualList").addEventListener("select",
			art_semanticturkey.individualListEditor.individualSelectionHandler);

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

art_semanticturkey.individualListEditor.classSelectionHandler = function(event) {
	var selectedClassResource = event.target.selectedClassResource;

	if (selectedClassResource) {
		document.getElementById("classInstanceList").className = selectedClassResource.getNominalValue();
	} else {
		document.getElementById("classInstanceList").className = "";
	}
};

art_semanticturkey.individualListEditor.classInstanceSelectionHandler = function(event) {
	var isDisabled = (event.target.selectedInstance == null);
	document.getElementById("addIndividualButton").disabled = isDisabled;
	document.getElementById("removeIndividualButton").disabled = isDisabled;
};

art_semanticturkey.individualListEditor.classInstanceSelectionHandler = function(event) {
	var isDisabled = (event.target.selectedInstance == null);
	document.getElementById("addIndividualButton").disabled = isDisabled;
};

art_semanticturkey.individualListEditor.individualSelectionHandler = function(event) {
	var isDisabled = (event.target.currentIndex == -1);
	document.getElementById("removeIndividualButton").disabled = isDisabled;

	if (isDisabled) {
		document.getElementById("moveUpButton").setAttribute("disabled", "true");
		document.getElementById("moveDownButton").setAttribute("disabled", "true");
	} else {
		document.getElementById("moveUpButton").setAttribute("disabled",
				event.target.currentIndex == 0 ? "true" : "false");
		document.getElementById("moveDownButton").setAttribute("disabled",
				event.target.currentIndex == event.target.view.rowCount - 1 ? "true" : "false");
	}
};

art_semanticturkey.individualListEditor.addIndividualHandler = function(event) {
	var selectedInstance = document.getElementById("classInstanceList").selectedInstanceResource;
	var individualList = document.getElementById("individualList");

	var treeItem = document.createElement("treeitem");
	var treeRow = document.createElement("treerow");
	var treeCell = document.createElement("treecell");

	treeCell.setAttribute("label", selectedInstance.getShow());
	treeCell.setAttribute("value", selectedInstance.getNominalValue());

	treeRow.appendChild(treeCell);
	treeItem.appendChild(treeRow);

	individualList.getElementsByTagName("treechildren")[0].appendChild(treeItem);

	individualList.boxObject.ensureRowIsVisible(individualList.view.rowCount - 1);
	individualList.view.selection.select(individualList.view.rowCount - 1);

	document.documentElement.setAttribute("buttondisabledaccept", "false");
};

art_semanticturkey.individualListEditor.removeIndividualHandler = function(event) {
	var individualList = document.getElementById("individualList");

	var ci = individualList.currentIndex;

	if (ci != -1) {
		var item = individualList.view.getItemAtIndex(ci);

		item.parentElement.removeChild(item);

		var newCurrent = ci < individualList.view.rowCount ? ci : individualList.view.rowCount - 1;

		if (newCurrent == -1) {
			individualList.view.selection.clearSelection();
		} else {
			individualList.boxObject.ensureRowIsVisible(newCurrent);
			individualList.view.selection.select(newCurrent);
		}
		
		document.documentElement.setAttribute("buttondisabledaccept", individualList.view.rowCount == 0 ? "true"
				: "false");
	}
}
art_semanticturkey.individualListEditor.dialogAcceptHandlder = function(event) {

	var individuals = [];

	var individualList = document.getElementById("individualList");

	for (var i = 0; i < individualList.view.rowCount; i++) {
		individuals.push(individualList.view.getItemAtIndex(i).getElementsByTagName("treecell")[0]
				.getAttribute("value"));
	}

	window.arguments[0].individuals = individuals;
};

art_semanticturkey.individualListEditor.moveUpHandler = function(event) {
	var individualList = document.getElementById("individualList");

	var currentIndex = individualList.currentIndex;

	if (currentIndex == -1) {
		return;
	}

	var selectedItem = individualList.view.getItemAtIndex(currentIndex);

	var previous = selectedItem.previousSibling;

	if (previous == null)
		return;

	selectedItem.parentElement.insertBefore(selectedItem, previous);

	individualList.boxObject.ensureRowIsVisible(currentIndex - 1);
	individualList.view.selection.select(currentIndex - 1);
};

art_semanticturkey.individualListEditor.moveDownHandler = function(event) {
	var individualList = document.getElementById("individualList");

	var currentIndex = individualList.currentIndex;

	if (currentIndex == -1) {
		return;
	}

	var selectedItem = individualList.view.getItemAtIndex(currentIndex);

	var next = selectedItem.nextSibling;

	if (next == null)
		return;

	selectedItem.parentElement.insertBefore(selectedItem, next.nextSibling);

	individualList.boxObject.ensureRowIsVisible(currentIndex + 1);
	individualList.view.selection.select(currentIndex + 1);
};

window.addEventListener("load", art_semanticturkey.individualListEditor.init, false);