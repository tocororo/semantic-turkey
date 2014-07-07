if (typeof art_semanticturkey == 'undefined') var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_ProjectsOLD.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/stEvtMgr.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

Components.utils.import("resource://stmodules/Context.jsm");

art_semanticturkey.closeProject = function(){
	try{
		var isContinuosEditing = art_semanticturkey.CurrentProject.isContinuosEditing();
		var isNull = art_semanticturkey.CurrentProject.isNull();
		var currentProjectName = art_semanticturkey.CurrentProject.getProjectName();
		art_semanticturkey.projectClosed(currentProjectName); 
		if((isNull == false) && (isContinuosEditing == false)){
			
			var parameters = new Object();
			parameters.parentWindow = window;
			parameters.projectName = currentProjectName;
			parameters.save = false;
			
			window.openDialog("chrome://semantic-turkey/content/projects/saveProject.xul", "_blank",
				"chrome,dependent,dialog,modal=yes,resizable,centerscreen", 
				parameters);
			
			var parentWindow = window.arguments[0].parentWindow;
			if(parameters.save)
				parentWindow.art_semanticturkey.save_project();
		}
		art_semanticturkey.CurrentProject.setCurrentProjet("no project currently active", true, "nullProject", "nullModel");
		
		var responseXML = art_semanticturkey.STRequests.Projects.disconnectFromProject(currentProjectName);
		
		
		
		art_semanticturkey.closeProject_RESPONSE(responseXML, currentProectName);
	}
	catch (e) {
		art_semanticturkey.Logger.debug("Catch in closeProject: "+e.name + ": " + e.message);
		//alert(e.name + ": " + e.message);
	}
};

art_semanticturkey.closeProject_RESPONSE = function(responseElement, projectName){
	art_semanticturkey.CurrentProject.setCurrentProjet("no project currently active", true, false, "nullProject");
};

art_semanticturkey.getCurrentProjectFromServer = function(){
	try{
		var responseXML = art_semanticturkey.STRequests.ProjectsOLD.getCurrentProject();
		return art_semanticturkey.getCurrentProject_RESPONSE(responseXML);
	}
	catch(e){
		return null;
	}
};

art_semanticturkey.getCurrentProject_RESPONSE = function(responseElement){
	var projectInfo = new Object();
	if(responseElement.getElementsByTagName("exists")[0]){
		projectInfo.isNull = true;
		projectInfo.projectName = "";
	}
	else {
		projectInfo.isNull = false;
		projectInfo.projectName = responseElement.getElementsByTagName("project")[0].textContent;
	}
	return projectInfo;
};

//These two function send the events of project
art_semanticturkey.projectOpened = function(newProjectName, type){
	var projectInfo = new art_semanticturkey.projectOpenedClass(newProjectName, type);
	//Context.addValue("project", newProjectName); 
	Context.setProject(newProjectName); 
	art_semanticturkey.evtMgr.fireEvent("projectOpened", projectInfo);
};

//These two function sends the events of project
art_semanticturkey.projectClosed = function(oldProjectName){
	var projectInfo = art_semanticturkey.projectClosedClass(oldProjectName);
	Context.removeValue("project"); 
	art_semanticturkey.evtMgr.fireEvent("projectClosed", projectInfo);
};


