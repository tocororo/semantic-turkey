if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Context.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SKOS.jsm", art_semanticturkey);

var openProjects = []; //contains the project already open when the alignment window is invoked
var previousSelectedProject; //used to close project when selection change

window.onload = function() {
	document.getElementById("okBtn").addEventListener("command", art_semanticturkey.accept, false);
	document.getElementById("cancelBtn").addEventListener("command", art_semanticturkey.cancel, false);
	
	document.getElementById("projectsMenulist").addEventListener("select", art_semanticturkey.projectMenuListener, false);
	document.getElementById("schemesMenulist").addEventListener("select", art_semanticturkey.schemeMenuListener, false);
	
	art_semanticturkey.populateProjectsList();
}

art_semanticturkey.populateProjectsList = function() {
	var projMenupopup = document.getElementById("projectsMenupopup");
	//get project list and populate the menu
	var responseXml = art_semanticturkey.STRequests.Projects.listProjects();
	var projectXml = responseXml.getElementsByTagName("project");
	for (var i=0; i<projectXml.length; i++){
		var ontoType = projectXml[i].getAttribute("ontoType");
		var projName = projectXml[i].textContent;
		var currentProject = art_semanticturkey.CurrentProject.getProjectName();
		//populate menulist with projects SKOS or SKOSXL and different from current project
		if ((ontoType == "it.uniroma2.art.owlart.models.SKOSXLModel" || 
				ontoType == "it.uniroma2.art.owlart.models.SKOSModel") && 
				projName != currentProject){
			var menuitem = document.createElement("menuitem");
			menuitem.setAttribute("label", projName);
			projMenupopup.appendChild(menuitem);
			if (projectXml[i].getAttribute("open") == "true")
				openProjects.push(projName);
		}
	}
}

/**
 * Listener to selection change of the project menu. When a project is selected, the scheme menu
 * is populated with the schemes of the given project
 */
art_semanticturkey.projectMenuListener = function() {
	var schemesMenulist = document.getElementById("schemesMenulist");
	var schemesMenupopup = document.getElementById("schemesMenupopup");
	var selectedProject = document.getElementById("projectsMenulist").selectedItem.label;

	schemesMenulist.setAttribute("disabled", "true");
	schemesMenulist.selectedIndex = 0;
	//populate menupopup with available schemes
	if (selectedProject != "---") {
		//reset schemesMenupopup
		while (schemesMenupopup.children.length > 1){
			schemesMenupopup.children[1].remove();
		}
		//get scheme for a project
		art_semanticturkey.STRequests.Projects.accessProject(selectedProject);
		//create a context to do requests with the selected project instead the default one 
		var specifiedContext = new art_semanticturkey.Context();
		specifiedContext.setProject(selectedProject);
		var newServiceInstance = art_semanticturkey.STRequests.SKOS.getAPI(specifiedContext);
		//populate schemeMenupopup 
		var schemeArray = newServiceInstance.getAllSchemesList();
		for (var i=0; i<schemeArray.length; i++){
			var scheme = schemeArray[i].getURI();
			var menuitem = document.createElement("menuitem");
			menuitem.setAttribute("label", scheme);
			schemesMenupopup.appendChild(menuitem);
		}
		schemesMenulist.setAttribute("disabled", "false");
	}
	//disconnect from previous selected project
	if (previousSelectedProject != "---" && previousSelectedProject != null){
		if (openProjects.indexOf(previousSelectedProject) == -1)
			art_semanticturkey.STRequests.Projects.disconnectFromProject(previousSelectedProject);
	}
	previousSelectedProject = selectedProject;
	var conceptTree = document.getElementById("conceptTree");
	if (conceptTree != null) {
		//first time the menu is changed and the listener called, concept tree is still not initialized
		conceptTree._view.powerOff();
	}
}

/**
 * Listener to selection change of the scheme menu. When a scheme is selected, it shows the
 * concept tree of the given project-scheme pair.
 */
art_semanticturkey.schemeMenuListener = function() {
	var selectedProject = document.getElementById("projectsMenulist").selectedItem.label;
	var selectedScheme = document.getElementById("schemesMenulist").selectedItem.label;
	var conceptTree = document.getElementById("conceptTree");
	if (selectedScheme != "---"){
		if (conceptTree == null){
			conceptTree = document.createElementNS("http://semanticturkey.uniroma2.it/xmlns/widget#", "conceptTree");
			conceptTree.setAttribute("id", "conceptTree");
			conceptTree.setAttribute("mutable", "false");
			conceptTree.setAttribute("hideheading", "true");
			conceptTree.setAttribute("hidetoolbar", "true");
			document.getElementById("conceptTreeBox").appendChild(conceptTree);
		}
		conceptTree.projectName = selectedProject;
		conceptTree.conceptScheme = selectedScheme;
	} else {
		conceptTree._view.powerOff();
	}
}

art_semanticturkey.accept = function() {
	var conceptTree = document.getElementById("conceptTree");
	window.arguments[0].selectedResource = conceptTree.selectedConcept;
	window.close();
}

art_semanticturkey.cancel = function() {
	window.close();
}

window.onunload = function (){
	//disconnect from project selected only if it was not already open before
	var selectedProject = document.getElementById("projectsMenulist").selectedItem.label;
	if (selectedProject != "---" && openProjects.indexOf(selectedProject) == -1)
		art_semanticturkey.STRequests.Projects.disconnectFromProject(selectedProject);
}