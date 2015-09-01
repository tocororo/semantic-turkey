if (typeof art_semanticturkey == "undefined") {
	var art_semanticturkey = {};
}

if (typeof art_semanticturkey.datasetMetadataConfigurableDialog == "undefined") {
	art_semanticturkey.datasetMetadataConfigurableDialog = {};
}

Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

Components.utils.import("resource://stservices/SERVICE_MetadataRegistry.jsm", art_semanticturkey);


art_semanticturkey.datasetMetadataConfigurableDialog.init = function() {
	document.addEventListener("dialogaccept", art_semanticturkey.datasetMetadataConfigurableDialog.handleDialogaccept);
	document.addEventListener("dialogcancel", art_semanticturkey.datasetMetadataConfigurableDialog.handleDialogcancel);
	
	var action = art_semanticturkey.datasetMetadataConfigurableDialog.getAction();
	
	try {
		if (action == "add") {
			document.documentElement.setAttribute("title", "Add dataset metadata");
		} else if (action == "edit") {
			document.documentElement.setAttribute("title", "Edit dataset metadata");
			
			var baseURI = (window.arguments[0] && window.arguments[0].baseURI) || null;
			
			if (baseURI == null){
				throw new Error("Missing required parameter 'baseURI'");
			}
			art_semanticturkey.datasetMetadataConfigurableDialog.populateFormWithExistingMetadata(baseURI);
		} else {
			throw Error("Unrecognized value for the parameter 'action': " + action);
		}
	} catch(e) {
		art_semanticturkey.Alert.alert(e);
	}
};

art_semanticturkey.datasetMetadataConfigurableDialog.getAction = function() {
	var action = "add";
	
	if (typeof window.arguments != "undefined") {
		if (typeof window.arguments[0] != "undefined" && typeof window.arguments[0].action != "undefined") {
			action = window.arguments[0].action;
		}
	}

	return action;
};

art_semanticturkey.datasetMetadataConfigurableDialog.populateFormWithExistingMetadata = function(baseURI) {
	var datasetMetadata = art_semanticturkey.STRequests.MetadataRegistry.getDatasetMetadata(baseURI);
	
	var baseURI = datasetMetadata.baseURI;
	var title = datasetMetadata.title;
	var sparqlEndpoint = datasetMetadata.sparqlEndpoint;
	var dereferenceable = datasetMetadata.dereferenceable;

	document.getElementById("baseURITextbox").setAttribute("value", baseURI);
	document.getElementById("titleTextbox").setAttribute("value", title);
	document.getElementById("sparqlEndpointTextbox").setAttribute("value", sparqlEndpoint);
	document.getElementById("dereferenceableCheckbox").setAttribute("checked", dereferenceable);

}

art_semanticturkey.datasetMetadataConfigurableDialog.handleDialogaccept = function(event) {
	var datasetOriginalBaseURI = document.getElementById("baseURITextbox").getAttribute("value"); // this attribute only provides the default (initial) value
	var datasetBaseURI = document.getElementById("baseURITextbox").value; // this property provides the current (updated) value
	var datasetTitle = document.getElementById("titleTextbox").value;
	var datasetSparqlEndpoint = document.getElementById("sparqlEndpointTextbox").value;
	var datasetDereferenceable = document.getElementById("dereferenceableCheckbox").checked;

	var action = art_semanticturkey.datasetMetadataConfigurableDialog.getAction();
	try {
		if (action == "add") {
			art_semanticturkey.STRequests.MetadataRegistry.addDatasetMetadata(datasetBaseURI, datasetTitle, datasetSparqlEndpoint, datasetDereferenceable);
		} else if (action == "edit") {
			art_semanticturkey.STRequests.MetadataRegistry.editDatasetMetadata(datasetOriginalBaseURI, datasetBaseURI, datasetTitle, datasetSparqlEndpoint, datasetDereferenceable);			
		}
	} catch (e) {
		event.preventDefault();
		art_semanticturkey.Alert.alert(e);
		window.focus();
		return;
	}

	if (typeof window.arguments != "undefined" && typeof window.arguments[0] != "undefined") {
		window.arguments[0].out = {
				baseURI : datasetBaseURI.trim(),
				title : datasetTitle.trim() || null
		};
	}
};

art_semanticturkey.datasetMetadataConfigurableDialog.handleDialogcancel = function(event) {
	// Nothing TODO
};

window.addEventListener("load", art_semanticturkey.datasetMetadataConfigurableDialog.init, false);
