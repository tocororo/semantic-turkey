Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");
Components.utils.import("resource://stmodules/STInfo.jsm");
Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.CustomRanges;
var serviceName = service.serviceName;

function runCoda(crEntryId, map) {
	Logger.debug('[SERVICE_CustomRange.jsm] addTriples');
	var params = [];
	for (var i=0; i<map.length; i++){
		params.push(map[i].key + "=" + map[i].value);
	}
	p_crEntryId = "crEntryId=" + crEntryId;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.runCodaRequest, this.context, p_crEntryId, params);
}


service.prototype.runCoda = runCoda;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;