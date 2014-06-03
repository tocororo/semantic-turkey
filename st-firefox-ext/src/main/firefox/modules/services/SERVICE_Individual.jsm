Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Deserializer.jsm");	
Components.utils.import("resource://stmodules/ARTResources.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "SemTurkeyHTTPLegacy", "STRequests" ];

var service = STRequests.Individual;
var serviceName = service.serviceName;

var pageService = STRequests.Page;
var pageServiceName = pageService.serviceName;

var deleteService = STRequests.Delete;
var deleteServiceName = deleteService.serviceName;

/**
 * gets the bookmarks associated to specified resource
 * 
 * @member STRequests.Page
 * @param individualName
 * @return
 */
function getBookmarks(individualName) {
	Logger.debug('[SERVICE_Individual.jsm] getBookmarks');
	var individualName = "instanceName=" + individualName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(pageServiceName, pageService.getBookmarksRequest, individualName, contextAsArray);
};

/**
 * remove specified resource
 * 
 * @member STRequests.Delete
 * @return
 */
function removeInstance(name) {
	var myName = "name=" + name;
	var myType = "type=Instance";
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return Deserializer.createURI(SemTurkeyHTTPLegacy.GET(deleteServiceName, deleteService.removeInstanceRequest, myName, myType, contextAsArray));
};

/**
 * gets the full description of resource <code>instanceQName</code>
 * 
 * @member STRequests.Individual
 * @param instanceQName
 * @param method
 * @return
 */
function getIndividualDescription(instanceQName, method) {
	var instanceQName = "instanceQName=" + instanceQName;
	var method = "method=" + method;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getIndividualDescriptionRequest, instanceQName, method, contextAsArray);
};

/**
 * adds a type to resource identified by first argument of this method
 * 
 * @member STRequests.Individual
 * @param indqname
 * @param typeqname
 * @return
 */
function addType(indqname, typeqname) {
	var indqname = "indqname=" + indqname;
	var typeqname = "typeqname=" + typeqname;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply = SemTurkeyHTTPLegacy.GET(serviceName, service.addTypeRequest, indqname, typeqname, contextAsArray);
	var resArray = new Array();
	resArray["type"] = Deserializer.createURI(reply.getElementsByTagName("Type")[0]);
	resArray["instance"] = Deserializer.createURI(reply.getElementsByTagName("Instance")[0]);
	return resArray;
};

/**
 * remove the rdf:type relation between resource specified as arguments of this method. Note that this does
 * not delete any other info about the two resources
 * 
 * @member STRequests.Individual
 * @param indqname
 * @param typeqname
 * @return
 */
function removeType(indqname, typeqname) {
	var indqname = "indqname=" + indqname;
	var typeqname = "typeqname=" + typeqname;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply = SemTurkeyHTTPLegacy.GET(serviceName, service.removeTypeRequest, indqname, typeqname, contextAsArray);
	var resArray = new Array();
	resArray["type"] = Deserializer.createURI(reply.getElementsByTagName("Type")[0]);
	resArray["instance"] = Deserializer.createURI(reply.getElementsByTagName("Instance")[0]);
	return resArray;
};


/**
 * gets only the <em>named</em> classes which are <em>direct types</em> for resource <code>indqname</code>
 * 
 * @member STRequests.Individual
 * @param indqname
 * @return
 */
function get_directNamedTypes(indqname) {
	var indqname = "indqname=" + indqname;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply = SemTurkeyHTTPLegacy.GET(serviceName, service.get_directNamedTypesRequest, indqname, contextAsArray);
	var resArray = new Array();
	resArray["types"] = Deserializer.createRDFArray(reply.getElementsByTagName("Types")[0]);
	resArray["instance"] = Deserializer.createURI(reply.getElementsByTagName("Instance")[0]);
	return resArray;
	//return Deserializer.createURI(SemTurkeyHTTPLegacy.GET(serviceName, service.get_directNamedTypesRequest, indqname));
};

//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.getIndividualDescription = getIndividualDescription;
service.prototype.addType = addType;
service.prototype.removeType = removeType;
service.prototype.get_directNamedTypes = get_directNamedTypes;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;

// Remove Individual SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
deleteService.prototype.getAPI = function(specifiedContext){
	var newObj = new deleteService();
	newObj.context = specifiedContext;
	return newObj;
}
deleteService.prototype.removeInstance = removeInstance;
deleteService.prototype.context = new Context();  // set the default context
deleteService.constructor = service;
deleteService.__proto__ = service.prototype;

// Page SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
pageService.prototype.getAPI = function(specifiedContext){
	var newObj = new pageService();
	newObj.context = specifiedContext;
	return newObj;
}
pageService.prototype.getBookmarks = getBookmarks;
pageService.prototype.context = new Context();  // set the default context
pageService.constructor = service;
pageService.__proto__ = service.prototype;
