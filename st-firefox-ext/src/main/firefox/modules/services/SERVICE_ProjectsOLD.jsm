Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/stEvtMgr.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "SemTurkeyHTTPLegacy", "STRequests" ];

var service = STRequests.ProjectsOLD;
var serviceName = service.serviceName;

/**
 * opens an existing project, given its <code>name</code>
 * 
 * @member STRequests.Projects
 * @param name
 * @return
 */
function openProject(name) {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] openProject');
	var name = "name=" + name;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.openProjectRequest, name, contextAsArray);
}

/**
 * opens the main project
 * 
 * @member STRequests.Projects
 * @return
 */
/*function openMainProject() {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] openMainProject');
	return SemTurkeyHTTPLegacy.GET(serviceName, service.openMainProjectRequest);
}*/

function repairProject(name){
	Logger.debug('[SERVICE_ProjectsOLD.jsm] repairProject');
	var name = "name=" + name;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.repairProjectRequest, name, contextAsArray);
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
function newProject(name, ontologyType, baseuri, ontmanager, ontMgrConfiguration, cfgParsArray) {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] newProject');
	var name = "name=" + name;
	var ontologyType = "ontologyType=" + ontologyType;
	var baseuri = "baseuri=" + baseuri;
	var ontmanager = "ontmanager=" + ontmanager;
	var ontMgrConfiguration = "ontMgrConfiguration=" + ontMgrConfiguration;
	var cfgPars = "cfgPars=";
	for ( var i=0; i < cfgParsArray.length; ++i){
		var namePar = cfgParsArray[i].name;
		var valuePar = cfgParsArray[i].value;
		if(i!=0)
			cfgPars +="|_|";
		cfgPars += namePar+":::"+valuePar;
	}
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.newProjectRequest, name, ontologyType,baseuri, ontmanager, ontMgrConfiguration, cfgPars, contextAsArray);
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
function newProjectFromFile(name, ontologyType, baseuri, ontmanager, ontMgrConfiguration, cfgParsArray, file) {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] newProjectFromFile');
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
}

/**
 * closes the current project
 * 
 * @member STRequests.Projects
 * @return
 */
function closeProject() {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] closeProject');
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.closeProjectRequest, contextAsArray);
}

/**
 * deletes a project identified by argument <em>name</em>
 * 
 * @member STRequests.Projects
 * @param name
 * @return
 */
function deleteProject(name) {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] deleteProject');
	var name = "name=" + name;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.deleteProjectRequest, name, contextAsArray);
}

/**
 * exports a project to a given file. The file is compiled according to Semantic Turkey project file format
 * 
 * @member STRequests.Projects
 * @param projfile
 * @return
 */
function exportProject(projfile) {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] exportProject');
	var projfile = "projfile=" + projfile;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.exportProjectRequest, projfile, contextAsArray);
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
function importProject(projfile, name) {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] importProject');
	var projfile = "projfile=" + projfile;
	var name = "name=" + name;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.importProjectRequest, projfile, name, contextAsArray);
}

/**
 * clones an existing project identified by <em>name</em> into a new one called <em>newName</em>
 * 
 * @member STRequests.Projects
 * @param name
 * @param newName
 * @return
 */
function cloneProject(name, newName) {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] cloneProject');
	var name = "name=" + name;
	var newName = "newName=" + newName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.cloneProjectRequest, name, newName, contextAsArray);
}

/**
 * saves the current project into a new one with given name <em>newName</em>. If the save is successful,
 * then the new project automatically becomes the current one and the previous one is being closed
 * 
 * @member STRequests.Projects
 * @param newName
 * @return
 */
function saveProjectAs(newName) {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] saveProjectAs');
	var newName = "newName=" + newName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.saveProjectAsRequest, newName, contextAsArray);
}

/**
 * saves the current project
 * 
 * @member STRequests.Projects
 * @return
 */
function saveProject() {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] saveProject');
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.saveProjectRequest, contextAsArray);
}

/**
 * returns a list of all projects available in Semantic Turkey (the <code>mainProject</code> is excluded
 * from this list, whether it has been initialized or not)
 * 
 * @member STRequests.Projects
 * @return
 */
function listProjects() {
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.listProjectsRequest, contextAsArray);
}

/**
 * gets the name of the current project
 * 
 * @member STRequests.Projects
 * @return
 */
function getCurrentProject() {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] getCurrentProject');
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getCurrentProjectRequest, contextAsArray);
}

function getProjectProperty(propNames, projectName) {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] getProjectProperty');
	var propNames_p = "propNames=" + propNames;
	var name_p = projectName == null ? "" : "name=" + projectName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getProjectPropertyRequest, propNames_p, name_p, contextAsArray);
}

function setProjectProperty(propName, propValue, context) {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] setProjectProperty');
	var propName_p = "name=" + propName;
	var propValue_p = "value=" + propValue;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	
	var reply = SemTurkeyHTTPLegacy.GET(serviceName, service.setProjectPropertyRequest, propName_p, propValue_p, contextAsArray);
	
	if (!reply.isFail()) {
		evtMgr.fireEvent("projectPropertySet", {getPropName : function(){return propName;}, getPropValue : function(){return propValue;}, getContext : function(){return context;}});
	}

	return reply;
}


function isCurrentProjectActive() {
	Logger.debug('[SERVICE_ProjectsOLD.jsm] isCurrentProjectActive');
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
service.prototype.openProject = openProject;
service.prototype.repairProject = repairProject;
//service.openMainProject = openMainProject;
service.prototype.newProject = newProject;
service.prototype.newProjectFromFile = newProjectFromFile;
service.prototype.closeProject = closeProject;
service.prototype.deleteProject = deleteProject;
service.prototype.exportProject = exportProject;
service.prototype.importProject = importProject;
service.prototype.cloneProject = cloneProject;
service.prototype.saveProject = saveProject;
service.prototype.saveProjectAs = saveProjectAs;
service.prototype.listProjects = listProjects;
service.prototype.getCurrentProject = getCurrentProject;
service.prototype.getProjectProperty = getProjectProperty;
service.prototype.setProjectProperty = setProjectProperty;
service.prototype.isCurrentProjectActive = isCurrentProjectActive;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;
