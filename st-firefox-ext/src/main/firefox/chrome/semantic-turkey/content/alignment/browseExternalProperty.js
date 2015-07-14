if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Context.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);

var openProjects = []; //contains the project already open when the alignment window is invoked
var previousSelectedProject; //used to close project when selection change

window.onload = function() {
	document.getElementById("okBtn").addEventListener("command", art_semanticturkey.accept, false);
	document.getElementById("cancelBtn").addEventListener("command", art_semanticturkey.cancel, false);
	
	document.getElementById("projectsMenulist").addEventListener("select", art_semanticturkey.projectMenuListener, false);
	
	art_semanticturkey.populateProjectsList();
}

art_semanticturkey.populateProjectsList = function() {
	var projMenupopup = document.getElementById("projectsMenupopup");
	//get project list and populate the menu
	var responseXml = art_semanticturkey.STRequests.Projects.listProjects();
	var projectXml = responseXml.getElementsByTagName("project");
	for (var i=0; i<projectXml.length; i++){
		var projName = projectXml[i].textContent;
		var currentProject = art_semanticturkey.CurrentProject.getProjectName();
		//populate menulist with projects different from current project
		if (projName != currentProject){
			var menuitem = document.createElement("menuitem");
			menuitem.setAttribute("label", projName);
			projMenupopup.appendChild(menuitem);
			if (projectXml[i].getAttribute("open") == "true")
				openProjects.push(projName);
		}
	}
}

/**
 * Listener to selection change of the project menu. When a project is selected, it shows
 * the property tree of the given project
 */
art_semanticturkey.projectMenuListener = function() {
	var selectedProject = document.getElementById("projectsMenulist").selectedItem.label;
	var propTree = document.getElementById("propTree");
	if (selectedProject != "---"){
		art_semanticturkey.STRequests.Projects.accessProject(selectedProject);
		if (propTree == null){
			propTree = document.createElementNS("http://semanticturkey.uniroma2.it/xmlns/widget#", "propertyTree");
			propTree.setAttribute("id", "propTree");
			propTree.setAttribute("mutable", "false");
			document.getElementById("propTreeBox").appendChild(propTree);
		}
		propTree.projectName = selectedProject;
	} else {
		propTree._view.powerOff();
	}
}

art_semanticturkey.accept = function() {
	var propTree = document.getElementById("propTree");
	window.arguments[0].selectedResource = propTree.selectedProperty;
	window.close();
}

art_semanticturkey.cancel = function() {
	window.close();
}

window.onunload = function (){
	//disconnect from project selected only if it was not already open before
	var selectedProject = document.getElementById("projectsMenulist").selectedItem.label;
	if (openProjects.indexOf(selectedProject) == -1)
		art_semanticturkey.STRequests.Projects.disconnectFromProject(selectedProject);
}