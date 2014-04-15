Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Deserializer.jsm");	
Components.utils.import("resource://stmodules/ARTResources.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "HttpMgr", "STRequests" ];

var service = STRequests.Property;
var serviceName = service.serviceName;

var deleteService = STRequests.Delete;
var deleteServiceName = deleteService.serviceName;

/**
 * gets the tree of all properties in the ontology
 * 
 * @member STRequests.Property
 * @return
 */
function getPropertyTree(instanceQName, method) {
	Logger.debug('[SERVICE_Property.jsm] getPropertyTree');
	if (typeof instanceQName != "undefined") {
		var instanceQName = "instanceQName=" + instanceQName;
		var method = "method=" + method;
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return HttpMgr.GET(serviceName, service.getPropertiesTreeRequest, instanceQName, method, contextAsArray);
	} else {
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return HttpMgr.GET(serviceName, service.getPropertiesTreeRequest, contextAsArray);
	}
}

/**
 * gets the tree of all object properties in the ontology
 * 
 * @member STRequests.Property
 * @return
 */
function getObjPropertyTree() {
	Logger.debug('[SERVICE_Property.jsm] getObjPropertyTree');
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.getObjPropertiesTreeRequest, contextAsArray);
}

/**
 * gets the tree of all datatype properties in the ontology
 * 
 * @member STRequests.Property
 * @return
 */
function getDatatypePropertiesTree() {
	Logger.debug('[SERVICE_Property.jsm] getDatatypePropertiesTree');
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.getDatatypePropertiesTreeRequest, contextAsArray);
}

/**
 * gets the tree of all annotation properties in the ontology
 * 
 * @member STRequests.Property
 * @return
 */
function getAnnotationPropertiesTree() {
	Logger.debug('[SERVICE_Property.jsm] getAnnotationPropertiesTree');
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.getAnnotationPropertiesTreeRequest, contextAsArray);
}

/**
 * deletes triples describing property identified by <em>name</em>
 * 
 * @member STRequests.Delete
 * @param name
 * @return
 */
function removeProperty(name) {
	var myName = "name=" + name;
	var myType = "type=Property";
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return Deserializer.createURI(HttpMgr.GET(deleteServiceName, deleteService.removePropertyRequest, myName, 
			myType, contextAsArray));
}

/**
 * adds a property to the current ontology
 * 
 * @member STRequests.Property
 * @param propertyQName
 * @param propertyType
 * @return
 */
function addProperty(propertyQName, propertyType) {
	var myPropertyQName = "propertyQName=" + propertyQName;
	var myPropertyType = "propertyType=" + propertyType;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply = HttpMgr.GET(serviceName, service.addPropertyRequest, myPropertyQName, myPropertyType,
			contextAsArray);
	var resArray = new Array();
	resArray["property"] = Deserializer.createURI(reply.getElementsByTagName("Property")[0]);
	return resArray;
}

/**
 * adds a property to the current ontology, and defines the type and superProperty for it
 * 
 * @member STRequests.Property
 * @param propertyQName
 * @param propertyType
 *            must be the name of an existing class
 * @param superPropertyQName
 *            must be the name of an existing property
 * @return
 */
function addSubProperty(propertyQName, propertyType, superPropertyQName) {
	var myPropertyQName = "propertyQName=" + propertyQName;
	var myPropertyType = "propertyType=" + propertyType;
	var mySuperPropertyQName = "superPropertyQName=" + superPropertyQName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply = HttpMgr.GET(serviceName, service.addPropertyRequest, myPropertyQName, myPropertyType,
			mySuperPropertyQName, contextAsArray);
	var resArray = new Array();
	resArray["property"] = Deserializer.createURI(reply.getElementsByTagName("Property")[0]);
	resArray["superProperty"] = Deserializer.createURI(reply.getElementsByTagName("SuperProperty")[0]);
	return resArray;
}

/**
 * gets a tree forest with roots provided by the classes in the range of property identified by
 * <em>propertyQName</em>
 * 
 * @member STRequests.Property
 * @param propertyQName
 * @return
 */
function getRangeClassesTree(propertyQName) {
	propertyQName = "propertyQName=" + propertyQName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.getRangeClassesTreeRequest, propertyQName, contextAsArray);
}

/**
 * retrieves the full description of property identified by <em>propertyQName</em>
 * 
 * @member STRequests.Property
 * @param propertyQName
 * @return
 */
function getPropertyDescription(propertyQName) {
	Logger.debug('[SERVICE_Property.jsm] getPropertyDescription');
	var propertyQName = "propertyQName=" + propertyQName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.getPropertyDescriptionRequest, propertyQName, contextAsArray);
}

/**
 * removes a given <em>value</em> associated to resource <em>instanceQName</em> through property
 * <em>propertyQName</em>
 * 
 * @member STRequests.Property
 * @param instanceQName
 * @param propertyQName
 * @param value
 * @param type
 *            (optional) if the property is a simple rdf:Property, this argument tells whether a literal
 *            should be passed as the value for the property, or a resource. <em>literal</em> should be
 *            passed via this argument to tell ST to add a literal value (only for pure rdf:property(es))
 * @param lang
 *            (optional) for annotation properties, tells the language of the entry which needs to be removed
 *            (it is necessary if the value to be removed had its language set up)
 * @return
 */
function removePropValue(instanceQName, propertyQName, value,rangeQName,type, lang) {
	Logger.debug('[SERVICE_Property.jsm] removePropValue');
	var instanceQName = "instanceQName=" + instanceQName;
	var propertyQName = "propertyQName=" + propertyQName;
	var value = "value=" + value;
	var rangeQName = "rangeQName=" + rangeQName;
	var type = "type=" + type;
	if (typeof lang != "undefined" && lang != "") {
		var lang = "lang=" + lang;
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return HttpMgr.GET(serviceName, service.removePropValueRequest, instanceQName, propertyQName, value,
				rangeQName,type, lang, contextAsArray);
		
	} else {
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return HttpMgr.GET(serviceName, service.removePropValueRequest, instanceQName, propertyQName, value,
				rangeQName,type, contextAsArray);
	}
}

/**
 * adds a given <em>value</em> to the description of a resource <em>instanceQName</em> through property
 * <em>propertyQName</em>
 * 
 * @member STRequests.Property
 * @param instanceQName
 * @param propertyQName
 * @param value
 * @param rangeClsQName
 *            (necessary only for resource values, otherwise set to null) tells the type for the newly created
 *            resource
 * @param type
 *            (optional) if the property is a simple rdf:Property, this argument tells whether a literal
 *            should be passed as the value for the property, or a resource. <em>literal</em> should be
 *            passed via this argument to tell ST to add a literal value (only for pure rdf:property(es))
 * @param lang
 *            (optional) for annotation properties, tells the language of the entry which needs to be set up
 * @return
 */
function createAndAddPropValue(instanceQName, propertyQName, value, rangeQName, type, lang) {
	Logger.debug('[SERVICE_Property.jsm] createAndAddPropValue');
	var instanceQName = "instanceQName=" + instanceQName;
	var propertyQName = "propertyQName=" + propertyQName;
	var value = "value=" + value;
	var rangeQName = "rangeQName=" + rangeQName;
	var type = "type=" + type;
	if (typeof lang != "undefined" && lang != "") {
		var lang = "lang=" + lang;
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return HttpMgr.GET(serviceName, service.createAndAddPropValueRequest, instanceQName, propertyQName,
				value, rangeQName, type, lang, contextAsArray);
	} else {
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return HttpMgr.GET(serviceName, service.createAndAddPropValueRequest, instanceQName, propertyQName,
				value, rangeQName, type, contextAsArray);
	}
}

/**
 * as for <code>createAndAddPropValue</code> but this does just relates the resource identified by
 * <em>createAndAddPropValue</em> to an existing resource
 * 
 * @member STRequests.Property
 * @param instanceQName
 * @param propertyQName
 * @param value
 * @return
 */
function addExistingPropValue(instanceQName, propertyQName, value,type) {
	Logger.debug('[SERVICE_Property.jsm] addExistingPropValueRequest');
	var instanceQName = "instanceQName=" + instanceQName;
	var propertyQName = "propertyQName=" + propertyQName;
	var value = "value=" + value;
	var type = "type=" + type;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.addExistingPropValueRequest, instanceQName, propertyQName, value,
			type, contextAsArray);
}

/**
 * adds a super property relationship between two existing properties
 * 
 * @member STRequests.Property
 * @param propertyQName
 * @param superPropertyQName
 * @return
 */
function addSuperProperty(propertyQName, superPropertyQName) {
	var propertyQName = "propertyQName=" + propertyQName;
	var superPropertyQName = "superPropertyQName=" + superPropertyQName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.addSuperPropertyRequest, propertyQName, superPropertyQName,
			contextAsArray);
}

/**
 * removes the super property relationship between two existing properties (but does not delete the
 * properties)
 * 
 * @member STRequests.Property
 * @param propertyQName
 * @param superPropertyQName
 * @return
 */
function removeSuperProperty(propertyQName, superPropertyQName) {
	var propertyQName = "propertyQName=" + propertyQName;
	var superPropertyQName = "superPropertyQName=" + superPropertyQName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.removeSuperPropertyRequest, propertyQName, superPropertyQName,
			contextAsArray);
}

/**
 * get info about Domain of a property
 * NEVER USED
 * 
 * @member STRequests.Property
 * @param propertyQName
 * @return
 */
function  getDomain(propertyQName){
	var propertyQName = "propertyQName=" + propertyQName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.getDomainRequest, propertyQName, contextAsArray);
}
/**
 * get info about Range of a property
 * 
 * @member STRequests.Property
 * @param propertyQName
 * @return
 */
function getRange(propertyQName,visualize) {
	var propertyQName = "propertyQName=" + propertyQName;
	var visualize = "visualize=" + visualize;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.getRangeRequest, propertyQName,visualize,contextAsArray);
}

/**
 * parses a list of literals representing values in a dataRange
 * 
 * @member STRequests.Property
 * @param dataRange
 * @param nodeType
 * @return
 */
function  parseDataRange(dataRange,nodeType) {
	var dataRange = "dataRange=" + dataRange;
	var nodeType = "nodeType=" + nodeType;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.parseDataRangeRequest, dataRange,nodeType,contextAsArray);
}

/**
 * specifies the domain for property <em>propertyQName</em> (actually, it just adds a class to the list of
 * classes the intersection of which represents the true property domain)
 * 
 * @member STRequests.Property
 * @param propertyQName
 * @param domainPropertyQName
 * @return
 */
function addPropertyDomain(propertyQName, domainPropertyQName) {
	var propertyQName = "propertyQName=" + propertyQName;
	var domainPropertyQName = "domainPropertyQName=" + domainPropertyQName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.addPropertyDomainRequest, propertyQName, domainPropertyQName,
			contextAsArray);
}

/**
 * removes property domain from property <em>propertyQName</em>
 * 
 * @member STRequests.Property
 * @param propertyQName
 * @param domainPropertyQName
 * @return
 */
function removePropertyDomain(propertyQName, domainPropertyQName) {
	var propertyQName = "propertyQName=" + propertyQName;
	var domainPropertyQName = "domainPropertyQName=" + domainPropertyQName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.removePropertyDomainRequest, propertyQName, domainPropertyQName,
			contextAsArray);
}

/**
 * specifies the range for property <em>propertyQName</em> (actually, it just adds a class to the list of
 * classes the intersection of which represents the true property range)
 * 
 * @member STRequests.Property
 * @param propertyQName
 * @param rangePropertyQName
 * @return
 */
function addPropertyRange(propertyQName, rangePropertyQName) {
	var propertyQName = "propertyQName=" + propertyQName;
	var rangePropertyQName = "rangePropertyQName=" + rangePropertyQName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.addPropertyRangeRequest, propertyQName, rangePropertyQName,
			contextAsArray);
}

/**
 * removes property range from property <em>propertyQName</em>
 * 
 * @member STRequests.Property
 * @param propertyQName
 * @param rangePropertyQName
 * @return
 */
function removePropertyRange(propertyQName, rangePropertyQName) {
	var propertyQName = "propertyQName=" + propertyQName;
	var rangePropertyQName = "rangePropertyQName=" + rangePropertyQName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.removePropertyRangeRequest, propertyQName, rangePropertyQName,
			contextAsArray);
}
// Property SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.getPropertyTree = getPropertyTree;
service.prototype.getAnnotationPropertiesTree = getAnnotationPropertiesTree;
service.prototype.getDatatypePropertiesTree = getDatatypePropertiesTree;
service.prototype.getObjPropertyTree = getObjPropertyTree;
service.prototype.getPropertyDescription = getPropertyDescription;
service.prototype.addProperty = addProperty;
service.prototype.addSubProperty = addSubProperty;
service.prototype.getRangeClassesTree = getRangeClassesTree;
service.prototype.removePropValue = removePropValue;
service.prototype.createAndAddPropValue = createAndAddPropValue;
service.prototype.addExistingPropValue = addExistingPropValue;
service.prototype.addSuperProperty = addSuperProperty;
service.prototype.removeSuperProperty = removeSuperProperty;
service.prototype.addPropertyDomain = addPropertyDomain;
service.prototype.removePropertyDomain = removePropertyDomain;
service.prototype.addPropertyRange = addPropertyRange;
service.prototype.removePropertyRange = removePropertyRange;
service.prototype.getDomain = getDomain;
service.prototype.getRange = getRange;
service.prototype.parseDataRange=parseDataRange;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;

//this return an implementation for Project with a specified context
deleteService.prototype.getAPI = function(specifiedContext){
	var newObj = new deleteService();
	newObj.context = specifiedContext;
	return newObj;
}
deleteService.removeProperty = removeProperty;
deleteService.prototype.context = new Context();  // set the default context
deleteService.constructor = deleteService;
deleteService.__proto__ = deleteService.prototype;
