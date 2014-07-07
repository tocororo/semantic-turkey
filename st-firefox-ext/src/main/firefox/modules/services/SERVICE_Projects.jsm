Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/stEvtMgr.jsm");

Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");
Components.utils.import("resource://stmodules/STInfo.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "SemTurkeyHTTPLegacy", "STRequests" ];

var service = STRequests.Projects;
var serviceName = service.serviceName;

/**
 * opens an existing project, given its <code>name</code>
 * 
 * @member STRequests.Projects
 * @param name
 * @return
 */
function accessProject(projectName) {//NEW
	Logger.debug('[SERVICE_Projects.jsm] accessProject');
	var name = "name=" + name;
	
	var consumer = "consumer=" + STInfo.getSystemProjectName();	// DEFAULT
	var projectName = "projectName="+projectName;
	var requestedAccessLevel = "requestedAccessLevel=" + STInfo.getAccessRW(); // DEFAULT
	var requestedLockLevel = "requestedLockLevel=" + STInfo.getLockNO(); // DEFAULT
	
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.accessProjectRequest, this.context,
			consumer, projectName, requestedAccessLevel, requestedLockLevel);
}

/**
 * opens the main project
 * 
 * @member STRequests.Projects
 * @return
 */
/*function openMainProject() {
	Logger.debug('[SERVICE_Projects.jsm] openMainProject');
	return SemTurkeyHTTPLegacy.GET(serviceName, service.openMainProjectRequest);
}*/

function repairProject(projectName){	//NEW
	Logger.debug('[SERVICE_Projects.jsm] repairProject');
	var projectName = "projectName=" + projectName;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.repairProjectRequest, this.context, projectName);
}

/**
 * creates a new empty project
 * 
 * @member STRequests.Projects
 * @param name
 *            the name of the project
 * @param baseuri
 *            the baseuri of the working ontology for the created project
 * @param ontmanager
 *            the id of the ontmanager technology which is being used for this project
 * @param type
 *            tells whether a project is always kept updated with modifications (<em>continuosEditing</em>)
 *            or if it needs to be explicitly saved (<em>saveToStore</em>)
 * @return
 */
function newProject(projectName, modelType, baseURI, ontManagerFactoryID, modelConfigurationClass, 
		modelConfigurationArray) { //NEW
	Logger.debug('[SERVICE_Projects.jsm] newProject');
	
	var consumer = "consumer="+ STInfo.getSystemProjectName(); 
	var projectName = "projectName=" + projectName;
	var modelType = "modelType=" + modelType;
	var baseURI = "baseURI=" + baseURI;
	var ontmanager = "ontManagerFactoryID=" + ontManagerFactoryID;
	var ontMgrConfiguration = "modelConfigurationClass=" + modelConfigurationClass;
	var modelConfiguration = "modelConfiguration=";
	for ( var i=0; i < modelConfigurationArray.length; ++i){
		var namePar = modelConfigurationArray[i].name;
		var valuePar = modelConfigurationArray[i].value;
		if(i!=0)
			modelConfiguration +="\n";
		modelConfiguration += namePar+"="+valuePar;
	}
	//var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.createProjectRequest, this.context, consumer, 
			projectName, modelType, baseURI, ontmanager, ontMgrConfiguration, modelConfiguration);
}

/**
 * creates a new project and immediately fills it with RDF data from a given RDF file
 * 
 * @member STRequests.Projects
 * @param name
 *            the name of the project
 * @param baseuri
 *            the baseuri of the working ontology for the created project
 * @param ontmanager
 *            the id of the ontmanager technology which is being used for this project
 * @param type
 *            tells whether a project is always kept updated with modifications (<em>continuosEditing</em>)
 *            or if it needs to be explicitly saved (<em>saveToStore</em>)
 * @param file
 *            the path to the RDF file the data of which is automatically loaded inside the new project
 * @return
 */
/*function newProjectFromFile(name, ontologyType, baseuri, ontmanager, ontMgrConfiguration, cfgParsArray, file) {
	Logger.debug('[SERVICE_Projects.jsm] newProjectFromFile');
	var name = "name=" + name;
	var ontologyType = "ontologyType=" + ontologyType;
	var baseuri = "baseuri=" + baseuri;
	var ontmanager = "ontmanager=" + ontmanager;
	var ontMgrConfiguration = "ontMgrConfiguration=" + ontMgrConfiguration;
	var file = "file=" + file;
	var cfgPars = "cfgPars=";
	for ( var i=0; i < cfgParsArray.length; ++i){
		var namePar = cfgParsArray[i].name;
		var valuePar = cfgParsArray[i].value;
		if(i!=0)
			cfgPars +="|_|";
		cfgPars += namePar+":::"+valuePar;
	}
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.newProjectFromFileRequest, name, ontologyType, baseuri, ontmanager, ontMgrConfiguration, file, cfgPars, contextAsArray);
}*/

/**
 * closes the current project
 * 
 * @member STRequests.Projects
 * @return
 */
function disconnectFromProject(projectName) {	//NEW
	Logger.debug('[SERVICE_Projects.jsm] disconnectFromProject');
	var consumer = "consumer="+ STInfo.getSystemProjectName(); 
	var projectName = "projectName="+projectName;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.disconnectFromProjectRequest, this.context,
			consumer, projectName);
}

/**
 * deletes a project identified by argument <em>name</em>
 * 
 * @member STRequests.Projects
 * @param name
 * @return
 */
function deleteProject(projectName) { // NEW
	Logger.debug('[SERVICE_Projects_NEW.jsm] deleteProject');
	var consumer = "consumer="+ STInfo.getSystemProjectName(); 
	var projectName = "projectName=" + projectName;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.deleteProjectRequest, this.context,
			consumer, projectName);
}

/**
 * exports a project to a given file. The file is compiled according to Semantic Turkey project file format
 * 
 * @member STRequests.Projects
 * @param projfile
 * @return
 */
function exportProject(projectName, exportPackage) {	//NEW
	Logger.debug('[SERVICE_Projects.jsm] exportProject');
	var projectName = "projectName=" + projectName;
	var exportPackage = "exportPackage="+exportPackage;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.exportProjectRequest, this.context, 
			projectName, exportPackage);
}

/**
 * imports a project compiled according to Semantic Turkey project file format.
 * 
 * @member STRequests.Projects
 * @param projfile
 * @param name
 *            (optional). If not specified, the name is the one declared in the imported project; however, it
 *            this name clashes with that of an existing project, then an exception is being thrown
 * @return
 */
function importProject(importPackage, newProjectName) { // new 
	Logger.debug('[SERVICE_Projects.jsm] importProject');
	var importPackage = "importPackage=" + importPackage;
	var newProjectName = "newProjectName=" + newProjectName;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.importProjectRequest, this.context, 
			importPackage, newProjectName);
}

/**
 * clones an existing project identified by <em>name</em> into a new one called <em>newName</em>
 * 
 * @member STRequests.Projects
 * @param name
 * @param newName
 * @return
 */
function cloneProject(projectName, newName) { // NEW
	Logger.debug('[SERVICE_Projects.jsm] cloneProject');
	var projectName = "projectName=" + projectName;
	var newName = "newName=" + newName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.cloneProjectRequest, this.context, 
			projectName, newName);
}



/**
 * saves the current project
 * 
 * @member STRequests.Projects
 * @return
 */
function saveProject(project) {	//NEW
	Logger.debug('[SERVICE_Projects.jsm] saveProject');
	var project = "project"+project;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.saveProjectRequest, this.context, project);
}

/**
 * returns a list of all projects available in Semantic Turkey (the <code>mainProject</code> is excluded
 * from this list, whether it has been initialized or not)
 * 
 * @member STRequests.Projects
 * @return
 */
function listProjects() {	//NEW
	Logger.debug('[SERVICE_Projects.jsm] listProjects');
	var consumer = "consumer=" + STInfo.getSystemProjectName();	// DEFAULT
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listProjectsRequest, this.context, consumer);
}

/**
 * gets the name of the current project
 * 
 * @member STRequests.Projects
 * @return
 */
function getCurrentProject() {
	Logger.debug('[SERVICE_Projects.jsm] getCurrentProject');
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getCurrentProjectRequest, contextAsArray);
}

function getProjectProperty(projectName, propertyNames) {	//NEW
	Logger.debug('[SERVICE_Projects.jsm] getProjectProperty');
	var projectName = "projectName=" + projectName;
	var propertyNames = "propertyNames="+propertyNames;
	//var propNameList = propNameList == null ? "" : "name=" + projectName;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.getProjectPropertyRequest, this.context,
			projectName, propertyNames);
}

function setProjectProperty(projectName, propName, propValue, context) { //NEW
	Logger.debug('[SERVICE_Projects.jsm] setProjectProperty');
	
	var projectName_orig = projectName;
	var propName_orig = propName;
	var propValue_orig = propValue;
	var context_orig = context;
	
	var projectName = "projectName="+projectName;
	var propName = "propName=" + propName;
	var propValue = "propValue=" + propValue;
	
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	var reply = currentSTHttpMgr.GET(null, serviceName, service.setProjectPropertyRequest, this.context, 
			projectName, propName, propValue);
	
	if (!reply.isFail()) {
		evtMgr.fireEvent("projectPropertySet", {getPropName : function(){return propName_orig;}, 
			getPropValue : function(){return propValue_orig;}, 
			getContext : function(){return context_orig;}});
	}

	return reply;
}


function isCurrentProjectActive() {
	Logger.debug('[SERVICE_Projects.jsm] isCurrentProjectActive');
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.isCurrentProjectActiveRequest, contextAsArray);
}


// this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}

// Projects SERVICE INITIALIZATION
service.prototype.accessProject = accessProject;	//NEW
service.prototype.repairProject = repairProject;	//NEW
//service.openMainProject = openMainProject;
service.prototype.newProject = newProject;	//NEW
//service.prototype.newProjectFromFile = newProjectFromFile;
service.prototype.disconnectFromProject = disconnectFromProject;	//NEW
service.prototype.deleteProject = deleteProject;	//NEW
service.prototype.exportProject = exportProject;
service.prototype.importProject = importProject;
service.prototype.cloneProject = cloneProject;	//NEW
service.prototype.saveProject = saveProject;	//NEW
service.prototype.listProjects = listProjects;	//NEW
service.prototype.getCurrentProject = getCurrentProject;
service.prototype.getProjectProperty = getProjectProperty;
service.prototype.setProjectProperty = setProjectProperty;
service.prototype.isCurrentProjectActive = isCurrentProjectActive;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;
