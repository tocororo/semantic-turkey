if (typeof art_semanticturkey == 'undefined') var art_semanticturkey = {};
Components.utils.import("resource://stmodules/ProjectST.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/stEvtMgr.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Preferences.jsm", art_semanticturkey);

Components.utils.import("resource://stmodules/Context.jsm");

/*
 * Helper function to close a project. Invoked without parameters, it will close the currently open project,
 * starting the relevant client-side procedures. However, it will not disconnect the SYSTEM consumer from
 * the server-side project, if the client is in multiClient mode.
 * 
 * It is also possible to specify the project to close, by providing its name an whether it is in continuous
 * editing mode or not. The third argument is still optional, and can be used to override the behavior with respect
 * the disconnection of the server-side project. Setting the parameter to <code>true</code> forces the disconnection
 * from the project, even if the client is in multi-client mode. 
 */
art_semanticturkey.closeProject = function(projectName, isContinuosEditing, forceDisconnect){
	try {
		var isNull = false

		if (typeof forceDisconnect == "undefined") {
			forceDisconnect = false;
		}

		if (arguments.length == 0) {
			projectName = art_semanticturkey.CurrentProject.getProjectName();
			isContinuosEditing = art_semanticturkey.CurrentProject.isContinuosEditing();
			isNull = art_semanticturkey.CurrentProject.isNull();
		}
		 
		if((isNull == false) && (isContinuosEditing == false)){
			
			var parameters = new Object();
			parameters.parentWindow = window;
			parameters.projectName = projectName;
			parameters.save = false;
			
			window.openDialog("chrome://semantic-turkey/content/projects/saveProject.xul", "_blank",
				"chrome,dependent,dialog,modal=yes,resizable,centerscreen", 
				parameters);
			
			var parentWindow = window.arguments[0].parentWindow;
			if(parameters.save)
				parentWindow.art_semanticturkey.save_project();
		}
		if (projectName == art_semanticturkey.CurrentProject.getProjectName() && !art_semanticturkey.CurrentProject.isNull()) {
			art_semanticturkey.projectClosed(projectName);
			art_semanticturkey.CurrentProject.setCurrentProjet("no project currently active", true, "nullProject", "nullModel");
		}
		var responseXML = null;
		
		if (!isNull && (forceDisconnect || !art_semanticturkey.Preferences.get("extensions.semturkey.multiClientMode", false))) {
			responseXML = art_semanticturkey.STRequests.Projects.disconnectFromProject(projectName);
		}
		
		// I won't call the closeProject_RESPONSE anymore, because the current project is already properly handled
		//// It seems that the argument responseXML is never used in the method, so it is safe to pass null, when the project is not close
		//art_semanticturkey.closeProject_RESPONSE(responseXML, projectName);
	}
	catch (e) {
		art_semanticturkey.Logger.debug("Catch in closeProject: "+e.name + ": " + e.message + "\nStack: " + e.stack);
		//alert(e.name + ": " + e.message);
	}
};

art_semanticturkey.closeProject_RESPONSE = function(responseElement, projectName){
	art_semanticturkey.CurrentProject.setCurrentProjet("no project currently active", true, false, "nullProject");
};

// this function is not called any more, it is called just by saveAsProject, which is not used anymore
/*art_semanticturkey.getCurrentProjectFromServer = function(){
	try{
		var responseXML = art_semanticturkey.STRequests.ProjectsOLD.getCurrentProject();
		return art_semanticturkey.getCurrentProject_RESPONSE(responseXML);
	}
	catch(e){
		return null;
	}
};*/

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
	var projectInfo = new art_semanticturkey.projectClosedClass(oldProjectName);
	Context.removeValue("project"); 
	art_semanticturkey.evtMgr.fireEvent("projectClosed", projectInfo);
};


