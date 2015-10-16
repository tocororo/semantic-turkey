if (typeof art_semanticturkey == "undefined")
	var art_semanticturkey = {};
if (typeof art_semanticturkey.individualPicker == "undefined")
	art_semanticturkey.individualPicker = {};

Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);

art_semanticturkey.individualPicker.init = function() {
	document.getElementById("cancelButton").addEventListener("command", function() {
		window.close();
	}, false);
	document.getElementById("pickExistingButton").addEventListener("command",
			art_semanticturkey.individualPicker.pickExistingHandler, false);

	document.getElementById("classTree").addEventListener(
			"it.uniroma2.art.semanticturkey.event.widget.tree.select",
			art_semanticturkey.individualPicker.selectHandler, false);

	var currentProject = art_semanticturkey.CurrentProject.getProjectName();
	document.getElementById("classTree").projectName = currentProject;
	document.getElementById("instanceList").projectName = currentProject;

	document.getElementById("instanceList").addEventListener("it.uniroma2.art.semanticturkey.event.widget.tree.select", function(event) {
		document.getElementById("pickExistingButton").disabled = (this.selectedInstance == null);
	});

};

art_semanticturkey.individualPicker.selectHandler = function(event) {
	var selectedClassResource = document.getElementById("classTree").selectedClassResource;

	if (selectedClassResource) {
		document.getElementById("instanceList").className = selectedClassResource.getNominalValue();
	} else {
		document.getElementById("instanceList").className = "";
	}
};



art_semanticturkey.individualPicker.pickExistingHandler = function(event) {
	try {
		window.arguments[0].individual = document.getElementById("instanceList").selectedInstance;
		window.arguments[0].individualResource = document.getElementById("instanceList").selectedInstanceResource;
		window.close();
	} catch(e) {
		art_semanticturkey.Alert.alert(e);
		window.close();
	}
};

window.addEventListener("load", art_semanticturkey.individualPicker.init, false);