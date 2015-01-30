if (typeof art_semanticturkey == "undefined") {
	var art_semanticturkey = {};
}

Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/stEvtMgr.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Deserializer.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ARTResources.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

art_semanticturkey.init = function() {
	var schemeList = document.getElementById("schemeList");

	schemeList.addEventListener("it.uniroma2.art.semanticturkey.event.widget.tree.dblclick", function(event) {
		var rowIndex = event.detail.rowIndex;
		var col = event.detail.col;

		if (rowIndex == -1 || col == null || col.id != "check") return;
		
		/*
		 * Check if the checbox was previously checked (and thus, it is going to be unchecked). In the
		 * affirmative case, set the skos.selected_scheme property to *, meaning that "projectPropertySet"
		 * listener is fired and it unchecks all schemes. Otherwise, if the checkbox was not checked, set
		 * the skos.selected_scheme property to the name of the concept scheme and again fires the above
		 * listener that unchecks the other schemes.
		 */
		if (schemeList._view.visibleRows2[rowIndex].record.check){
			art_semanticturkey.STRequests.Projects.setProjectProperty(
					art_semanticturkey.CurrentProject.getProjectName(), "skos.selected_scheme", "*");
		} else {
			art_semanticturkey.STRequests.Projects.setProjectProperty(
					art_semanticturkey.CurrentProject.getProjectName(), "skos.selected_scheme", 
					schemeList._view.visibleRows2[rowIndex].id);
		}
	}, false);
	
	var predefRoots = schemeList._view.sourceAdapter.fetchRoots;
	schemeList._view.sourceAdapter.fetchRoots = function() {

		var response = art_semanticturkey.STRequests.Projects.getProjectProperty(
				art_semanticturkey.CurrentProject.getProjectName(), "skos.selected_scheme");
		
		var selSc = art_semanticturkey.Deserializer.createPropertyValue(response);
		//var collectionValues = art_semanticturkey.deserializer.createRDFArray(response);
		var roots = predefRoots();
		for (var i = 0 ; i < roots.length ; i++) {
			if (roots[i].id == selSc) {
				roots[i].record.check = true;
			} else {
				roots[i].record.check = false;
			}
		}

		return roots;
	};
	schemeList._view.reload();
	
	var stEventArray = new art_semanticturkey.eventListenerArrayClass();
	
	stEventArray.addEventListenerToArrayAndRegister("projectPropertySet", function(eventId, projectPropertySetObj) {
		if (projectPropertySetObj.getPropName() == "skos.selected_scheme") {
			var context = projectPropertySetObj.getContext();
			
			// Do not update the list in case of renaming. Let the rename event handler perform the update.
			if (typeof context != "undefined") {
				if (context.getName() == "rename") return;				
			}
			
			for (var i = 0 ; i < schemeList._view.visibleRows2.length ; i++) {
				if (schemeList._view.visibleRows2[i].id == projectPropertySetObj.getPropValue()) {
					schemeList._view.visibleRows2[i].record.check = true;
					schemeList._view.invalidateRow(i);				
				} else if (schemeList._view.visibleRows2[i].record.check) {
					schemeList._view.visibleRows2[i].record.check = false;
					schemeList._view.invalidateRow(i);
				}
			}	
		}
	}, null);
	
	window.addEventListener("unload", function(e){
		stEventArray.deregisterAllListener();		
	}, false);
}

window.addEventListener("load", art_semanticturkey.init, false);