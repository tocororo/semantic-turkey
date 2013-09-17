EXPORTED_SYMBOLS = [ "annotation" ];

Components.utils.import("resource://gre/modules/Services.jsm");

Components.utils.import("resource://stservices/SERVICE_Projects.jsm");
Components.utils.import("resource://stservices/SERVICE_Resource.jsm");
Components.utils.import("resource://stservices/SERVICE_Cls.jsm");
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm");

Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/AnnotationManager.jsm");
Components.utils.import("resource://stmodules/Preferences.jsm");

Components.utils.import("resource://gre/modules/Dict.jsm");

if (typeof annotation == "undefined") {
	var annotation = {};
}

annotation.commons = {};

/*
 * Common event handlers. They are assumed to be invoked in the context of a family i.e. this instanceof
 * Family). The family is assumed to have the function furtherAnn(event) for the creation of a new annotation
 * of the resource in the event.
 */

annotation.commons.handlers = {};

// Creates an annotation for the given resource and content. This common handler
// depends on the availability of conventional function "furtherAnn".
annotation.commons.handlers.furtherAnn = function(event) {
	this.furtherAnn(event);
};

// Handles the loading of a content into a tab. This common handler depends on
// the availability of conventional function "checkAnnotationsForContent".
annotation.commons.handlers.contentLoaded = function(event) {
	var contentId = event.document.documentURI;

	if (typeof this.checkAnnotationsForContent != "undefined") {
		var act = this.checkAnnotationsForContent(contentId);
		event.document.setUserData("stAnnotationsExist", "" + act, null);
	}
};

// Sets the value for a property. This common handler depends on the
// availability
// of the common function "furtherAnn".
annotation.commons.handlers.valueForProperty = function(event, parentWindow) {
	var parameters = {};
	parameters.event = event;
	parameters.subject = event.resource.getURI();
	parameters.object = event.selection.toString();
	parameters.lexicalization = event.selection.toString();
	parameters.functors = {};
	parameters.parentWindow = parentWindow;

	if (typeof event.skos != "undefined") {
		parameters.skos = Object.create(event.skos);
	}

	parameters.functors.addAnnotation = this.furtherAnn.bind(this);

	// A Browser Window: needed for accessing eventObjects and rangy (because
	// they are not modules)
	var window = Services.wm.getMostRecentWindow("navigator:browser");

	window.openDialog("chrome://semantic-turkey/content/class/annotator/annotator.xul", "_blank",
			"modal=yes,resizable,centerscreen", parameters);
};

// Creates a narrower (sub)concept. This common handler depends on the
// availability
// of the common function "furtherAnn".
annotation.commons.handlers.createNarrowerConcept = function(event) {
	var resource = event.resource;
	var doc = event.document;
	var selection = event.selection;

	var conceptScheme;

	if (typeof event.skos != "undefined" && typeof event.skos.conceptScheme != "undefined") {
		conceptScheme = event.skos.conceptScheme;
	} else {
		conceptScheme = STRequests.Projects.getProjectProperty("skos.selected_scheme", null)
				.getElementsByTagName("property")[0].getAttribute("value");
	}

	var language = Preferences.get("extensions.semturkey.annotprops.defaultlang", "en");

	var conceptResource = STRequests.SKOS.createConcept(selection.toString(), resource.getURI(),
			conceptScheme, selection.toString(), language);

	var event2 = Object.create(event);
	event2.resource = conceptResource;
	this.furtherAnn(event2);
};

// Creates an instance. This common handler depends on the availability
// of the common function "furtherAnn".
annotation.commons.handlers.createInstance = function(event, parentWindow) {
	var response1 = STRequests.Cls.addIndividual(event.resource.getURI(), event.selection.toString());
	var event2 = Object.create(event);
	event2.resource = response1.instance;

	// We assume that additional information is provided by the caller
	var tree = parentWindow.document.getElementById("classesTree");
	if (tree != null) {
		parentWindow.art_semanticturkey.classDragDrop_RESPONSE(response1, tree, true, event.addons.domEvent);
	}

	this.furtherAnn(event2);
};

/*
 * Common conventional functions. They are assumed to be invoked in the context of a family i.e. this
 * instanceof Family).
 * 
 * A concrete family is assumed to provide just the following conventional functions.
 * 
 * checkAnnotationsForContent: to check the existence of any annotation for the given content resource
 * 
 * getAnnotationsForContent: to retrieve the annotations for a content resource
 * 
 * deleteAnnotation: to delete an annotation
 * 
 * annotation2ranges: to produce DOM ranges for the presentation of a Web annotation
 * 
 * furtherAnn: to create a further annotation
 * 
 * Other conventional functions are suitably implemented by the framework.
 */
annotation.commons.conventions = {};

// Initializes the default color map for the highlights. Each resource is assigned a perceptually unique
// color, represented in HSV as per CSS specs. TODO: obtain the color from the server.
annotation.commons.conventions.initializeColorMap = function(annotations /* not used at moment */) {
	var colorsEnabled = Preferences.get("extensions.semturkey.annotation.colorsEnabled", true);
	
	if (colorsEnabled) {
		var colorMap = new Dict();
		
		return {
			getColor : function (resource) {
				if (colorMap.has(resource)) {
					return colorMap.get(resource);
				} else {
					var n = colorMap.count;
					var hue = 0;
					var delta = 180;
					while (n != 0) {
						hue = hue + (n % 2) * delta;
						delta = delta / 2;
						n = n / 2;
					}
					
					var newColor = "hsl(" + hue + ", 20%, 50%)";
					colorMap.set(resource, newColor);
					return newColor;
				}
			},
			isEnabled : function() {
				return colorsEnabled;
			}
		}
	} else {
		return {
			getColor : function (resource) {
				return "hsl(60, 100, 50)";
			}
		}
	}
};

// Decorates a content resource with annotations attached to it. This
// conventional functions depends on the availability of the conventional
// functions "annotation2ranges", "viewAnnotation" and "viewResource"
annotation.commons.conventions.decorateContent = function(document, annotations) {
	// Get the color map
	var colorMap = this.initializeColorMap(annotations);
	
	var xptrService = Components.classes["@mozilla.org/xpointer-service;1"].getService();
	xptrService = xptrService.QueryInterface(Components.interfaces.nsIXPointerService);

	// A Browser Window: needed for accessing eventObjects and rangy (because
	// they are not modules)
	var window = Services.wm.getMostRecentWindow("navigator:browser");

	// Local variable bound to this object for the use within inner functions
	var self = this;

	// Add CSS style, if not already there
	var stStyleElement = document.getElementById("st-style-element");

	if (stStyleElement == null) {
		var stStyleElement = document.createElement("style");
		stStyleElement.setAttribute("id", "st-style-element");
		stStyleElement.type = "text/css";
		stStyleElement.innerHTML = ".st-annotation { background-color: yellow; cursor: pointer; }";
		document.body.appendChild(stStyleElement);

		// This is the first time the document is highlighted

		function addButton(panel, label, callback) {
			var b = window.document.createElement("button");
			b.setAttribute("label", label);
			panel.appendChild(b);
			b.addEventListener("command", function() {
				try {
					callback();
				} catch (e) {
					window.alert(e);
				}
				panel.hidePopup();
			}, false);
		}

		// Set the event handler for the click
		document.addEventListener("click", function(e) {

			if (e.target.classList.contains("st-annotation")) {
				// Ignores non left-button clicks
				if (e.button != 0)
					return;

				e.preventDefault(); // to avoid that a click on a link causes
				// the change of page
				var ann = e.target.stAnnotation;

				if (typeof ann != "undefined" && typeof self.deleteAnnotation != "undefined"
						&& typeof ann.id != "undefined") {
					var panel = window.document.createElement("panel");

					addButton(panel, "Delete Annotation", self.deleteAnnotation.bind(self, ann.id));
					addButton(panel, "View Annotation", self.viewAnnotation.bind(self, ann.id, window));
					addButton(panel, "View Resource", self.viewResource.bind(self, ann.resource, window));
					// addButton(panel, "show resource in the tree", function()
					// {window.alert("" + ann.resource);});

					panel.onpopuphidden = function(e) {
						e.target.parentNode.removeChild(e.target);
					};
					window.document.documentElement.appendChild(panel);
					panel.openPopup(e.target, "after_pointer", 0, 0, true, false);
				}
			}
		}, true);

		// Register an event handler for the unload, in order to deregister ST
		// event handlers
		document.addEventListener("unload", function(event) {
			if (typeof event.target.stEventArray != "undefined") {
				event.target.stEventArray.deregisterAllListener();
			}
		}, false);
	}

	// Deregister previous event listeners, if any.
	if (typeof document.stEventArray != "undefined") {
		document.stEventArray.deregisterAllListener();
		document.stEventArray = undefined;
	}

	// Initialize rangy
	window.rangy.init();

	// Delete previous annotations. By first, we must remove the attribute style, which defines the background
	// color, otherwise rangy will not be able to completely remove the range.
	var oldSpans = document.querySelectorAll(".st-annotation");
	for (var i = 0 ; i < oldSpans.length ; i++) {
		if (oldSpans.item(i).hasAttribute("style")) {
			oldSpans.item(i).removeAttribute("style");
		}
	}

	// Then use rangy to remove old annotations.
	
	var bodyRange = window.rangy.createRange();
	bodyRange.selectNode(document.body);
	var cssApplier = window.rangy.createCssClassApplier("st-annotation", {
		normalize : false
	});
	cssApplier.undoToRange(bodyRange);
	bodyRange.detach();

	window.setTimeout(function() {
		// Highlight annotations
		for (var i = 0; i < annotations.length; i++) {
			var ann = annotations[i];
			// FIXME: dispatch pending events on the event loop. It seems that in case of bookmarking
			// annotations there are pending events that need to be dispatched before the function
			// annotation2ranges can be invoked again.
			var thread = Components.classes["@mozilla.org/thread-manager;1"]
					.getService(Components.interfaces.nsIThreadManager).currentThread;
			thread.processNextEvent(true);
			
			// Get DOM ranges for i-th annotations
			var ranges = this.annotation2ranges(document, ann);

			// FIXME: we need to represent the ranges associated with an annotation as XPointers because
			// ranges might be temporarily (until control returns to the event loop) invalid.
			var xpointers = ranges.map(function(r) {
				return xptrService.createXPointerFromRange(r, document);
			});
						
			// Highlight each range
			for (var k = 0; k < xpointers.length; k++) {
				var range = xptrService.parseXPointerToRange(xpointers[k], document);
				var range2 = window.rangy.createRangyRange(document);
				range2.setStart(range.startContainer, range.startOffset);
				range2.setEnd(range.endContainer, range.endOffset);

				var cssApplier = window.rangy.createCssClassApplier("st-annotation", {
					normalize : false,
					elementProperties : {
						stAnnotation : ann,
					}
				});

				cssApplier.applyToRange(range2);				
	
				// Get the span created by range
				var spans = range2.getNodes([3]).map(function(n){return n.parentElement;}).filter(function(e){return e.classList.contains("st-annotation");});
				
				var colorForResource = colorMap.getColor(ann.resource);
				
				// Apply the background color
				for (var m = 0 ; m < spans.length ; m++) {
					spans[m].style.backgroundColor = colorForResource;
				}
			}
		}
	}.bind(this), 0);

	// Register listeners for the deletion of annotations
	document.stEventArray = new window.art_semanticturkey.eventListenerArrayClass();

	document.stEventArray.addEventListenerToArrayAndRegister("annotationDeleted", function(eventId,
			rangeAnnotationDeletedObj) {
		try {
			var ann = null;

			annotations.filter(function(ann) { // There should be at most one
				// annotation for the deleted ID
				return ann.id == rangeAnnotationDeletedObj.getId();
			}).forEach(function(ann) { // Process those annotations
				var ranges = self.annotation2ranges(document, ann);
				// FIXME: we need to represent the ranges associated with an annotation as XPointers because
				// ranges might be temporarily (until control returns to the event loop) invalid.
				var xpointers = ranges.map(function(r) {
					return xptrService.createXPointerFromRange(r, document);
				});

				xpointers.forEach(function(xptr) {
					var range = xptrService.parseXPointerToRange(xptr, document);
					var rangyRange = window.rangy.createRangyRange();
					rangyRange.setStart(range.startContainer, range.startOffset);
					rangyRange.setEnd(range.endContainer, range.endOffset);

					var spans = rangyRange.getNodes([3]).map(function(n){return n.parentElement;}).filter(function(e){return e.classList.contains("st-annotation");});
					
					for (var i = 0 ; i < spans.length ; i++) {
						spans[i].removeAttribute("style");
					}
					
					var cssApplier = window.rangy.createCssClassApplier("st-annotation", {
						normalize : false
					});

					cssApplier.undoToRange(rangyRange);
					rangyRange.detach();
					range.detach();
				});
			});
		} catch (e) {
			window.alert(e.message + ":" + e.lineNumber);
		}
	}, null);
};

/*
 * In the following viewXXX functions the property isFirstEditor has been set to false, in order to prevent
 * the editor panel from accessing the property sourceElement which is undefined.
 */

annotation.commons.conventions.viewAnnotation = function(annotationId, window) {
	var parameters = {
		sourceElement : null, // elemento contenente i dati della riga
		// corrente
		sourceType : "individual", // tipo di editor: clss, ..., determina le
		// funzioni custom ed il titolo della
		// finestra
		sourceElementName : annotationId, // nome dell'elemento corrente
		// (quello usato per
		// identificazione: attualmente il
		// qname)
		sourceParentElementName : "", // nome dell'elemento genitore
		isFirstEditor : false, // l'editor è stato aperto direttamente dall
		// class/... tree o da un altro editor?
		deleteForbidden : false, // cancellazione vietata
		parentWindow : window, // finestra da cui viene aperto l'editor
	// skos : {selectedScheme : this.conceptScheme}
	};
	window.openDialog("chrome://semantic-turkey/content/editors/editorPanel.xul", "_blank",
			"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
};

annotation.commons.conventions.viewResource = function(resourceId, window) {
	var resourceRole = STRequests.Resource.getRole(resourceId);
	var parameters = {
		sourceElement : null, // elemento contenente i dati della riga
		// corrente
		sourceType : resourceRole, // tipo di editor: clss, ..., determina le
		// funzioni custom ed il titolo della
		// finestra
		sourceElementName : resourceId, // nome dell'elemento corrente (quello
		// usato per identificazione:
		// attualmente il qname)
		sourceParentElementName : "", // nome dell'elemento genitore
		isFirstEditor : false, // l'editor è stato aperto direttamente dall
		// class/... tree o da un altro editor?
		deleteForbidden : false, // cancellazione vietata
		parentWindow : window, // finestra da cui viene aperto l'editor
	// skos : {selectedScheme : this.conceptScheme}
	};
	window.openDialog("chrome://semantic-turkey/content/editors/editorPanel.xul", "_blank",
			"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
};

annotation.commons.registerCommonHandlers = function(family) {
	family.addContentLoadedHandler("Default handler", annotation.commons.handlers.contentLoaded);
	family.addSelectionOverResourceHandler("Create instance",
			"to create a new instance of {target-resource}", annotation.commons.handlers.createInstance,
			"Role.Cls");
	family.addSelectionOverResourceHandler("Create narrower concept",
			"to create a narrower concept of {target-resource}",
			annotation.commons.handlers.createNarrowerConcept, "Role.Concept");
	family.addSelectionOverResourceHandler("Value for property",
			"as a value for a property of {target-resource}", annotation.commons.handlers.valueForProperty);
	family.addSelectionOverResourceHandler("Further annotation",
			"as a further annotation of {target-resource}", annotation.commons.handlers.furtherAnn);

	family.initializeColorMap = annotation.commons.conventions.initializeColorMap;
	family.decorateContent = annotation.commons.conventions.decorateContent;
	family.viewAnnotation = annotation.commons.conventions.viewAnnotation;
	family.viewResource = annotation.commons.conventions.viewResource;

};