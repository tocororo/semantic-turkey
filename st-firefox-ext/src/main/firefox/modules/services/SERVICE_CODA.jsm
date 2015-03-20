Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");
Components.utils.import("resource://stmodules/STInfo.jsm");
Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.CODA;
var serviceName = service.serviceName;

function runCoda(subject, predicate, crEntryId, map) {
	Logger.debug('[SERVICE_CODA.jsm] addTriples');
	var params = [];
	for (var i=0; i<map.length; i++){
		params.push(map[i].key + "=" + map[i].value);
	}
	p_subject = "subject=" + subject;
	p_predicate = "predicate=" + predicate;
	p_crEntryId = "crEntryId=" + crEntryId;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.runCodaRequest, this.context, p_subject, p_predicate, p_crEntryId, params);
}

function executeURIConverter(converter, value) {
	Logger.debug('[SERVICE_CODA.jsm] executeConverter');
	p_converter = "converter=" + converter;
	p_value = "value=" + value;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.executeURIConverterRequest, this.context, p_converter, p_value);
}

service.prototype.executeURIConverter = executeURIConverter;
service.prototype.runCoda = runCoda;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;