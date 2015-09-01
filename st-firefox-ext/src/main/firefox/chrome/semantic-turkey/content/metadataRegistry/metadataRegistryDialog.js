if (typeof art_semanticturkey == "undefined") {
	var art_semanticturkey = {};
}

if (typeof art_semanticturkey.datasetMetadataRepositoryDialog == "undefined") {
	art_semanticturkey.datasetMetadataRepositoryDialog = {};
}

Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

Components.utils.import("resource://stservices/SERVICE_MetadataRegistry.jsm", art_semanticturkey);


art_semanticturkey.datasetMetadataRepositoryDialog.init = function() {
	var closeButton = document.getElementById("closeButton");
	closeButton.addEventListener("command", art_semanticturkey.datasetMetadataRepositoryDialog.closeDialog);
	
	var addDatasetMetadataButton = document.getElementById("addDatasetMetadataButton");
	addDatasetMetadataButton.addEventListener("command", art_semanticturkey.datasetMetadataRepositoryDialog.addDatasetMetadataButtonCommandEventHandler);

	var editDatasetMetadataButton = document.getElementById("editDatasetMetadataButton");
	editDatasetMetadataButton.addEventListener("command", art_semanticturkey.datasetMetadataRepositoryDialog.editDatasetMetadataButtonCommandEventHandler);

	var deleteDatasetMetadataButton = document.getElementById("deleteDatasetMetadataButton");
	deleteDatasetMetadataButton.addEventListener("command", art_semanticturkey.datasetMetadataRepositoryDialog.deleteDatasetMetadataButtonCommandEventHandler);

	var datasetList = document.getElementById("datasetList");
	datasetList.addEventListener("select", art_semanticturkey.datasetMetadataRepositoryDialog.listSelectEventHandler);
	
	try {
		var datasetArray = art_semanticturkey.STRequests.MetadataRegistry.listDatasets();
		datasetArray.sort(function(a,b){
			var ta = null;
			
			if (a.title !== null) {
				ta = a.title.toLowerCase();
			}
			
			var tb = null;
			
			if (b.title !== null) {
				tb = b.title.toLowerCase();
			}
			
			if (ta === tb) {
				return 0;
			} else if (ta === null) {
				return -1;
			} else if (tb === null) {
				 return +1; 
			} else {
				 return ta < tb ? -1 : +1;
			}
		});
		
		art_semanticturkey.datasetMetadataRepositoryDialog.populateDialog(datasetArray);
	} catch(e) {
		art_semanticturkey.Alert.alert(e);
	}
};

art_semanticturkey.datasetMetadataRepositoryDialog.populateDialog = function (datasetArray) {
	var datasetList = document.getElementById("datasetList");
	
	while (datasetList.firstChild) {
		datasetList.removeChild(datasetList.firstChild);
	}
	
	for (var i = 0 ; i < datasetArray.length ; i++) {
		var ds = datasetArray[i];
		
		art_semanticturkey.datasetMetadataRepositoryDialog.insertDatasetItem(datasetList, ds);		
	}
};

art_semanticturkey.datasetMetadataRepositoryDialog.insertDatasetItem = function(datasetListElement, dataset, referenceNode) {
	if (typeof referenceNode == "undefined") {
		referenceNode = null;
	}
	var listItem = datasetListElement.ownerDocument.createElement("listitem");
	listItem.setAttribute("value", dataset.baseURI);
	
	var vbox = datasetListElement.ownerDocument.createElement("vbox");
	
	var datasetTitle = datasetListElement.ownerDocument.createElement("label");
	datasetTitle.classList.add("datasetTitle");
	
	if (dataset.title === null) {
		datasetTitle.setAttribute("value", "Untitled");
		datasetTitle.classList.add("nullProperty")
	} else {
		datasetTitle.setAttribute("value", dataset.title);		
	}
	
	var datasetBaseURI = datasetListElement.ownerDocument.createElement("label");
	datasetBaseURI.classList.add("datasetBaseURI");
	datasetBaseURI.setAttribute("value", dataset.baseURI);
	
	vbox.appendChild(datasetTitle);
	vbox.appendChild(datasetBaseURI);

	listItem.appendChild(vbox);
	
	datasetListElement.insertBefore(listItem, referenceNode);
	
	return listItem;
};

art_semanticturkey.datasetMetadataRepositoryDialog.listSelectEventHandler = function(event) {
	var isAnythingSelected = (event.target.selectedItem != null);
	document.getElementById("editDatasetMetadataButton").disabled = false;
	document.getElementById("deleteDatasetMetadataButton").disabled = false;
};


art_semanticturkey.datasetMetadataRepositoryDialog.addDatasetMetadataButtonCommandEventHandler = function(event) {
	var datasetList = document.getElementById("datasetList");

	var parameters = {
			action : "add"
	};
	
	window.openDialog("chrome://semantic-turkey/content/metadataRegistry/datasetMetadataConfigurableDialog.xul",
			"_blank", "modal=yes,resizable,centerscreen", parameters);
	
	if (typeof parameters.out != "undefined") {
		var datasetList = document.getElementById("datasetList");
		
		var left = 0;
		var right = datasetList.itemCount;
				
		while (left < right) {
			var mid = (left + right) >> 1;
						
			var item = datasetList.getItemAtIndex(mid);
			
			var itemTitle = item.getElementsByTagName("label")[0].getAttribute("value");
						
			
			if (itemTitle.toLowerCase() == parameters.out.title.toLowerCase()) {
				right = mid;
			} else if (itemTitle.toLowerCase() > parameters.out.title.toLowerCase()) {
				right = mid
			} else {
				left = mid + 1;
			}
			
		}
		
		var referenceNode = right < datasetList.itemCount ? datasetList.getItemAtIndex(right) : null ;
		var newItem = art_semanticturkey.datasetMetadataRepositoryDialog.insertDatasetItem(datasetList, parameters.out, referenceNode);
		
		window.setTimeout(function(){
			datasetList.selectItem(newItem);
			datasetList.ensureElementIsVisible(newItem);
		}, 0);
	}
};

art_semanticturkey.datasetMetadataRepositoryDialog.editDatasetMetadataButtonCommandEventHandler = function(event) {
	var datasetList = document.getElementById("datasetList");

	var parameters = {
			action : "edit",
			baseURI : datasetList.selectedItem.value
	};
	
	window.openDialog("chrome://semantic-turkey/content/metadataRegistry/datasetMetadataConfigurableDialog.xul",
			"_blank", "modal=yes,resizable,centerscreen", parameters);
	
	if (typeof parameters.out != "undefined") {
		var datasetList = document.getElementById("datasetList");
		var items = datasetList.getElementsByTagName("listitem");
		
		var datasetRenamed = (parameters.baseURI != parameters.out.baseURI);
		
		for (var i = 0 ; i < items.length ; i++) {
			if (items[i].value == parameters.baseURI) {
				if (datasetRenamed) {
					items[i].value = parameters.out.baseURI;
				}
				var labels = items[i].getElementsByTagName("label");
				
				for (var j = 0 ; j < labels.length ; j++) {
					if (labels[j].classList.contains("datasetTitle")) {
						if (parameters.out.title === null) {
							labels[j].setAttribute("value", "Untitled");
							labels[j].classList.add("nullProperty");
						} else {
							labels[j].setAttribute("value", parameters.out.title.trim());
							labels[j].classList.remove("nullProperty");
						}
					}
					
					if (labels[j].classList.contains("datasetBaseURI") && datasetRenamed) {
						labels[j].setAttribute("value", parameters.out.baseURI);					
					}
				}
			}
		}
	}
};

art_semanticturkey.datasetMetadataRepositoryDialog.deleteDatasetMetadataButtonCommandEventHandler = function(event) {
	var datasetList = document.getElementById("datasetList");

	var selectedDatasetItem = datasetList.selectedItem;
	
	art_semanticturkey.STRequests.MetadataRegistry.deleteDatasetMetadata(selectedDatasetItem.value);
	selectedDatasetItem.parentElement.removeChild(selectedDatasetItem);
};

art_semanticturkey.datasetMetadataRepositoryDialog.closeDialog = function() {
	window.close();
};

window.addEventListener("load", art_semanticturkey.datasetMetadataRepositoryDialog.init, false);
