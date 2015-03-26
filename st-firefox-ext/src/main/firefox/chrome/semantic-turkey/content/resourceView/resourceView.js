if (typeof art_semanticturkey == "undefined") {
	var art_semanticturkey = {};
}

Components.utils.import("resource://stservices/SERVICE_Property.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Cls.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Individual.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Resource.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOSXL.jsm", art_semanticturkey);

Components.utils.import("resource://stservices/SERVICE_ResourceView.jsm", art_semanticturkey);

Components.utils.import("resource://stmodules/Deserializer.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ARTResources.jsm", art_semanticturkey);

Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/STResUtils.jsm", art_semanticturkey);

if (typeof art_semanticturkey.resourceView == "undefined") {
	art_semanticturkey.resourceView = {};
}

art_semanticturkey.resourceView.init = function() {
	// --------------------
	// Parses the arguments

	var parametersFromURL = art_semanticturkey.resourceView.utils.parseQueryString(window.location.search);

	var resource = parametersFromURL.resource;

	if (typeof resource != "string") {
		art_semanticturkey.Alert.alert("Missing argument for mandatory parameter \"resource\"");
		window.close();
		return;
	}

	// --------------------------
	// Initializes basic behavior

	var resourceNameBox = document.getElementById("resourceNameBox");

	var renameResourceButton = document.getElementById("renameResourceButton");
	renameResourceButton.addEventListener("command", art_semanticturkey.resourceView.doRenameResource, false);

	var resourceViewBox = document.getElementById("resourceViewBox");

	var eventListenerArrayObject = new art_semanticturkey.eventListenerArrayClass();

	eventListenerArrayObject.addEventListenerToArrayAndRegister("resourceRenamed", function(eventId,
			resourceRenamedObj) {
		if (resourceRenamedObj.getOldName() == resourceNameBox.stRdfNode.getNominalValue()) {
			art_semanticturkey.resourceView.refreshView(resourceRenamedObj.getNewName());
		}
	}, null);

	eventListenerArrayObject.addEventListenerToArrayAndRegister("refreshEditor", function(eventId, eventObj) {
		art_semanticturkey.resourceView.refreshView();
	}, null);

	window.addEventListener("unload", function() {
		eventListenerArrayObject.deregisterAllListener();
	}, true);

	window.addEventListener("rdfnodeBaseEvent",
			art_semanticturkey.resourceView.partitions.internal.dblClickHandler, true);

	// -------------------------------------------------
	// Request and rendering of the resource description

	try {
		var response = art_semanticturkey.STRequests.ResourceView.getResourceView(resource);
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
		window.close();
		return;
	}

	art_semanticturkey.resourceView.getResourceView_RESPONSE(response);
};

art_semanticturkey.resourceView.isEditable = function(subjectResource) {
	return !(subjectResource.explicit == "false");
}

art_semanticturkey.resourceView.refreshView = function(newName) {
	var resourceNameBox = document.getElementById("resourceNameBox");
	var resourceName = newName || resourceNameBox.stRdfNode.getNominalValue();

	art_semanticturkey.resourceView.utils.openResourceView(resourceName, window);
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
	resourceNameElement.stRdfNode = resourceObj;

	// Handles the resource editability (rename button)
	var isResourceEditable = art_semanticturkey.resourceView.isEditable(resourceObj);

	var renameResourceButton = document.getElementById("renameResourceButton");
	renameResourceButton.disabled = !(isResourceEditable && resourceObj.isURIResource());

	// ----------------------
	// Handles the partitions

	var partitionsBox = document.getElementById("partitionsBox");
	while (partitionsBox.firstChild) {
		partitionsBox.removeChild(partitionsBox.firstChild);
	}

	// Specially handled partitions
	var excludedPartitions = [ "resource" ];

	var responsePartitions = responseDataElement.children;

	// For each partition
	for (var i = 0; i < responsePartitions.length; i++) {
		var responsePartition = responsePartitions[i];
		var responsePartitionTagName = responsePartition.tagName;

		if (excludedPartitions.indexOf(responsePartitionTagName) != -1)
			continue;

		var partitionHandler = art_semanticturkey.resourceView.partitions
				.getPartitionHandler(responsePartition.tagName);
		partitionHandler.render(resourceObj, responsePartition, partitionsBox);
	}

};

art_semanticturkey.resourceView.doRenameResource = function(event) {

	try {
		var resourceNameBox = document.getElementById("resourceNameBox");

		var parameters = {};
		parameters.currentName = resourceNameBox.stRdfNode.getNominalValue();

		window.openDialog("renameDialog/renameDialog.xul", "dlg",
				"chrome=yes,dialog,resizable=yes,modal,centerscreen", parameters);
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
};

art_semanticturkey.resourceView.partitions = {};

art_semanticturkey.resourceView.partitions.registerPartitionHandler = function(partitionName, pojo) {
	art_semanticturkey.resourceView.partitions.internal.partition2handlerMap[partitionName] = art_semanticturkey.resourceView.partitions.internal
			.wrapPojoHandler(partitionName, pojo);
};
art_semanticturkey.resourceView.partitions.getPartitionHandler = function(partitionName) {
	return art_semanticturkey.resourceView.partitions.internal.partition2handlerMap[partitionName]
			|| art_semanticturkey.resourceView.partitions.internal.wrapPojoHandler(partitionName, {});
};

art_semanticturkey.resourceView.partitions.internal = {};
art_semanticturkey.resourceView.partitions.internal.partition2handlerMap = {};
art_semanticturkey.resourceView.partitions.internal.wrapPojoHandler = function(partitionName, pojo) {
	var obj = Object.create(pojo);

	if (typeof obj.partitionLabel == "undefined") {
		obj.partitionLabel = partitionName;
	}

	if (typeof obj.render == "undefined") {
		obj.render = art_semanticturkey.resourceView.partitions.internal.defaultPartitionRender;
	}
	
	if (typeof obj.addTooltiptext == "undefined") {
		obj.addTooltiptext = "Add";
	}

	if (typeof obj.addIcon == "undefined") {
		var stereotypicalRole;
		
		if (typeof obj["addIcon|fromRole"] != "undefined") {
			stereotypicalRole = obj["addIcon|fromRole"];
		} else {
			stereotypicalRole = obj.expectedContentType == "predicateObjectsList" ? "property" : "individual";
		}
		
		obj.addIcon = art_semanticturkey.STResUtils.getImageSrc(new art_semanticturkey.ARTURIResource("foo", stereotypicalRole, "http://foo.it"), "add")
	}

	return obj;
};
art_semanticturkey.resourceView.partitions.internal.defaultPartitionRender = function(subjectResource,
		responsePartition, partitionsBox) {

	var partitionName = responsePartition.tagName;

	var editable = art_semanticturkey.resourceView.isEditable(subjectResource);

	var addSupported = (typeof this["onAdd"] != "undefined");
	var removeSupported = (typeof this["onRemove"] != "undefined");

	// Handles the partition header
	var partitionGroupBox = document.createElement("groupbox");
	var partitionCaption = document.createElement("caption");

	var partitionLabelElement = document.createElement("label");
	partitionLabelElement.setAttribute("value", this.partitionLabel);
	partitionCaption.appendChild(partitionLabelElement);

	var partitionButton = null;
	var operations = [];

	if (addSupported) {
		partitionButton = document.createElement("toolbarbutton");
		partitionButton.setAttribute("tooltiptext", this.addTooltiptext);
		partitionButton.setAttribute("disabled", "" + (!editable));
		partitionButton.setAttribute("image", this.addIcon);
		partitionCaption.appendChild(partitionButton);
		partitionButton.setAttribute("st-partitionName", partitionName);
		operations.push("add");
	}

	if (removeSupported) {
		operations.push("remove");
	}
	partitionGroupBox.appendChild(partitionCaption);

	// Handles the partition content

	var partitionContent = responsePartition.children[0];

	if (typeof partitionContent != "undefined") {

		// A predicateObjectsList
		if ((this.expectedContentType == "predicateObjectsList")
				|| art_semanticturkey.resourceView.partitions.internal
						.isPredicateObjectsList(partitionContent)) {
			if (addSupported) {
				partitionButton.addEventListener("command", this["onAdd"].bind(this, subjectResource,
						undefined, undefined), true);
			}

			var predicateObjectsList = art_semanticturkey.Deserializer
					.createPredicateObjectsList(partitionContent);

			for (var j = 0; j < predicateObjectsList.length; j++) {
				var po = predicateObjectsList[j];
				var predicateObjectsBox = document.createElement("box");
				predicateObjectsBox.rdfSubject = subjectResource;
				predicateObjectsBox.showSubjInGUI = false;
				predicateObjectsBox.rdfPredicate = po.getPredicate();
				predicateObjectsBox.rdfResourcesArray = po.getObjects();
				predicateObjectsBox.operations = operations.join(";");
				predicateObjectsBox.classList.add("predicate-objects-widget");

				predicateObjectsBox.addEventListener("predicateObjectsEvent",
						art_semanticturkey.resourceView.partitions.internal.predicateObjectsEventHandler);

				predicateObjectsBox.setAttribute("st-partitionName", partitionName);

				partitionGroupBox.appendChild(predicateObjectsBox);
			}
		} else { // Otherwise, assume an objects list
			if (addSupported) {
				partitionButton.addEventListener("command", this["onAdd"].bind(this, subjectResource), true);
			}

			var objects = art_semanticturkey.Deserializer.createRDFArray(responsePartition);
			var objectListBox = document.createElement("box");
			objectListBox.rdfResourcesArray = objects;
			objectListBox.addRemoveButton = removeSupported;
			objectListBox.operations = operations.join(";");
			objectListBox.classList.add("object-list-widget");

			objectListBox.addEventListener("rdfnodeContainerEvent",
					art_semanticturkey.resourceView.partitions.internal.objectListEventHandler);
			objectListBox.addEventListener("objectListEvent",
					art_semanticturkey.resourceView.partitions.internal.objectListEventHandler);

			objectListBox.setAttribute("st-partitionName", partitionName);

			partitionGroupBox.appendChild(objectListBox);
		}
	}

	// Append the partition
	partitionsBox.appendChild(partitionGroupBox);
};

art_semanticturkey.resourceView.partitions.internal.isPredicateObjectsList = function(element) {
	return element && element.tagName == "collection" && element.children[0]
			&& element.children[0].tagName == "predicateObjects";
};

art_semanticturkey.resourceView.partitions.internal.dblClickHandler = function(event) {
	var res = event.detail.rdfResource;

	if (res.isResource()) {
		art_semanticturkey.resourceView.utils.openResourceView(res.getNominalValue());
	}
};

art_semanticturkey.resourceView.partitions.internal.predicateObjectsEventHandler = function(event) {
	var partitionName = event.target.getAttribute("st-partitionName");
	var rdfSubject = event.detail.rdfSubject;
	var rdfPredicate = event.detail.rdfPredicate;
	var rdfObject = event.detail.rdfObject;
	var button = art_semanticturkey.resourceView.partitions.internal.getButtonType(event.detail.button);

	var handler = art_semanticturkey.resourceView.partitions.getPartitionHandler(partitionName);

	try {
		if (button == "add") {
			handler["onAdd"](rdfSubject, rdfPredicate, rdfObject);
		} else if (button == "remove") {
			handler["onRemove"](rdfSubject, rdfPredicate, rdfObject);
		}
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
};

art_semanticturkey.resourceView.partitions.internal.objectListEventHandler = function(event) {
	var partitionName = event.target.getAttribute("st-partitionName");
	var rdfObject = event.detail.rdfResource;
	var button = art_semanticturkey.resourceView.partitions.internal.getButtonType(event.detail.button);

	var handler = art_semanticturkey.resourceView.partitions.getPartitionHandler(partitionName);

	var rdfSubject = document.getElementById("resourceNameBox").stRdfNode;

	try {
		if (button == "add") {
			handler["onAdd"](rdfSubject);
		} else if (button == "remove") {
			handler["onRemove"](rdfSubject, rdfObject);
		}
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
};

art_semanticturkey.resourceView.partitions.internal.getButtonType = function(button) {
	return button.getAttribute("label");
};

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"lexicalizations",
				{
					"partitionLabel" : "Lexicalizations",
					"expectedContentType" : "predicateObjectsList",
					"addTooltiptext" : "Add a lexicalization",
//					"addTooltiptext|http://www.w3.org/2000/01/rdf-schema#label" : "Add an RDFS label",
//					"addTooltiptext|http://www.w3.org/2004/02/skos/core#prefLabel" : "Add a SKOS preferred label",
//					"addTooltiptext|http://www.w3.org/2004/02/skos/core#altLabel" : "Add a SKOS alternative label",
//					"addTooltiptext|http://www.w3.org/2004/02/skos/core#hiddenLabel" : "Add a SKOS hidden label",
//					"addTooltiptext|http://www.w3.org/2008/05/skos-xl#prefLabel" : "Add a SKOS-XL preferred label",
//					"addTooltiptext|http://www.w3.org/2008/05/skos-xl#altLabel" : "Add a SKOS-XL alternative label",
//					"addTooltiptext|http://www.w3.org/2008/05/skos-xl#hiddenLabel" : "Add a SKOS-XL hidden label",

					"onAdd" : function(rdfSubject, rdfPredicate, rdfObject) {
						if (typeof rdfPredicate == "undefined") {
							
							var parameters = {};
							parameters.resource = rdfSubject.getNominalValue();
							parameters.out = {};
							
							window.openDialog("lexicalizationPropertyChooser/lexicalizationPropertyChooser.xul", "dlg",
									"chrome=yes,dialog,resizable=yes,modal,centerscreen", parameters);
							
							if (typeof parameters.out.chosenProperty == "undefined") {
								return;
							}
							
							rdfPredicate = parameters.out.chosenProperty;
						}
						
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

							try {
								switch (predURI) {
								case "http://www.w3.org/2004/02/skos/core#prefLabel":
									art_semanticturkey.STRequests.SKOS.setPrefLabel(rdfSubject.getURI(),
											label, lang);
									break;
								case "http://www.w3.org/2004/02/skos/core#altLabel":
									art_semanticturkey.STRequests.SKOS.addAltLabel(rdfSubject.getURI(),
											label, lang);
									break;
								case "http://www.w3.org/2004/02/skos/core#hiddenLabel":
									art_semanticturkey.STRequests.SKOS.addHiddenLabel(
											rdfSubject.getURI(), label, lang);
									break;

								case "http://www.w3.org/2008/05/skos-xl#prefLabel":
									art_semanticturkey.STRequests.SKOSXL.setPrefLabel(
											rdfSubject.getURI(), label, lang, "bnode");
									break;
								case "http://www.w3.org/2008/05/skos-xl#altLabel":
									art_semanticturkey.STRequests.SKOSXL.addAltLabel(rdfSubject.getURI(),
											label, lang, "bnode");
									break;
								case "http://www.w3.org/2008/05/skos-xl#hiddenLabel":
									art_semanticturkey.STRequests.SKOSXL.addHiddenLabel(rdfSubject
											.getURI(), label, lang, "bnode");
									break;
								case "http://www.w3.org/2000/01/rdf-schema#label":
									art_semanticturkey.STRequests.Property.createAndAddPropValue(
											rdfSubject.getURI(), predURI, label, null, "plainLiteral",
											lang);
									break;
								}

								art_semanticturkey.resourceView.refreshView();
							} catch (e) {
								art_semanticturkey.Alert.alert(e);
							}
						};
						parameters.oncancel = false;

						if (typeof window.arguments != "undefined"
								&& typeof window.arguments[0] != "undefined") {
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
					},
					"onRemove" : function(rdfSubject, rdfPredicate, rdfObject) {
						switch (rdfPredicate.getNominalValue()) {
						case "http://www.w3.org/2004/02/skos/core#prefLabel":
							art_semanticturkey.STRequests.SKOS.removePrefLabel(rdfSubject.getURI(), rdfObject
									.getLabel(), rdfObject.getLang());
							break;
						case "http://www.w3.org/2004/02/skos/core#altLabel":
							art_semanticturkey.STRequests.SKOS.removeAltLabel(rdfSubject.getURI(), rdfObject
									.getLabel(), rdfObject.getLang());
							break;
						case "http://www.w3.org/2004/02/skos/core#hiddenLabel":
							art_semanticturkey.STRequests.SKOS.removeHiddenLabel(rdfSubject.getURI(), rdfObject
									.getLabel(), rdfObject.getLang());
							break;

						case "http://www.w3.org/2008/05/skos-xl#prefLabel":
						case "http://www.w3.org/2008/05/skos-xl#altLabel":
						case "http://www.w3.org/2008/05/skos-xl#hiddenLabel":
							// TODO: this is just an hack!!
							var renderizedLabel = rdfObject.getShow();
							var re = /(.*)\s\((.*)\)/;
							var m = renderizedLabel.match(re);
							
							var label = m[1];
							var lang = m[2];
							
							if (rdfPredicate.getNominalValue() == "http://www.w3.org/2008/05/skos-xl#prefLabel") {
								art_semanticturkey.STRequests.SKOSXL.removePrefLabel(rdfSubject.getURI(), label, lang);
							} else if (rdfPredicate.getNominalValue() == "http://www.w3.org/2008/05/skos-xl#altLabel") {
								art_semanticturkey.STRequests.SKOSXL.removeAltLabel(rdfSubject.getURI(), label, lang);
							} else {
								art_semanticturkey.STRequests.SKOSXL.removeHiddenLabel(rdfSubject.getURI(), label, lang);
							}
							
							break;
						case "http://www.w3.org/2000/01/rdf-schema#label":
							art_semanticturkey.STRequests.Property.removePropValue(rdfSubject.getNominalValue(),	rdfPredicate.getNominalValue(), rdfObject.getLabel(), null, "plainLiteral", rdfObject.getLang());
							break;
						}

						art_semanticturkey.resourceView.refreshView();
					}
				});

window.addEventListener("load", art_semanticturkey.resourceView.init, true);

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"properties",
				{
					"partitionLabel" : "Properties",
					"expectedContentType" : "predicateObjectsList",
					"addTooltiptext" : "Add a property value",
					"onAdd" : function(rdfSubject, rdfPredicate, rdfObject) { // Based on sources by
						// NScarpato
						var predicateName;

						if (typeof rdfPredicate == "undefined") {
							var parameters = new Object();
							var selectedProp = "";
							var selectedPropType = "";
							parameters.selectedProp = selectedProp;
							parameters.selectedPropType = selectedPropType;
							parameters.oncancel = false;
							parameters.source = "AddNewProperty";
							parameters.type = "All";
							parameters.forResource = rdfSubject.getNominalValue();
							
							window.openDialog(
									"chrome://semantic-turkey/content/editors/property/propertyTree.xul",
									"_blank", "modal=yes,resizable,centerscreen", parameters);
							var propType = parameters.selectedPropType;
							if (parameters.oncancel != false) {
								return;
							}

							predicateName = parameters.selectedProp;
						} else {
							predicateName = rdfPredicate.getNominalValue();
						}

						var parameters = {};
						parameters.predicate = predicateName;
						parameters.winTitle = "Add Property Value";
						parameters.action = "createAndAddPropValue";
						parameters.subject = rdfSubject.getNominalValue();
						// parameters.parentBox = document.getElementById("parentBoxRows");;
						// parameters.rowBox = document.getElementById("rowsBox");
						// parameters.typeValue = typeValue;
						// parameters.parentWindow = window.arguments[0].parentWindow;
						parameters.parentWindow = window;
						parameters.oncancel = false;
						// parameters.skos = window.arguments[0].skos;

						var responseXML = art_semanticturkey.STRequests.Property.getRange(predicateName,
								"false");
						var ranges = responseXML.getElementsByTagName("ranges")[0];

						if (ranges.getAttribute("rngType").indexOf("resource") != -1) {
							window.openDialog(
									"chrome://semantic-turkey/content/enrichProperty/enrichProperty.xul",
									"_blank", "modal=yes,resizable,centerscreen", parameters);

						} else if (ranges.getAttribute("rngType").indexOf("plainLiteral") != -1) {
							window
									.openDialog(
											"chrome://semantic-turkey/content/enrichProperty/enrichPlainLiteralRangedProperty.xul",
											"_blank", "modal=yes,resizable,centerscreen", parameters);
						} else if (ranges.getAttribute("rngType").indexOf("typedLiteral") != -1) {
							var rangeList = ranges.childNodes;
							for (var i = 0; i < rangeList.length; ++i) {
								if (typeof (rangeList[i].tagName) != 'undefined') {

									parameters.rangeType = rangeList[i].textContent;
								}
							}
							window
									.openDialog(
											"chrome://semantic-turkey/content/enrichProperty/enrichTypedLiteralRangedProperty.xul",
											"_blank", "modal=yes,resizable,centerscreen", parameters);
						} else if (ranges.getAttribute("rngType").indexOf("literal") != -1) {
							var rangeList = ranges.childNodes;
							var role = null;
							if (rangeList.length > 0) {
								for (var i = 0; i < rangeList.length; ++i) {
									if (typeof (rangeList[i].tagName) != 'undefined') {
										var dataRangeBNodeID = rangeList[i].textContent;
										var role = rangeList[i].getAttribute("role");
										var nodeType = rangeList[i].tagName;
									}
								}
								if (role.indexOf("dataRange") != -1) {
									var responseXML = art_semanticturkey.STRequests.Property.parseDataRange(
											dataRangeBNodeID, nodeType);

									var dataElement = responseXML.getElementsByTagName("data")[0];
									var dataRangesList = dataElement.childNodes;
									var dataRangesValueList = new Array();
									var k = 0;
									for (var i = 0; i < dataRangesList.length; ++i) {
										if (typeof (dataRangesList[i].tagName) != 'undefined') {
											var dataRangeValue = new Object();
											dataRangeValue.type = dataRangesList[i].tagName;
											dataRangeValue.rangeType = dataRangesList[i].getAttribute("type");
											dataRangeValue.show = dataRangesList[i].getAttribute("show");
											dataRangesValueList[k] = dataRangeValue;
											k++;
										}
									}
									parameters.rangeType = "dataRange";
									parameters.dataRangesValueList = dataRangesValueList;
									window
											.openDialog(
													"chrome://semantic-turkey/content/enrichProperty/enrichTypedLiteralRangedProperty.xul",
													"_blank", "modal=yes,resizable,centerscreen", parameters);
								}
							} else {
								var literalsParameters = new Object();
								literalsParameters.isLiteral = "literal";
								window.openDialog(
										"chrome://semantic-turkey/content/enrichProperty/isLiteral.xul",
										"_blank", "modal=yes,resizable,centerscreen", literalsParameters);
								if (literalsParameters.isLiteral == "plainLiteral") {
									window
											.openDialog(
													"chrome://semantic-turkey/content/enrichProperty/enrichPlainLiteralRangedProperty.xul",
													"_blank", "modal=yes,resizable,centerscreen", parameters);
								} else if (literalsParameters.isLiteral == "typedLiteral") {
									window
											.openDialog(
													"chrome://semantic-turkey/content/enrichProperty/enrichTypedLiteralRangedProperty.xul",
													"_blank", "modal=yes,resizable,centerscreen", parameters);
								}
							}
						} else if (ranges.getAttribute("rngType").indexOf("undetermined") != -1) {
							var literalsParameters = new Object();
							literalsParameters.isLiteral = "undetermined";
							window.openDialog(
									"chrome://semantic-turkey/content/enrichProperty/isLiteral.xul",
									"_blank", "modal=yes,resizable,centerscreen", literalsParameters);
							if (literalsParameters.isLiteral == "plainLiteral") {
								window
										.openDialog(
												"chrome://semantic-turkey/content/enrichProperty/enrichPlainLiteralRangedProperty.xul",
												"_blank", "modal=yes,resizable,centerscreen", parameters);
							} else if (literalsParameters.isLiteral == "typedLiteral") {
								var rangeList = ranges.childNodes;
								for (var i = 0; i < rangeList.length; ++i) {
									if (typeof (rangeList[i].tagName) != 'undefined') {
										parameters.rangeType = rangeList[i].textContent;
									}
								}
								window
										.openDialog(
												"chrome://semantic-turkey/content/enrichProperty/enrichTypedLiteralRangedProperty.xul",
												"_blank", "modal=yes,resizable,centerscreen", parameters);
							} else if (literalsParameters.isLiteral == "resource") {
								window.openDialog(
										"chrome://semantic-turkey/content/enrichProperty/enrichProperty.xul",
										"_blank", "modal=yes,resizable,centerscreen", parameters);
							}
						} else if (ranges.getAttribute("rngType").indexOf("inconsistent") != -1) {
							alert("Error range of " + propertyQName + " property is inconsistent");
						}

						if (parameters.oncancel == false) {
							// if (window.arguments[0].sourceType == "skosConcept") {
							// // Luca Mastrogiovanni: fire event propertyValueAdded
							// var obj = new Object();
							// art_semanticturkey.evtMgr.fireEvent("propertyValueAdded", obj);
							//
							// }
							art_semanticturkey.evtMgr.fireEvent("refreshEditor",
									(new art_semanticturkey.genericEventClass()));
						}
					},
					"onRemove" : function(rdfSubject, rdfPredicate, rdfObject) {
						if (rdfObject.explicit == "true") {
							art_semanticturkey.STRequests.Resource.removePropertyValue(rdfSubject.toNT(),
									rdfPredicate.toNT(), rdfObject.toNT());
							art_semanticturkey.evtMgr.fireEvent("refreshEditor",
									(new art_semanticturkey.genericEventClass()));
						} else {
							art_semanticturkey.Alert
									.alert("You cannot remove this type, it's a system resource!");
						}
					}
				});

art_semanticturkey.resourceView.partitions.registerPartitionHandler("types", {
	"partitionLabel" : "Types",
	"expectedContentType" : "objectList",
	"addTooltiptext" : "Add a type",
	"addIcon|fromRole" : "cls",
	"onAdd" : function(rdfSubject) { // Based on sources by
		// NScarpato
		var parameters = {};
		parameters.source = "editorIndividual";
		parameters.selectedClass = "";
		// parameters.parentWindow = window.arguments[0].parentWindow;
		parameters.parentWindow = window;

		window.openDialog("chrome://semantic-turkey/content/editors/class/classTree.xul", "_blank",
				"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);

		if (parameters.selectedClass != "") {
			var responseArray;

			if (rdfSubject.getRole() == "individual") {
				responseArray = art_semanticturkey.STRequests.Individual.addType(
						rdfSubject.getNominalValue(), parameters.selectedClass);
			} else {
				responseArray = art_semanticturkey.STRequests.Cls.addType(rdfSubject.getNominalValue(),
						parameters.selectedClass);
			}
			art_semanticturkey.evtMgr.fireEvent("addedType", (new art_semanticturkey.typeAddedClass(
					responseArray["instance"], responseArray["type"], true, rdfSubject.getRole())));
			art_semanticturkey.evtMgr
					.fireEvent("refreshEditor", (new art_semanticturkey.genericEventClass()));
		}

	},
	"onRemove" : function(rdfSubject, rdfObject) { // Based on sources by
		// NScarpato
		var responseArray;

		if (rdfObject.explicit == "true") {
			if (rdfSubject.getRole() == "individual") {
				responseArray = art_semanticturkey.STRequests.Individual.removeType(rdfSubject
						.getNominalValue(), rdfObject.getNominalValue());
			} else {
				responseArray = art_semanticturkey.STRequests.Cls.removeType(rdfSubject.getNominalValue(),
						rdfObject.getNominalValue());
			}
			art_semanticturkey.evtMgr.fireEvent("removedType", (new art_semanticturkey.typeRemovedClass(
					responseArray["instance"], responseArray["type"])));
			art_semanticturkey.evtMgr
					.fireEvent("refreshEditor", (new art_semanticturkey.genericEventClass()));
		} else {
			art_semanticturkey.Alert.alert("You cannot remove this type, it's a system resource!");
		}

	}
});

art_semanticturkey.resourceView.partitions.registerPartitionHandler("supertypes", {
	"partitionLabel" : "Supertypes",
	"expectedContentType" : "objectList",
	"addTooltiptext" : "Add a super-type",
	"addIcon|fromRole" : "cls",
	"onAdd" : function(rdfSubject) { // Based on sources by
		// NScarpato
		var parameters = {};
		parameters.source = "editorClass";
		parameters.selectedClass = "";
		// parameters.parentWindow = window.arguments[0].parentWindow;
		parameters.parentWindow = window;

		window.openDialog("chrome://semantic-turkey/content/editors/class/classTree.xul", "_blank",
				"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);

		if (parameters.selectedClass != "") {
			var responseArray = art_semanticturkey.STRequests.Cls.addSuperCls(rdfSubject.getNominalValue(),
					parameters.selectedClass);
			var classRes = responseArray["class"];
			var superClassRes = responseArray["superClass"];
			art_semanticturkey.evtMgr.fireEvent("subClsOfAddedClass",
					(new art_semanticturkey.subClsOfAddedClass(classRes, superClassRes)));
			art_semanticturkey.evtMgr
					.fireEvent("refreshEditor", (new art_semanticturkey.genericEventClass()));
		}

	},
	"onRemove" : function(rdfSubject, rdfObject) { // Based on sources by
		// NScarpato
		if (rdfObject.explicit == "true") {
			var responseArray = art_semanticturkey.STRequests.Cls.removeSuperCls(
					rdfSubject.getNominalValue(), rdfObject.getNominalValue());
			// art_semanticturkey.refreshPanel();
			var classRes = responseArray["class"];
			var superClassRes = responseArray["superClass"];
			art_semanticturkey.evtMgr.fireEvent("subClsOfRemovedClass",
					(new art_semanticturkey.subClsOfRemovedClass(classRes, superClassRes)));
			art_semanticturkey.evtMgr
					.fireEvent("refreshEditor", (new art_semanticturkey.genericEventClass()));
		} else {
			art_semanticturkey.Alert.alert("You cannot remove this type, it's a system resource!");
		}

	}
});

art_semanticturkey.resourceView.partitions.registerPartitionHandler("superproperties", {
	"partitionLabel" : "Superproperties",
	"expectedContentType" : "objectList",
	"addTooltiptext" : "Add a super-property",
	"addIcon|fromRole" : "cls",
	"onAdd" : function(rdfSubject) { // Based on sources by
		// NScarpato
		var parameters = {};
		parameters.selectedProp = "";
		parameters.selectedPropType = "";
		parameters.type = rdfSubject.getRole();
		window.openDialog("chrome://semantic-turkey/content/editors/property/propertyTree.xul", "_blank",
				"modal=yes,resizable,centerscreen", parameters);
		if (parameters.selectedProp != "") {
			art_semanticturkey.STRequests.Property.addSuperProperty(rdfSubject.getNominalValue(),
					parameters.selectedProp);
		}
		art_semanticturkey.evtMgr.fireEvent("refreshEditor", (new art_semanticturkey.genericEventClass()));
	},
	"onRemove" : function(rdfSubject, rdfObject) { // Based on sources by
		// NScarpato
		if (rdfObject.explicit == "true") {
			art_semanticturkey.STRequests.Property.removeSuperProperty(rdfSubject.getNominalValue(),
					rdfObject.getNominalValue());
			art_semanticturkey.evtMgr
					.fireEvent("refreshEditor", (new art_semanticturkey.genericEventClass()));
		} else {
			art_semanticturkey.Alert.alert("You cannot remove this type, it's a system resource!");
		}

	}
});

art_semanticturkey.resourceView.partitions.registerPartitionHandler("domains", {
	"partitionLabel" : "Domains",
	"expectedContentType" : "objectList",
	"addTooltiptext" : "Add a domain",
	"addIcon|fromRole" : "cls",
	"onAdd" : function(rdfSubject) { // Based on sources by
		// NScarpato

		var domainName = "";
		var parameters = {};
		parameters.source = "domain";
		parameters.domainName = "";
		// parameters.parentWindow = window.arguments[0].parentWindow;
		parameters.parentWindow = window;

		window.openDialog("chrome://semantic-turkey/content/editors/class/classTree.xul", "_blank",
				"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
		var domainName = parameters.domainName;
		if (domainName != "none domain selected") {
			art_semanticturkey.STRequests.Property
					.addPropertyDomain(rdfSubject.getNominalValue(), domainName);
			art_semanticturkey.evtMgr
					.fireEvent("refreshEditor", (new art_semanticturkey.genericEventClass()));
		}
	},
	"onRemove" : function(rdfSubject, rdfObject) { // Based on sources by
		// NScarpato
		if (rdfObject.explicit == "true") {
			art_semanticturkey.STRequests.Property.removePropertyDomain(rdfSubject.getNominalValue(),
					rdfObject.getNominalValue());
			art_semanticturkey.evtMgr
					.fireEvent("refreshEditor", (new art_semanticturkey.genericEventClass()));
		} else {
			art_semanticturkey.Alert.alert("You cannot remove this type, it's a system resource!");
		}
	}
});

art_semanticturkey.resourceView.partitions.registerPartitionHandler("ranges", {
	"partitionLabel" : "Ranges",
	"addTooltiptext" : "Add a range",
	"expectedContentType" : "objectList",
	"addIcon|fromRole" : "cls",
	"onAdd" : function(rdfSubject) { // Based on sources by
		// NScarpato

		var parameters = {};
		parameters.source = "range";
		parameters.rangeName = "";

		if (rdfSubject.getRole().toLowerCase().indexOf("objectproperty") != -1) {
			// parameters.parentWindow = window.arguments[0].parentWindow;
			parameters.parentWindow = window;
			window.openDialog("chrome://semantic-turkey/content/editors/class/classTree.xul", "_blank",
					"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
			if (parameters.rangeName != "") {
				art_semanticturkey.STRequests.Property.addPropertyRange(rdfSubject.getNominalValue(),
						parameters.rangeName);
				art_semanticturkey.evtMgr.fireEvent("refreshEditor",
						(new art_semanticturkey.genericEventClass()));
			}

		} else {
			window.openDialog("chrome://semantic-turkey/content/editors/property/rangeList.xul", "_blank",
					"modal=yes,resizable,centerscreen", parameters);
			if (parameters.rangeName != "") {
				art_semanticturkey.STRequests.Property.addPropertyRange(rdfSubject.getNominalValue(),
						parameters.rangeName);
				art_semanticturkey.evtMgr.fireEvent("refreshEditor",
						(new art_semanticturkey.genericEventClass()));
			}
		}
	},
	"onRemove" : function(rdfSubject, rdfObject) { // Based on sources by
		// NScarpato
		if (rdfObject.explicit == "true") {

			art_semanticturkey.STRequests.Property.removePropertyRange(rdfSubject.getNominalValue(),
					rdfObject.getNominalValue());
			art_semanticturkey.evtMgr
					.fireEvent("refreshEditor", (new art_semanticturkey.genericEventClass()));
		} else {
			art_semanticturkey.Alert.alert("You cannot remove this type, it's a system resource!");
		}
	}
});

art_semanticturkey.resourceView.partitions.registerPartitionHandler("broaders", {
	"partitionLabel" : "Broaders",
	"expectedContentType" : "objectList",
	"addTooltiptext" : "Add a broader concept",
	"addIcon|fromRole" : "concept",
	"onAdd" : function(rdfSubject) {
		var parameters = {};
		parameters.conceptScheme = "*"; // TODO which concept scheme?
		// parameters.parentWindow = window.arguments[0].parentWindow;
		parameters.parentWindow = window;
		window.openDialog("chrome://semantic-turkey/content/skos/editors/concept/conceptTree.xul", "_blank",
				"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);

		if (typeof parameters.out == "undefined" || typeof parameters.out.selectedConcept == "undefined")
			return;

		art_semanticturkey.STRequests.SKOS.addBroaderConcept(rdfSubject.getNominalValue(),
				parameters.out.selectedConcept);
		art_semanticturkey.evtMgr.fireEvent("refreshEditor", (new art_semanticturkey.genericEventClass()));
	},
	"onRemove" : function(rdfSubject, rdfObject) {
		if (rdfObject.explicit == "true") {

			art_semanticturkey.STRequests.SKOS.removeBroaderConcept(rdfSubject.getNominalValue(), rdfObject
					.getNominalValue());
			art_semanticturkey.evtMgr
					.fireEvent("refreshEditor", (new art_semanticturkey.genericEventClass()));
		} else {
			art_semanticturkey.Alert.alert("You cannot remove this type, it's a system resource!");
		}
	}
});

art_semanticturkey.resourceView.partitions.registerPartitionHandler("topconcepts", {
	"partitionLabel" : "Top Concepts",
	"expectedContentType" : "objectList",
	"addTooltiptext" : "Add a top concept",
	"addIcon|fromRole" : "concept",
	"onAdd" : function(rdfSubject) {
		var parameters = {};
		parameters.conceptScheme = "*"; // TODO which concept scheme?
		// parameters.parentWindow = window.arguments[0].parentWindow;
		parameters.parentWindow = window;
		window.openDialog("chrome://semantic-turkey/content/skos/editors/concept/conceptTree.xul", "_blank",
				"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);

		if (typeof parameters.out == "undefined" || typeof parameters.out.selectedConcept == "undefined")
			return;

		art_semanticturkey.STRequests.SKOS.addTopConcept(rdfSubject.getNominalValue(),
				parameters.out.selectedConcept);

		art_semanticturkey.evtMgr.fireEvent("refreshEditor", (new art_semanticturkey.genericEventClass()));
	},
	"onRemove" : function(rdfSubject, rdfObject) {
		if (rdfObject.explicit == "true") {

			art_semanticturkey.STRequests.SKOS.removeTopConcept(rdfSubject.getNominalValue(), rdfObject
					.getNominalValue());
			art_semanticturkey.evtMgr
					.fireEvent("refreshEditor", (new art_semanticturkey.genericEventClass()));
		} else {
			art_semanticturkey.Alert.alert("You cannot remove this type, it's a system resource!");
		}
	}
});