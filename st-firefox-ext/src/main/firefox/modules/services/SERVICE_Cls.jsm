Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Deserializer.jsm");	
Components.utils.import("resource://stmodules/ARTResources.jsm");

Components.utils.import("resource://stmodules/Context.jsm");


EXPORTED_SYMBOLS = [ "SemTurkeyHTTPLegacy", "STRequests" ];

var service = STRequests.Cls;
var serviceName = service.serviceName;

var deleteService = STRequests.Delete;
var deleteServiceName = deleteService.serviceName;

var renameService = STRequests.ModifyName;
var renameServiceName = STRequests.ModifyName.serviceName;

var graphService = STRequests.Graph;
var graphServiceName = STRequests.Graph.serviceName;

/**
 * this method gets all information about a given class useful for setting it as the root of an ontology tree.<br/>
 * argument <code>instNum</code> may be <code>true</code> or <code>false</code>; if true, the number of
 * instances of that class is returned
 * 
 * @member STRequests.Cls
 * @param instNum
 * @param clsqnames
 * @return
 */
function getClassesInfoAsRootsForTree(instNum, clsqnames) {
	Logger.debug('[SERVICE_Cls.jsm] getClassTree');
	var instNum = "instNum=" + instNum; // instNum should be true or false
	var clsesqnames = "clsesqnames=" + clsqnames;
	for ( var i = 2; i < arguments.length; i++)
		clsesqnames += "|_|" + arguments[i];
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return Deserializer.createRDFArray(SemTurkeyHTTPLegacy.GET(serviceName, service.getClassesInfoAsRootsForTreeRequest, clsesqnames, instNum, contextAsArray));
}

/**
 * gets the tree of all classes in the ontology. Note that this method is no more used in Semantic Turkey,
 * since the tree is progressively built through the <code>getSubClasses</code> method, when the user
 * explicitly asks to explore subclasses in the class tree
 * 
 * @member STRequests.Cls
 * @return
 */
function getClassTree(clsName) {
	Logger.debug('[SERVICE_Cls.jsm] getClassTree');
	if (typeof clsName != "undefined") {
		var className = "clsName=" + clsName;
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return SemTurkeyHTTPLegacy.GET(serviceName, service.getClassTreeRequest, className, contextAsArray);
	} else {
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return SemTurkeyHTTPLegacy.GET(serviceName, service.getClassTreeRequest, contextAsArray);
	}
}

/**
 * returns info about subclasses of a given class identified by <code>clsName</code>.<br/>
 * 
 * @member STRequests.Cls
 * @param clsName
 * @param tree
 *            if this argument is <code>true</code>, then info about the presence of subclasses of each
 *            returned subclass is provided
 * @param instNum
 *            if this argument is <code>true</code>, then info about the number of instances of each
 *            returned subclass is provided
 * @return
 */
function getSubClasses(clsName, tree, instNum) {
	Logger.debug('[SERVICE_Cls.jsm] getSubClasses');
	var className = "clsName=" + clsName;
	var tree = "tree=" + tree;
	var instNum = "instNum=" + instNum;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return Deserializer.createRDFArray(SemTurkeyHTTPLegacy.GET(serviceName, service.getSubClassesRequest, className, tree, instNum, contextAsArray));
}

/**
 * gets full description of a class
 * 
 * @member STRequests.Cls
 * @return
 */
function getClassDescription(clsName, method) {
	Logger.debug('[SERVICE_Cls.jsm] getClassTree');
	var className = "clsName=" + clsName;
	var method = "method=" + method;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getClassDescriptionRequest, className, method, contextAsArray);
}

/**
 * gets the list of instances of a given class, with additional info like if these are explicit instances or
 * are computed through inference, etc...
 * 
 * @member STRequests.Cls
 * @return
 */
function getClassAndInstancesInfo(clsName, hasSubClasses) {
	Logger.debug('[SERVICE_Cls.jsm] getClassAndInstancesInfo' + clsName);
	var clsName = "clsName=" + clsName;
	var resArray = new Array();
	
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	if(typeof hasSubClasses != "undefined"){
		var hasSubClasses = "hasSubClasses="+hasSubClasses;
		var reply = SemTurkeyHTTPLegacy.GET(serviceName, service.getClassAndInstancesInfoRequest, clsName,hasSubClasses, contextAsArray)
	}
	var reply = SemTurkeyHTTPLegacy.GET(serviceName, service.getClassAndInstancesInfoRequest, clsName, contextAsArray);
	resArray["class"] = Deserializer.createURI(reply.getElementsByTagName("Class")[0]);
	resArray["instances"] = Deserializer.createRDFArray(reply.getElementsByTagName("Instances")[0]);
	return resArray;
}

/**
 * remove selected class from the current ontology
 * 
 * @member STRequests.Delete
 * @return
 */
function removeClass(name) {
	var myName = "name=" + name;
	var myType = "type=Class";
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return Deserializer.createURI(SemTurkeyHTTPLegacy.GET(deleteServiceName, deleteService.removeClassRequest, myName, myType, contextAsArray));
}

/**
 * rename selected resource
 * 
 * @member STRequests.ModifyName
 * @return
 */
function renameResource(newResourceName, oldResourceName) {
	var myNewName = "newName=" + newResourceName;
	var myOldName = "oldName=" + oldResourceName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(renameServiceName, renameService.renameRequest, myNewName, myOldName, contextAsArray);
}

/**
 * as for <code>addSubClass</code>, but <code>superClassName</code> is set to <code>owl:Thing</code>
 * 
 * @member STRequests.Cls
 * @param newClassName
 * @return
 */
function addClass(newClassName) {
	//var superClassName = "superClassName=http://www.w3.org/2002/07/owl#Thing";
	//var superClassName = "superClassName=owl:Thing";
	//var newClassName = "newClassName=" + newClassName;
	//return SemTurkeyHTTPLegacy.GET(serviceName, service.createClassRequest, superClassName, newClassName);
	return addSubClass(newClassName, "owl:Thing");
}

/**
 * adds a resource to the ontology, classified with type <code>clsName</code>
 * 
 * @member STRequests.Cls
 * @param clsName
 * @param instanceName
 * @return
 */
function addIndividual(clsName, instanceName) {
	var clsName = "clsName=" + clsName;
	var instanceName = "instanceName=" + instanceName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply = SemTurkeyHTTPLegacy.GET(serviceName, service.createInstanceRequest, clsName, instanceName, contextAsArray);
	var resArray = new Array();
	resArray["class"] = Deserializer.createURI(reply.getElementsByTagName("Class")[0]);
	resArray["instance"] = Deserializer.createURI(reply.getElementsByTagName("Instance")[0]);
	return resArray;
}

/**
 * adds a class to the ontology, with superclass <code>superClassName</code>. superClassName must identify
 * an existing class
 * 
 * @member STRequests.Cls
 * @param newClassName
 * @param superClassName
 * @return
 */
function addSubClass(newClassName, superClassName) {
	var superClassName = "superClassName=" + superClassName;
	var newClassName = "newClassName=" + newClassName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply =  SemTurkeyHTTPLegacy.GET(serviceName, service.createClassRequest, superClassName, newClassName, contextAsArray);
	var resArray = new Array();
	resArray["class"] = Deserializer.createURI(reply.getElementsByTagName("Class")[0]);
	resArray["superClass"] = Deserializer.createURI(reply.getElementsByTagName("SuperClass")[0]);
	return resArray;
}

/**
 * returns a graph view of the ontology. Used by the graph functionality of Semantic Turkey, this is not
 * currently active, though this API may occasionally work on current version
 * 
 * @member STRequests.Graph
 * @return
 */
function graph() {
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(graphServiceName, graphService.graphRequest, contextAsArray);
}

/**
 * returns a partial graph view of the ontology centred on <code>className</code>. Used by the graph
 * functionality of Semantic Turkey, this is not currently active, though this API may occasionally work on
 * current version
 * 
 * @member STRequests.Graph
 * @return
 */
function partialGraph(className) {
	var className = "className=" + className;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(graphServiceName, graphService.partialGraphRequest, className, contextAsArray);
}

/**
 * adds type <code>typeqname</code> as the classyfing resource for resource <code>clsqname</code>
 * 
 * @member STRequests.Cls
 * @param clsqname
 * @param typeqname
 * @return
 */
function addType(clsqname, typeqname) {
	var clsqname = "clsqname=" + clsqname;
	var typeqname = "typeqname=" + typeqname;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply = SemTurkeyHTTPLegacy.GET(serviceName, service.addTypeRequest, clsqname, typeqname, contextAsArray);
	var resArray = new Array();
	resArray["type"] = Deserializer.createURI(reply.getElementsByTagName("Type")[0]);
	resArray["instance"] = Deserializer.createURI(reply.getElementsByTagName("Instance")[0]);
	return resArray;
}

/**
 * removes type <code>typeqname</code> from class <code>clsqname</code>
 * 
 * @member STRequests.Cls
 * @param clsqname
 * @param typeqname
 * @return
 */
function removeType(clsqname, typeqname) {
	var clsqname = "clsqname=" + clsqname;
	var typeqname = "typeqname=" + typeqname;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply = SemTurkeyHTTPLegacy.GET(serviceName, service.removeTypeRequest, clsqname, typeqname, contextAsArray);
	var resArray = new Array();
	resArray["type"] = Deserializer.createURI(reply.getElementsByTagName("Type")[0]);
	resArray["instance"] = Deserializer.createURI(reply.getElementsByTagName("Instance")[0]);
	return resArray;
}

/**
 * adds superclass <code>superclsqname</code> to class <code>clsqname</code>. Both class need to be
 * existing classes in the ontology
 * 
 * @member STRequests.Cls
 * @param clsqname
 * @param superclsqname
 * @return
 */
function addSuperCls(clsqname, superclsqname) {
	var clsqname = "clsqname=" + clsqname;
	var superclsqname = "superclsqname=" + superclsqname;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply = SemTurkeyHTTPLegacy.GET(serviceName, service.addSuperClsRequest, clsqname, superclsqname, contextAsArray);
	var resArray = new Array();
	resArray["class"] = Deserializer.createURI(reply.getElementsByTagName("Class")[0]);
	resArray["superClass"] = Deserializer.createURI(reply.getElementsByTagName("SuperClass")[0]);
	return resArray;
}

/**
 * removes the superclass relationship between the two classes passed as arguments to this method. This does
 * not delete any of the two classes
 * 
 * @member STRequests.Cls
 * @param clsqname
 * @param superclsqname
 * @return
 */
function removeSuperCls(clsqname, superclsqname) {
	var clsqname = "clsqname=" + clsqname;
	var superclsqname = "superclsqname=" + superclsqname;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply = SemTurkeyHTTPLegacy.GET(serviceName, service.removeSuperClsRequest, clsqname, superclsqname, contextAsArray);
	var resArray = new Array();
	resArray["class"] = Deserializer.createURI(reply.getElementsByTagName("Class")[0]);
	resArray["superClass"] = Deserializer.createURI(reply.getElementsByTagName("SuperClass")[0]);
	return resArray;
}

/**
 * returns the list of superclasses of class <code>clsName</code>
 * 
 * @member STRequests.Cls
 * @param clsName
 * @return
 */
function getSuperClasses(clsName) {
	var clsName = "clsName=" + clsName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return Deserializer.createRDFArray(SemTurkeyHTTPLegacy.GET(serviceName, service.getSuperClassesRequest, clsName, contextAsArray));
}


// Add a specific context
/*service.prototype.getAPI = function(specificContext){
	var newObj = new STRequests.Cls();
	newObj.context = specificContext;
	return newObj;
}*/

// Class SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.getClassesInfoAsRootsForTree = getClassesInfoAsRootsForTree;
service.prototype.getClassTree = getClassTree;
service.prototype.addType = addType;
service.prototype.removeType = removeType;
service.prototype.addSuperCls = addSuperCls;
service.prototype.removeSuperCls = removeSuperCls;
service.prototype.getSuperClasses = getSuperClasses;
service.prototype.addClass = addClass;
service.prototype.addSubClass = addSubClass;
service.prototype.getClassDescription = getClassDescription;
service.prototype.getSubClasses = getSubClasses;
// Instance SERVICE INITIALIZATION
service.prototype.getClassAndInstancesInfo = getClassAndInstancesInfo;
service.prototype.addIndividual = addIndividual;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;

renameService.prototype.getAPI = function(specifiedContext){
	var newObj = new renameService();
	newObj.context = specifiedContext;
	return newObj;
}
renameService.prototype.renameResource = renameResource;
renameService.prototype.context = new Context();  // set the default context
renameService.constructor = renameService;
renameService.__proto__ = renameService.prototype;

graphService.prototype.getAPI = function(specifiedContext){
	var newObj = new renameService();
	newObj.context = specifiedContext;
	return newObj;
}
graphService.prototype.graph = graph;
graphService.prototype.partialGraph = partialGraph;
graphService.prototype.context = new Context();  // set the default context
graphService.constructor = graphService;
graphService.__proto__ = graphService.prototype;

deleteService.prototype.getAPI = function(specifiedContext){
	var newObj = new renameService();
	newObj.context = specifiedContext;
	return newObj;
}
deleteService.prototype.removeClass = removeClass;
deleteService.prototype.context = new Context();  // set the default context
deleteService.constructor = deleteService;
deleteService.__proto__ = deleteService.prototype;