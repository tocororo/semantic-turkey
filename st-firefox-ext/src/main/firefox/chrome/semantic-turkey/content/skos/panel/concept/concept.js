if (typeof art_semanticturkey == "undefined") {
	var art_semanticturkey = {};
}

Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/stEvtMgr.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ARTResources.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/AnnotationManager.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/SkosScheme.jsm", art_semanticturkey);

art_semanticturkey.init = function() {
	
	art_semanticturkey.associateOntologySearchEventsOnGraphicElements("concept");
	
	var conceptTree = document.getElementById("conceptTree");
	
	document.getElementById("noSchemeBtn").addEventListener("command", art_semanticturkey.noSchemeButtonEvent, false);

	// Delay the registration of the listener, after the execution of the binding constructor
	window.setTimeout(function() {
		conceptTree._addStateChangedListener(art_semanticturkey.conceptTreeStateChanged);
	}, 0);
	
	conceptTree.conceptScheme = art_semanticturkey.SkosScheme.getSelectedScheme(
			art_semanticturkey.CurrentProject.getProjectName());	
	
	conceptTree._view.canDrop = function(index, orientation, dataTransfer) {
		if (index != -1 && orientation == 0) {
			if (dataTransfer.types.contains("application/skos.concept")) {
					var droppedConcept = dataTransfer.getData("application/skos.concept");
					var targetNode = conceptTree._view.getRow(index).id;
					
					if (droppedConcept == targetNode) {
						return false;
					}
			}
			
			return true;
		}
	};
	
	conceptTree.addEventListener("dragstart", function(event){
		if (event.originalTarget.tagName != "xul:treechildren") {
			event.preventDefault();
			return;
		}
		
		event.dataTransfer.setData("application/skos.concept", conceptTree.selectedConcept);
	}, false);
	
	conceptTree._view.drop = function(index, orientation, dataTransfer) {
		if (index == -1 || orientation != 0) {
			return;
		}
		
		if (dataTransfer.types.contains("application/skos.concept")) {
			try {
				var droppedConcept = dataTransfer.getData("application/skos.concept");
				var targetNode = conceptTree._view.getRow(index).id;
				
//				if (droppedConcept == targetNode) {
//					return;
//				}
				
				art_semanticturkey.STRequests.SKOS.addBroaderConcept(droppedConcept, targetNode);
			} catch (e) {
				alert(e.name + ": " + e.message);
			}
			
			return;
		}
		
		var selectedRange = dataTransfer.mozSourceNode.ownerDocument.getSelection().getRangeAt(0);

		var event = {};
		event.name = "selectionOverResource";
		event.resource = new art_semanticturkey.ARTURIResource(
				conceptTree._view.visibleRows2[index].record.label,
				"concept",
				conceptTree._view.visibleRows2[index].record.uri);
		event.selection = selectedRange;
		event.document = dataTransfer.mozSourceNode.ownerDocument;
		event.skos = {};
		event.skos.selectedScheme = conceptTree.conceptScheme;

		art_semanticturkey.annotation.AnnotationManager.handleEvent(window, event);
	};
	
	var stEventArray = new art_semanticturkey.eventListenerArrayClass();
	
	stEventArray.addEventListenerToArrayAndRegister("projectPropertySet", function(eventId, projectPropertySetObj) {
		if (projectPropertySetObj.getPropName() == "skos.selected_scheme") {
			conceptTree.conceptScheme = projectPropertySetObj.getPropValue();
		}
	}, null);

	window.addEventListener("unload", function(e){
		stEventArray.deregisterAllListener();		
	}, false);
}

art_semanticturkey.conceptTreeStateChanged = function(state) {
	var noSchemeBtn = document.getElementById("noSchemeBtn");
	if (state.indexOf("conceptSchemeSelected") == -1) {
		noSchemeBtn.style.visibility = "visible";
	} else {
		noSchemeBtn.style.visibility = "hidden";
	}
};

art_semanticturkey.noSchemeButtonEvent = function(event) {
	var event = new CustomEvent("it.uniroma2.art.skos.intent.select_scheme");
	window.dispatchEvent(event);
};

window.addEventListener("load", art_semanticturkey.init, false);