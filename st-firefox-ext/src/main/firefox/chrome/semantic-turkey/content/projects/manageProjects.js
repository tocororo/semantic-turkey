if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/stEvtMgr.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/OntManager.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Preferences.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/PrefUtils.jsm", art_semanticturkey);

window.onload = function() {
	document.getElementById("projects-main-window").setAttribute("title",
			"Projects Management ( " + art_semanticturkey.CurrentProject.getProjectName() + " )");

	document.getElementById("openProject").addEventListener("command", art_semanticturkey.openProject, true);
	document.getElementById("imporProject").addEventListener("command", art_semanticturkey.importProject,
			true);
	document.getElementById("newProject").addEventListener("command", art_semanticturkey.newProject, true);
	//document.getElementById("newProjectFromFile").addEventListener("command",
	//		art_semanticturkey.newProjectFromFile, true);
	document.getElementById("deleteProject").addEventListener("command", art_semanticturkey.deleteProject,
			true);
	document.getElementById("setDeafultProject").addEventListener("command", art_semanticturkey.setDeafultProject,
			true);
	document.getElementById("cloneProject")
			.addEventListener("command", art_semanticturkey.cloneProject, true);
	document.getElementById("projectProperty")
			.addEventListener("command", art_semanticturkey.projectProperty, true);
	document.getElementById("editACLProject")
			.addEventListener("command", art_semanticturkey.editACL, true);
	document.getElementById("aclMatrix")
			.addEventListener("command", art_semanticturkey.openACLMatrix, true);
	document.getElementById("fixProject")
			.addEventListener("command", art_semanticturkey.tryRepairProject, true);
	document.getElementById("close").addEventListener("command", art_semanticturkey.close, true);

	document.getElementById("projectsTree")
			.addEventListener("dblclick", art_semanticturkey.openProject, true);

	document.getElementById("projectsTree")
			.addEventListener("click", art_semanticturkey.clickOnAProject, true);		
	
	document.getElementById("nonClosingMode").addEventListener("command", art_semanticturkey.toggleClosingModality,true);
	
	
	var prefListener = new art_semanticturkey.PrefListener("extensions.semturkey.nonClosingMode", function(branch, name) {
		document.getElementById("nonClosingMode").checked = art_semanticturkey.Preferences.get("extensions.semturkey.nonClosingMode", false);
	});
	prefListener.register(true);
	
	window.addEventListener("unload", function() {prefListener.unregister();}, false);
	
	
	art_semanticturkey.populateProjectsList();
};


art_semanticturkey.clickOnAProject = function(event){
	var tree = document.getElementById("projectsTree");
	var range = tree.view.selection.getRangeCount();
	if (range <= 0) {
		document.getElementById("clipmenu").hidden = true; //hide the context menu form showing
		alert("Please Select a Project");
		return;
	}
	document.getElementById("clipmenu").hidden = false;
	var row = {};
	var col = {};
	var part = {};
	tree.treeBoxObject.getCellAt(event.clientX, event.clientY, row, col, part);
	var selTreeitem = tree.contentView.getItemAtIndex(row.value);
	var projName = selTreeitem.getElementsByTagName("treecell")[2].getAttribute("label");
	var hasIssues = selTreeitem.getElementsByTagName("treecell")[6].getAttribute("hasIssues");
	if(event.button == 2){
		if(hasIssues == "true"){
			document.getElementById("openProject").setAttribute("hidden", "true");
			document.getElementById("deleteProject").setAttribute("hidden", "false");
			document.getElementById("cloneProject").setAttribute("hidden", "true");
			document.getElementById("setDeafultProject").setAttribute("hidden", "true");
			document.getElementById("fixProject").setAttribute("hidden", "false");
			document.getElementById("projectProperty").setAttribute("hidden", "false");
		}
		else{
			document.getElementById("openProject").setAttribute("hidden", "false");
			document.getElementById("deleteProject").setAttribute("hidden", "false");
			document.getElementById("cloneProject").setAttribute("hidden", "false");
			document.getElementById("setDeafultProject").setAttribute("hidden", "false");
			document.getElementById("fixProject").setAttribute("hidden", "true");
			document.getElementById("projectProperty").setAttribute("hidden", "false");
		}
		return;
	}
	if(col.value.id == "defaultProjectSel"){
		
		art_semanticturkey.setDeafultProject(event, hasIssues);
		
		
		
	}
	
}


art_semanticturkey.setDeafultProject = function(event, hasIssues){
	var tree = document.getElementById("projectsTree");
	/*var row = {};
	var col = {};
	var part = {};
	tree.treeBoxObject.getCellAt(event.clientX, event.clientY, row, col, part);
	alert("b row.value = "+row.value+" col.value = "+col.value+" tree.currentIndex = "+tree.currentIndex);*/
	var selTreeitem = tree.contentView.getItemAtIndex(tree.currentIndex);
	if( typeof hasIssues == 'undefined' ){
		hasIssues = selTreeitem.getElementsByTagName("treecell")[6].getAttribute("hasIssues") ;
	}
	//check if the selected problem does not have any issues
	if(hasIssues == "false"){
		var hint = art_semanticturkey.Preferences.get("extensions.semturkey.hints.defaultProject", true);
		if(hint == true){
			var projectName = selTreeitem.getElementsByTagName("treecell")[2].getAttribute("label");
			var parameters = new Object();
			parameters.parentWindow = window;
			parameters.projectName = projectName;
			parameters.defaultProjectSet = false;
			
			window.openDialog("chrome://semantic-turkey/content/projects/defaultProjectHint.xul", "_blank",
					"chrome,dependent,dialog,modal=yes,resizable,centerscreen", 
					parameters);
		}
		if(hint == false || parameters.defaultProjectSet == true){
			//remove the old default project (set to "" the first column of all the other projects)
			var treerowList = document.getElementById("ProjectsTreeChildren").getElementsByTagName("treerow");
			for(var i=0; i<treerowList.length; ++i){
				var temp = treerowList[i].getElementsByTagName("treecell")[0].setAttribute("src", "");
			}
			//set this project as the default one
			selTreeitem.getElementsByTagName("treecell")[0].setAttribute("src", "chrome://global/skin/checkbox/cbox-check.gif");
			art_semanticturkey.Preferences.set("extensions.semturkey.isDefaultSet", true);
			var projName = selTreeitem.getElementsByTagName("treecell")[2].getAttribute("label");
			art_semanticturkey.Preferences.set("extensions.semturkey.defaultProjectName", projName);
			var projOntType = selTreeitem.getElementsByTagName("treecell")[5].getAttribute("label");
			art_semanticturkey.Preferences.set("extensions.semturkey.defaultProjectOntType", projOntType);
		}
	}
};

art_semanticturkey.populateProjectsList = function() {
	try {
		var responseXML = art_semanticturkey.STRequests.Projects.listProjects();
		art_semanticturkey.getListProjects_RESPONSE(responseXML);
	} catch (e) {
		art_semanticturkey.Logger.alertMessage(e.name + ": " + e.message);
		art_semanticturkey.Logger.printException(e.name + ": " + e.message);
		//alert(e.name + ": " + e.message);
	}
};

art_semanticturkey.getListProjects_RESPONSE = function(responseElement) {
	var projects = responseElement.getElementsByTagName("project");
	var node = document.getElementById("ProjectsTreeChildren");

	var isDefaultSet = art_semanticturkey.Preferences.get("extensions.semturkey.isDefaultSet", false);
	var defaultProjectName = art_semanticturkey.Preferences.get("extensions.semturkey.defaultProjectName", "null");
	// empty project tree
	while (node.hasChildNodes()) {
		node.removeChild(node.lastChild);
	}

	for ( var i = 0; i < projects.length; i++) {
		var projectName = projects[i].textContent;
		var projectTS = projects[i].getAttribute("ontmgr");
		var status = projects[i].getAttribute("status");
		var type = projects[i].getAttribute("type");
		var ontoType = projects[i].getAttribute("ontoType");
		var projectOpen = projects[i].getAttribute("open");

		var tr = document.createElement("treerow");
		
		var tcMainProject = document.createElement("treecell");
		
		if(isDefaultSet == true && defaultProjectName == projectName)
			tcMainProject.setAttribute("src", "chrome://global/skin/checkbox/cbox-check.gif");
		else
			tcMainProject.setAttribute("src", "");
		tr.appendChild(tcMainProject);
		
		var tcOpen = document.createElement("treecell");
		tcOpen.setAttribute("src", projectOpen == "true" ? "chrome://semantic-turkey/skin/images/folder-open.gif" : "chrome://semantic-turkey/skin/images/folder.gif");
		tcOpen.setAttribute("open", projectOpen);
		tr.appendChild(tcOpen);
		
		var tcName = document.createElement("treecell");
		tcName.setAttribute("label", projectName);
		tcName.setAttribute("typeProject", type);
		tr.appendChild(tcName);

		var prettyPrintTripleStore = art_semanticturkey.OntManager.getOntManagerPrettyPrint(projectTS);
		var tcTS = document.createElement("treecell");
		tcTS.setAttribute("label", prettyPrintTripleStore);
		tr.appendChild(tcTS);

		var tcTS = document.createElement("treecell");
		tcTS.setAttribute("label", type);
		tr.appendChild(tcTS);

		var ontoTypePrettyPrint = art_semanticturkey.OntManager.getRDFModelPrettyPrint(ontoType);	
		var tcTS = document.createElement("treecell");
		tcTS.setAttribute("label", ontoTypePrettyPrint);
		tr.appendChild(tcTS);

		var tcStatus = document.createElement("treecell");
		if (status != "ok") {
			tcStatus.setAttribute("src", "chrome://global/skin/checkbox/cbox-check.gif");
			tcStatus.setAttribute("hasIssues", "true");
			var message = projects[i].getAttribute("stMsg");
			tcStatus.setAttribute("issue", message); //TODO take the issue form the response
		} else
			tcStatus.setAttribute("hasIssues", "false");
		tr.appendChild(tcStatus);

		var ti = document.createElement("treeitem");
		ti.appendChild(tr);

		node.appendChild(ti);
	}
};

art_semanticturkey.openProject = function() {

	var tree = document.getElementById("projectsTree");
	var range = tree.view.selection.getRangeCount();
	if (range <= 0) {
		alert("Please Select a Project, or use the openMain to open the main project");
		return;
	}
	var currentElement = tree.treeBoxObject.view.getItemAtIndex(tree.currentIndex);
	var treerow = currentElement.getElementsByTagName('treerow')[0];
	var treecellName = treerow.getElementsByTagName('treecell')[2];
	var projectName = treecellName.getAttribute("label");
	var projectType = treecellName.getAttribute("typeProject");
	var ontoType = treerow.getElementsByTagName('treecell')[5].getAttribute("label");
	var issueItem = currentElement.getElementsByTagName("treecell")[6];
	if(issueItem.getAttribute("hasIssues") == "true"){
		var issue = issueItem.getAttribute("issue");
		alert(issue);
		return;
	}
	try {
		art_semanticturkey.DisabledAllButton(true);
		art_semanticturkey.closeProject(); // close the current project
		var responseXML = art_semanticturkey.STRequests.Projects.accessProject(projectName);
		art_semanticturkey.openProject_RESPONSE(responseXML, projectName, projectType,ontoType);
	} catch (e) {
		alert(e.name + ": " + e.message);
		//art_semanticturkey.CurrentProject.setCurrentProjet("no project currently active", true, "");
		art_semanticturkey.properClose();
		art_semanticturkey.DisabledAllButton(false);
	}
};


art_semanticturkey.openProject_RESPONSE = function(responseElement, projectName, type, ontoType) {
	art_semanticturkey.CurrentProject.setCurrentProjet(projectName, false, type, ontoType);
	art_semanticturkey.projectOpened(projectName, ontoType);
	art_semanticturkey.properClose();
};


art_semanticturkey.importProject = function() {
	var parameters = new Object();
	parameters.parentWindow = window;
	parameters.importedProject = false;
	parameters.newProjectName = "";
	parameters.newProjectType = "";
	parameters.newProjectOntoType = "";

	art_semanticturkey.DisabledAllButton(true);

	window.openDialog("chrome://semantic-turkey/content/projects/importProject.xul", "_blank",
			"chrome,dependent,dialog,modal=yes,resizable,centerscreen", 
			parameters);

	art_semanticturkey.DisabledAllButton(false);

	if (parameters.importedProject == true) {
		var newProjectName = parameters.newProjectName;
		var newProjectType = parameters.newProjectType;
		var newProjectOntoType = art_semanticturkey.OntManager.getRDFModelPrettyPrint(parameters.newProjectOntoType);
		art_semanticturkey.CurrentProject.setCurrentProjet(newProjectName, false, newProjectType, newProjectOntoType);
		art_semanticturkey.properClose();
		art_semanticturkey.projectOpened(newProjectName, newProjectOntoType);
	}
};

art_semanticturkey.newProject = function() {
	var parameters = new Object();
	parameters.parentWindow = window;
	parameters.fromFile = false;
	parameters.newProject = false;
	parameters.newProjectName = "";
	parameters.newProjectType = "";
	parameters.newProjectOntoType = "";

	art_semanticturkey.DisabledAllButton(true);

	window.openDialog("chrome://semantic-turkey/content/projects/newProject.xul", "_blank",
			"chrome,dependent,dialog,modal=yes,resizable,centerscreen", 
			parameters);

	art_semanticturkey.DisabledAllButton(false);

	if (parameters.newProject == true) {
		var newProjectName = parameters.newProjectName;
		var newProjectType = parameters.newProjectType;
		var newProjectOntoType = art_semanticturkey.OntManager.getRDFModelPrettyPrint(parameters.newProjectOntoType);
		art_semanticturkey.CurrentProject.setCurrentProjet(newProjectName, false, newProjectType, newProjectOntoType);
		art_semanticturkey.properClose();
		art_semanticturkey.projectOpened(newProjectName, newProjectOntoType);
	}
};

art_semanticturkey.newProjectFromFile = function() {
	var parameters = new Object();
	parameters.parentWindow = window;
	parameters.fromFile = true;
	parameters.newProject = false;
	parameters.newProjectName = "";
	parameters.newProjectType = "";

	art_semanticturkey.DisabledAllButton(true);

	window.openDialog("chrome://semantic-turkey/content/projects/newProject.xul", "_blank",
			"chrome,dependent,dialog,modal=yes,resizable,centerscreen", 
			parameters);

	art_semanticturkey.DisabledAllButton(false);

	if (parameters.newProject == true) {
		var newProjectName = parameters.newProjectName;
		var newProjectType = parameters.newProjectType;
		var newProjectOntoType = art_semanticturkey.OntManager.getRDFModelPrettyPrint(parameters.newProjectOntoType);
		art_semanticturkey.CurrentProject.setCurrentProjet(newProjectName, false, newProjectType, newProjectOntoType);
		art_semanticturkey.properClose();
		art_semanticturkey.projectOpened(newProjectName);
	}
};

art_semanticturkey.deleteProject = function() {
	var tree = document.getElementById("projectsTree");
	var range = tree.view.selection.getRangeCount();
	if (range <= 0) {
		alert("Please Select a Project");
		return;
	}
	var currentElement = tree.treeBoxObject.view.getItemAtIndex(tree.currentIndex);
	var treerow = currentElement.getElementsByTagName('treerow')[0];
	var treecell = treerow.getElementsByTagName('treecell')[2];
	var projectName = treecell.getAttribute("label");

	try {
		if (projectName == art_semanticturkey.CurrentProject.getProjectName()) {
			alert("You cannot delete the project " + projectName + " because it's the current project"
					+ "\nLoad another project and then you can delete this one");
			return;
		}
		var responseXML = art_semanticturkey.STRequests.Projects.deleteProject(projectName);
		art_semanticturkey.deleteProject_RESPONSE(responseXML, projectName);
	} catch (e) {
		alert(e.name + ": " + e.message);
	}
};

art_semanticturkey.deleteProject_RESPONSE = function(responseElement, projectName) {
	art_semanticturkey.populateProjectsList();
};

art_semanticturkey.cloneProject = function() {
	var tree = document.getElementById("projectsTree");
	var range = tree.view.selection.getRangeCount();
	if (range <= 0) {
		alert("Please Select a Project");
		return;
	}
	var currentElement = tree.treeBoxObject.view.getItemAtIndex(tree.currentIndex);
	var treerow = currentElement.getElementsByTagName('treerow')[0];
	var treecell = treerow.getElementsByTagName('treecell')[2];
	var projectName = treecell.getAttribute("label");

	var parameters = new Object();
	parameters.parentWindow = window;
	parameters.projectName = projectName;
	parameters.clonedProject = false;

	window.openDialog("chrome://semantic-turkey/content/projects/cloneProject.xul", "_blank",
			"chrome,dependent,dialog,modal=yes,resizable,centerscreen", 
			parameters);

	if (parameters.clonedProject == true) {
		art_semanticturkey.populateProjectsList();
	}
};


art_semanticturkey.tryRepairProject = function() {
	var tree = document.getElementById("projectsTree");
	var range = tree.view.selection.getRangeCount();
	if (range <= 0) {
		alert("Please Select a Project");
		return;
	}
	var currentElement = tree.treeBoxObject.view.getItemAtIndex(tree.currentIndex);
	var treerow = currentElement.getElementsByTagName('treerow')[0];
	var treecell = treerow.getElementsByTagName('treecell')[2];
	var projectName = treecell.getAttribute("label");

	var issue = treerow.getElementsByTagName('treecell')[6].getAttribute("issue");
	
	var parameters = new Object();
	parameters.parentWindow = window;
	parameters.projectName = projectName;
	parameters.issue = issue;

	window.openDialog("chrome://semantic-turkey/content/projects/repairProject.xul", "_blank",
			"chrome,dependent,dialog,modal=yes,resizable,centerscreen", 
			parameters);
	
	art_semanticturkey.populateProjectsList();
	
}

art_semanticturkey.projectProperty = function() {
	var tree = document.getElementById("projectsTree");
	var range = tree.view.selection.getRangeCount();
	if (range <= 0) {
		alert("Please Select a Project");
		return;
	}
	var currentElement = tree.treeBoxObject.view.getItemAtIndex(tree.currentIndex);
	var treerow = currentElement.getElementsByTagName('treerow')[0];
	var treecell = treerow.getElementsByTagName('treecell')[2];
	var projectName = treecell.getAttribute("label");
	
	if (projectName == art_semanticturkey.CurrentProject.getProjectName()){
		alert("Cannot edit project properties of an open project.")
	} else {
		var parameters = { projectName : projectName}; 
		window.openDialog("chrome://semantic-turkey/content/projects/projectPropertiesEditor/propertiesEditor.xul",
				"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
	}
}

art_semanticturkey.editACL = function() {
	var tree = document.getElementById("projectsTree");
	var range = tree.view.selection.getRangeCount();
	if (range <= 0) {
		alert("Please Select a Project");
		return;
	}
	var currentElement = tree.treeBoxObject.view.getItemAtIndex(tree.currentIndex);
	var treerow = currentElement.getElementsByTagName('treerow')[0];
	var treecell = treerow.getElementsByTagName('treecell')[2];
	var projectName = treecell.getAttribute("label");
	
	var parameters = { projectName : projectName}; 
	window.openDialog("chrome://semantic-turkey/content/projects/aclEditor/aclEditor.xul",
			"_blank", "chrome,dependent,dialog,modal=yes,centerscreen", parameters);
}

art_semanticturkey.openACLMatrix = function() { 
	window.openDialog("chrome://semantic-turkey/content/projects/aclEditor/aclMatrix.xul",
			"_blank", "chrome,dependent,dialog,modal=yes,centerscreen");
}

art_semanticturkey.toggleClosingModality = function() {
	art_semanticturkey.Preferences.set("extensions.semturkey.nonClosingMode", document.getElementById("nonClosingMode").checked);
};

/*
art_semanticturkey.closeAndRestartFirefox = function() {
	var risp = confirm("Restart Firefox?");
	if (risp) {
		art_semanticturkey.properClose();
		window.arguments[0].parentWindow.art_semanticturkey.restartFirefox();
	}
};
*/

art_semanticturkey.close = function() {
	art_semanticturkey.properClose();
};

art_semanticturkey.properClose = function() {
	close();
};

art_semanticturkey.DisabledAllButton = function(disabled) {
	document.getElementById("close").disabled = disabled;
	document.getElementById("newProject-menulist").disabled = disabled;
};
