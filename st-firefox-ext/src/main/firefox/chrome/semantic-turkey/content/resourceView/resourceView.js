if (typeof art_semanticturkey == "undefined") {
	var art_semanticturkey = {};
}

Components.utils.import("resource://stservices/SERVICE_Property.jsm",
		art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Cls.jsm",
		art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Individual.jsm",
		art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Resource.jsm",
		art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm",
		art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOSXL.jsm",
		art_semanticturkey);

Components.utils.import("resource://stmodules/Deserializer.jsm",
		art_semanticturkey);
Components.utils.import("resource://stmodules/ResourceViewLauncher.jsm",
		art_semanticturkey);

Components.utils.import("resource://stservices/SERVICE_ResourceView.jsm",
		art_semanticturkey);

Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/AnnotationCommons.jsm",
		art_semanticturkey);
Components.utils.import("resource://stmodules/ARTResources.jsm",
		art_semanticturkey);
Components.utils.import("resource://stmodules/STResUtils.jsm",
		art_semanticturkey);

if (typeof art_semanticturkey.resourceView == "undefined") {
	art_semanticturkey.resourceView = {};
}

art_semanticturkey.resourceView.init = function() {
	// --------------------
	// Parses the arguments

	var parametersFromURL = art_semanticturkey.resourceView.utils
			.parseQueryString(window.location.search);

	var resource = parametersFromURL.resource;

	if (typeof resource != "string") {
		art_semanticturkey.Alert
				.alert("Missing argument for mandatory parameter \"resource\"");
		window.close();
		return;
	}

	// --------------------------
	// Initializes basic behavior

	var resourceNameBox = document.getElementById("resourceNameBox");

	var renameResourceButton = document.getElementById("renameResourceButton");
	renameResourceButton.addEventListener("command",
			art_semanticturkey.resourceView.doRenameResource, false);

	var resourceViewBox = document.getElementById("resourceViewBox");

	var eventListenerArrayObject = new art_semanticturkey.eventListenerArrayClass();

	eventListenerArrayObject
			.addEventListenerToArrayAndRegister(
					"resourceRenamed",
					function(eventId, resourceRenamedObj) {
						if (resourceRenamedObj.getOldName() == resourceNameBox.stRdfNode
								.getNominalValue()) {
							art_semanticturkey.resourceView
									.refreshView(resourceRenamedObj
											.getNewName());
						}
					}, null);

	eventListenerArrayObject.addEventListenerToArrayAndRegister(
			"refreshEditor", function(eventId, eventObj) {
				art_semanticturkey.resourceView.refreshView();
			}, null);

	window.addEventListener("unload", function() {
		eventListenerArrayObject.deregisterAllListener();
	}, true);

	window
			.addEventListener(
					"rdfnodeBaseEvent",
					art_semanticturkey.resourceView.partitions.internal.dblClickHandler,
					true);

	// -------------------------------------------------
	// Request and rendering of the resource description

	try {
		var response = art_semanticturkey.STRequests.ResourceView
				.getResourceView(resource);
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

	art_semanticturkey.resourceView.utils
			.openResourceView(resourceName, window);
};

art_semanticturkey.resourceView.getResourceView_RESPONSE = function(response) {
	// -----------------------------
	// Populates the resource header

	// Handles the resource name (resource name box)
	var responseDataElement = response.getElementsByTagName("data")[0];
	var responseResourceElement = responseDataElement
			.getElementsByTagName("resource")[0];

	var resourceObj = art_semanticturkey.Deserializer
			.createRDFNode(responseResourceElement.children[0]);

	var resourceNameElement = document.getElementById("resourceNameBox");
	resourceNameElement.value = resourceObj.getNominalValue();
	resourceNameElement.stRdfNode = resourceObj;

	// Handles the resource role icon
	var resourceRoleIconElement = document.getElementById("resourceRoleIcon");
	resourceRoleIconElement.setAttribute("src", art_semanticturkey.STResUtils
			.getImageSrc(resourceObj));

	// Handles the resource editability (rename button)
	var isResourceEditable = art_semanticturkey.resourceView
			.isEditable(resourceObj);

	var renameResourceButton = document.getElementById("renameResourceButton");
	renameResourceButton.disabled = !(isResourceEditable && resourceObj
			.isURIResource());

	// --------------------------------
	// Configure the copy-url popupmenu
	document.getElementById("web-link-copy").addEventListener("command",
			art_semanticturkey.resourceView.copyWebLink, true);

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

	// ----------------------------------------------
	// Separate handling of annotations and bookmarks

	// TODO: maybe it is the case to move this information into the resource
	// decription so that it can be processed as the other partitions

	var defaultAnnotationFamily = art_semanticturkey.annotation.AnnotationManager
			.getDefaultFamily();

	if (typeof defaultAnnotationFamily.getAnnotatedContentResources != "undefined") {
		var annotatedContentResources = defaultAnnotationFamily
				.getAnnotatedContentResources(resourceObj.getNominalValue());
		art_semanticturkey.resourceView.getWebLinks_RESPONSE(partitionsBox,
				annotatedContentResources);
	}

	if (resourceObj.getRole() == "concept") {
		var responseXML = art_semanticturkey.STRequests.Annotation
				.getBookmarksByTopic(resourceObj.getNominalValue());
		art_semanticturkey.resourceView.getBookmarksByTopic_RESPONSE(
				partitionsBox, responseXML);
	}
};

art_semanticturkey.resourceView.doRenameResource = function(event) {

	try {
		var resourceNameBox = document.getElementById("resourceNameBox");

		var parameters = {};
		parameters.currentName = resourceNameBox.stRdfNode.getNominalValue();

		window.openDialog("renameDialog/renameDialog.xul", "dlg",
				"chrome=yes,dialog,resizable=yes,modal,centerscreen",
				parameters);
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
};

/**
 * Populate the resource view with the web link of the selected resource
 * 
 * @param {}
 *            responseElement
 */
art_semanticturkey.resourceView.getWebLinks_RESPONSE = function(partitionsBox,
		annotatedContentResources) {
	var partitionGroupBox = document.createElement("groupbox");
	var partitionCaption = document.createElement("caption");

	var partitionLabelElement = document.createElement("label");
	partitionLabelElement.setAttribute("value", "Annotated Web Documents");
	partitionCaption.appendChild(partitionLabelElement);
	partitionGroupBox.appendChild(partitionCaption);

	for (var i = 0; i < annotatedContentResources.length; i++) {
		var linkTitle = annotatedContentResources[i].title;
		var linkUrl = annotatedContentResources[i].value;

		var row = document.createElement("row");

		var label = document.createElement("label");
		label.setAttribute("value", linkTitle);
		label.setAttribute("href", linkUrl);
		label.setAttribute("class", "text-link");
		label.setAttribute("context", "web-link-context-menu");

		row.appendChild(label);

		partitionGroupBox.appendChild(row);
	}

	partitionsBox.appendChild(partitionGroupBox);
};

/**
 * Populate the resource view with the web links whose topic is the current SKOS
 * concept
 * 
 * @param {}
 *            responseElement
 */
art_semanticturkey.resourceView.getBookmarksByTopic_RESPONSE = function(
		partitionsBox, responseElement) {
	var partitionGroupBox = document.createElement("groupbox");
	var partitionCaption = document.createElement("caption");

	var partitionLabelElement = document.createElement("label");
	partitionLabelElement.setAttribute("value", "Web Documents in Topic");
	partitionCaption.appendChild(partitionLabelElement);
	partitionGroupBox.appendChild(partitionCaption);

	var bookmarksList = responseElement.getElementsByTagName("page");

	for (var i = 0; i < bookmarksList.length; i++) {
		var linkTitle = bookmarksList[i].getAttribute("title");

		var linkUrl = bookmarksList[i].getAttribute("url");
		linkUrl = linkUrl.substring(1, linkUrl.length - 1); // strip surrounding
		// quotes

		var row = document.createElement("row");

		var label = document.createElement("label");
		label.setAttribute("value", linkTitle);
		label.setAttribute("href", linkUrl);
		label.setAttribute("class", "text-link");
		label.setAttribute("context", "web-link-context-menu");

		row.appendChild(label);

		partitionGroupBox.appendChild(row);
	}

	partitionsBox.appendChild(partitionGroupBox);
};

art_semanticturkey.resourceView.copyWebLink = function(event) {
	var element = document.popupNode;

	var url = element.getAttribute("href");

	const
	gClipboardHelper = Components.classes["@mozilla.org/widget/clipboardhelper;1"]
			.getService(Components.interfaces.nsIClipboardHelper);
	gClipboardHelper.copyString(url);
};

art_semanticturkey.resourceView.partitions = {};

art_semanticturkey.resourceView.partitions.registerPartitionHandler = function(
		partitionName, pojo) {
	art_semanticturkey.resourceView.partitions.internal.partition2handlerMap[partitionName] = art_semanticturkey.resourceView.partitions.internal
			.wrapPojoHandler(partitionName, pojo);
};
art_semanticturkey.resourceView.partitions.getPartitionHandler = function(
		partitionName) {
	return art_semanticturkey.resourceView.partitions.internal.partition2handlerMap[partitionName]
			|| art_semanticturkey.resourceView.partitions.internal
					.wrapPojoHandler(partitionName, {});
};

art_semanticturkey.resourceView.partitions.internal = {};
art_semanticturkey.resourceView.partitions.internal.partition2handlerMap = {};
art_semanticturkey.resourceView.partitions.internal.wrapPojoHandler = function(
		partitionName, pojo) {
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
			stereotypicalRole = obj.expectedContentType == "predicateObjectsList" ? "property"
					: "individual";
		}

		obj.addIcon = art_semanticturkey.STResUtils.getImageSrcOrNull(
				new art_semanticturkey.ARTURIResource("foo", stereotypicalRole,
						"http://foo.it"), "add");
	}

	return obj;
};
art_semanticturkey.resourceView.partitions.internal.defaultPartitionRender = function(
		subjectResource, responsePartition, partitionsBox) {

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
		if (this.expectedContentType == "mixed") {

			var rv = {};

			if (typeof this.renderHelper != "undefined") {
				rv = this.renderHelper(subjectResource, responsePartition,
						partitionGroupBox)
						|| rv;
			}

			var excludedSections = excludedSections = rv.excludedSections || [];

			var childrenElement = partitionGroupBox;

			if (typeof rv.childrenElement != "undefined") {
				childrenElement = rv.childrenElement;
			}

			for (var i = 0; i < responsePartition.children.length; i++) {
				if (excludedSections
						.indexOf(responsePartition.children[i].tagName) != -1)
					continue;

				var subPartitionHandler = art_semanticturkey.resourceView.partitions
						.getPartitionHandler(responsePartition.children[i].tagName);
				subPartitionHandler.render(subjectResource,
						responsePartition.children[i], childrenElement);

			}

		} else if ((this.expectedContentType == "predicateObjectsList")
				|| art_semanticturkey.resourceView.partitions.internal
						.isPredicateObjectsList(partitionContent)) {
			if (addSupported) {
				partitionButton.addEventListener("command",
						art_semanticturkey.resourceView.partitions.internal
								.wrapFunctionWithErrorManagement(this["onAdd"]
										.bind(this, subjectResource, undefined,
												undefined)), true);
			}

			var predicateObjectsList = art_semanticturkey.Deserializer
					.createPredicateObjectsList(partitionContent);

			for (var j = 0; j < predicateObjectsList.length; j++) {
				var po = predicateObjectsList[j];
				var predicateObjectsBox = document.createElement("box");
				predicateObjectsBox.rdfSubject = subjectResource;
				predicateObjectsBox.showSubjInGUI = false;
				predicateObjectsBox.rdfPredicate = po.getPredicate();
				predicateObjectsBox.hasCustomRange = po.getPredicate().hasCustomRange;
				predicateObjectsBox.rdfResourcesArray = po.getObjects();
				predicateObjectsBox.operations = operations.join(";");
				predicateObjectsBox.classList.add("predicate-objects-widget");

				predicateObjectsBox
						.addEventListener(
								"predicateObjectsEvent",
								art_semanticturkey.resourceView.partitions.internal.predicateObjectsEventHandler);

				predicateObjectsBox.setAttribute("st-partitionName",
						partitionName);

				partitionGroupBox.appendChild(predicateObjectsBox);
			}
		} else { // Otherwise, assume an objects list
			if (addSupported) {
				partitionButton.addEventListener("command",
						art_semanticturkey.resourceView.partitions.internal
								.wrapFunctionWithErrorManagement(this["onAdd"]
										.bind(this, subjectResource)), true);
			}

			var objects = art_semanticturkey.Deserializer
					.createRDFArray(responsePartition);
			var objectListBox = document.createElement("box");
			objectListBox.rdfResourcesArray = objects;
			objectListBox.addRemoveButton = removeSupported;
			objectListBox.operations = operations.join(";");
			objectListBox.classList.add("object-list-widget");

			objectListBox
					.addEventListener(
							"rdfnodeContainerEvent",
							art_semanticturkey.resourceView.partitions.internal.objectListEventHandler);
			objectListBox
					.addEventListener(
							"objectListEvent",
							art_semanticturkey.resourceView.partitions.internal.objectListEventHandler);

			objectListBox.setAttribute("st-partitionName", partitionName);

			partitionGroupBox.appendChild(objectListBox);
		}
	}

	// Append the partition
	partitionsBox.appendChild(partitionGroupBox);
};

art_semanticturkey.resourceView.partitions.internal.wrapFunctionWithErrorManagement = function(
		aFun) {
	return function() {
		try {
			aFun.apply(null, arguments);
		} catch (e) {
			art_semanticturkey.Alert.alert(e);
		}
	};
};

art_semanticturkey.resourceView.partitions.internal.isPredicateObjectsList = function(
		element) {
	return element && element.tagName == "collection" && element.children[0]
			&& element.children[0].tagName == "predicateObjects";
};

art_semanticturkey.resourceView.partitions.internal.dblClickHandler = function(
		event) {
	var res = event.detail.rdfResource;

	if (res.isResource()) {
		art_semanticturkey.ResourceViewLauncher.openResourceView(res
				.getNominalValue());
	}
};

art_semanticturkey.resourceView.partitions.internal.predicateObjectsEventHandler = function(
		event) {
	var partitionName = event.target.getAttribute("st-partitionName");
	var rdfSubject = event.detail.rdfSubject;
	var rdfPredicate = event.detail.rdfPredicate;
	var rdfObject = event.detail.rdfObject;
	var button = art_semanticturkey.resourceView.partitions.internal
			.getButtonType(event.detail.button);

	var handler = art_semanticturkey.resourceView.partitions
			.getPartitionHandler(partitionName);

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

art_semanticturkey.resourceView.partitions.internal.objectListEventHandler = function(
		event) {
	var partitionName = event.target.getAttribute("st-partitionName");
	var rdfObject = event.detail.rdfResource;
	var button = art_semanticturkey.resourceView.partitions.internal
			.getButtonType(event.detail.button);

	var handler = art_semanticturkey.resourceView.partitions
			.getPartitionHandler(partitionName);

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

art_semanticturkey.resourceView.partitions.internal.getButtonType = function(
		button) {
	return button.getAttribute("label");
};

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"lexicalizations",
				{
					"partitionLabel" : "Lexicalizations",
					"expectedContentType" : "predicateObjectsList",
					"addTooltiptext" : "Add a lexicalization",
					// "addTooltiptext|http://www.w3.org/2000/01/rdf-schema#label"
					// : "Add an RDFS label",
					// "addTooltiptext|http://www.w3.org/2004/02/skos/core#prefLabel"
					// : "Add a SKOS preferred label",
					// "addTooltiptext|http://www.w3.org/2004/02/skos/core#altLabel"
					// : "Add a SKOS alternative label",
					// "addTooltiptext|http://www.w3.org/2004/02/skos/core#hiddenLabel"
					// : "Add a SKOS hidden label",
					// "addTooltiptext|http://www.w3.org/2008/05/skos-xl#prefLabel"
					// : "Add a SKOS-XL preferred label",
					// "addTooltiptext|http://www.w3.org/2008/05/skos-xl#altLabel"
					// : "Add a SKOS-XL alternative label",
					// "addTooltiptext|http://www.w3.org/2008/05/skos-xl#hiddenLabel"
					// : "Add a SKOS-XL hidden label",

					"onAdd" : function(rdfSubject, rdfPredicate, rdfObject) {
						if (typeof rdfPredicate == "undefined") {

							var parameters = {};
							parameters.resource = rdfSubject.getNominalValue();
							parameters.out = {};

							window
									.openDialog(
											"lexicalizationPropertyChooser/lexicalizationPropertyChooser.xul",
											"dlg",
											"chrome=yes,dialog,resizable=yes,modal,centerscreen",
											parameters);

							if (typeof parameters.out.chosenProperty == "undefined") {
								return;
							}

							rdfPredicate = parameters.out.chosenProperty;
						}

						var predURI = rdfPredicate.getURI();

						const
						supportedProps = [
								"http://www.w3.org/2000/01/rdf-schema#label",
								"http://www.w3.org/2004/02/skos/core#prefLabel",
								"http://www.w3.org/2004/02/skos/core#altLabel",
								"http://www.w3.org/2004/02/skos/core#hiddenLabel",
								"http://www.w3.org/2008/05/skos-xl#prefLabel",
								"http://www.w3.org/2008/05/skos-xl#altLabel",
								"http://www.w3.org/2008/05/skos-xl#hiddenLabel" ];

						var parameters = {};

						parameters.winTitle = "Add " + rdfPredicate.getShow()
								+ " Lexicalization";
						parameters.action = function(label, lang) {

							try {
								switch (predURI) {
								case "http://www.w3.org/2004/02/skos/core#prefLabel":
									art_semanticturkey.STRequests.SKOS
											.setPrefLabel(rdfSubject.getURI(),
													label, lang);
									break;
								case "http://www.w3.org/2004/02/skos/core#altLabel":
									art_semanticturkey.STRequests.SKOS
											.addAltLabel(rdfSubject.getURI(),
													label, lang);
									break;
								case "http://www.w3.org/2004/02/skos/core#hiddenLabel":
									art_semanticturkey.STRequests.SKOS
											.addHiddenLabel(
													rdfSubject.getURI(), label,
													lang);
									break;

								case "http://www.w3.org/2008/05/skos-xl#prefLabel":
									art_semanticturkey.STRequests.SKOSXL
											.setPrefLabel(rdfSubject.getURI(),
													label, lang, "bnode");
									break;
								case "http://www.w3.org/2008/05/skos-xl#altLabel":
									art_semanticturkey.STRequests.SKOSXL
											.addAltLabel(rdfSubject.getURI(),
													label, lang, "bnode");
									break;
								case "http://www.w3.org/2008/05/skos-xl#hiddenLabel":
									art_semanticturkey.STRequests.SKOSXL
											.addHiddenLabel(
													rdfSubject.getURI(), label,
													lang, "bnode");
									break;
								case "http://www.w3.org/2000/01/rdf-schema#label":
									art_semanticturkey.STRequests.Property
											.createAndAddPropValue(rdfSubject
													.getURI(), predURI, label,
													null, "plainLiteral", lang);
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
											"_blank",
											"modal=yes,resizable,centerscreen",
											parameters);
						} else {
							alert("Unsupported predicate type");
						}
					},
					"onRemove" : function(rdfSubject, rdfPredicate, rdfObject) {
						switch (rdfPredicate.getNominalValue()) {
						case "http://www.w3.org/2004/02/skos/core#prefLabel":
							art_semanticturkey.STRequests.SKOS.removePrefLabel(
									rdfSubject.getURI(), rdfObject.getLabel(),
									rdfObject.getLang());
							break;
						case "http://www.w3.org/2004/02/skos/core#altLabel":
							art_semanticturkey.STRequests.SKOS.removeAltLabel(
									rdfSubject.getURI(), rdfObject.getLabel(),
									rdfObject.getLang());
							break;
						case "http://www.w3.org/2004/02/skos/core#hiddenLabel":
							art_semanticturkey.STRequests.SKOS
									.removeHiddenLabel(rdfSubject.getURI(),
											rdfObject.getLabel(), rdfObject
													.getLang());
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
								art_semanticturkey.STRequests.SKOSXL
										.removePrefLabel(rdfSubject.getURI(),
												label, lang);
							} else if (rdfPredicate.getNominalValue() == "http://www.w3.org/2008/05/skos-xl#altLabel") {
								art_semanticturkey.STRequests.SKOSXL
										.removeAltLabel(rdfSubject.getURI(),
												label, lang);
							} else {
								art_semanticturkey.STRequests.SKOSXL
										.removeHiddenLabel(rdfSubject.getURI(),
												label, lang);
							}

							break;
						case "http://www.w3.org/2000/01/rdf-schema#label":
							art_semanticturkey.STRequests.Property
									.removePropValue(rdfSubject
											.getNominalValue(), rdfPredicate
											.getNominalValue(), rdfObject
											.getLabel(), null, "plainLiteral",
											rdfObject.getLang());
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
					"onAdd" : function(rdfSubject, rdfPredicate, rdfObject) {
						// Based on sources by NScarpato
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
							parameters.forResource = rdfSubject
									.getNominalValue();

							window
									.openDialog(
											"chrome://semantic-turkey/content/editors/property/propertyTree.xul",
											"_blank",
											"modal=yes,resizable,centerscreen",
											parameters);
							var propType = parameters.selectedPropType;
							if (parameters.oncancel != false) {
								return;
							}

							predicateName = parameters.selectedProp;
						} else {
							predicateName = rdfPredicate.getNominalValue();
						}

						var changesDone = enrichProperty(rdfSubject
								.getNominalValue(), predicateName);
						if (changesDone) {
							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						}

					},
					"onRemove" : function(rdfSubject, rdfPredicate, rdfObject) {
						if (rdfObject.explicit == "true") {
							art_semanticturkey.STRequests.Resource
									.removePropertyValue(rdfSubject.toNT(),
											rdfPredicate.toNT(), rdfObject
													.toNT());
							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						} else {
							art_semanticturkey.Alert
									.alert("You cannot remove this type, it's a system resource!");
						}
					}
				});

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"types",
				{
					"partitionLabel" : "Types",
					"expectedContentType" : "objectList",
					"addTooltiptext" : "Add a type",
					"addIcon|fromRole" : "cls",
					"onAdd" : function(rdfSubject) { // Based on sources by
						// NScarpato
						var parameters = {};
						parameters.source = "editorIndividual";
						parameters.selectedClass = "";
						// parameters.parentWindow =
						// window.arguments[0].parentWindow;
						parameters.parentWindow = window;

						window
								.openDialog(
										"chrome://semantic-turkey/content/editors/class/classTree.xul",
										"_blank",
										"chrome,dependent,dialog,modal=yes,resizable,centerscreen",
										parameters);

						if (parameters.selectedClass != "") {
							var responseArray;

							if (rdfSubject.getRole() == "individual") {
								responseArray = art_semanticturkey.STRequests.Individual
										.addType(rdfSubject.getNominalValue(),
												parameters.selectedClass);
							} else {
								responseArray = art_semanticturkey.STRequests.Cls
										.addType(rdfSubject.getNominalValue(),
												parameters.selectedClass);
							}
							art_semanticturkey.evtMgr.fireEvent("addedType",
									(new art_semanticturkey.typeAddedClass(
											responseArray["instance"],
											responseArray["type"], true,
											rdfSubject.getRole())));
							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						}

					},
					"onRemove" : function(rdfSubject, rdfObject) { // Based on
						// sources
						// by
						// NScarpato
						var responseArray;

						if (rdfObject.explicit == "true") {
							if (rdfSubject.getRole() == "individual") {
								responseArray = art_semanticturkey.STRequests.Individual
										.removeType(rdfSubject
												.getNominalValue(), rdfObject
												.getNominalValue());
							} else {
								responseArray = art_semanticturkey.STRequests.Cls
										.removeType(rdfSubject
												.getNominalValue(), rdfObject
												.getNominalValue());
							}
							art_semanticturkey.evtMgr.fireEvent("removedType",
									(new art_semanticturkey.typeRemovedClass(
											responseArray["instance"],
											responseArray["type"])));
							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						} else {
							art_semanticturkey.Alert
									.alert("You cannot remove this type, it's a system resource!");
						}

					}
				});

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"supertypes",
				{
					"partitionLabel" : "Supertypes",
					"expectedContentType" : "objectList",
					"addTooltiptext" : "Add a super-type",
					"addIcon|fromRole" : "cls",
					"onAdd" : function(rdfSubject) { // Based on sources by
						// NScarpato
						var parameters = {};
						parameters.source = "editorClass";
						parameters.selectedClass = "";
						// parameters.parentWindow =
						// window.arguments[0].parentWindow;
						parameters.parentWindow = window;

						window
								.openDialog(
										"chrome://semantic-turkey/content/editors/class/classTree.xul",
										"_blank",
										"chrome,dependent,dialog,modal=yes,resizable,centerscreen",
										parameters);

						if (parameters.selectedClass != "") {
							var responseArray = art_semanticturkey.STRequests.Cls
									.addSuperCls(rdfSubject.getNominalValue(),
											parameters.selectedClass);
							var classRes = responseArray["class"];
							var superClassRes = responseArray["superClass"];
							art_semanticturkey.evtMgr.fireEvent(
									"subClsOfAddedClass",
									(new art_semanticturkey.subClsOfAddedClass(
											classRes, superClassRes)));
							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						}

					},
					"onRemove" : function(rdfSubject, rdfObject) { // Based on
						// sources
						// by
						// NScarpato
						if (rdfObject.explicit == "true") {
							var responseArray = art_semanticturkey.STRequests.Cls
									.removeSuperCls(rdfSubject
											.getNominalValue(), rdfObject
											.getNominalValue());
							// art_semanticturkey.refreshPanel();
							var classRes = responseArray["class"];
							var superClassRes = responseArray["superClass"];
							art_semanticturkey.evtMgr
									.fireEvent(
											"subClsOfRemovedClass",
											(new art_semanticturkey.subClsOfRemovedClass(
													classRes, superClassRes)));
							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						} else {
							art_semanticturkey.Alert
									.alert("You cannot remove this type, it's a system resource!");
						}

					}
				});

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"superproperties",
				{
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
						window
								.openDialog(
										"chrome://semantic-turkey/content/editors/property/propertyTree.xul",
										"_blank",
										"modal=yes,resizable,centerscreen",
										parameters);
						if (parameters.selectedProp != "") {
							art_semanticturkey.STRequests.Property
									.addSuperProperty(rdfSubject
											.getNominalValue(),
											parameters.selectedProp);
						}
						art_semanticturkey.evtMgr.fireEvent("refreshEditor",
								(new art_semanticturkey.genericEventClass()));
					},
					"onRemove" : function(rdfSubject, rdfObject) { // Based on
						// sources
						// by
						// NScarpato
						if (rdfObject.explicit == "true") {
							art_semanticturkey.STRequests.Property
									.removeSuperProperty(rdfSubject
											.getNominalValue(), rdfObject
											.getNominalValue());
							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						} else {
							art_semanticturkey.Alert
									.alert("You cannot remove this type, it's a system resource!");
						}

					}
				});

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"domains",
				{
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
						// parameters.parentWindow =
						// window.arguments[0].parentWindow;
						parameters.parentWindow = window;

						window
								.openDialog(
										"chrome://semantic-turkey/content/editors/class/classTree.xul",
										"_blank",
										"chrome,dependent,dialog,modal=yes,resizable,centerscreen",
										parameters);
						var domainName = parameters.domainName;
						if (domainName != "none domain selected") {
							art_semanticturkey.STRequests.Property
									.addPropertyDomain(rdfSubject
											.getNominalValue(), domainName);
							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						}
					},
					"onRemove" : function(rdfSubject, rdfObject) { // Based on
						// sources
						// by
						// NScarpato
						if (rdfObject.explicit == "true") {
							art_semanticturkey.STRequests.Property
									.removePropertyDomain(rdfSubject
											.getNominalValue(), rdfObject
											.getNominalValue());
							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						} else {
							art_semanticturkey.Alert
									.alert("You cannot remove this type, it's a system resource!");
						}
					}
				});

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"ranges",
				{
					"partitionLabel" : "Ranges",
					"addTooltiptext" : "Add a range",
					"expectedContentType" : "objectList",
					"addIcon|fromRole" : "cls",
					"onAdd" : function(rdfSubject) { // Based on sources by
						// NScarpato

						var parameters = {};
						parameters.source = "range";
						parameters.rangeName = "";

						if (rdfSubject.getRole().toLowerCase().indexOf(
								"objectproperty") != -1) {
							// parameters.parentWindow =
							// window.arguments[0].parentWindow;
							parameters.parentWindow = window;
							window
									.openDialog(
											"chrome://semantic-turkey/content/editors/class/classTree.xul",
											"_blank",
											"chrome,dependent,dialog,modal=yes,resizable,centerscreen",
											parameters);
							if (parameters.rangeName != "") {
								art_semanticturkey.STRequests.Property
										.addPropertyRange(rdfSubject
												.getNominalValue(),
												parameters.rangeName);
								art_semanticturkey.evtMgr
										.fireEvent(
												"refreshEditor",
												(new art_semanticturkey.genericEventClass()));
							}

						} else {
							window
									.openDialog(
											"chrome://semantic-turkey/content/editors/property/rangeList.xul",
											"_blank",
											"modal=yes,resizable,centerscreen",
											parameters);
							if (parameters.rangeName != "") {
								art_semanticturkey.STRequests.Property
										.addPropertyRange(rdfSubject
												.getNominalValue(),
												parameters.rangeName);
								art_semanticturkey.evtMgr
										.fireEvent(
												"refreshEditor",
												(new art_semanticturkey.genericEventClass()));
							}
						}
					},
					"onRemove" : function(rdfSubject, rdfObject) { // Based on
						// sources
						// by
						// NScarpato
						if (rdfObject.explicit == "true") {

							art_semanticturkey.STRequests.Property
									.removePropertyRange(rdfSubject
											.getNominalValue(), rdfObject
											.getNominalValue());
							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						} else {
							art_semanticturkey.Alert
									.alert("You cannot remove this type, it's a system resource!");
						}
					}
				});

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"broaders",
				{
					"partitionLabel" : "Broaders",
					"expectedContentType" : "objectList",
					"addTooltiptext" : "Add a broader concept",
					"addIcon|fromRole" : "concept",
					"onAdd" : function(rdfSubject) {
						var parameters = {};
						parameters.conceptScheme = "*"; // TODO which concept
						// scheme?
						// parameters.parentWindow =
						// window.arguments[0].parentWindow;
						parameters.parentWindow = window;
						window
								.openDialog(
										"chrome://semantic-turkey/content/skos/editors/concept/conceptTree.xul",
										"_blank",
										"chrome,dependent,dialog,modal=yes,resizable,centerscreen",
										parameters);

						if (typeof parameters.out == "undefined"
								|| typeof parameters.out.selectedConcept == "undefined")
							return;

						art_semanticturkey.STRequests.SKOS.addBroaderConcept(
								rdfSubject.getNominalValue(),
								parameters.out.selectedConcept);
						art_semanticturkey.evtMgr.fireEvent("refreshEditor",
								(new art_semanticturkey.genericEventClass()));
					},
					"onRemove" : function(rdfSubject, rdfObject) {
						if (rdfObject.explicit == "true") {

							art_semanticturkey.STRequests.SKOS
									.removeBroaderConcept(rdfSubject
											.getNominalValue(), rdfObject
											.getNominalValue());
							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						} else {
							art_semanticturkey.Alert
									.alert("You cannot remove this type, it's a system resource!");
						}
					}
				});

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"topconcepts",
				{
					"partitionLabel" : "Top Concepts",
					"expectedContentType" : "objectList",
					"addTooltiptext" : "Add a top concept",
					"addIcon|fromRole" : "concept",
					"onAdd" : function(rdfSubject) {
						var parameters = {};
						parameters.conceptScheme = "*"; // TODO which concept
						// scheme?
						// parameters.parentWindow =
						// window.arguments[0].parentWindow;
						parameters.parentWindow = window;
						window
								.openDialog(
										"chrome://semantic-turkey/content/skos/editors/concept/conceptTree.xul",
										"_blank",
										"chrome,dependent,dialog,modal=yes,resizable,centerscreen",
										parameters);

						if (typeof parameters.out == "undefined"
								|| typeof parameters.out.selectedConcept == "undefined")
							return;

						art_semanticturkey.STRequests.SKOS.addTopConcept(
								rdfSubject.getNominalValue(),
								parameters.out.selectedConcept);

						art_semanticturkey.evtMgr.fireEvent("refreshEditor",
								(new art_semanticturkey.genericEventClass()));
					},
					"onRemove" : function(rdfSubject, rdfObject) {
						if (rdfObject.explicit == "true") {

							art_semanticturkey.STRequests.SKOS
									.removeTopConcept(rdfSubject
											.getNominalValue(), rdfObject
											.getNominalValue());
							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						} else {
							art_semanticturkey.Alert
									.alert("You cannot remove this type, it's a system resource!");
						}
					}
				});

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"facets",
				{
					"partitionLabel" : "Property facets",
					"expectedContentType" : "mixed",
					"renderHelper" : function(subjectResource,
							responsePartition, containingElement) { // Based on
						// sources
						// by
						// NScarpato
						var doc = containingElement.ownerDocument;
						var checkboxContainer = doc.createElement("hbox");

						function createCheckbox(facetName, associatedOwlClass) {
							var facetElem = responsePartition
									.getElementsByTagName(facetName)[0];

							var isEnabled = facetElem ? (facetElem
									.getAttribute("explicit") == "true" && art_semanticturkey.resourceView
									.isEditable(subjectResource))
									: art_semanticturkey.resourceView
											.isEditable(subjectResource);

							var checkboxElement = doc.createElement("checkbox");
							checkboxElement.setAttribute("label", facetName);
							checkboxElement
									.setAttribute("disabled", !isEnabled);
							checkboxElement.setAttribute("checked",
									facetElem ? facetElem.getAttribute("value")
											: "false");
							checkboxElement
									.setAttribute("st-associatedOwlClass",
											associatedOwlClass);

							checkboxContainer.appendChild(checkboxElement);

						}

						function facetCheckboxEventHandler(event) {
							var checked = event.target.checked;
							var associatedOwlClass = event.target
									.getAttribute("st-associatedOwlClass");

							try {
								if (checked) { // Add facet
									art_semanticturkey.STRequests.Property
											.addExistingPropValue(
													subjectResource
															.getNominalValue(),
													"http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
													associatedOwlClass, "uri");
								} else { // Remove facet
									art_semanticturkey.STRequests.Property
											.removePropValue(
													subjectResource
															.getNominalValue(),
													"http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
													associatedOwlClass, null,
													"uri");
								}
								art_semanticturkey.evtMgr
										.fireEvent(
												"refreshEditor",
												(new art_semanticturkey.genericEventClass()));
							} catch (e) {
								art_semanticturkey.Alert.alert(e);
							}

						}

						createCheckbox("symmetric",
								"http://www.w3.org/2002/07/owl#SymmetricProperty");
						createCheckbox("functional",
								"http://www.w3.org/2002/07/owl#FunctionalProperty");
						createCheckbox("inverseFunctional",
								"http://www.w3.org/2002/07/owl#InverseFunctionalProperty");
						createCheckbox("transitive",
								"http://www.w3.org/2002/07/owl#TransitiveProperty");

						containingElement.appendChild(checkboxContainer);

						checkboxContainer.addEventListener("command",
								facetCheckboxEventHandler);

						return {
							excludedSections : [ "symmetric", "functional",
									"inverseFunctional", "transitive" ]
						}
					}

				});

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"inverseof",
				{
					"partitionLabel" : "Inverse of",
					"expectedContentType" : "objectList",
					"addTooltiptext" : "Add new property",
					"addIcon|fromRole" : "individual",
					"onAdd" : function(rdfSubject) { // Based on sources by
						// NScarpato
						var sourceElementName = rdfSubject.getNominalValue();

						var responseXML = art_semanticturkey.STRequests.Property
								.getRange("owl:inverseOf", "false");
						var ranges = responseXML.getElementsByTagName("ranges")[0];
						var type = (ranges.getAttribute("rngType"));

						var parameters = new Object();
						parameters.selectedProp = "";
						parameters.selectedPropType = "";
						parameters.oncancel = false;
						parameters.type = type;
						window
								.openDialog(
										"chrome://semantic-turkey/content/editors/property/propertyTree.xul",
										"_blank",
										"modal=yes,resizable,centerscreen",
										parameters);

						if (parameters.oncancel == false) {
							art_semanticturkey.STRequests.Property
									.addExistingPropValue(sourceElementName,
											"owl:inverseOf",
											parameters.selectedProp, type);
							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						}

					},
					"onRemove" : function(rdfSubject, rdfObject) { // Based on
						// sources
						// by
						// NScarpato
						if (rdfObject.explicit == "true") {

							art_semanticturkey.STRequests.Property
									.removePropValue(rdfSubject
											.getNominalValue(),
											"owl:inverseOf", rdfObject
													.getNominalValue(), null,
											rdfObject.isURIResource() ? "uri"
													: "bnode");

							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						} else {
							art_semanticturkey.Alert
									.alert("You cannot remove this type, it's a system resource!");
						}

					}
				});

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"topconceptof",
				{
					"partitionLabel" : "Top concept of",
					"expectedContentType" : "objectList",
					"addTooltiptext" : "Add to concept scheme as top concept",
					"addIcon|fromRole" : "scheme",
					"onAdd" : function(rdfSubject) {
						var parameters = {};

						window
								.openDialog(
										"chrome://semantic-turkey/content/skos/editors/scheme/schemeList.xul",
										"dlg",
										"chrome=yes,dialog,resizable=yes,modal,centerscreen",
										parameters);

						if (typeof parameters.out == "undefined") {
							return;
						}

						art_semanticturkey.STRequests.SKOS.addTopConcept(
								parameters.out.selectedScheme, rdfSubject
										.getNominalValue());

						art_semanticturkey.evtMgr.fireEvent("refreshEditor",
								(new art_semanticturkey.genericEventClass()));
					},
					"onRemove" : function(rdfSubject, rdfObject) {
						if (rdfObject.explicit == "true") {

							art_semanticturkey.STRequests.SKOS
									.removeTopConcept(rdfObject
											.getNominalValue(), rdfSubject
											.getNominalValue());

							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						} else {
							art_semanticturkey.Alert
									.alert("You cannot remove this concept scheme, it's a system resource!");
						}

					}
				});

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"schemes",
				{
					"partitionLabel" : "Schemes",
					"expectedContentType" : "objectList",
					"addTooltiptext" : "Add to a concept scheme",
					"addIcon|fromRole" : "scheme",
					"onAdd" : function(rdfSubject) {
						alert("added!");

						art_semanticturkey.evtMgr.fireEvent("refreshEditor",
								(new art_semanticturkey.genericEventClass()));
					},
					"onRemove" : function(rdfSubject, rdfObject) {
						if (rdfObject.explicit == "true") {

							alert("removed!");

							art_semanticturkey.evtMgr
									.fireEvent(
											"refreshEditor",
											(new art_semanticturkey.genericEventClass()));
						} else {
							art_semanticturkey.Alert
									.alert("You cannot remove this concept scheme, it's a system resource!");
						}

					}
				});