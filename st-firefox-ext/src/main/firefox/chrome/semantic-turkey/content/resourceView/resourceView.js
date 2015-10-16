if (typeof art_semanticturkey == "undefined") {
	var art_semanticturkey = {};
}

Components.utils.import("resource://stservices/SERVICE_Property.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Cls.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Individual.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Resource.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOSXL.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_CustomRanges.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Manchester.jsm", art_semanticturkey);

Components.utils.import("resource://stmodules/Deserializer.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ResourceViewLauncher.jsm", art_semanticturkey);

Components.utils.import("resource://stservices/SERVICE_ResourceView.jsm", art_semanticturkey);

Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/AnnotationCommons.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ARTResources.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/STResUtils.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Preferences.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

if (typeof art_semanticturkey.resourceView == "undefined") {
	art_semanticturkey.resourceView = {};
}

art_semanticturkey.resourceView.init = function() {
	// --------------------
	// Parses the arguments

	var parametersFromURL = art_semanticturkey.resourceView.utils.parseQueryString(window.location.search);

	var resource = parametersFromURL.resource;
	var resourcePosition = parametersFromURL.resourcePosition || null;

	if (typeof resource != "string") {
		art_semanticturkey.Alert.alert("Missing argument for mandatory parameter \"resource\"");
		window.close();
		return;
	}

	// --------------------------
	// Initializes basic behavior

	document.getElementById("closeButton").addEventListener("command", function(){window.close();}, false);
	
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

	var resourceNameBox = document.getElementById("resourceNameBox");

	var renameResourceButton = document.getElementById("alignResourceButton");
	renameResourceButton.addEventListener("command", art_semanticturkey.resourceView.doAlignResource, false);

	// -------------------------------------------------
	// Request and rendering of the resource description

	try {
		var response = art_semanticturkey.STRequests.ResourceView.getResourceView(resource, resourcePosition);
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

	// Handles the resource role icon
	var resourceRoleIconElement = document.getElementById("resourceRoleIcon");
	var roleIcon = art_semanticturkey.STResUtils.getImageSrcOrNull(resourceObj);
	resourceRoleIconElement.setAttribute("src", roleIcon);

	var resourceDecorationDeck = document.getElementById("resourceDecorationDeck");
	resourceDecorationDeck.selectedIndex = 0;

	if (roleIcon == null && typeof resourceObj.lang != "undefined" && resourceObj.lang != null) {
		var resourceLangBox = document.getElementById("resourceLangBox");
		resourceLangBox.setAttribute("value", resourceObj.lang);
		resourceDecorationDeck.selectedIndex = 1;
	}

	// Handles the resource editability (rename button)
	var isResourceEditable = art_semanticturkey.resourceView.isEditable(resourceObj);

	var renameResourceButton = document.getElementById("renameResourceButton");
	renameResourceButton.disabled = !(isResourceEditable && resourceObj.isURIResource());

	// Handles the resource editability (align button)
	var alignResourceButton = document.getElementById("alignResourceButton");
	alignResourceButton.disabled = !(isResourceEditable && resourceObj.isURIResource());

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
	// description so that it can be processed as the other partitions

	if (resourceObj.isURIResource()) {
		var defaultAnnotationFamily = art_semanticturkey.annotation.AnnotationManager.getDefaultFamily();

		if (typeof defaultAnnotationFamily.getAnnotatedContentResources != "undefined") {
			var annotatedContentResources = defaultAnnotationFamily.getAnnotatedContentResources(resourceObj
					.getNominalValue());
			art_semanticturkey.resourceView.getWebLinks_RESPONSE(partitionsBox, annotatedContentResources);
		}

		if (resourceObj.getRole() == "concept") {
			var responseXML = art_semanticturkey.STRequests.Annotation.getBookmarksByTopic(resourceObj
					.getNominalValue());
			art_semanticturkey.resourceView.getBookmarksByTopic_RESPONSE(partitionsBox, responseXML);
		}
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

art_semanticturkey.resourceView.doAlignResource = function(event) {

	try {
		var resourceNameBox = document.getElementById("resourceNameBox");

		var parameters = {};
		parameters.resource = resourceNameBox.stRdfNode.getNominalValue();
		parameters.resourceType = resourceNameBox.stRdfNode.getRole();

		var newWindow = window.openDialog("chrome://semantic-turkey/content/alignment/alignment.xul", "dlg",
				"chrome=yes,dialog,resizable=yes,modal,centerscreen", parameters);

		if (newWindow.onaccept == true) {
			art_semanticturkey.resourceView.refreshView();
		}

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
art_semanticturkey.resourceView.getWebLinks_RESPONSE = function(partitionsBox, annotatedContentResources) {
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
 * Populate the resource view with the web links whose topic is the current SKOS concept
 * 
 * @param {}
 *            responseElement
 */
art_semanticturkey.resourceView.getBookmarksByTopic_RESPONSE = function(partitionsBox, responseElement) {
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

window.addEventListener("load", art_semanticturkey.resourceView.init, true);

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

		obj.addIcon = art_semanticturkey.STResUtils.getImageSrcOrNull(new art_semanticturkey.ARTURIResource(
				"foo", stereotypicalRole, "http://foo.it"), "add");
	}

	return obj;
};
art_semanticturkey.resourceView.partitions.internal.defaultPartitionRender = function(subjectResource,
		responsePartition, partitionsBox) {

	var outerThis = this;

	var partitionName = responsePartition.tagName;

	var editable = art_semanticturkey.resourceView.isEditable(subjectResource);

	// Handles the partition header
	var partitionGroupBox = document.createElement("groupbox");
	var partitionCaption = document.createElement("caption");

	var partitionLabelElement = document.createElement("label");
	partitionLabelElement.setAttribute("value", this.partitionLabel);
	partitionCaption.appendChild(partitionLabelElement);

	var partitionButton = null;
	var operations = [];

	var addSupported = false;
	var removeSupported = false;

	var interestingHandlerSection = null;

	if (typeof this.expectedContentType != "undefined") {
		if (this.expectedContentType == "predicateObjectsList") {
			interestingHandlerSection = "predicate";
		} else if (this.expectedContentType == "objectList") {
			interestingHandlerSection = "objects";
		}
	}

	if (interestingHandlerSection != null) {
		if (typeof this[interestingHandlerSection] != "undefined"
				&& typeof this[interestingHandlerSection].add != "undefined") {
			addSupported = true;
			removeSupported = true; // TODO: not truly accurate
			partitionButton = document.createElement("toolbarbutton");
			if (this[interestingHandlerSection].add.label) {
				partitionButton.setAttribute("tooltiptext", this[interestingHandlerSection].add.label);
			}
			partitionButton.setAttribute("disabled", "" + (!editable));
			partitionButton.setAttribute("image", this.addIcon);
			partitionButton.setAttribute("st-partitionName", partitionName);

			var partitionActions = [];

			if (typeof this[interestingHandlerSection].add.action == "function") {
				partitionActions.push(this[interestingHandlerSection].add);
			} else if (this[interestingHandlerSection].add.actions instanceof Array) {
				partitionActions = partitionActions.concat(this[interestingHandlerSection].add.actions);
			}

			var enabledFilter = art_semanticturkey.resourceView.partitions.internal
					.createEnabledActionFilter(subjectResource);

			partitionActions = partitionActions.filter(enabledFilter);

			if (partitionActions.length == 0) {
				partitionButton.setAttribute("disabled", "true");
			} else if (partitionActions.length == 1) {
				partitionButton
						.addEventListener(
								"command",
								art_semanticturkey.resourceView.partitions.internal
										.wrapFunctionWithErrorManagement(art_semanticturkey.resourceView.partitions.internal
												.createAddHandlerFromAction(outerThis,
														partitionActions[0].action, subjectResource)), false);
			} else {
				partitionButton.setAttribute("type", "menu");
				var menupop = document.createElement("menupopup");

				for (var i = 0; i < partitionActions.length; i++) {
					var anAction = partitionActions[i];
					var menuitem = document.createElement("menuitem");
					menuitem.setAttribute("label", anAction.label || "");

					menuitem
							.addEventListener(
									"command",
									art_semanticturkey.resourceView.partitions.internal
											.wrapFunctionWithErrorManagement(art_semanticturkey.resourceView.partitions.internal
													.createAddHandlerFromAction(outerThis, anAction.action,
															subjectResource)), false);
					menupop.appendChild(menuitem);
				}

				partitionButton.appendChild(menupop);
			}
		}
	}

	if (addSupported) {
		operations.push("add");
	}

	if (removeSupported) {
		operations.push("remove");
	}

	if (partitionButton != null) {
		partitionCaption.appendChild(partitionButton);
	}

	partitionGroupBox.appendChild(partitionCaption);

	// Handles the partition content

	var partitionContent = responsePartition.children[0];

	if (typeof partitionContent != "undefined") {

		// A predicateObjectsList
		if (this.expectedContentType == "mixed") {

			var rv = {};

			if (typeof this.renderHelper != "undefined") {
				rv = this.renderHelper(subjectResource, responsePartition, partitionGroupBox) || rv;
			}

			var excludedSections = excludedSections = rv.excludedSections || [];

			var childrenElement = partitionGroupBox;

			if (typeof rv.childrenElement != "undefined") {
				childrenElement = rv.childrenElement;
			}

			for (var i = 0; i < responsePartition.children.length; i++) {
				if (excludedSections.indexOf(responsePartition.children[i].tagName) != -1)
					continue;

				var subPartitionHandler = art_semanticturkey.resourceView.partitions
						.getPartitionHandler(responsePartition.children[i].tagName);
				subPartitionHandler.render(subjectResource, responsePartition.children[i], childrenElement);

			}

		} else if ((this.expectedContentType == "predicateObjectsList")
				|| art_semanticturkey.resourceView.partitions.internal
						.isPredicateObjectsList(partitionContent)) {
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

				predicateObjectsBox.addEventListener("predicateObjectsEvent",
						art_semanticturkey.resourceView.partitions.internal.predicateObjectsEventHandler);

				predicateObjectsBox.setAttribute("st-partitionName", partitionName);

				partitionGroupBox.appendChild(predicateObjectsBox);
			}
		} else { // Otherwise, assume an objects list
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

art_semanticturkey.resourceView.partitions.internal.createAddHandlerFromAction = function(handler,
		partitionAction, subjectResource) {
	return function() {
		var rv = partitionAction.call(handler, subjectResource);

		if (typeof rv != "object")
			return;

		art_semanticturkey.resourceView.partitions.internal.handlePredicateProcessingAlternatives(handler,
				subjectResource, rv);
	}
};

art_semanticturkey.resourceView.partitions.internal.handlePredicateProcessingAlternatives = function(handler,
		rdfSubject, rdfPredicate) {
	var actions = art_semanticturkey.resourceView.partitions.internal.getAddActionsForProperty(handler,
			rdfSubject, rdfPredicate);

	if (actions.length == 0) {
		throw new Error("No handler for the event");
	}

	var anAction = null;

	if (actions.length > 1) {
		var selectList = actions.map(function(val) {
			return val.label || ""
		});

		var promptService = Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
				.getService(Components.interfaces.nsIPromptService);

		var param = {};
		var isOk = promptService.select(window, "Select a handler", "", selectList.length, selectList, param);

		if (isOk) {
			anAction = actions[param.value];
		}
	} else {
		anAction = actions[0];
	}

	if (anAction != null) {
		anAction.action.call(handler, rdfSubject, rdfPredicate);
	}
}

art_semanticturkey.resourceView.partitions.internal.getAddActionsForProperty = function(handler, rdfSubject,
		property) {
	if (typeof handler["predicateObjects"] == "undefined")
		return [];

	var enabledFilter = art_semanticturkey.resourceView.partitions.internal
			.createEnabledActionFilter(rdfSubject);

	var container = undefined;

	if (typeof handler["predicateObjects"][property.getNominalValue()] != "undefined") {
		container = handler["predicateObjects"][property.getNominalValue()];
	} else if (typeof handler["predicateObjects"]["*"] != "undefined") {
		container = handler["predicateObjects"]["*"];
	}

	if (typeof container != "undefined") {
		if (typeof container.add == "undefined")
			return [];

		if (container.add.actions instanceof Array) {
			return container.add.actions.filter(enabledFilter);
		}

		if (typeof container.add.action == "function") {
			return [ container.add ].filter(enabledFilter);
		}
	}

	return [];
};

art_semanticturkey.resourceView.partitions.internal.getRemoveActionForProperty = function(handler, property) {
	if (typeof handler["predicateObjects"] == "undefined")
		return function() {
		};

	var container = undefined;

	if (typeof handler["predicateObjects"][property.getNominalValue()] != "undefined") {
		container = handler["predicateObjects"][property.getNominalValue()];
	} else if (typeof handler["predicateObjects"]["*"] != "undefined") {
		container = handler["predicateObjects"]["*"];
	}

	if (typeof container != "undefined") {
		if (typeof container.remove == "undefined")
			return function() {
			};

		if (typeof container.remove.action == "function") {
			return container.remove.action;
		}
	}

	return function() {
	};
};

art_semanticturkey.resourceView.partitions.internal.wrapFunctionWithErrorManagement = function(aFun) {
	return function() {
		try {
			aFun.apply(null, arguments);
		} catch (e) {
			art_semanticturkey.Alert.alert(e);
		}
	};
};

art_semanticturkey.resourceView.partitions.internal.isPredicateObjectsList = function(element) {
	return element && element.tagName == "collection" && element.children[0]
			&& element.children[0].tagName == "predicateObjects";
};

art_semanticturkey.resourceView.partitions.internal.dblClickHandler = function(event) {
	var resourceNameElement = document.getElementById("resourceNameBox");
	var subjectObj = resourceNameElement.stRdfNode;

	var res = event.detail.rdfResource;

	if (res.isResource()) {
		// If the resource to be shown is a bnode, then assume it is always located together with the subject
		// resource
		if (res.isBNode()) {
			art_semanticturkey.ResourceViewLauncher.openResourceView(res.getNominalValue(),
					subjectObj.resourcePosition);
		} else {
			art_semanticturkey.ResourceViewLauncher.openResourceView(res.getNominalValue());
		}
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
			art_semanticturkey.resourceView.partitions.internal.handlePredicateProcessingAlternatives(
					handler, rdfSubject, rdfPredicate);
		} else if (button == "remove") {
			var fun = art_semanticturkey.resourceView.partitions.internal.getRemoveActionForProperty(handler,
					rdfPredicate);
			fun.call(handler, rdfSubject, rdfPredicate, rdfObject);
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
		if (button == "remove") {
			if (typeof handler["objects"] != "undefined" && typeof handler["objects"].remove != "undefined"
					&& typeof handler["objects"].remove.action == "function") {
				handler["objects"].remove.action(rdfSubject, rdfObject);
			} else {
				throw new Error("Cannot find a determined function to invoke");
			}
		}
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
};

art_semanticturkey.resourceView.partitions.internal.getButtonType = function(button) {
	return button.getAttribute("label");
};

art_semanticturkey.resourceView.partitions.internal.createEnabledActionFilter = function(rdfSubject) {
	return (function(anAction) {
		if (typeof anAction.enabled == "undefined")
			return true;

		return anAction.enabled(rdfSubject);
	});
};

art_semanticturkey.resourceView.partitions.internal.existingClassTemplate = function(innerFunction) {
	return {
		label : "Add existing class",
		action : function(rdfSubject, rdfPredicate) { // Based on sources by
			// NScarpato
			var parameters = {};
			parameters.source = "editorClass";
			parameters.selectedClass = "";
			// parameters.parentWindow =
			// window.arguments[0].parentWindow;
			parameters.parentWindow = window;

			window.openDialog("chrome://semantic-turkey/content/editors/class/classTree.xul", "_blank",
					"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);

			if (parameters.selectedClass != "") {
				if (typeof innerFunction == "undefined") {
					art_semanticturkey.STRequests.Property.addExistingPropValue(rdfSubject.getNominalValue(),
							rdfPredicate.getNominalValue(), parameters.selectedClass, "uri");
				} else {
					innerFunction(rdfSubject, rdfPredicate, parameters.selectedClass);
				}

				art_semanticturkey.evtMgr.fireEvent("refreshEditor",
						(new art_semanticturkey.genericEventClass()));
			}
		}
	};
};

art_semanticturkey.resourceView.partitions.internal.classExpressionTemplate = function(innerFunction) {
	return {
		label : "Create and add class expression",
		action : function(rdfSubject, rdfPredicate) {
			var parameters = {
				expression : ""
			};
			window.openDialog(
					"chrome://semantic-turkey/content/editors/classExpression/classExpressionEditor.xul",
					"dlg", "chrome=yes,dialog,resizable=yes,modal,centerscreen", parameters);

			if (!!parameters.expression) {
				if (typeof innerFunction == "undefined") {

					art_semanticturkey.STRequests.Manchester.createRestriction(rdfSubject.getNominalValue(),
							rdfPredicate.getNominalValue(), parameters.expression);
				} else {
					innerFunction(rdfSubject, rdfPredicate, parameters.expression);
				}
				art_semanticturkey.evtMgr.fireEvent("refreshEditor",
						(new art_semanticturkey.genericEventClass()));
			}
		}
	};
};

art_semanticturkey.resourceView.partitions.internal.clsRemoveTemplate = function(innerFunction) {
	return {
		"action" : function(rdfSubject, rdfPredicate, rdfObject) { // Based on
			// sources by NScarpato
			if (rdfObject.explicit == "true") {
				if (rdfObject.isBNode()) {
					art_semanticturkey.STRequests.Manchester.removeExpression(rdfSubject.getNominalValue(),
							rdfPredicate.getNominalValue(), rdfObject.toNT());
				} else {
					if (typeof innerFunction == "undefined") {
						art_semanticturkey.STRequests.Property.removePropValue(rdfSubject.getNominalValue(),
								rdfPredicate.getNominalValue(), rdfObject.getNominalValue(), null, rdfObject
										.isURIResource() ? "uri" : "bnode");
					} else {
						innerFunction(rdfSubject, rdfPredicate, rdfObject);
					}
				}
				art_semanticturkey.evtMgr.fireEvent("refreshEditor",
						(new art_semanticturkey.genericEventClass()));
			} else {
				art_semanticturkey.Alert.alert("You cannot remove this type, it's a system resource!");
			}

		}
	};
};
art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"lexicalizations",
				{
					"partitionLabel" : "Lexicalizations",
					"expectedContentType" : "predicateObjectsList",
					"predicate" : {
						"add" : {
							"label" : "Add a lexicalization",
							"action" : function(rdfSubject) {
								var parameters = {};
								parameters.resource = rdfSubject.getNominalValue();
								parameters.out = {};

								window.openDialog(
										"lexicalizationPropertyChooser/lexicalizationPropertyChooser.xul",
										"dlg", "chrome=yes,dialog,resizable=yes,modal,centerscreen",
										parameters);

								if (typeof parameters.out.chosenProperty == "undefined") {
									return;
								}

								return parameters.out.chosenProperty;
							}
						}
					},
					"predicateObjects" : {
						"*" : {
							"add" : {
								"actions" : [ {
									"action" : function(rdfSubject, rdfPredicate) {
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

										parameters.winTitle = "Add " + rdfPredicate.getShow()
												+ " Lexicalization";
										parameters.action = function(label, lang) {

											try {
												switch (predURI) {
												case "http://www.w3.org/2004/02/skos/core#prefLabel":
													art_semanticturkey.STRequests.SKOS.setPrefLabel(
															rdfSubject.getURI(), label, lang);
													break;
												case "http://www.w3.org/2004/02/skos/core#altLabel":
													art_semanticturkey.STRequests.SKOS.addAltLabel(rdfSubject
															.getURI(), label, lang);
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
													art_semanticturkey.STRequests.SKOSXL.addAltLabel(
															rdfSubject.getURI(), label, lang, "bnode");
													break;
												case "http://www.w3.org/2008/05/skos-xl#hiddenLabel":
													art_semanticturkey.STRequests.SKOSXL.addHiddenLabel(
															rdfSubject.getURI(), label, lang, "bnode");
													break;
												case "http://www.w3.org/2000/01/rdf-schema#label":
													art_semanticturkey.STRequests.Property
															.createAndAddPropValue(rdfSubject.getURI(),
																	predURI, label, null, "plainLiteral",
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
															"_blank", "modal=yes,resizable,centerscreen",
															parameters);
										} else {
											alert("Unsupported predicate type");
										}
									}
								} ]
							},
							"remove" : {
								action : function(rdfSubject, rdfPredicate, rdfObject) {
									switch (rdfPredicate.getNominalValue()) {
									case "http://www.w3.org/2004/02/skos/core#prefLabel":
										art_semanticturkey.STRequests.SKOS.removePrefLabel(rdfSubject
												.getURI(), rdfObject.getLabel(), rdfObject.getLang());
										break;
									case "http://www.w3.org/2004/02/skos/core#altLabel":
										art_semanticturkey.STRequests.SKOS.removeAltLabel(
												rdfSubject.getURI(), rdfObject.getLabel(), rdfObject
														.getLang());
										break;
									case "http://www.w3.org/2004/02/skos/core#hiddenLabel":
										art_semanticturkey.STRequests.SKOS.removeHiddenLabel(rdfSubject
												.getURI(), rdfObject.getLabel(), rdfObject.getLang());
										break;

									case "http://www.w3.org/2008/05/skos-xl#prefLabel":
									case "http://www.w3.org/2008/05/skos-xl#altLabel":
									case "http://www.w3.org/2008/05/skos-xl#hiddenLabel":
										var label = rdfObject.getShow();
										var lang = rdfObject.lang || null;

										if (rdfPredicate.getNominalValue() == "http://www.w3.org/2008/05/skos-xl#prefLabel") {
											art_semanticturkey.STRequests.SKOSXL.removePrefLabel(rdfSubject
													.getURI(), label, lang);
										} else if (rdfPredicate.getNominalValue() == "http://www.w3.org/2008/05/skos-xl#altLabel") {
											art_semanticturkey.STRequests.SKOSXL.removeAltLabel(rdfSubject
													.getURI(), label, lang);
										} else {
											art_semanticturkey.STRequests.SKOSXL.removeHiddenLabel(rdfSubject
													.getURI(), label, lang);
										}

										break;
									case "http://www.w3.org/2000/01/rdf-schema#label":
										art_semanticturkey.STRequests.Property.removePropValue(rdfSubject
												.getNominalValue(), rdfPredicate.getNominalValue(), rdfObject
												.getLabel(), null, "plainLiteral", rdfObject.getLang());
										break;
									}

									art_semanticturkey.resourceView.refreshView();
								}
							}
						}
					}
				});

art_semanticturkey.resourceView.partitions.registerPartitionHandler("properties", {
	"partitionLabel" : "Properties",
	"expectedContentType" : "predicateObjectsList",
	"predicate" : {
		"add" : {
			"label" : "Add a property value",
			"action" : function(rdfSubject) {
				// Based on sources by NScarpato
				var parameters = new Object();
				var selectedProp = "";
				var selectedPropType = "";
				parameters.selectedProp = selectedProp;
				parameters.selectedPropType = selectedPropType;
				parameters.oncancel = false;
				parameters.source = "AddNewProperty";
				parameters.type = "All";
				parameters.forResource = rdfSubject.getNominalValue();

				window.openDialog("chrome://semantic-turkey/content/editors/property/propertyTree.xul",
						"_blank", "modal=yes,resizable,centerscreen", parameters);
				var propType = parameters.selectedPropType;
				if (parameters.oncancel != false) {
					return;
				}

				predicateName = parameters.selectedProp;

				var rv = {};
				rv.getNominalValue = function() {
					return predicateName;
				};
				return rv;
			}
		}
	},
	"predicateObjects" : {
		"*" : {
			"add" : {
				"actions" : [ {
					"action" : function(rdfSubject, rdfPredicate) {
						var changesDone = enrichProperty(rdfSubject.getNominalValue(), rdfPredicate
								.getNominalValue());
						if (changesDone) {
							art_semanticturkey.evtMgr.fireEvent("refreshEditor",
									(new art_semanticturkey.genericEventClass()));
						}
					}
				} ]
			},
			"remove" : {
				"action" : function(rdfSubject, rdfPredicate, rdfObject) {
					if (rdfObject.explicit == "true") {
						if (rdfPredicate.hasCustomRange == "true" && rdfObject.isResource()) {
							art_semanticturkey.STRequests.CustomRanges.removeReifiedResource(rdfSubject
									.getNominalValue(), rdfPredicate.getNominalValue(), rdfObject
									.getNominalValue());
						} else {
							art_semanticturkey.STRequests.Resource.removePropertyValue(rdfSubject.toNT(),
									rdfPredicate.toNT(), rdfObject.toNT());
						}
						art_semanticturkey.evtMgr.fireEvent("refreshEditor",
								(new art_semanticturkey.genericEventClass()));
					} else {
						art_semanticturkey.Alert
								.alert("You cannot remove this type, it's a system resource!");
					}
				}
			}
		}
	}
});

art_semanticturkey.resourceView.partitions.registerPartitionHandler("types", {
	"partitionLabel" : "Types",
	"expectedContentType" : "objectList",
	"addTooltiptext" : "Add a type",
	"addIcon|fromRole" : "cls",
	"objects" : {
		"add" : {
			"label" : "Add a type",
			"action" : function(rdfSubject) { // Based on sources by NScarpato
				var parameters = {};
				parameters.source = "editorIndividual";
				parameters.selectedClass = "";
				// parameters.parentWindow =
				// window.arguments[0].parentWindow;
				parameters.parentWindow = window;

				window.openDialog("chrome://semantic-turkey/content/editors/class/classTree.xul", "_blank",
						"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);

				if (parameters.selectedClass != "") {
					var responseArray;

					if (rdfSubject.getRole() == "individual") {
						responseArray = art_semanticturkey.STRequests.Individual.addType(rdfSubject
								.getNominalValue(), parameters.selectedClass);
					} else {
						responseArray = art_semanticturkey.STRequests.Cls.addType(rdfSubject
								.getNominalValue(), parameters.selectedClass);
					}
					art_semanticturkey.evtMgr.fireEvent("addedType", (new art_semanticturkey.typeAddedClass(
							responseArray["instance"], responseArray["type"], true, rdfSubject.getRole())));
					art_semanticturkey.evtMgr.fireEvent("refreshEditor",
							(new art_semanticturkey.genericEventClass()));
				}

			}
		},
		"remove" : {
			"action" : function(rdfSubject, rdfObject) { // Based on
				// sources
				// by
				// NScarpato
				var responseArray;

				if (rdfObject.explicit == "true") {
					if (rdfSubject.getRole() == "individual") {
						responseArray = art_semanticturkey.STRequests.Individual.removeType(rdfSubject
								.getNominalValue(), rdfObject.getNominalValue());
					} else {
						responseArray = art_semanticturkey.STRequests.Cls.removeType(rdfSubject
								.getNominalValue(), rdfObject.getNominalValue());
					}
					art_semanticturkey.evtMgr.fireEvent("removedType",
							(new art_semanticturkey.typeRemovedClass(responseArray["instance"],
									responseArray["type"])));
					art_semanticturkey.evtMgr.fireEvent("refreshEditor",
							(new art_semanticturkey.genericEventClass()));
				} else {
					art_semanticturkey.Alert.alert("You cannot remove this type, it's a system resource!");
				}

			}
		}
	}
});

art_semanticturkey.resourceView.partitions.registerPartitionHandler("classaxioms", (function() {

	return {
		"partitionLabel" : "Class axioms",
		"expectedContentType" : "predicateObjectsList",
		"addIcon|fromRole" : "cls",
		"predicate" : {
			"add" : {
				"label" : "Add a class axiom",
				"actions" : [
						{
							"label" : "Equivalent class",
							"action" : function() {
								return new art_semanticturkey.ARTURIResource(
										"http://www.w3.org/2002/07/owl#equivalentClass", "property",
										"http://www.w3.org/2002/07/owl#equivalentClass");
							}
						},
						{
							"label" : "Subclass of",
							"action" : function() {
								return new art_semanticturkey.ARTURIResource(
										"http://www.w3.org/2000/01/rdf-schema#subClassOf", "property",
										"http://www.w3.org/2000/01/rdf-schema#subClassOf");
							}
						},
						{
							"label" : "Disjoint with",
							"action" : function() {
								return new art_semanticturkey.ARTURIResource(
										"http://www.w3.org/2002/07/owl#disjointWith", "property",
										"http://www.w3.org/2002/07/owl#disjointWith");
							}
						},
						{
							"label" : "Complement of",
							"action" : function() {
								return new art_semanticturkey.ARTURIResource(
										"http://www.w3.org/2002/07/owl#complementOf", "property",
										"http://www.w3.org/2002/07/owl#complementOf");
							}
						},
						{
							"label" : "Intersection of",
							"action" : function() {
								return new art_semanticturkey.ARTURIResource(
										"http://www.w3.org/2002/07/owl#intersectionOf", "property",
										"http://www.w3.org/2002/07/owl#intersectionOf");
							}
						},
						{
							"label" : "One of",
							"action" : function() {
								return new art_semanticturkey.ARTURIResource(
										"http://www.w3.org/2002/07/owl#oneOf", "property",
										"http://www.w3.org/2002/07/owl#oneOf");
							}
						},
						{
							"label" : "Union of",
							"action" : function() {
								return new art_semanticturkey.ARTURIResource(
										"http://www.w3.org/2002/07/owl#unionOf", "property",
										"http://www.w3.org/2002/07/owl#unionOf");
							}
						} ]
			}
		},
		"predicateObjects" : {
			"http://www.w3.org/2002/07/owl#equivalentClass" : {
				"add" : {
					"actions" : [
							art_semanticturkey.resourceView.partitions.internal.existingClassTemplate(),
							art_semanticturkey.resourceView.partitions.internal.classExpressionTemplate() ]
				},
				"remove" : art_semanticturkey.resourceView.partitions.internal.clsRemoveTemplate()
			},
			"http://www.w3.org/2000/01/rdf-schema#subClassOf" : {
				"add" : {
					"actions" : [
							art_semanticturkey.resourceView.partitions.internal
									.existingClassTemplate(function(rdfSubject, rdfPredicate, classUri) {
										var responseArray = art_semanticturkey.STRequests.Cls.addSuperCls(
												rdfSubject.getNominalValue(), classUri);
										var classRes = responseArray["class"];
										var superClassRes = responseArray["superClass"];
										art_semanticturkey.evtMgr.fireEvent("subClsOfAddedClass",
												(new art_semanticturkey.subClsOfAddedClass(classRes,
														superClassRes)));
									}),
							art_semanticturkey.resourceView.partitions.internal.classExpressionTemplate() ]
				},
				"remove" : art_semanticturkey.resourceView.partitions.internal.clsRemoveTemplate(function(
						rdfSubject, rdfPredicate, rdfObject) {
					var responseArray = art_semanticturkey.STRequests.Cls.removeSuperCls(rdfSubject
							.getNominalValue(), rdfObject.getNominalValue());
					// art_semanticturkey.refreshPanel();
					var classRes = responseArray["class"];
					var superClassRes = responseArray["superClass"];
					art_semanticturkey.evtMgr.fireEvent("subClsOfRemovedClass",
							(new art_semanticturkey.subClsOfRemovedClass(classRes, superClassRes)));
				})
			},
			"http://www.w3.org/2002/07/owl#disjointWith" : {
				"add" : {
					"actions" : [
							art_semanticturkey.resourceView.partitions.internal.existingClassTemplate(),
							art_semanticturkey.resourceView.partitions.internal.classExpressionTemplate() ]
				},
				"remove" : art_semanticturkey.resourceView.partitions.internal.clsRemoveTemplate()
			},
			"http://www.w3.org/2002/07/owl#complementOf" : {
				"add" : {
					"actions" : [
							art_semanticturkey.resourceView.partitions.internal.existingClassTemplate(),
							art_semanticturkey.resourceView.partitions.internal.classExpressionTemplate() ]
				},
				"remove" : art_semanticturkey.resourceView.partitions.internal.clsRemoveTemplate()
			},
			"http://www.w3.org/2002/07/owl#intersectionOf" : {
				"add" : {
					"action" : function(rdfSubject, rdfPredicate) {
						var parameters = {
							list : "",
							clsDescriptions : null
						};
						window.openDialog("chrome://semantic-turkey/content/editors/classList/classListEditor.xul",
								"_blank", "chrome=yes,dialog,resizable=yes,modal,centerscreen", parameters);

						if (parameters.clsDescriptions != null) {
							var response = art_semanticturkey.STRequests.Cls.addIntersectionOf(rdfSubject
									.getNominalValue(), parameters.clsDescriptions);
							if (response.isFail()) {
								throw Error(response.getMsg());
							}
							art_semanticturkey.evtMgr.fireEvent("refreshEditor",
									(new art_semanticturkey.genericEventClass()));
						}
					}
				},
				"remove" : {
					"action" : function(rdfSubject, rdfPredicate, rdfObject) {
						if (rdfObject.explicit == "true") {
							var response = art_semanticturkey.STRequests.Cls.removeIntersectionOf(rdfSubject
									.getNominalValue(), rdfObject.getNominalValue());
							if (response.isFail()) {
								throw Error(response.getMsg());
							}
							art_semanticturkey.evtMgr.fireEvent("refreshEditor",
									(new art_semanticturkey.genericEventClass()));
						} else {
							art_semanticturkey.Alert
									.alert("You cannot remove this collection, it's a system resource!");
						}
					}
				}
			},
			"http://www.w3.org/2002/07/owl#unionOf" : {
				"add" : {
					"action" : function(rdfSubject, rdfPredicate) {
						var parameters = {
							list : "",
							clsDescriptions : null
						};
						window.openDialog("chrome://semantic-turkey/content/editors/classList/classListEditor.xul",
								"_blank", "chrome=yes,dialog,resizable=yes,modal,centerscreen", parameters);

						if (parameters.clsDescriptions != null) {
							var response = art_semanticturkey.STRequests.Cls.addUnionOf(rdfSubject
									.getNominalValue(), parameters.clsDescriptions);
							if (response.isFail()) {
								throw Error(response.getMsg());
							}
							art_semanticturkey.evtMgr.fireEvent("refreshEditor",
									(new art_semanticturkey.genericEventClass()));
						}
					}
				},
				"remove" : {
					"action" : function(rdfSubject, rdfPredicate, rdfObject) {
						if (rdfObject.explicit == "true") {
							var response = art_semanticturkey.STRequests.Cls.removeUnionOf(rdfSubject
									.getNominalValue(), rdfObject.getNominalValue());
							if (response.isFail()) {
								throw Error(response.getMsg());
							}
							art_semanticturkey.evtMgr.fireEvent("refreshEditor",
									(new art_semanticturkey.genericEventClass()));
						} else {
							art_semanticturkey.Alert
									.alert("You cannot remove this collection, it's a system resource!");
						}
					}
				}
			}
		}
	}
})());

art_semanticturkey.resourceView.partitions.registerPartitionHandler("superproperties", {
	"partitionLabel" : "Superproperties",
	"expectedContentType" : "objectList",
	"addIcon|fromRole" : "property",
	"objects" : {
		"add" : {
			"label" : "Add a super-property",
			"action" : function(rdfSubject) { // Based on sources by
				// NScarpato
				var parameters = {};
				parameters.selectedProp = "";
				parameters.selectedPropType = "";
				parameters.type = rdfSubject.getRole();
				window.openDialog("chrome://semantic-turkey/content/editors/property/propertyTree.xul",
						"_blank", "modal=yes,resizable,centerscreen", parameters);
				if (parameters.selectedProp != "") {
					art_semanticturkey.STRequests.Property.addSuperProperty(rdfSubject.getNominalValue(),
							parameters.selectedProp);
				}
				art_semanticturkey.evtMgr.fireEvent("refreshEditor",
						(new art_semanticturkey.genericEventClass()));
			}
		},
		"remove" : {
			"action" : function(rdfSubject, rdfObject) { // Based on
				// sources
				// by
				// NScarpato
				if (rdfObject.explicit == "true") {
					art_semanticturkey.STRequests.Property.removeSuperProperty(rdfSubject.getNominalValue(),
							rdfObject.getNominalValue());
					art_semanticturkey.evtMgr.fireEvent("refreshEditor",
							(new art_semanticturkey.genericEventClass()));
				} else {
					art_semanticturkey.Alert.alert("You cannot remove this type, it's a system resource!");
				}

			}
		}
	}
});

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"domains",
				{
					"partitionLabel" : "Domains",
					"expectedContentType" : "objectList",
					"addIcon|fromRole" : "cls",
					"objects" : {
						"add" : {
							"label" : "Add a domain",
							"actions" : [
									{
										"label" : "Add existing class",
										"action" : function(rdfSubject) { // Based on sources by
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
												art_semanticturkey.STRequests.Property.addPropertyDomain(
														rdfSubject.getNominalValue(), domainName);
												art_semanticturkey.evtMgr.fireEvent("refreshEditor",
														(new art_semanticturkey.genericEventClass()));
											}
										}
									},
									{
										"label" : "Create and add class expression",
										"action" : function(rdfSubject) {
											var parameters = {
												expression : ""
											};
											window
													.openDialog(
															"chrome://semantic-turkey/content/editors/classExpression/classExpressionEditor.xul",
															"_blank",
															"chrome=yes,dialog,resizable=yes,modal,centerscreen",
															parameters);

											if (!!parameters.expression) {
												art_semanticturkey.STRequests.Manchester.createRestriction(
														rdfSubject.getNominalValue(),
														"http://www.w3.org/2000/01/rdf-schema#domain",
														parameters.expression);
												art_semanticturkey.evtMgr.fireEvent("refreshEditor",
														(new art_semanticturkey.genericEventClass()));
											}
										}
									} ]
						},
						"remove" : {
							"action" : function(rdfSubject, rdfObject) { // Based on
								// sources
								// by
								// NScarpato
								if (rdfObject.explicit == "true") {
									if (rdfObject.isBNode()) {
										art_semanticturkey.STRequests.Manchester.removeExpression(rdfSubject
												.getNominalValue(),
												"http://www.w3.org/2000/01/rdf-schema#domain", rdfObject
														.toNT());

									} else {
										art_semanticturkey.STRequests.Property.removePropertyDomain(
												rdfSubject.getNominalValue(), rdfObject.getNominalValue());
										art_semanticturkey.evtMgr.fireEvent("refreshEditor",
												(new art_semanticturkey.genericEventClass()));
									}
								} else {
									art_semanticturkey.Alert
											.alert("You cannot remove this type, it's a system resource!");
								}
							}
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
					"objects" : {
						"add" : {
							"label" : "Add a range",
							"actions" : [
									{
										"label" : "Add existing class",
										"enabled" : function(rdfSubject) {
											return rdfSubject.getRole().toLowerCase().indexOf(
													"objectproperty") != -1;
										},
										"action" : function(rdfSubject) { // Based on sources by
											// NScarpato

											var parameters = {};
											parameters.source = "range";
											parameters.rangeName = "";

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
												art_semanticturkey.STRequests.Property.addPropertyRange(
														rdfSubject.getNominalValue(), parameters.rangeName);
												art_semanticturkey.evtMgr.fireEvent("refreshEditor",
														(new art_semanticturkey.genericEventClass()));
											}
										}
									},
									{
										"label" : "Add existing datatype",
										"enabled" : function(rdfSubject) {
											return rdfSubject.getRole().toLowerCase().indexOf(
													"objectproperty") == -1;
										},
										"action" : function(rdfSubject) { // Based on sources by
											// NScarpato

											var parameters = {};
											parameters.source = "range";
											parameters.rangeName = "";

											// parameters.parentWindow =
											// window.arguments[0].parentWindow;
											parameters.parentWindow = window;

											window
													.openDialog(
															"chrome://semantic-turkey/content/editors/property/rangeList.xul",
															"_blank", "modal=yes,resizable,centerscreen",
															parameters);
											if (parameters.rangeName != "") {
												art_semanticturkey.STRequests.Property.addPropertyRange(
														rdfSubject.getNominalValue(), parameters.rangeName);
												art_semanticturkey.evtMgr.fireEvent("refreshEditor",
														(new art_semanticturkey.genericEventClass()));
											}

										}
									},
									{
										"label" : "Create and add class expression",
										"enabled" : function(rdfSubject) {
											return rdfSubject.getRole() == "objectProperty";
										},
										"action" : function(rdfSubject) {
											var parameters = {
												expression : ""
											};
											window
													.openDialog(
															"chrome://semantic-turkey/content/editors/classExpression/classExpressionEditor.xul",
															"dlg",
															"chrome=yes,dialog,resizable=yes,modal,centerscreen",
															parameters);

											if (!!parameters.expression) {
												art_semanticturkey.STRequests.Manchester.createRestriction(
														rdfSubject.getNominalValue(),
														"http://www.w3.org/2000/01/rdf-schema#range",
														parameters.expression);
												art_semanticturkey.evtMgr.fireEvent("refreshEditor",
														(new art_semanticturkey.genericEventClass()));
											}
										}
									} ]
						},
						"remove" : {
							"action" : function(rdfSubject, rdfObject) { // Based on
								// sources
								// by
								// NScarpato
								if (rdfObject.explicit == "true") {
									if (rdfObject.isBNode()) {
										art_semanticturkey.STRequests.Manchester.removeExpression(rdfSubject
												.getNominalValue(),
												"http://www.w3.org/2000/01/rdf-schema#range", rdfObject
														.toNT());
									} else {
										art_semanticturkey.STRequests.Property.removePropertyRange(rdfSubject
												.getNominalValue(), rdfObject.getNominalValue());
										art_semanticturkey.evtMgr.fireEvent("refreshEditor",
												(new art_semanticturkey.genericEventClass()));
									}
								} else {
									art_semanticturkey.Alert
											.alert("You cannot remove this type, it's a system resource!");
								}
							}
						}
					}
				});

art_semanticturkey.resourceView.partitions
		.registerPartitionHandler(
				"facets",
				{
					"partitionLabel" : "Property facets",
					"expectedContentType" : "mixed",
					"renderHelper" : function(subjectResource, responsePartition, containingElement) { // Based
						// on
						// sources
						// by
						// NScarpato
						var doc = containingElement.ownerDocument;
						var checkboxContainer = doc.createElement("hbox");

						function createCheckbox(facetName, associatedOwlClass) {
							var facetElem = responsePartition.getElementsByTagName(facetName)[0];

							var isEnabled = facetElem ? (facetElem.getAttribute("explicit") == "true" && art_semanticturkey.resourceView
									.isEditable(subjectResource))
									: art_semanticturkey.resourceView.isEditable(subjectResource);

							var checkboxElement = doc.createElement("checkbox");
							checkboxElement.setAttribute("label", facetName);
							checkboxElement.setAttribute("disabled", !isEnabled);
							checkboxElement.setAttribute("checked", facetElem ? facetElem
									.getAttribute("value") : "false");
							checkboxElement.setAttribute("st-associatedOwlClass", associatedOwlClass);

							checkboxContainer.appendChild(checkboxElement);

						}

						function facetCheckboxEventHandler(event) {
							var checked = event.target.checked;
							var associatedOwlClass = event.target.getAttribute("st-associatedOwlClass");

							try {
								if (checked) { // Add facet
									art_semanticturkey.STRequests.Property.addExistingPropValue(
											subjectResource.getNominalValue(),
											"http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
											associatedOwlClass, "uri");
								} else { // Remove facet
									art_semanticturkey.STRequests.Property.removePropValue(subjectResource
											.getNominalValue(),
											"http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
											associatedOwlClass, null, "uri");
								}
								art_semanticturkey.evtMgr.fireEvent("refreshEditor",
										(new art_semanticturkey.genericEventClass()));
							} catch (e) {
								art_semanticturkey.Alert.alert(e);
							}

						}

						createCheckbox("symmetric", "http://www.w3.org/2002/07/owl#SymmetricProperty");
						createCheckbox("functional", "http://www.w3.org/2002/07/owl#FunctionalProperty");
						createCheckbox("inverseFunctional",
								"http://www.w3.org/2002/07/owl#InverseFunctionalProperty");
						createCheckbox("transitive", "http://www.w3.org/2002/07/owl#TransitiveProperty");

						containingElement.appendChild(checkboxContainer);

						checkboxContainer.addEventListener("command", facetCheckboxEventHandler);

						return {
							excludedSections : [ "symmetric", "functional", "inverseFunctional", "transitive" ]
						}
					}

				});

art_semanticturkey.resourceView.partitions.registerPartitionHandler("inverseof", {
	"partitionLabel" : "Inverse of",
	"expectedContentType" : "objectList",
	"addIcon|fromRole" : "individual",
	"objects" : {
		"add" : {
			"label" : "Add new property",
			"action" : function(rdfSubject) { // Based on sources by
				// NScarpato
				var sourceElementName = rdfSubject.getNominalValue();

				var responseXML = art_semanticturkey.STRequests.Property.getRange("owl:inverseOf", "false");
				var ranges = responseXML.getElementsByTagName("ranges")[0];
				var type = (ranges.getAttribute("rngType"));

				var parameters = new Object();
				parameters.selectedProp = "";
				parameters.selectedPropType = "";
				parameters.oncancel = false;
				parameters.type = type;
				window.openDialog("chrome://semantic-turkey/content/editors/property/propertyTree.xul",
						"_blank", "modal=yes,resizable,centerscreen", parameters);

				if (parameters.oncancel == false) {
					art_semanticturkey.STRequests.Property.addExistingPropValue(sourceElementName,
							"owl:inverseOf", parameters.selectedProp, type);
					art_semanticturkey.evtMgr.fireEvent("refreshEditor",
							(new art_semanticturkey.genericEventClass()));
				}

			}
		},
		"remove" : {
			"action" : function(rdfSubject, rdfObject) { // Based on
				// sources
				// by
				// NScarpato
				if (rdfObject.explicit == "true") {

					art_semanticturkey.STRequests.Property.removePropValue(rdfSubject.getNominalValue(),
							"owl:inverseOf", rdfObject.getNominalValue(), null,
							rdfObject.isURIResource() ? "uri" : "bnode");

					art_semanticturkey.evtMgr.fireEvent("refreshEditor",
							(new art_semanticturkey.genericEventClass()));
				} else {
					art_semanticturkey.Alert.alert("You cannot remove this type, it's a system resource!");
				}

			}
		}
	}

});

art_semanticturkey.resourceView.partitions.registerPartitionHandler("topconceptof", {
	"partitionLabel" : "Top concept of",
	"expectedContentType" : "objectList",
	"addIcon|fromRole" : "conceptScheme",
	"objects" : {
		"add" : {
			"label" : "Add to concept scheme as top concept",
			"action" : function(rdfSubject) {
				var parameters = {};

				window.openDialog("chrome://semantic-turkey/content/skos/editors/scheme/schemeList.xul",
						"dlg", "chrome=yes,dialog,resizable=yes,modal,centerscreen", parameters);

				if (typeof parameters.out == "undefined") {
					return;
				}

				var language = null;

				if (art_semanticturkey.Preferences.get("extensions.semturkey.skos.humanReadable", false)) {
					language = art_semanticturkey.Preferences.get(
							"extensions.semturkey.annotprops.defaultlang", "en");
				}

				art_semanticturkey.STRequests.SKOS.addTopConcept(parameters.out.selectedScheme, rdfSubject
						.getNominalValue(), language);

				art_semanticturkey.evtMgr.fireEvent("refreshEditor",
						(new art_semanticturkey.genericEventClass()));
			}
		},
		"remove" : {
			"action" : function(rdfSubject, rdfObject) {
				if (rdfObject.explicit == "true") {

					art_semanticturkey.STRequests.SKOS.removeTopConcept(rdfObject.getNominalValue(),
							rdfSubject.getNominalValue());

					art_semanticturkey.evtMgr.fireEvent("refreshEditor",
							(new art_semanticturkey.genericEventClass()));
				} else {
					art_semanticturkey.Alert
							.alert("You cannot remove this concept scheme, it's a system resource!");
				}

			}

		}
	}
});

art_semanticturkey.resourceView.partitions.registerPartitionHandler("schemes", {
	"partitionLabel" : "Schemes",
	"expectedContentType" : "objectList",
	"addIcon|fromRole" : "conceptScheme",
	"objects" : {
		"add" : {
			"label" : "Add to a concept scheme",
			"action" : function(rdfSubject) {
				var parameters = {};

				window.openDialog("chrome://semantic-turkey/content/skos/editors/scheme/schemeList.xul",
						"dlg", "chrome=yes,dialog,resizable=yes,modal,centerscreen", parameters);

				if (typeof parameters.out == "undefined") {
					return;
				}

				var language = null;

				if (art_semanticturkey.Preferences.get("extensions.semturkey.skos.humanReadable", false)) {
					language = art_semanticturkey.Preferences.get(
							"extensions.semturkey.annotprops.defaultlang", "en");
				}

				art_semanticturkey.STRequests.SKOS.addConceptToScheme(rdfSubject.getNominalValue(),
						parameters.out.selectedScheme, language);

				art_semanticturkey.evtMgr.fireEvent("refreshEditor",
						(new art_semanticturkey.genericEventClass()));
			}
		},
		"remove" : {
			"action" : function(rdfSubject, rdfObject) {
				if (rdfObject.explicit == "true") {

					art_semanticturkey.STRequests.SKOS.removeConceptFromScheme(rdfSubject.getNominalValue(),
							rdfObject.getNominalValue());

					art_semanticturkey.evtMgr.fireEvent("refreshEditor",
							(new art_semanticturkey.genericEventClass()));
				} else {
					art_semanticturkey.Alert
							.alert("You cannot remove this concept scheme, it's a system resource!");
				}

			}
		}
	}
});

art_semanticturkey.resourceView.partitions.registerPartitionHandler("broaders", {
	"partitionLabel" : "Broaders",
	"expectedContentType" : "objectList",
	"addTooltiptext" : "Add a broader concept",
	"addIcon|fromRole" : "concept",
	"objects" : {
		"add" : {
			"label" : "Add a broader concept",
			"action" : function(rdfSubject) {
				var parameters = {};
				parameters.conceptScheme = "*"; // TODO which concept
				// scheme?
				// parameters.parentWindow =
				// window.arguments[0].parentWindow;
				parameters.parentWindow = window;
				window.openDialog("chrome://semantic-turkey/content/skos/editors/concept/conceptTree.xul",
						"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);

				if (typeof parameters.out == "undefined"
						|| typeof parameters.out.selectedConcept == "undefined")
					return;

				art_semanticturkey.STRequests.SKOS.addBroaderConcept(rdfSubject.getNominalValue(),
						parameters.out.selectedConcept);
				art_semanticturkey.evtMgr.fireEvent("refreshEditor",
						(new art_semanticturkey.genericEventClass()));
			}
		},
		"remove" : {
			"action" : function(rdfSubject, rdfObject) {
				if (rdfObject.explicit == "true") {

					art_semanticturkey.STRequests.SKOS.removeBroaderConcept(rdfSubject.getNominalValue(),
							rdfObject.getNominalValue());
					art_semanticturkey.evtMgr.fireEvent("refreshEditor",
							(new art_semanticturkey.genericEventClass()));
				} else {
					art_semanticturkey.Alert.alert("You cannot remove this type, it's a system resource!");
				}
			}
		}
	}
});
