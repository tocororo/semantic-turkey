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
	Logger.debug('[SERVICE_CustomRanges.jsm] executeConverter');
	p_converter = "converter=" + converter;
	p_value = "value=" + value;
	return currentSTHttpMgr.GET(null, serviceName, service.executeURIConverterRequest, this.context, p_converter, p_value);
}

function getReifiedResDescription(resource, predicate) {
	Logger.debug('[SERVICE_CustomRanges.jsm] getReifiedResDescription');
	p_resource = "resource=" + resource;
	p_predicate = "predicate=" + predicate;
	return currentSTHttpMgr.GET(null, serviceName, service.getReifiedResDescriptionRequest, this.context, p_resource, p_predicate);
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

service.prototype.executeURIConverter = executeURIConverter;
service.prototype.runCoda = runCoda;
service.prototype.getReifiedResDescription = getReifiedResDescription;
service.prototype.getCustomRangeConfigMap = getCustomRangeConfigMap;
service.prototype.getCustomRange = getCustomRange;
service.prototype.getCustomRangeEntry = getCustomRangeEntry;
service.prototype.getCustomRangeEntries = getCustomRangeEntries;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;