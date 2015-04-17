Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");
Components.utils.import("resource://stmodules/STInfo.jsm");
Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.CustomRanges;
var serviceName = service.serviceName;

const currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());

function runCoda(subject, predicate, crEntryId, map) {
	Logger.debug('[SERVICE_CustomRanges.jsm] addTriples');
	var params = [];
	for (var i=0; i<map.length; i++){
		params.push(map[i].key + "=" + map[i].value);
	}
	p_subject = "subject=" + subject;
	p_predicate = "predicate=" + predicate;
	p_crEntryId = "crEntryId=" + crEntryId;
	return currentSTHttpMgr.GET(null, serviceName, service.runCodaRequest, this.context, p_subject, p_predicate, p_crEntryId, params);
}

function executeURIConverter(converter, value) {
	Logger.debug('[SERVICE_CustomRanges.jsm] executeURIConverter');
	p_converter = "converter=" + converter;
	if (typeof value != "undefined"){
		p_value = "value=" + value;
		return currentSTHttpMgr.GET(null, serviceName, service.executeURIConverterRequest, this.context, p_converter, p_value);
	} else {
		return currentSTHttpMgr.GET(null, serviceName, service.executeURIConverterRequest, this.context, p_converter);
	}
}

function executeLiteralConverter(converter, value, datatype, lang) {
	Logger.debug('[SERVICE_CustomRanges.jsm] executeLiteralConverter');
	var params = [];
	params.push("converter=" + converter);
	params.push("value=" + value);
	if (typeof datatype != "undefined"){
		params.push("datatype=" + datatype);
	}
	if (typeof lang != "undefined"){
		params.push("lang=" + lang);
	}
	return currentSTHttpMgr.GET(null, serviceName, service.executeURIConverterRequest, this.context, params);
}

function getReifiedResourceDescription(resource, predicate) {
	Logger.debug('[SERVICE_CustomRanges.jsm] getReifiedResDescription');
	p_resource = "resource=" + resource;
	p_predicate = "predicate=" + predicate;
	return currentSTHttpMgr.GET(null, serviceName, service.getReifiedResourceDescriptionRequest, this.context, p_resource, p_predicate);
}

function getCustomRangeConfigMap() {
	Logger.debug('[SERVICE_CustomRanges.jsm] getCustomRangeConfigMap');
	return currentSTHttpMgr.GET(null, serviceName, service.getCustomRangeConfigMapRequest, this.context);
}

function getCustomRange(id) {
	Logger.debug('[SERVICE_CustomRanges.jsm] getCustomRange');
	p_id = "id=" + id;
	return currentSTHttpMgr.GET(null, serviceName, service.getCustomRangeRequest, this.context, p_id);
}

function getCustomRangeEntry(id) {
	Logger.debug('[SERVICE_CustomRanges.jsm] getCustomRangeEntry');
	p_id = "id=" + id;
	return currentSTHttpMgr.GET(null, serviceName, service.getCustomRangeEntryRequest, this.context, p_id);
}

function getCustomRangeEntries(property) {
	Logger.debug('[SERVICE_CustomRanges.jsm] getCustomRangeEntries');
	p_prop = "property=" + property;
	return currentSTHttpMgr.GET(null, serviceName, service.getCustomRangeEntriesRequest, this.context, p_prop);
}

function removeReifiedResource(subject, predicate, resource) {
	Logger.debug('[SERVICE_CustomRanges.jsm] removeReifiedResource');
	params = [];
	params.push("subject=" + subject);
	params.push("predicate=" + predicate);
	params.push("resource=" + resource);
	return currentSTHttpMgr.GET(null, serviceName, service.removeReifiedResourceRequest, this.context, params);
}

function createCustomRangeEntry(type, id, name, description, ref, showProp) {
	Logger.debug('[SERVICE_CustomRanges.jsm] createCustomRangeEntry');
	var formData = Components.classes["@mozilla.org/files/formdata;1"]
		.createInstance(Components.interfaces.nsIDOMFormData);
	formData.append("type", type);
	formData.append("id", id);
	formData.append("name", name);
	formData.append("description", description);
	formData.append("ref", ref);
	if (typeof showProp != "undefined")
		formData.append("showProp", showProp);
	return currentSTHttpMgr.POST(null, serviceName, service.createCustomRangeEntryRequest, this.context, formData);
}

function createCustomRange(id) {
	Logger.debug('[SERVICE_CustomRanges.jsm] createCustomRange');
	var p_id = "id=" + id;
	return currentSTHttpMgr.GET(null, serviceName, service.createCustomRangeRequest, this.context, p_id);
}

function addEntryToCustomRange(customRangeId, customRangeEntryId) {
	Logger.debug('[SERVICE_CustomRanges.jsm] addEntryToCustomRange');
	var params = [];
	params.push("customRangeId=" + customRangeId);
	params.push("customRangeEntryId=" + customRangeEntryId);
	return currentSTHttpMgr.GET(null, serviceName, service.addEntryToCustomRangeRequest, this.context, params);
}

function addCustomRangeToPredicate(customRangeId, predicate, replaceRanges) {
	Logger.debug('[SERVICE_CustomRanges.jsm] addCustomRangeToPredicate');
	var params = [];
	params.push("customRangeId=" + customRangeId);
	params.push("predicate=" + predicate);
	if (typeof replaceRanges != "undefined")
		params.push("replaceRanges=" + replaceRanges);
	return currentSTHttpMgr.GET(null, serviceName, service.addCustomRangeToPredicateRequest, this.context, params);
}

service.prototype.executeURIConverter = executeURIConverter;
service.prototype.runCoda = runCoda;
service.prototype.getReifiedResourceDescription = getReifiedResourceDescription;
service.prototype.getCustomRangeConfigMap = getCustomRangeConfigMap;
service.prototype.getCustomRange = getCustomRange;
service.prototype.getCustomRangeEntry = getCustomRangeEntry;
service.prototype.getCustomRangeEntries = getCustomRangeEntries;
service.prototype.executeLiteralConverter = executeLiteralConverter;
service.prototype.removeReifiedResource = removeReifiedResource;
service.prototype.createCustomRangeEntry = createCustomRangeEntry;
service.prototype.createCustomRange = createCustomRange;
service.prototype.addEntryToCustomRange = addEntryToCustomRange;
service.prototype.addCustomRangeToPredicate = addCustomRangeToPredicate;

service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;