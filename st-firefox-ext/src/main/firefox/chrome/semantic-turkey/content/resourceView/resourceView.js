if (typeof art_semanticturkey == "undefined") {
	var art_semanticturkey = {};
}

Components.utils.import("resource://stservices/SERVICE_Resource.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_ResourceView.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Deserializer.jsm", art_semanticturkey);

if (typeof art_semanticturkey.resourceView == "undefined") {
	art_semanticturkey.resourceView = {};
}

art_semanticturkey.resourceView.init = function() {
	var parametersFromURL = art_semanticturkey.resourceView.utils.parseQueryString(window.location.search);

	var resource = parametersFromURL.resource;

	if (typeof resource != "string") {
		alert("Missing argument for mandatory parameter \"resource\"");
	}

	try {
		var response = art_semanticturkey.STRequests.ResourceView.getResourceView(resource);
	} catch (e) {
		alert(e.message);
		return;
	}

	var resourceViewBox = document.getElementById("resourceViewBox");
	art_semanticturkey.resourceView.getResourceView_RESPONSE(response);
};

art_semanticturkey.resourceView.refreshView = function() {
	var resourceNameElement = document.getElementById("resourceNameBox");
	
	try {
		var response = art_semanticturkey.STRequests.ResourceView.getResourceView(resourceNameElement.value);
	} catch (e) {
		alert(e.message);
		return;
	}

	var resourceViewBox = document.getElementById("resourceViewBox");
	art_semanticturkey.resourceView.getResourceView_RESPONSE(response);
};

art_semanticturkey.resourceView.getResourceView_RESPONSE = function(response) {
	// -----------------------------
	// Populates the resource header

	// Handles the resource name (resource name box)
	var responseDataElement = response.getElementsByTagName("data")[0];
	var responseResourceElement = responseDataElement.getElementsByTagName("resource")[0];

	var resourceObj = art_semanticturkey.Deserializer.createRDFNode(responseResourceElement.children[0]);

	var resourceNameElement = document.getElementById("resourceNameBox");
	resourceNameElement.value = resourceObj.getNominalValue();

	// Handles the resource editability (rename button)
	var isResourceEditable = !(resourceObj.explicit == "false");

	var renameResourceButton = document.getElementById("renameResourceButton");
	renameResourceButton.disabled = !isResourceEditable;

	// ----------------------
	// Handles the partitions

	// Specially handled partitions
	var excludedPartitions = [ "resource" ];

	var responsePartitions = responseDataElement.children;

	var partitionsBox = document.getElementById("partitionsBox");
	while (partitionsBox.firstChild) {
		partitionsBox.removeChild(partitionsBox.firstChild);
	}
	
	// For each partition
	for (var i = 0; i < responsePartitions.length; i++) {	
		var responsePartition = responsePartitions[i];
		var responsePartitionTagName = responsePartition.tagName;

		if (excludedPartitions.indexOf(responsePartitionTagName) != -1)
			continue;
		
		art_semanticturkey.resourceView.renderPartition(resourceObj, responsePartition, partitionsBox);
	}

};

art_semanticturkey.resourceView.renderPartition = function(subjectResource, responsePartition, partitionsBox) {
	var partitionName = responsePartition.tagName;	
	var partitionHandler = partition2handlerMap[partitionName];

	var renderFunction = art_semanticturkey.resourceView.getPropertyPathValue(partitionHandler, "render") || art_semanticturkey.resourceView.renderPartitionDefault;
	renderFunction(subjectResource, responsePartition, partitionsBox);
};

art_semanticturkey.resourceView.renderPartitionDefault = function(subjectResource, responsePartition, partitionsBox) {
	
	var partitionName = responsePartition.tagName;
	
	var partitionHandler = partition2handlerMap[partitionName];

	var partitionLabel = art_semanticturkey.resourceView.getPropertyPathValue(partitionHandler, "partitionLabel") || partitionName;
	
	// Handles the partition header
	var partitionGroupBox = document.createElement("groupbox");
	var partitionCaption = document.createElement("caption");
	partitionCaption.setAttribute("label", partitionLabel);
	partitionGroupBox.appendChild(partitionCaption);

	// Handles the partition content

	var predicateObjectsList = art_semanticturkey.Deserializer
			.createPredicateObjectList(responsePartition.children[0]);

	for (var j = 0; j < predicateObjectsList.length; j++) {
		var po = predicateObjectsList[j];
		var predicateObjectsBox = document.createElement("box");
		predicateObjectsBox.rdfSubject = subjectResource;
		predicateObjectsBox.showSubjInGUI = false;
		predicateObjectsBox.rdfPredicate = po.getPredicate();
		predicateObjectsBox.rdfResourcesArray = po.getObjects();
		predicateObjectsBox.operations = "add;remove";
		predicateObjectsBox.classList.add("predicate-objects-widget");

		predicateObjectsBox.addEventListener("rdfnodeBaseEvent",
				art_semanticturkey.resourceView.dblClickHandler);
		predicateObjectsBox.addEventListener("predicateObjectsEvent",
				art_semanticturkey.resourceView.predicateObjectsEventHandler);

		predicateObjectsBox.setAttribute("st-partitionName", partitionName);

		partitionGroupBox.appendChild(predicateObjectsBox);
	}
	
	// Append the partition
	partitionsBox.appendChild(partitionGroupBox);	
};

art_semanticturkey.ARTPredicateObjects = function(predicate, objects) {

	this.getPredicate = function() {
		return predicate;
	};

	this.getObjects = function() {
		return objects;
	};

};

art_semanticturkey.Deserializer.createPredicateObjectList = function(element) {
	if (element.tagName != "collection") {
		new Error("Not a collection");
	}

	var elements = element.children;

	var result = [];

	for (var i = 0; i < elements.length; i++) {
		var el = elements[i];

		if (el.tagName != "predicateObjects")
			continue;

		var predicate = art_semanticturkey.Deserializer
				.createRDFNode(el.getElementsByTagName("predicate")[0].children[0]);
		var objects = art_semanticturkey.Deserializer.createRDFArray(el.getElementsByTagName("objects")[0]);

		var predicateObjects = new art_semanticturkey.ARTPredicateObjects(predicate, objects);
		result.push(predicateObjects);
	}

	return result;
};

art_semanticturkey.resourceView.dblClickHandler = function(event) {
	var res = event.detail.rdfResource;

	if (res.isResource()) {
		art_semanticturkey.resourceView.utils.openResourceView(res.getNominalValue());
	}
};

art_semanticturkey.resourceView.predicateObjectsEventHandler = function(event) {
	var partitionName = event.target.getAttribute("st-partitionName");
	var rdfSubject = event.detail.rdfSubject;
	var rdfPredicate = event.detail.rdfPredicate;
	var rdfObject = event.detail.rdfObject;
	var button = art_semanticturkey.resourceView.getButtonType(event.detail.button);

	var partitionHandler = partition2handlerMap[partitionName];

	if (typeof partitionHandler == "undefined") {
		alert("Unhandler operation");
		return;
	}

	var handler = partitionHandler[button];

	if (typeof handler == "undefined") {
		alert("Unhandled operation");
		return;
	}

	try {
		handler(rdfSubject, rdfPredicate, rdfObject);
	} catch (e) {
		alert(e.message);
	}
};

var partition2handlerMap = {
	"lexicalizations" : {
		"partitionLabel" : "Lexicalizations",
		"add" : function(rdfSubject, rdfPredicate, rdfObject) {
			if (typeof rdfPredicate != "undefined") {
				var predURI = rdfPredicate.getURI();

				const
				supportedProps = [ "http://www.w3.org/2000/01/rdf-schema#label",
						"http://www.w3.org/2004/02/skos/core#prefLabel",
						"http://www.w3.org/2004/02/skos/core#altLabel",
						"http://www.w3.org/2004/02/skos/core#hiddenLabel",
						"http://www.w3.org/2008/05/skos-xl#prefLabel",
						"http://www.w3.org/2008/05/skos-xl#altLabel",
						"http://www.w3.org/2008/05/skos-xl#hiddenLabel" ];

				var parameters = {};

				parameters.winTitle = "Add " + rdfPredicate.getShow() + " Lexicalization";
				parameters.action = function(label, lang) {

					art_semanticturkey.resourceView.refreshView();
				};
				parameters.oncancel = false;

				if (typeof window.arguments != "undefined" && typeof window.arguments[0] != "undefined") {
					parameters.skos = window.arguments[0].skos;
				}

				if (supportedProps.indexOf(predURI) != -1) {
					window
							.openDialog(
									"chrome://semantic-turkey/content/enrichProperty/enrichPlainLiteralRangedProperty.xul",
									"_blank", "modal=yes,resizable,centerscreen", parameters);
				} else {
					alert("Unsupported predicate type");
				}
			} else {
				alert("Choose lexicalization property");
			}
		},
		"remove" : function(rdfSubject, rdfPredicate, rdfObject) {
			alert("Remove lexicalization");
		}
	},
	"properties" : {
		"partitionLabel" : "Properties",
//      "render" : function(subjectResource, responsePartition, partitionsBox) {},
		"add" : function(rdfSubject, rdfPredicate, rdfObject) {
			alert("Add property");
		},
		"remove" : function(rdfSubject, rdfPredicate, rdfObject) {
			art_semanticturkey.STRequests.Resource.removePropertyValue(rdfSubject.toNT(), rdfPredicate.toNT(), rdfObject.toNT());
		}
	}
};

art_semanticturkey.resourceView.getButtonType = function(button) {
	return button.getAttribute("label");
};

art_semanticturkey.resourceView.getPropertyPathValue = function(object) {
	var temp = object;
	
	for (var i = 1 ; i < arguments.length ; i++) {
		if (typeof temp == "undefined") return;
		
		temp = temp[arguments[i]];
	}
	
	return temp;
};

window.addEventListener("load", art_semanticturkey.resourceView.init, true);