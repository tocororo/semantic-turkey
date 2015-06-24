if (typeof art_semanticturkey == "undefined") {
	var art_semanticturkey = {};
}

Components.utils.import("resource://stservices/SERVICE_ResourceView.jsm",
		art_semanticturkey);

Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm");

art_semanticturkey.lexicalizationPropertyChooser = {};
art_semanticturkey.lexicalizationPropertyChooser.loadHandler = function(event) {
	try {
		var resource = window.arguments[0].resource;

		if (typeof resource == "undefined") {
			throw Error("Missing argument \"resource\"");
		}

		window.addEventListener("dialogaccept",
				art_semanticturkey.lexicalizationPropertyChooser.acceptHander,
				false);
		document.getElementById("checkAll").addEventListener("command",
				art_semanticturkey.lexicalizationPropertyChooser.visualizeTree,
				false);

		art_semanticturkey.lexicalizationPropertyChooser.visualizeTree();
	} catch (e) {
		close();
		throw e;
	}
};

art_semanticturkey.lexicalizationPropertyChooser.visualizeTree = function() {
	var allPropertiesRequired = document.getElementById("checkAll").checked;

	var lexicalizationProperties = art_semanticturkey.STRequests.ResourceView
			.getLexicalizationProperties(allPropertiesRequired ? undefined
					: window.arguments[0].resource);

	var lexicalizationPropertiesListBox = document.createElement("box");
	lexicalizationPropertiesListBox.rdfResourcesArray = lexicalizationProperties;
	lexicalizationPropertiesListBox.forceList = true;
	lexicalizationPropertiesListBox.addRemoveButton = false;
	lexicalizationPropertiesListBox.classList.add("object-list-widget");

	var lexicalizationPropertiesContainer = document
			.getElementById("lexicalizationPropertiesContainer");
	while (lexicalizationPropertiesContainer.lastChild != null) {
		lexicalizationPropertiesContainer
				.removeChild(lexicalizationPropertiesContainer.lastChild);
	}

	lexicalizationPropertiesContainer
			.appendChild(lexicalizationPropertiesListBox);
	
	window.sizeToContent();
}

art_semanticturkey.lexicalizationPropertyChooser.acceptHander = function(event) {
	window.arguments[0].out.chosenProperty = document.getElementById(
			"lexicalizationPropertiesContainer").children[0].getSelectedRDFResource();
};

window.addEventListener("load",
		art_semanticturkey.lexicalizationPropertyChooser.loadHandler, false);