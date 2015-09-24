Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");
Components.utils.import("resource://stmodules/STInfo.jsm");
Components.utils.import("resource://stmodules/Context.jsm");
Components.utils.import("resource://stmodules/Deserializer.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.Alignment;
var serviceName = service.serviceName;

const currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());

//======= Alignment creation (in Res. view) services ===========

function addAlignment(sourceResource, predicate, targetResource) {
	Logger.debug('[SERVICE_Alignment.jsm] addAlignment');
	p_source = "sourceResource=" + sourceResource;
	p_predicate = "predicate=" + predicate;
	p_target = "targetResource=" + targetResource;
	currentSTHttpMgr.GET(null, serviceName, service.addAlignmentRequest, this.context, p_source, p_predicate, p_target);
}

function getMappingRelations(resource, allMappingProps) {
	Logger.debug("[SERVICE_Alignment.jsm] getMappingRelations");
	var params = [];
	params.push("resource="+resource);
	if (typeof allMappingProps != "undefined")
		params.push("allMappingProps=" + allMappingProps);
	return Deserializer.createRDFArray(currentSTHttpMgr.GET(
			null, serviceName, service.getMappingRelationsRequest, this.context, params));
}

//======= Alignment Validation services ===========

function loadAlignment(file) {
	Logger.debug("[SERVICE_Alignment.jsm] loadAlignment " + this.context);
	var formData = Components.classes["@mozilla.org/files/formdata;1"]
		.createInstance(Components.interfaces.nsIDOMFormData);
	formData.append("inputFile", file);
	return currentSTHttpMgr.POST(null, serviceName, service.loadAlignmentRequest, this.context, formData);
}

function acceptAlignment(entity1, entity2, relation) {
	Logger.debug("[SERVICE_Alignment.jsm] acceptAlignment");
	var params = [];
	params.push("entity1=" + entity1);
	params.push("entity2=" + entity2);
	params.push("relation=" + relation);
	return currentSTHttpMgr.GET(null, serviceName, service.acceptAlignmentRequest, this.context, params);
}

function acceptAllAlignment() {
	Logger.debug("[SERVICE_Alignment.jsm] acceptAllAlignment");
	return currentSTHttpMgr.GET(null, serviceName, service.acceptAllAlignmentRequest, this.context);
}

function acceptAllAbove(threshold) {
	Logger.debug("[SERVICE_Alignment.jsm] acceptAllAbove");
	var p_threshold = "threshold=" + threshold;
	return currentSTHttpMgr.GET(null, serviceName, service.acceptAllAboveRequest, this.context, p_threshold);
}

function rejectAlignment(entity1, entity2, relation) {
	Logger.debug("[SERVICE_Alignment.jsm] rejectAlignment");
	var params = [];
	params.push("entity1=" + entity1);
	params.push("entity2=" + entity2);
	params.push("relation=" + relation);
	return currentSTHttpMgr.GET(null, serviceName, service.rejectAlignmentRequest, this.context, params);
}

function rejectAllAlignment() {
	Logger.debug("[SERVICE_Alignment.jsm] rejectAllAlignment");
	return currentSTHttpMgr.GET(null, serviceName, service.rejectAllAlignmentRequest, this.context);
}

function rejectAllUnder(threshold) {
	Logger.debug("[SERVICE_Alignment.jsm] rejectAllUnder");
	var p_threshold = "threshold=" + threshold;
	return currentSTHttpMgr.GET(null, serviceName, service.rejectAllUnderRequest, this.context, p_threshold);
}

function changeRelation(entity1, entity2, relation) {
	Logger.debug("[SERVICE_Alignment.jsm] changeRelation");
	var params = [];
	params.push("entity1=" + entity1);
	params.push("entity2=" + entity2);
	params.push("relation=" + relation);
	return currentSTHttpMgr.GET(null, serviceName, service.changeRelationRequest, this.context, params);
}

function changeMappingProperty(entity1, entity2, mappingProperty) {
	Logger.debug("[SERVICE_Alignment.jsm] changeMappingProperty");
	var params = [];
	params.push("entity1=" + entity1);
	params.push("entity2=" + entity2);
	params.push("mappingProperty=" + mappingProperty);
	return currentSTHttpMgr.GET(null, serviceName, service.changeMappingPropertyRequest, this.context, params);
}

function applyValidation() {
	Logger.debug('[SERVICE_Alignment.jsm] applyValidation');
	return currentSTHttpMgr.GET(null, serviceName, service.applyValidationRequest, this.context);
}

function listSuggestedProperties(entity, relation) {
	Logger.debug('[SERVICE_Alignment.jsm] listSuggestedProperties');
	var params = [];
	params.push("entity=" + entity);
	params.push("relation=" + relation);
	return currentSTHttpMgr.GET(null, serviceName, service.listSuggestedPropertiesRequest, this.context, params);
}

function exportAlignment() {
	Logger.debug("[SERVICE_Alignment.jsm] exportAlignment");
	//here doesn't use the GET method, because it needs the raw responseText 
	var url = currentSTHttpMgr.getRequestUrl(serviceName, service.exportAlignmentRequest, this.context);
	Logger.debug("GET " + url);
	var httpReq = Components.classes["@mozilla.org/xmlextras/xmlhttprequest;1"].createInstance();
	httpReq.open("GET", url, false);
	httpReq.send(null);
	
	if (httpReq.status != 200) {
		throw new HTTPError(httpReq.status, httpReq.statusText);
	}
	
	return httpReq.responseText;
}

function closeSession(){
	Logger.debug("[SERVICE_Alignment.jsm] closeSession");
	currentSTHttpMgr.GET(null, serviceName, service.closeSessionRequest, this.context);
}


service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}

service.prototype.addAlignment = addAlignment;
service.prototype.getMappingRelations = getMappingRelations;
service.prototype.loadAlignment = loadAlignment;
service.prototype.acceptAlignment = acceptAlignment;
service.prototype.acceptAllAlignment = acceptAllAlignment;
service.prototype.acceptAllAbove = acceptAllAbove;
service.prototype.rejectAlignment = rejectAlignment;
service.prototype.rejectAllAlignment = rejectAllAlignment;
service.prototype.rejectAllUnder = rejectAllUnder;
service.prototype.changeRelation = changeRelation;
service.prototype.changeMappingProperty = changeMappingProperty;
service.prototype.applyValidation = applyValidation;
service.prototype.listSuggestedProperties = listSuggestedProperties;
service.prototype.exportAlignment = exportAlignment;
service.prototype.closeSession = closeSession;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;