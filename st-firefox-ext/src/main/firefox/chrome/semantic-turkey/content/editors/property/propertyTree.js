/*
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * 
 * The Original Code is SemanticTurkey.
 * 
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2007.
 * All Rights Reserved.
 * 
 * SemanticTurkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata (ART) Current
 * information about SemanticTurkey can be obtained at
 * http://semanticturkey.uniroma2.it
 * 
 */
if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_Property.jsm",
		art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

window.onload = function() {
	document.getElementById("cancel").addEventListener("command",
			art_semanticturkey.onCancel, true);
	document.getElementById("accept").addEventListener("command",
			art_semanticturkey.onAccept, true);

	document.getElementById("checkAll").addEventListener("command",
			art_semanticturkey.visualizeTree, true);

	var forResource = window.arguments[0].forResource;

	if (typeof forResource == "undefined") {
		document.getElementById("checkAll").style.display = "none";
		document.getElementById("checkAll").checked = true;
	}

	art_semanticturkey.visualizeTree();
};

art_semanticturkey.visualizeTree = function() {
	var allPropertiesRequired = document.getElementById("checkAll").checked;

	try {
		var responseXML;

		if (allPropertiesRequired) {
			if (window.arguments[0].type.toLowerCase()
					.indexOf("objectproperty") != -1) {
				responseXML = art_semanticturkey.STRequests.Property
						.getObjPropertyTree();
			} else if (window.arguments[0].type.toLowerCase().indexOf(
					"datatypeproperty") != -1) {
				responseXML = art_semanticturkey.STRequests.Property
						.getDatatypePropertiesTree();
			} else if (window.arguments[0].type.toLowerCase().indexOf(
					"annotationproperty") != -1) {
				responseXML = art_semanticturkey.STRequests.Property
						.getAnnotationPropertiesTree();
			} else {
				responseXML = art_semanticturkey.STRequests.Property
						.getPropertyTree();
			}
			art_semanticturkey.getPropertiesTreePanel_RESPONSE(responseXML);
		} else {
			if (window.arguments[0].type.toLowerCase()
					.indexOf("objectproperty") != -1) {
				responseXML = art_semanticturkey.STRequests.Property
						.getObjPropertyTree(window.arguments[0].forResource);
			} else if (window.arguments[0].type.toLowerCase().indexOf(
					"datatypeproperty") != -1) {
				responseXML = art_semanticturkey.STRequests.Property
						.getDatatypePropertiesTree(window.arguments[0].forResource);
			} else if (window.arguments[0].type.toLowerCase().indexOf(
					"annotationproperty") != -1) {
				responseXML = art_semanticturkey.STRequests.Property
						.getAnnotationPropertiesTree(window.arguments[0].forResource);
			} else {
				responseXML = art_semanticturkey.STRequests.Property
						.getPropertyTree(window.arguments[0].forResource);
			}
			art_semanticturkey.getPropertiesTreePanel_RESPONSE(responseXML);
		}
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
};

art_semanticturkey.getPropertiesTreePanel_RESPONSE = function(responseElement) {
	var node = document.getElementById('rootPropertyTreePanelChildren');
	while (node.lastChild != null) {
		node.removeChild(node.lastChild);
	}

	var dataElement = responseElement.getElementsByTagName('data')[0];
	var propertyList = dataElement.childNodes;
	for (var i = 0; i < propertyList.length; i++) {
		if (propertyList[i].nodeType == 1) {
			art_semanticturkey.parsingProperties(propertyList[i], node, true);
		}
	}
};
art_semanticturkey.onAccept = function() {
	var tree = document.getElementById('propertiesTreePanel');
	var range = tree.view.selection.getRangeCount();
	if (range <= 0) {
		alert("Please Select a Property");
		return;
	}
	var currentelement = tree.treeBoxObject.view
			.getItemAtIndex(tree.currentIndex);
	var myTreeSelectedProperty = currentelement.getAttribute("propertyName");
	var treerow = currentelement.getElementsByTagName('treerow')[0];
	var treecell = treerow.getElementsByTagName('treecell')[0];
	var selPropType = treecell.getAttribute("propType");
	window.arguments[0].selectedProp = myTreeSelectedProperty;
	window.arguments[0].selectedPropType = selPropType;
	close();
};

/**
 * @author NScarpato 26/03/2008 onCancel
 * 
 */
art_semanticturkey.onCancel = function() {
	window.arguments[0].oncancel = true;
	window.close();
};